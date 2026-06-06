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
import ru.yandex.maps.workshop.common.agent.tools.AgentToolset

private const val MAX_STEPS = 10
private const val SYSTEM_PROMPT = """
You are Mappy, a friendly AI-assistant in Yandex Maps. 

This is a mobile app for navigation and discovery.

You operate inside a chat and help users by answering their questions and solving their tasks.
    
Rules:
1. You only answer questions related to navigation and discovery.
2. You always answer in the user's language. Focus on the latest message.
3. You give helpful answers, but short and concise.
"""

class ChatRepository(
    private val openAIClient: OpenAIClient,
    private val assistantApi: AssistantApi,
) {

    private val entries = MutableStateFlow<List<ChatEntry>>(emptyList())

    val messages: Flow<List<ChatMessage>> = entries.map { it.flatMap(ChatEntry::toMessages) }

    private val toolset = AgentToolset(assistantApi)

    suspend fun sendMessage(text: String) {
        append(ChatEntry.User(id = nextId(), text = text))
        try {
            runAgentLoop()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            append(ChatEntry.Error(id = nextId(), description = t.message ?: t.toString()))
        }
    }

    private suspend fun runAgentLoop() {
        repeat(MAX_STEPS) {
            val response = openAIClient.complete {
                system(SYSTEM_PROMPT)
                tools(toolset.specs)
                entries.value.forEach { entry ->
                    when (entry) {
                        is ChatEntry.User -> user(entry.text)
                        is ChatEntry.Assistant -> assistant(content = entry.text, toolCalls = entry.toolCalls)
                        is ChatEntry.Tool -> toolResult(toolCallId = entry.callId, content = entry.result)
                        is ChatEntry.Error -> Unit
                    }
                }
            }

            val message = response.choices.firstOrNull()?.message
            val calls = message?.toolCalls
            append(ChatEntry.Assistant(id = nextId(), text = message?.content, toolCalls = calls))

            if (calls.isNullOrEmpty()) return

            for (call in calls) {
                val result = toolset.dispatch(call)
                append(ChatEntry.Tool(
                    id = nextId(),
                    callId = call.id,
                    name = call.function.name,
                    result = result,
                ))
            }
        }
    }

    fun clear() {
        entries.value = emptyList()
    }

    private fun append(entry: ChatEntry) = entries.update { it + entry }

    @OptIn(ExperimentalUuidApi::class)
    private fun nextId(): String = "msg_${Uuid.random()}"
}
