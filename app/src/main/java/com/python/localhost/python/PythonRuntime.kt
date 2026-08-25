package com.python.localhost.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

/**
 * Wraps the embedded Chaquopy CPython interpreter. All Python execution in the app
 * goes through this class. The interpreter is started once, on the main thread, from
 * PyMobileApplication.onCreate.
 */
class PythonRuntime(private val context: Context) {
    private var started = false

    /** Must be called from Application.onCreate on the main thread. */
    fun start() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context.applicationContext))
        }
        started = true
    }

    fun isStarted(): Boolean = started && Python.isStarted()

    private fun py(): Python {
        if (!Python.isStarted()) {
            throw IllegalStateException(
                "Python runtime is not available. The embedded interpreter failed to initialise."
            )
        }
        return Python.getInstance()
    }

    /** e.g. "3.11.9" -> returns "3.11.9". */
    fun getVersion(): String =
        py().getModule("sys").get("version").toString().substringBefore(" ")

    fun runScript(
        scriptPath: String,
        argv: List<String>,
        env: Map<String, String>,
        siteDirs: List<String>,
        listener: PyOutputListener,
        workdir: String = "",
    ): Int {
        val module = py().getModule("pymobile")
        return module.callAttr(
            "run_path_with_redirect", listener, scriptPath, argv, env, siteDirs, workdir
        ).toInt()
    }

    fun pipInstall(
        targetDir: String,
        packages: List<String>,
        indexUrl: String?,
        extraIndexUrl: String?,
        listener: PyOutputListener,
    ): Int {
        val module = py().getModule("pymobile")
        return module.callAttr(
            "pip_install", listener, targetDir, packages, indexUrl, extraIndexUrl
        ).toInt()
    }

    fun replEval(code: String, siteDirs: List<String>, listener: PyOutputListener): Int {
        val module = py().getModule("pymobile")
        return module.callAttr("repl_eval", listener, code, siteDirs).toInt()
    }

    fun isImportable(name: String): Boolean = try {
        py().getModule("pymobile").callAttr("is_importable", name).toBoolean()
    } catch (e: Exception) {
        false
    }

    fun pipAvailable(): Boolean = try {
        py().getModule("pip")
        true
    } catch (e: Exception) {
        false
    }
}
