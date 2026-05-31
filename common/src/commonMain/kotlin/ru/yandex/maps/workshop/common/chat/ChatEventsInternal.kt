package ru.yandex.maps.workshop.common.chat

internal sealed interface BaseChatEvent : ChatEvent

internal data class SetMessages(val messages: List<ChatMessage>) : BaseChatEvent

internal data class SetSending(val sending: Boolean) : BaseChatEvent

internal fun ChatViewModel.State.reduce(
    event: BaseChatEvent
): ChatViewModel.State = when (event) {
    is SetMessages -> copy(messages = event.messages)
    is SetSending -> copy(isSending = event.sending)
}
