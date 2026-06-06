package ru.yandex.maps.workshop.common.chat

import ru.yandex.maps.workshop.common.additional.udf.Event

interface ChatEvent : Event

data class SendMessageEvent(val text: String) : ChatEvent

data object ClearChatEvent : ChatEvent
