package com.python.localhost.process

import com.python.localhost.python.PythonRuntime
import com.python.localhost.python.RunSession
import com.python.localhost.python.RunState
import com.python.localhost.python.RunStateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class RunRequest(
    val projectId: String,
    val projectName: String,
    val scriptPath: String,
    val argv: List<String>,
    val env: Map<String, String>,
    val siteDirs: List<String>,
    // Note: siteDirs intentionally kept; see RunSession signature.
    val foreground: Boolean,
)

/**
 * Tracks all running project executions and exposes their live state and output as
 * StateFlows so the UI (dashboard, running screen, terminal) can observe them.
 */
class ProcessManager(private val runtime: PythonRuntime) {
    private val sessions = ConcurrentHashMap<String, RunSession>()
    private val buffers = ConcurrentHashMap<String, StringBuilder>()
    private val _states = MutableStateFlow<Map<String, RunStateInfo>>(emptyMap())
    val states = _states.asStateFlow()
    private val _outputs = MutableStateFlow<Map<String, String>>(emptyMap())
    val outputs = _outputs.asStateFlow()

    fun startRun(req: RunRequest): RunStateInfo {
        stopRun(req.projectId)
        lastRequest = req
        val sb = StringBuilder()
        buffers[req.projectId] = sb
        val session = RunSession(
            projectId = req.projectId,
            projectName = req.projectName,
            scriptPath = req.scriptPath,
            argv = req.argv,
            env = req.env,
            siteDirs = req.siteDirs,
            foreground = req.foreground,
            runtime = runtime,
            onState = { st -> updateState(st) },
            onOutput = { text ->
                synchronized(sb) {
                    if (sb.length > 200_000) sb.delete(0, sb.length - 150_000)
                    sb.append(text)
                }
                updateOutput(req.projectId, sb.toString())
                logManager.append(req.projectId, LogCategory.OUTPUT, text)
            },
        )
        sessions[req.projectId] = session
        session.start()
        return session.status()
    }

    fun stopRun(projectId: String) {
        sessions[projectId]?.stop()
    }

    /** Re-launch the most recent run for a project (used by the notification Restart action). */
    fun restartLast(projectId: String): RunStateInfo? {
        val req = lastRequest?.takeIf { it.projectId == projectId } ?: return null
        return startRun(req)
    }

    fun getStatus(projectId: String): RunStateInfo? = sessions[projectId]?.status()

    fun getOutput(projectId: String): String = buffers[projectId]?.toString() ?: ""

    fun runningProjects(): List<RunStateInfo> = _states.value.values.filter {
        it.state == RunState.RUNNING || it.state == RunState.STARTING
    }

    private fun updateState(st: RunStateInfo) {
        _states.value = _states.value.toMutableMap().apply { put(st.projectId, st) }
    }

    private fun updateOutput(projectId: String, text: String) {
        _outputs.value = _outputs.value.toMutableMap().apply { put(projectId, text) }
    }
}
