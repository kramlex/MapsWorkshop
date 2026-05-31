package ru.yandex.maps.workshop.common.additional.llm

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Body of an OpenAI chat-completions request.
 *
 * @param model the model identifier to run the completion on.
 * @param messages the conversation so far, in chronological order.
 * @param tools the tools the model is allowed to call, or `null` when none are offered.
 */
@Serializable
data class CompletionRequest(
    val model: String,
    val messages: List<Message>,
    val tools: List<ToolSpecs>? = null,
)

/**
 * Successful response of a chat-completions request.
 *
 * @param id server-assigned identifier of the completion.
 * @param model the model that actually produced the response.
 * @param choices the generated alternatives; typically a single entry, defaulting to empty.
 */
@Serializable
data class CompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice> = emptyList(),
)

/**
 * A single generated alternative within a [CompletionResponse].
 *
 * @param index position of this choice in the list.
 * @param message the generated message (text and/or tool calls).
 * @param finishReason why generation stopped, e.g. `stop`, `length` or `tool_calls`.
 */
@Serializable
data class Choice(
    val index: Int = 0,
    val message: Message,
    @SerialName("finish_reason") val finishReason: String? = null,
)

/**
 * A single conversation message, shared by requests and responses.
 *
 * @param role one of `system`, `user`, `assistant` or `tool`.
 * @param content the textual content, or `null` for assistant turns that only request tool calls.
 * @param toolCalls tool calls requested by an assistant turn, if any.
 * @param toolCallId for a `tool` message, the id of the [ToolCall] it responds to.
 */
@Serializable
data class Message(
    val role: String,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
)

/**
 * Declaration of a tool the model may call.
 *
 * @param type the tool kind; currently always `function`.
 * @param function the callable function's signature.
 */
@Serializable
data class ToolSpecs(
    val type: String = "function",
    val function: FunctionSpec,
)

/**
 * Signature of a callable function exposed to the model.
 *
 * @param name the function name the model uses when calling it.
 * @param description natural-language description guiding when the model should call it.
 * @param parameters a JSON Schema object describing the accepted arguments.
 */
@Serializable
data class FunctionSpec(
    val name: String,
    val description: String? = null,
    val parameters: JsonObject,
)

/**
 * A tool invocation requested by the model.
 *
 * @param id unique id of this call, echoed back in the matching `tool` [Message].
 * @param type the tool kind; currently always `function`.
 * @param function the function name and its serialized arguments.
 */
@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall,
)

/**
 * The function name and arguments of a [ToolCall].
 *
 * @param name the name of the function to invoke.
 * @param arguments the arguments as a raw JSON string, to be parsed by the caller.
 */
@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String,
)

/**
 * Minimal client for an OpenAI-compatible chat-completions endpoint.
 *
 * @param apiKey bearer token sent in the `Authorization` header.
 * @param model default model used for requests built by [complete].
 * @param baseUrl full URL of the chat-completions endpoint.
 * @param httpClient the underlying ktor client; a sensibly configured one is created by default.
 */
class OpenAIClient(
    private val apiKey: String,
    val model: String,
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient(),
) {
    /**
     * Sends a chat-completion request built via the [ChatRequestBuilder] DSL.
     *
     * @param block configures the request (messages, tools) using the builder receiver.
     * @return the parsed successful [CompletionResponse].
     * @throws OpenAIException if the endpoint responds with a non-2xx status; its
     *  [OpenAIException.responseBody] carries the raw error payload returned by the server.
     */
    suspend fun complete(block: ChatRequestBuilder.() -> Unit): CompletionResponse {
        val request = ChatRequestBuilder(model).apply(block).build()
        val response = httpClient.post(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw OpenAIException(response.status, response.bodyAsText())
        }
        return response.body()
    }
}

/**
 * Thrown by [OpenAIClient.complete] when the endpoint returns a non-2xx status.
 *
 * Reading the raw body explicitly avoids the misleading `NoTransformationFoundException`
 * that content negotiation raises when an error payload isn't valid JSON.
 *
 * @param status the HTTP status returned by the server.
 * @param responseBody the raw response body, preserved for diagnostics.
 */
class OpenAIException(
    val status: HttpStatusCode,
    val responseBody: String,
) : Exception("OpenAI request failed (${status.value} ${status.description}): $responseBody")

/**
 * Builds the default ktor client: lenient JSON, generous timeouts for long completions,
 * and body logging with the `Authorization` header redacted.
 */
private fun createHttpClient() = HttpClient {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
        requestTimeoutMillis = 120_000
        socketTimeoutMillis = 120_000
        connectTimeoutMillis = 30_000
    }
    install(Logging) {
        level = LogLevel.BODY
        logger = object : Logger {
            override fun log(message: String) = println(message)
        }
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
}
