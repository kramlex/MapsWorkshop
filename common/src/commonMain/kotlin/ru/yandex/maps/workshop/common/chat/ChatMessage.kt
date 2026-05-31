package ru.yandex.maps.workshop.common.chat

sealed interface ChatMessage {
    val id: String

    data class User(override val id: String, val text: String) : ChatMessage

    data class Assistant(override val id: String, val text: String) : ChatMessage

    data class ToolCall(override val id: String, val toolName: String) : ChatMessage

    data class ToolResult(override val id: String, val toolName: String) : ChatMessage

    data class Error(override val id: String, val description: String) : ChatMessage
}
