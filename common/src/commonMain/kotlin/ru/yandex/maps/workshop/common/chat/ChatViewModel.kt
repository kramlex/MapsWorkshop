package ru.yandex.maps.workshop.common.chat

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.yandex.maps.workshop.common.additional.udf.Event
import ru.yandex.maps.workshop.common.additional.udf.Reducer
import ru.yandex.maps.workshop.common.additional.udf.ViewModel

private val ChatReducer: Reducer<ChatViewModel.State> get() = { state, event ->
    when (event) {
        is BaseChatEvent -> state.reduce(event)
        else -> state
    }
}

class ChatViewModel(
    private val repository: ChatRepository,
) : ViewModel<ChatViewModel.State>(
    initialState = State.Initial,
    reducer = ChatReducer
) {

    private var sendJob: Job? = null

    init {
        listenRepository()
    }

    override fun sideEffect(viewEvent: Event) {
        when (viewEvent) {
            is SendMessageEvent -> sendMessage(viewEvent.text)
            is ClearChatEvent -> repository.clear()
        }
    }

    data class State(
        val messages: List<ChatMessage>,
        val isSending: Boolean,
    ) {
        companion object {
            val Initial = State(messages = emptyList(), isSending = false)
        }
    }

    private fun listenRepository() {
        repository.messages
            .onEach { store.dispatch(SetMessages(it)) }
            .launchIn(viewModelScope)
    }

    private fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || sendJob?.isActive == true) return
        sendJob = viewModelScope.launch {
            store.dispatch(SetSending(true))
            try {
                repository.sendMessage(trimmed)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                println("Chat error: ${t.message}")
            } finally {
                store.dispatchAsync(SetSending(false))
            }
        }
    }
}
