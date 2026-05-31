package ru.yandex.maps.workshop.internal.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.yandex.maps.workshop.common.chat.ChatMessage
import ru.yandex.maps.workshop.common.chat.ChatViewModel
import ru.yandex.maps.workshop.common.chat.ClearChatEvent
import ru.yandex.maps.workshop.common.chat.SendMessageEvent

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.viewStates().collectAsState()
    val messages = state.messages
    val isSending = state.isSending

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.lastOrNull()?.id, isSending) {
        val target = messages.size - 1 + if (isSending) 1 else 0
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ассистент",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (messages.isNotEmpty()) {
                TextButton(onClick = { viewModel.dispatch(ClearChatEvent) }) {
                    Text("Очистить")
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp, max = 480.dp)
                .weight(1f, fill = false),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (messages.isEmpty() && !isSending) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Спросите что-нибудь о карте или местах рядом.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
            if (isSending) {
                item { ThinkingIndicator() }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val canSend = input.isNotBlank() && !isSending
            val send = {
                if (canSend) {
                    val text = input
                    input = ""
                    viewModel.dispatch(SendMessageEvent(text))
                }
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Сообщение") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                singleLine = false,
                maxLines = 4,
                enabled = !isSending,
            )
            Spacer(Modifier.size(8.dp))
            IconButton(
                enabled = canSend,
                onClick = send,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = if (canSend) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val UserBubbleShape = RoundedCornerShape(
    topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp,
)
private val AssistantBubbleShape = RoundedCornerShape(
    topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp,
)
private val SystemBubbleShape = RoundedCornerShape(12.dp)

private val ToolCallContainerColor = Color(0xFFE8E8E8)
private val ToolCallContentColor = Color(0xFF424242)
private val ToolResultContainerColor = Color(0xFFD7F0DB)
private val ToolResultContentColor = Color(0xFF2E7D32)

@Composable
private fun MessageBubble(message: ChatMessage) {
    when (message) {
        is ChatMessage.User -> ChatBubble(
            text = message.text,
            alignment = Alignment.CenterEnd,
            shape = UserBubbleShape,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        is ChatMessage.Assistant -> ChatBubble(
            text = message.text,
            alignment = Alignment.CenterStart,
            shape = AssistantBubbleShape,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is ChatMessage.ToolCall -> ToolCallBubble(
            toolName = message.toolName,
        )
        is ChatMessage.ToolResult -> ToolResultBubble(
            toolName = message.toolName,
        )
        is ChatMessage.Error -> ErrorBubble(message)
    }
}

@Composable
private fun ChatBubble(
    text: String,
    alignment: Alignment,
    shape: RoundedCornerShape,
    backgroundColor: Color,
    textColor: Color,
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(backgroundColor, shape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ToolCallBubble(toolName: String) = SystemBubble(
    text = "Вызов инструмента: $toolName",
    containerColor = ToolCallContainerColor,
    contentColor = ToolCallContentColor,
)

@Composable
private fun ToolResultBubble(toolName: String) = SystemBubble(
    text = "Результат получен от: $toolName",
    containerColor = ToolResultContainerColor,
    contentColor = ToolResultContentColor,
)

@Composable
private fun SystemBubble(text: String, containerColor: Color, contentColor: Color) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(containerColor, SystemBubbleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ErrorBubble(message: ChatMessage.Error) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = message.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ThinkingIndicator() {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "Думаю…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
        )
    }
}