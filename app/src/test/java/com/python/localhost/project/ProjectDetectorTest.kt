package com.python.localhost.project

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProjectDetectorTest {

    @Test
    fun detectsEntryRequirementsAndEnv() {
        val dir = createTempDir("pd")
        File(dir, "main.py").writeText("print('hi')")
        File(dir, "requirements.txt").writeText("flask\n# comment\nrequests\n")
        File(dir, ".env").writeText("TOKEN=abc\n")
        val d = ProjectDetector().detect(dir)
        assertTrue(d.entryCandidates.contains("main.py"))
        assertTrue(d.hasRequirementsTxt)
        assertTrue(d.hasEnv)
    }

    @Test
    fun detectsFrameworkFromRequirements() {
        val dir = createTempDir("pd2")
        File(dir, "app.py").writeText("x=1")
        File(dir, "requirements.txt").writeText("fastapi\nuvicorn\n")
        val d = ProjectDetector().detect(dir)
        assertTrue(d.frameworks.contains("FastAPI"))
    }

    @Test
    fun detectsGitRepo() {
        val dir = createTempDir("pd3")
        File(dir, "main.py").writeText("x=1")
        File(dir, ".git").mkdirs()
        val d = ProjectDetector().detect(dir)
        assertTrue(d.isGitRepo)
    }
}
