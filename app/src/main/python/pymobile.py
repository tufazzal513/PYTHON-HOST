# pymobile.py
# Bundled Python helper for PyMobile IDE.
# Provides: running a project file as __main__ with output redirected to a Kotlin
# listener, runtime pip installation, a small REPL, and import checks.
#
# `listener` is a Java/Kotlin object exposed to Python by Chaquopy. It must expose
# a `write(text: str)` method and a `flush()` method.

import os
import sys
import runpy
import traceback


class _Redirect:
    def __init__(self, listener):
        self._listener = listener

    def write(self, s):
        try:
            self._listener.write(s)
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


def run_path_with_redirect(listener, path, argv, env, site_dirs):
    """Run `path` as __main__ with stdout/stderr redirected to `listener`.

    Returns an int exit code (0 = success).
    """
    for k, v in (env or {}).items():
        os.environ[str(k)] = str(v)
    for d in (site_dirs or []):
        if d and d not in sys.path:
            sys.path.insert(0, d)

    old_stdout = sys.stdout
    old_stderr = sys.stderr
    redirect = _Redirect(listener)
    sys.stdout = redirect
    sys.stderr = redirect
    try:
        sys.argv = [path] + list(argv or [])
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


def _pip_entry():
    """Locate a pip entry point across pip versions."""
    try:
        from pip import main as fn
        return fn
    except Exception:
        pass
    try:
        from pip._internal import main as fn
        return fn
    except Exception:
        pass
    try:
        from pip._internal.cli.main import main as fn
        return fn
    except Exception:
        pass
    return None


def pip_install(listener, target_dir, packages, index_url, extra_index_url):
    """Install packages into `target_dir` using the embedded pip.

    Returns pip's exit code (0 = success). Returns 2 if pip is unavailable.
    """
    try:
        import pip  # noqa: F401
    except ImportError:
        listener.write("ERROR: pip is not available in this Python runtime.\n")
        return 2

    args = ["install", "--target", target_dir, "--no-input"]
    if index_url:
        args += ["--index-url", index_url]
    if extra_index_url:
        args += ["--extra-index-url", extra_index_url]
    args += list(packages)

    fn = _pip_entry()
    if fn is None:
        listener.write("ERROR: could not locate a pip entry point in this runtime.\n")
        return 2

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


def repl_eval(listener, code, site_dirs):
    """Execute Python `code` (a statement/expression) and redirect output."""
    for d in (site_dirs or []):
        if d and d not in sys.path:
            sys.path.insert(0, d)
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    redirect = _Redirect(listener)
    sys.stdout = redirect
    sys.stderr = redirect
    try:
        exec(compile(code, "<pymobile-console>", "exec"), {})
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
