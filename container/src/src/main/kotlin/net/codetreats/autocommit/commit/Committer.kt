package net.codetreats.autocommit.commit

import net.codetreats.autocommit.scan.GitService
import net.codetreats.autocommit.model.Commits
import net.codetreats.autocommit.util.StepRunner
import org.apache.logging.log4j.Logger
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Committer(
    private val logger: Logger,
    private val stepRunner: StepRunner,
    private val commitFile: File,
    private val backupDir: File,
    private val gitService: GitService
) {
    var now: LocalDateTime = LocalDateTime.now()
    var formatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    var formattedNow: String? = now.format(formatter)

    fun commit() {
        if (commitFile.exists().not()) {
            logger.info("Commit file not found. Skipping commit.")
            return
        }
        val commitText = commitFile.readText()
        val commits = Commits.from(commitText).commits.filter { it.exists() }
        val backup = File(backupDir, "commits.$formattedNow.json")
        backup.writeText(commitText)
        commitFile.delete()
        val categories = commits.map { it.category }.toSet()
        categories.forEach { category ->
            stepRunner.logStep("Commit $category")
            commits.filter { it.category == category }.forEach { commit ->
                logger.info("Committing: ${commit.category}/${commit.repo}: ${commit.message}")
                val repoPath = File("/git/${commit.category}/${commit.repo}")
                val git = gitService.openRepository(repoPath)
                gitService.addAll(git)
                gitService.commit(git, commit.message())
                logger.info("Successfully committed: ${commit.category}/${commit.repo}")
            }
        }
        categories.forEach { category ->
            stepRunner.logStep("Push $category")
            commits.filter { it.category == category }.forEach { commit ->
                logger.info("Pushing: ${commit.category}/${commit.repo}: ${commit.message}")
                val repoPath = File("/git/${commit.category}/${commit.repo}")
                val git = gitService.openRepository(repoPath)
                gitService.push(git)
                logger.info("Successfully pushed: ${commit.category}/${commit.repo}")
            }
        }
    }
}