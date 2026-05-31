package ru.yandex.maps.workshop.common.chat

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class ChatRepository {

    private val entries = MutableStateFlow<List<ChatEntry>>(emptyList())

    val messages: Flow<List<ChatMessage>> = entries.map { it.flatMap(ChatEntry::toMessages) }

    suspend fun sendMessage(text: String) {
        append(ChatEntry.User(id = nextId(), text = text))
        delay(1000)
        append(ChatEntry.Assistant(id = nextId(), text = "MOCK: Привет!"))
    }

    fun clear() {
        entries.value = emptyList()
    }

    private fun append(entry: ChatEntry) = entries.update { it + entry }

    @OptIn(ExperimentalUuidApi::class)
    private fun nextId(): String = "msg_${Uuid.random()}"
}
