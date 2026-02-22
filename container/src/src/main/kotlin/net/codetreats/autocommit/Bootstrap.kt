package de.codetreats.autocommit

import de.codetreats.autocommit.http.PageCommit
import net.codetreats.autocommit.Cleanup
import net.codetreats.autocommit.commit.Committer
import net.codetreats.autocommit.scan.CommitMessageGenerator
import net.codetreats.autocommit.scan.GitService
import net.codetreats.autocommit.scan.RepoScanner
import net.codetreats.autocommit.scan.SYSTEM_PROMPT
import net.codetreats.autocommit.util.StepRunner
import net.codetreats.openai.OpenAiService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File



class Bootstrap(args: Array<String>) {

    val LLM_URL: String = System.getenv("LLM_URL")

    val LLM_MODEL: String = System.getenv("LLM_MODEL")

    val LLM_KEY: String = System.getenv("LLM_KEY")

    val LLM_THROTTLING_TIME: Int = System.getenv("LLM_THROTTLING_TIME").toInt()

    val GIT_USER: String = System.getenv("GIT_USER")

    val GIT_MAIL: String = System.getenv("GIT_MAIL")

    val GITHUB_TOKEN: String = System.getenv("GITHUB_TOKEN")

    val gitBaseDir  by lazy { File("/git") }

    val outputDir by lazy { File("/var/www/html/diffs") }

    val commitFile by lazy { File("/var/www/html/diffs/commits.json") }

    val backupDir by lazy { File("/var/www/html/backup").also { it.mkdirs() } }

    val triggerFile by lazy { File("/var/www/html/pipeline/trigger/trigger.flag") }

    val stepRunner by lazy { StepRunner(args[0]) }

    val openAiService by lazy { OpenAiService(getLogger<OpenAiService>(), LLM_URL, LLM_KEY, LLM_MODEL, LLM_THROTTLING_TIME, SYSTEM_PROMPT) }

    val commitMessageGenerator by lazy { CommitMessageGenerator(openAiService) }

    val gitService by lazy { GitService(GIT_USER, GIT_MAIL, GITHUB_TOKEN) }

    val repoScanner by lazy { RepoScanner(getLogger<RepoScanner>(), stepRunner, gitBaseDir, outputDir, commitMessageGenerator, gitService) }

    val committer by lazy { Committer(getLogger<Committer>(), stepRunner, commitFile, backupDir, gitService) }

    val cleanup by lazy { Cleanup(getLogger<Cleanup>(), stepRunner, outputDir) }

    val pageCommit by lazy { PageCommit(getLogger<PageCommit>(), commitFile, triggerFile) }

    inline fun <reified T> getLogger(): Logger = LogManager.getLogger(T::class.simpleName)
}