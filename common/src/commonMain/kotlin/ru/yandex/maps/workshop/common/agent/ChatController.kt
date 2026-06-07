package ru.yandex.maps.workshop.common.agent

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface ChatCommand {
    data object Close : ChatCommand
}

class ChatController {

    private val _commands = MutableSharedFlow<ChatCommand>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val commands: SharedFlow<ChatCommand> = _commands.asSharedFlow()

    fun close() {
        _commands.tryEmit(ChatCommand.Close)
    }
}
