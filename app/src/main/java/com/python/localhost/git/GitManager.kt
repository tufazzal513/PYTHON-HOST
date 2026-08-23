package com.python.localhost.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.ByteArrayOutputStream
import java.io.File

data class Credentials(val user: String, val token: String)
data class GitResult(val success: Boolean, val message: String)

/**
 * Git operations backed by JGit (pure Java, no system git required). Supports clone,
 * status, commit, push, pull, branch listing/creation, and checkout for local repos.
 */
class GitManager {

    fun isGitRepo(dir: File): Boolean = File(dir, ".git").exists()

    fun init(dir: File): GitResult {
        return try {
            Git.init().setDirectory(dir).call().close()
            GitResult(true, "Initialised git repository")
        } catch (e: Exception) {
            GitResult(false, e.message ?: "init failed")
        }
    }

    /** Returns a unified-diff string for a tracked/modified file, or null if not a git repo. */
    fun diff(dir: File, relPath: String): String? {
        val git = open(dir) ?: return null
        return try {
            val entries = git.diff().setPath(relPath).call()
            if (entries.isEmpty()) return "(no changes vs last commit)"
            val out = ByteArrayOutputStream()
            val formatter = DiffFormatter(out)
            formatter.setRepository(git.repository)
            formatter.format(entries)
            formatter.flush()
            formatter.close()
            out.toString("UTF-8")
        } catch (e: Exception) {
            e.message
        } finally {
            git.close()
        }
    }

    fun clone(url: String, target: File, credentials: Credentials? = null): GitResult {
        target.mkdirs()
        // JGit needs an (almost) empty target directory.
        target.listFiles()?.filter { it.name != ".git" }?.forEach { it.deleteRecursively() }
        return try {
            val cmd = Git.cloneRepository().setURI(url).setDirectory(target)
            if (credentials != null) {
                cmd.setCredentialsProvider(
                    UsernamePasswordCredentialsProvider(credentials.user, credentials.token)
                )
            }
            cmd.call().close()
            GitResult(true, "Cloned $url")
        } catch (e: Exception) {
            GitResult(false, e.message ?: "Clone failed")
        }
    }

    fun open(dir: File): Git? = try {
        Git.open(File(dir, ".git"))
    } catch (e: Exception) {
        null
    }

    fun status(dir: File): GitStatusInfo {
        val git = open(dir) ?: return GitStatusInfo(
            null, true, emptyList(), emptyList(), emptyList(), emptyList(),
            0, 0, null, "Not a git repository",
        )
        return try {
            val st = git.status().call()
            GitStatusInfo(
                branch = git.repository.branch,
                isClean = st.isClean,
                added = st.added.toList(),
                modified = st.modified.toList(),
                deleted = st.removed.toList(),
                untracked = st.untracked.toList(),
                aheadCount = 0,
                behindCount = 0,
                remoteUrl = remoteUrl(dir),
            )
        } catch (e: Exception) {
            GitStatusInfo(
                null, true, emptyList(), emptyList(), emptyList(), emptyList(),
                0, 0, remoteUrl(dir), e.message,
            )
        } finally {
            git.close()
        }
    }

    fun addAllAndCommit(
        dir: File,
        message: String,
        authorName: String = "PyMobile IDE",
        authorEmail: String = "ide@pymobile.local",
    ): GitResult {
        val git = open(dir) ?: return GitResult(false, "Not a git repository")
        return try {
            git.add().addFilepattern(".").call()
            git.commit().setMessage(message).setAuthor(authorName, authorEmail).call()
            GitResult(true, "Committed changes")
        } catch (e: Exception) {
            GitResult(false, e.message ?: "Commit failed")
        } finally {
            git.close()
        }
    }

    fun push(dir: File, credentials: Credentials): GitResult {
        val git = open(dir) ?: return GitResult(false, "Not a git repository")
        return try {
            git.push().setCredentialsProvider(
                UsernamePasswordCredentialsProvider(credentials.user, credentials.token)
            ).call()
            GitResult(true, "Pushed to remote")
        } catch (e: Exception) {
            GitResult(false, e.message ?: "Push failed")
        } finally {
            git.close()
        }
    }

    fun pull(dir: File, credentials: Credentials? = null): GitResult {
        val git = open(dir) ?: return GitResult(false, "Not a git repository")
        return try {
            val cmd = git.pull()
            if (credentials != null) {
                cmd.setCredentialsProvider(
                    UsernamePasswordCredentialsProvider(credentials.user, credentials.token)
                )
            }
            cmd.call()
            GitResult(true, "Pulled latest changes")
        } catch (e: Exception) {
            GitResult(false, e.message ?: "Pull failed")
        } finally {
            git.close()
        }
    }

    fun branches(dir: File): List<String> {
        val git = open(dir) ?: return emptyList()
        return try {
            git.branchList().call().map { it.name.substringAfterLast("/") }
        } catch (e: Exception) {
            emptyList()
        } finally {
            git.close()
        }
    }

    fun createBranch(dir: File, name: String): GitResult {
        val git = open(dir) ?: return GitResult(false, "Not a git repository")
        return try {
            git.checkout().setCreateBranch(true).setName(name).call()
            GitResult(true, "Created and switched to $name")
        } catch (e: Exception) {
            GitResult(false, e.message ?: "Branch creation failed")
        } finally {
            git.close()
        }
    }

    fun checkout(dir: File, name: String): GitResult {
        val git = open(dir) ?: return GitResult(false, "Not a git repository")
        return try {
            git.checkout().setName(name).call()
            GitResult(true, "Checked out $name")
        } catch (e: Exception) {
            GitResult(false, e.message ?: "Checkout failed")
        } finally {
            git.close()
        }
    }

    private fun remoteUrl(dir: File): String? {
        val git = open(dir) ?: return null
        return try {
            git.repository.config.getString("remote", "origin", "url")
        } catch (e: Exception) {
            null
        } finally {
            git.close()
        }
    }
}
