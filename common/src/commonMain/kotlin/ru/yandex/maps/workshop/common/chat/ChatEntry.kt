package ru.yandex.maps.workshop.common.chat

import ru.yandex.maps.workshop.common.additional.llm.ToolCall

internal sealed interface ChatEntry {
    val id: String

    data class User(override val id: String, val text: String) : ChatEntry

    data class Assistant(
        override val id: String,
        val text: String?,
        val toolCalls: List<ToolCall>? = emptyList(),
    ) : ChatEntry

    data class Tool(
        override val id: String,
        val callId: String,
        val name: String,
        val result: String,
    ) : ChatEntry

    data class Error(override val id: String, val description: String) : ChatEntry
}

internal fun ChatEntry.toMessages(): List<ChatMessage> = when (this) {
    is ChatEntry.User -> listOf(ChatMessage.User(id = id, text = text))
    is ChatEntry.Assistant -> buildList {
        if (!text.isNullOrBlank()) add(ChatMessage.Assistant(id = id, text = text))
        toolCalls?.forEach { call ->
            add(ChatMessage.ToolCall(id = call.id, toolName = call.function.name))
        }
    }
    is ChatEntry.Tool -> listOf(ChatMessage.ToolResult(id = id, toolName = name))
    is ChatEntry.Error -> listOf(ChatMessage.Error(id = id, description = description))
}
