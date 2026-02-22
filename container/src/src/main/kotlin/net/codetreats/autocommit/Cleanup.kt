package net.codetreats.autocommit

import net.codetreats.autocommit.util.StepRunner
import org.apache.logging.log4j.Logger
import java.io.File

class Cleanup(private val logger: Logger, private val stepRunner: StepRunner, private val outputDir: File) {
    fun clean() {
        stepRunner.logStep("Cleanup")
        outputDir.listFiles()!!.filter { it.isFile }.forEach { file ->
            file.delete()
        }
    }
}