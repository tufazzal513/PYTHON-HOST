package com.python.localhost.python

/**
 * Receives output from the embedded Python interpreter. Chaquopy proxies this Java
 * object into Python, so the bundled `pymobile.py` can call `listener.write(text)`.
 *
 * Instances of this class are invoked on the Python execution thread, so consumers
 * should marshal updates onto the appropriate dispatcher themselves.
 */
class PyOutputListener(
    private val onText: (String) -> Unit,
) {
    fun write(text: String) {
        onText(text)
    }

    fun flush() {
        // Output is delivered line-by-line / chunk-by-chunk; nothing to flush here.
    }
}
