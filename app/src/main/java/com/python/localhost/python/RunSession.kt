package com.python.localhost.python

import com.python.localhost.util.SecretMasker
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

enum class RunState { STARTING, RUNNING, STOPPED, FAILED, RESTARTING, COMPLETED }

data class RunStateInfo(
    val projectId: String,
    val projectName: String,
    val state: RunState,
    val startedAt: Long,
    val endedAt: Long?,
    val exitCode: Int?,
    val foreground: Boolean,
)

/**
 * Represents one execution of a project's entry point. Because Chaquopy runs Python
 * in-process, a "process" is actually a dedicated thread. Stopping interrupts that
 * thread; cooperative scripts (servers/bots) may need a moment or a force-stop.
 */
class RunSession(
    val projectId: String,
    val projectName: String,
    val scriptPath: String,
    val argv: List<String>,
    val env: Map<String, String>,
    val siteDirs: List<String>,
    val foreground: Boolean,
    private val runtime: PythonRuntime,
    private val onState: (RunStateInfo) -> Unit,
    private val onOutput: (String) -> Unit,
) {
    private val thread = AtomicReference<Thread?>(null)
    private val running = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)
    private val startedAt = System.currentTimeMillis()
    @Volatile private var endedAt: Long? = null
    @Volatile private var exitCode: Int? = null
    @Volatile private var state: RunState = RunState.STARTING

    fun start() {
        if (running.get()) return
        running.set(true)
        state = RunState.STARTING
        emit()
        val t = Thread({
            try {
                state = RunState.RUNNING
                emit()
                val listener = PyOutputListener { text -> onOutput(SecretMasker.mask(text)) }
                val rc = runtime.runScript(scriptPath, argv, env, siteDirs, listener)
                if (stopRequested.get()) {
                    state = RunState.STOPPED
                } else {
                    exitCode = rc
                    state = if (rc == 0) RunState.COMPLETED else RunState.FAILED
                }
            } catch (e: Throwable) {
                exitCode = -1
                state = RunState.FAILED
                onOutput("\n[ERROR] ${e.message}\n")
            } finally {
                endedAt = System.currentTimeMillis()
                running.set(false)
                emit()
            }
        }, "pymobile-run-$projectId")
        t.start()
        thread.set(t)
    }

    fun stop() {
        stopRequested.set(true)
        state = RunState.STOPPED
        thread.get()?.interrupt()
        emit()
    }

    fun status(): RunStateInfo = RunStateInfo(
        projectId, projectName, state, startedAt, endedAt, exitCode, foreground,
    )

    fun isRunning(): Boolean = running.get()

    private fun emit() = onState(status())
}
