
package net.codetreats.openai

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import net.codetreats.rest.RestClient
import org.apache.logging.log4j.Logger

class OpenAiService(
    private val logger: Logger?,
    private val llmUrl: String,
    private val llmApiKey: String,
    private val llmModel: String,
    private val llmThrottlingTime: Int,
    private val systemPrompt: String
) {
    private val llmClient by lazy {
        logger?.info("Initializing OpenAI service:")
        logger?.info("  URL: $llmUrl")
        logger?.info("  KEY: ${"*".repeat(llmApiKey.length)}")
        logger?.info("  MODEL: $llmModel")
        logger?.info("  SYSTEM-PROMPT: $systemPrompt")
        RestClient(llmUrl, mapOf("Authorization" to "Bearer $llmApiKey"))
    }

    fun init() {
        llmClient
    }

    fun sendPrompt(prompt: String): String {
        val client = llmClient
        val body = ApiBody(
            model = llmModel,
            messages = listOf(
                ApiMessage(role = "system", content = systemPrompt),
                ApiMessage(role = "user", content = prompt)
            )
        )
        logger?.debug("[SEND] $prompt")
        val response = ChatCompletionResponse.from(client.post("", body = body.toJson()).message).choices.first().message.content!!
        if (llmThrottlingTime > 0) {
            Thread.sleep(llmThrottlingTime * 1000L)
        }
        logger?.debug("[RECV] $response")
        return response
    }


}

data class ApiBody(
    val model: String,
    val messages: List<ApiMessage>
) {
    fun toJson(): String {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(ApiBody::class.java)
        return adapter.toJson(this)
    }
}

data class ApiMessage(
    val role: String,
    val content: String
) 

data class ChatCompletionResponse(
    val id: String,
    val created: Long, // Unix-Timestamp (seconds)
    val model: String,
    val choices: List<Choice>,
    val usage: Usage? = null,
){
    companion object {
        fun from(message: String): ChatCompletionResponse {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(ChatCompletionResponse::class.java)
            return adapter.fromJson(message)!!
        }
    }
}

data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String?,
)

data class Message(
    val role: String, 
    val content: String? = null
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)
