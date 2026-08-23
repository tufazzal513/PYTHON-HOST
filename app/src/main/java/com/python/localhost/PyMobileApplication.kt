package com.python.localhost

import android.app.Application
import com.python.localhost.di.AppContainer

class PyMobileApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
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
