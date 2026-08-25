# pymobile.py
# Bundled Python helper for PyMobile IDE.
# Provides: running a project file as __main__ with output redirected to a Kotlin
# listener, runtime pip installation (Chaquopy Android wheel repo by default),
# a persistent REPL, and import checks.
#
# `listener` is a Java/Kotlin object exposed to Python by Chaquopy. It must expose
# a `write(text: str)` method and a `flush()` method.

import os
import sys
import runpy
import traceback

# Package indexes, mirroring Chaquopy's own build-time configuration:
# PyPI for pure-Python packages, plus Chaquopy's repository for Android wheels.
DEFAULT_INDEX = "https://pypi.org/simple"
DEFAULT_EXTRA_INDEX = "https://chaquo.com/pypi-13.1"


def _to_dict(m):
    """Convert a Kotlin/Java Map proxy (or any mapping) into a real Python dict.

    Chaquopy passes Java collections into Python as Java proxies, so `.items()`
    does not exist on them. Iterating entrySet() always works.
    """
    if m is None:
        return {}
    try:
        return dict(m)
    except Exception:
        pass
    out = {}
    try:
        it = m.entrySet().iterator()
        while it.hasNext():
            e = it.next()
            out[str(e.getKey())] = str(e.getValue())
    except Exception:
        pass
    return out


def _to_list(xs):
    """Convert a Kotlin/Java List proxy into a real Python list.

    Java List proxies are not always iterable from Python, so fall back to a
    Java-style iterator walk (list(xs) silently returns [] in some cases
    otherwise, losing e.g. pip requirement arguments).
    """
    if xs is None:
        return []
    try:
        out = list(xs)
        if out or not hasattr(xs, "iterator"):
            return out
    except Exception:
        pass
    try:
        out = []
        it = xs.iterator()
        while it.hasNext():
            out.append(str(it.next()))
        return out
    except Exception:
        return []


class _Redirect:
    def __init__(self, listener):
        self._listener = listener

    def write(self, s):
        try:
            self._listener.write(str(s))
        except Exception:
            pass

    def flush(self):
        try:
            self._listener.flush()
        except Exception:
            pass

    def isatty(self):
        return False

    def fileno(self):
        return -1

    def read(self, *args):
        return ""

    def readline(self):
        return ""


def _apply_env(env):
    for k, v in _to_dict(env).items():
        os.environ[k] = v


def _apply_site_dirs(site_dirs):
    for d in _to_list(site_dirs):
        if d and d not in sys.path:
            sys.path.insert(0, d)


def run_path_with_redirect(listener, path, argv, env, site_dirs, workdir=""):
    """Run `path` as __main__ with stdout/stderr redirected to `listener`.

    Changes the current working directory to `workdir` (project root or the
    configured working directory) so relative paths inside user scripts work.

    Returns an int exit code (0 = success).
    """
    _apply_env(env)
    _apply_site_dirs(site_dirs)

    old_cwd = None
    try:
        if workdir and os.path.isdir(workdir):
            old_cwd = os.getcwd()
            os.chdir(workdir)
    except Exception:
        old_cwd = None

    old_stdout = sys.stdout
    old_stderr = sys.stderr
    redirect = _Redirect(listener)
    sys.stdout = redirect
    sys.stderr = redirect
    try:
        sys.argv = [path] + [str(a) for a in _to_list(argv)]
        runpy.run_path(path, run_name="__main__")
        return 0
    except SystemExit as e:
        code = e.code
        if code is None:
            return 0
        return code if isinstance(code, int) else 1
    except BaseException:
        try:
            sys.stderr.write(traceback.format_exc())
        except Exception:
            pass
        return 1
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr
        if old_cwd is not None:
            try:
                os.chdir(old_cwd)
            except Exception:
                pass


def _pip_entry():
    """Locate a pip entry point across pip versions (modern first)."""
    try:
        from pip._internal.cli.main import main as fn
        return fn
    except Exception:
        pass
    try:
        from pip._internal import main as fn
        return fn
    except Exception:
        pass
    try:
        from pip import main as fn
        return fn
    except Exception:
        pass
    return None


def pip_install(listener, target_dir, packages, index_url, extra_index_url):
    """Install packages into `target_dir` using the embedded pip.

    If no index is given, the Chaquopy Android wheel repository is used, because
    plain PyPI mostly serves wheels that are incompatible with Android.

    Returns pip's exit code (0 = success). Returns 2 if pip is unavailable.
    """
    try:
        import pip  # noqa: F401
    except ImportError:
        listener.write("ERROR: pip is not available in this Python runtime.\n")
        return 2

    os.environ.setdefault("PIP_DISABLE_PIP_VERSION_CHECK", "1")

    args = [
        "install",
        "--target", target_dir,
        "--no-input",
        "--no-cache-dir",
        "--index-url", index_url or DEFAULT_INDEX,
    ]
    # null extra index -> Chaquopy Android wheels; empty string -> none.
    extra = extra_index_url if extra_index_url is not None else DEFAULT_EXTRA_INDEX
    if extra:
        args += ["--extra-index-url", extra]
    args += [str(p) for p in _to_list(packages)]

    fn = _pip_entry()
    if fn is None:
        listener.write("ERROR: could not locate a pip entry point in this runtime.\n")
        return 2

    listener.write("$ pip %s\n" % " ".join(args[1:]))
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    redirect = _Redirect(listener)
    sys.stdout = redirect
    sys.stderr = redirect
    try:
        rc = fn(args)
        return rc if isinstance(rc, int) else 0
    except SystemExit as e:
        return e.code if isinstance(e.code, int) else 1
    except BaseException:
        try:
            sys.stderr.write(traceback.format_exc())
        except Exception:
            pass
        return 1
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr


# ---------------------------------------------------------------------------
# Persistent REPL: variables, imports and functions survive across submits,
# like a real Python console.
# ---------------------------------------------------------------------------
_REPL_GLOBALS = {
    "__name__": "__console__",
    "__builtins__": __builtins__,
}


def repl_eval(listener, code, site_dirs):
    """Execute Python `code` with persistent state; echo expression results."""
    _apply_site_dirs(site_dirs)
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    redirect = _Redirect(listener)
    sys.stdout = redirect
    sys.stderr = redirect
    try:
        src = str(code)
        # Echo expression results like an interactive interpreter.
        try:
            import ast
            tree = ast.parse(src, mode="exec")
            last = tree.body[-1] if tree.body else None
            if isinstance(last, ast.Expr):
                tree.body = tree.body[:-1]
                if tree.body:
                    exec(compile(tree, "<console>", "exec"), _REPL_GLOBALS)
                result = eval(
                    compile(ast.Expression(last.value), "<console>", "eval"),
                    _REPL_GLOBALS,
                )
                if result is not None:
                    sys.stdout.write("%r\n" % (result,))
                return 0
        except SyntaxError:
            pass  # fall through to plain exec (prints its own traceback)
        except SystemExit as e:
            return e.code if isinstance(e.code, int) else 1

        exec(compile(src, "<console>", "exec"), _REPL_GLOBALS)
        return 0
    except SystemExit as e:
        return e.code if isinstance(e.code, int) else 1
    except BaseException:
        try:
            sys.stderr.write(traceback.format_exc())
        except Exception:
            pass
        return 1
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr


def is_importable(name):
    import importlib.util
    try:
        return importlib.util.find_spec(name) is not None
    except Exception:
        return False
