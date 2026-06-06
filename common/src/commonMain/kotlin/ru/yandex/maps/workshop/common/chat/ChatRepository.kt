package ru.yandex.maps.workshop.common.chat

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.yandex.maps.workshop.common.additional.llm.OpenAIClient
import ru.yandex.maps.workshop.common.agent.AssistantApi

class ChatRepository(
    private val openAIClient: OpenAIClient,
    private val assistantApi: AssistantApi,
) {

    private val entries = MutableStateFlow<List<ChatEntry>>(emptyList())

    val messages: Flow<List<ChatMessage>> = entries.map { it.flatMap(ChatEntry::toMessages) }

    suspend fun sendMessage(text: String) {
        append(ChatEntry.User(id = nextId(), text = text))
        try {
            val response = openAIClient.complete {
                user(text)
            }

            val message = response.choices.firstOrNull()?.message
            append(ChatEntry.Assistant(id = nextId(), text = message?.content))
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            append(ChatEntry.Error(id = nextId(), description = t.message ?: t.toString()))
        }
    }

    fun clear() {
        entries.value = emptyList()
    }

    private fun append(entry: ChatEntry) = entries.update { it + entry }

    @OptIn(ExperimentalUuidApi::class)
    private fun nextId(): String = "msg_${Uuid.random()}"
}
