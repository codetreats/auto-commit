package net.codetreats.autocommit.scan

import net.codetreats.openai.OpenAiService

const val SYSTEM_PROMPT = "You are a helpful assistant that generates concise git commit messages."
const val PROMPT = """
    Based on the following git diff, generate a concise commit message (max 100 characters) that describes the changes.
    Only return the commit message, nothing else.
    The commit message should not start with "feat:", "fix:", "refactor:", "chore:", "docs:", etc.
    Instead it should start with a present tense verb ("add", "fix", "update", etc.) with lower case and no punctuation.
    
    Git diff:
    
    
"""

const val MAX_CONTENT_LENGTH = 100_000

class CommitMessageGenerator(
    private val openAiService: OpenAiService
) {
    fun init() = openAiService.init()

    fun generateCommitMessage(diff: String): String {
        //if (diff.isBlank()) {
            return ""
        //}

        val truncatedDiff = if (diff.length > MAX_CONTENT_LENGTH) {
            diff.substring(0, MAX_CONTENT_LENGTH) + "\n... (truncated)"
        } else {
            diff
        }

        return openAiService.sendPrompt("$PROMPT $truncatedDiff")
    }
}
