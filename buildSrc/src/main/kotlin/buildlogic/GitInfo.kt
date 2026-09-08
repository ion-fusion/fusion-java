// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package buildlogic

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject


/**
 * Facts about the Git commit that a build was made from.
 */
data class GitInfo(
    val shortCommitHash: String,
    val longCommitHash: String,
    val commitDate: String,
    val repoStatus: String
)
{
    companion object
    {
        /**
         * Stand-ins used when Git can't tell us anything: no `git` on the PATH, or a
         * build from a source archive with no repository. Every field still gets a
         * value, so consumers never have to cope with a partially populated record.
         */
        val UNKNOWN = GitInfo(
            shortCommitHash = "0",
            longCommitHash  = "0".repeat(40),
            commitDate      = "1970-01-01T00:00:00Z",
            repoStatus      = "unknown"
        )
    }
}


/**
 * Reads [GitInfo] from a repository by shelling out to `git`.
 *
 * This is a `ValueSource` rather than task logic so that it plays well with the
 * configuration cache: Gradle re-runs it each build to decide whether cached
 * configuration is still valid, which is exactly the behavior we want from something
 * that reads mutable state outside the build.
 */
abstract class GitInfoValueSource : ValueSource<GitInfo, GitInfoValueSource.Params>
{
    interface Params : ValueSourceParameters
    {
        /** Any directory within the repository to interrogate. */
        val repositoryDirectory: DirectoryProperty
    }

    @get:Inject
    abstract val execOperations: ExecOperations


    override fun obtain(): GitInfo
    {
        val dir = parameters.repositoryDirectory.get().asFile

        // Prints "<short-hash> <long-hash> <commit-date>" for the current commit.
        // `iso-strict` gives us an ISO-8601 (and thus Ion) timestamp.
        val lastCommit =
            git(dir, "log", "-1", "--format=format:%h %H %cd", "--date=iso-strict")
                ?: return GitInfo.UNKNOWN

        val fields = lastCommit.trim().split(" ")
        if (fields.size != 3) return GitInfo.UNKNOWN

        // Prints one line per modified path; empty output means a clean tree.
        val status = git(dir, "status", "--porcelain")

        return GitInfo(
            shortCommitHash = fields[0],
            longCommitHash  = fields[1],
            commitDate      = fields[2],
            repoStatus      = when
            {
                status == null   -> "unknown"
                status.isBlank() -> "clean"
                else             -> "dirty"
            }
        )
    }


    /**
     * Runs a Git command, capturing its output.
     *
     * @return null if Git is unavailable or the command fails for any reason.
     */
    private fun git(dir: File, vararg args: String): String?
    {
        val stdout = ByteArrayOutputStream()

        val result =
            try
            {
                execOperations.exec {
                    workingDir(dir)
                    commandLine("git", *args)
                    standardOutput = stdout
                    // Git narrates failures on stderr; we report them ourselves.
                    errorOutput = ByteArrayOutputStream()
                    isIgnoreExitValue = true
                }
            }
            catch (_: Exception)
            {
                return null   // No `git` on the PATH.
            }

        return if (result.exitValue == 0) stdout.toString("UTF-8") else null
    }
}
