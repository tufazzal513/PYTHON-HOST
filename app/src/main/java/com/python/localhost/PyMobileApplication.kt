package com.python.localhost

import android.app.Application
import com.python.localhost.di.AppContainer
import java.io.File
import java.util.Date

class PyMobileApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // Persist every uncaught crash to crash.log so it can be viewed and shared
        // from the Diagnostics screen (A-to-Z error visibility).
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val f = File(filesDir, "crash.log")
                f.appendText("---- ${Date()} thread=${t.name} ----\n${e.stackTraceToString()}\n\n")
                if (f.length() > 300_000) {
                    val lines = f.readLines()
                    f.writeText(lines.takeLast(lines.size / 2).joinToString("\n"))
                }
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(t, e)
        }

        container = AppContainer(this)
        try {
            // Must happen on the main thread; Chaquopy initialises the CPython interpreter.
            container.pythonRuntime.start()
        } catch (e: Throwable) {
            // Surfaces later as errors when the user tries to run a project.
            e.printStackTrace()
        }
    }
}
