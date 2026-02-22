package net.codetreats.autocommit.util

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class StepRunner(private val statusFilePath: String) {
    fun logStep(stepName: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss")
        val dateTime = dateFormat.format(Date())

        println("[STEP]")
        println("[STEP] #############################################")
        println("[STEP] $dateTime: $stepName")
        println("[STEP] #############################################")

        File(statusFilePath).appendText("$dateTime: $stepName\n")
    }
}