package de.codetreats.autocommit.http

import net.codetreats.autocommit.model.Commits
import org.apache.logging.log4j.Logger
import java.io.File

class PageCommit(private val logger: Logger, private val commitFile: File, private val triggerFile: File)  {
    fun post(body: String) : String {
        val commits = Commits.from(body)
        commitFile.writeText(commits.toJson())
        logger.info("Commits written to ${commitFile.absolutePath}" )
        triggerFile.createNewFile()
        return System.getenv("HOST_URL")
    }
}
