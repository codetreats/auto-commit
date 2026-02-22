package net.codetreats.autocommit.scan

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import net.codetreats.autocommit.scan.model.*
import net.codetreats.autocommit.util.StepRunner
import org.apache.logging.log4j.Logger
import org.eclipse.jgit.lib.BranchTrackingStatus
import java.io.File
import java.nio.file.Files
import java.util.*

class RepoScanner(
    private val logger: Logger,
    private val stepRunner: StepRunner,
    private val gitBaseDir: File,
    private val outputDir: File,
    private val commitMessageGenerator: CommitMessageGenerator,
    private val gitService: GitService
) {
    fun scanAllRepos() {
        logger.info("Starting repository scan...")
        commitMessageGenerator.init()

        val summary = mutableListOf<SummaryEntry>()
        val categories = gitBaseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

        for (category in categories) {
            stepRunner.logStep("Scan ${category.name}")
            logger.info("\nScanning category: ${category.name}")

            val repos = category.listFiles()?.filter { it.isDirectory } ?: emptyList()

            for (repo in repos) {
                logger.info("  Analyzing: ${category.name}/${repo.name}")

                val repoStatus = analyzeRepo(category.name, repo.name)

                val jsonFilename = "${category.name}_${repo.name}.json"
                val jsonPath = File(outputDir, jsonFilename)
                jsonPath.writeText(repoStatus.toJson())

                summary.add(
                    SummaryEntry(
                        category = category.name,
                        repo = repo.name,
                        status = repoStatus.status.name,
                        changesCount = repoStatus.changes.size,
                        jsonFile = jsonFilename,
                        suggestedCommitMessage = repoStatus.suggestedCommitMessage
                    )
                )

                logger.info("    Status: ${repoStatus.status} (${repoStatus.changes.size} changes)")
                logger.info("    Message: ${repoStatus.suggestedCommitMessage}")
            }
        }

        val summaryPath = File(outputDir, "summary.json")
        summaryPath.writeText(summary.toJson())

        logger.info("\n✅ Repository scan completed successfully!")
        logger.info("   Total repositories: ${summary.size}")
        logger.info("   With changes: ${summary.count { it.status == "CHANGED" }}")
    }

    private fun analyzeRepo(category: String, repo: String): RepoStatus {
        val repoPath = File(gitBaseDir, "$category/$repo")
        val gitDir = File(repoPath, ".git")

        if (!gitDir.exists()) {
            return RepoStatus(
                status = net.codetreats.autocommit.scan.model.Status.UNTRACKED,
                category = category,
                repo = repo,
                path = repoPath.absolutePath,
                changes = emptyList()
            )
        }

        val git = gitService.openRepository(repoPath)

        val status: Status = git.status().call()

        val hasUncommittedChanges = status.modified.isNotEmpty() ||
                status.added.isNotEmpty() ||
                status.removed.isNotEmpty() ||
                status.changed.isNotEmpty()

        if (hasUncommittedChanges) {
            val changes = processChanges(repoPath, git, status)
            val diff = gitService.getDiff(git)
            val suggestedCommitMessage = commitMessageGenerator.generateCommitMessage(diff)

            return RepoStatus(
                status = net.codetreats.autocommit.scan.model.Status.CHANGED,
                category = category,
                repo = repo,
                path = repoPath.absolutePath,
                changes = changes,
                suggestedCommitMessage = suggestedCommitMessage
            )
        }

        val branch = git.repository.branch
        val trackingStatus = BranchTrackingStatus.of(git.repository, branch)

        if (trackingStatus != null && trackingStatus.aheadCount > 0) {
            return RepoStatus(
                status = net.codetreats.autocommit.scan.model.Status.UNPUSHED,
                category = category,
                repo = repo,
                path = repoPath.absolutePath,
                changes = emptyList()
            )
        }

        return RepoStatus(
            status = net.codetreats.autocommit.scan.model.Status.UPTODATE,
            category = category,
            repo = repo,
            path = repoPath.absolutePath,
            changes = emptyList()
        )
    }

    private fun processChanges(repoPath: File, git: Git, status: Status): List<Change> {
        val changes = mutableListOf<Change>()

        val allFiles = mutableListOf<Pair<String, ChangeType>>()
        allFiles.addAll(status.modified.map { it to ChangeType.MODIFIED })
        allFiles.addAll(status.added.map { it to ChangeType.ADDED })
        allFiles.addAll(status.removed.map { it to ChangeType.DELETED })
        allFiles.addAll(status.changed.map { it to ChangeType.MODIFIED })

        for ((filePath, type) in allFiles) {
            if (isBinaryFile(repoPath, filePath)) {
                logger.info("  Skipping binary file: $filePath")
                continue
            }

            var oldFile: String? = null
            var newFile: String? = null

            if (type == ChangeType.MODIFIED || type == ChangeType.DELETED) {
                val oldContent = gitService.getFileContent(git, filePath, "HEAD")
                if (oldContent != null) {
                    oldFile = saveFileWithUUID(oldContent)
                }
            }

            if (type == ChangeType.MODIFIED || type == ChangeType.ADDED) {
                val newContent = getCurrentFileContent(repoPath, filePath)
                if (newContent != null) {
                    newFile = saveFileWithUUID(newContent)
                }
            }

            changes.add(
                Change(
                    path = filePath,
                    type = type,
                    oldFile = oldFile,
                    newFile = newFile
                )
            )
        }

        return changes
    }

    private fun saveFileWithUUID(content: String): String {
        val uuid = UUID.randomUUID().toString()
        val filename = "$uuid.txt"
        val filepath = File(outputDir, filename)
        filepath.writeText(content, Charsets.UTF_8)
        return filename
    }

    private fun isBinaryFile(repoPath: File, filePath: String): Boolean {
        val file = File(repoPath, filePath)
        return try {
            val content = Files.readAllBytes(file.toPath())
            content.any { it == 0.toByte() }
        } catch (e: Exception) {
            false
        }
    }

    private fun getCurrentFileContent(repoPath: File, filePath: String): String? {
        val file = File(repoPath, filePath)
        return try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
