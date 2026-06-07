package ru.yandex.maps.workshop.common.additional.llm

@DslMarker
annotation class ChatRequestDsl

/**
 * Type-safe builder for a single [CompletionRequest].
 *
 * Messages are appended in call order, so invoke [system], [user], [assistant] and [toolResult]
 * in the order they should appear in the conversation. Used via [OpenAIClient.complete].
 *
 * @param model the model identifier sent with the request (e.g. `gpt-4o`).
 */
@ChatRequestDsl
class ChatRequestBuilder(private val model: String) {

    private val messages = mutableListOf<Message>()
    private val toolSpecs = mutableListOf<ToolSpecs>()

    /** Appends a `system` message setting the assistant's behaviour and instructions. */
    fun system(content: String) {
        messages += Message(role = "system", content = content)
    }

    /** Appends a `user` message with the given [content]. */
    fun user(content: String) {
        messages += Message(role = "user", content = content)
    }

    /**
     * Appends an `assistant` message.
     *
     * @param content the assistant's textual reply, or `null` when the turn only requested tool calls.
     * @param toolCalls tool calls the assistant requested, echoed back so the model can match them
     *  to their [toolResult]s on the next turn.
     */
    fun assistant(content: String? = null, toolCalls: List<ToolCall>? = null) {
        messages += Message(role = "assistant", content = content, toolCalls = toolCalls)
    }

    /**
     * Appends a `tool` message carrying the result of a tool call.
     *
     * @param toolCallId the id of the [ToolCall] this result answers.
     * @param content the tool's output fed back to the model.
     */
    fun toolResult(toolCallId: String, content: String) {
        messages += Message(role = "tool", toolCallId = toolCallId, content = content)
    }

    /** Registers the tools the model may call. May be invoked multiple times; specs are accumulated. */
    fun tools(toolSpecs: List<ToolSpecs>) {
        this.toolSpecs += toolSpecs
    }

    /**
     * Builds the immutable [CompletionRequest] from the accumulated state.
     * `tools` is omitted entirely when no specs were registered.
     */
    fun build(): CompletionRequest = CompletionRequest(
        model = model,
        messages = messages.toList(),
        tools = toolSpecs.takeIf { it.isNotEmpty() }?.toList(),
    )
}
