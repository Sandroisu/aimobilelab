package dev.sandroisu.aimobilelab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.sandroisu.aimobilelab.core.chat.AssistantMessageReducer
import dev.sandroisu.aimobilelab.core.chat.AssistantMessageState
import dev.sandroisu.aimobilelab.core.chat.ChatMessage
import dev.sandroisu.aimobilelab.core.chat.Role
import dev.sandroisu.aimobilelab.core.chat.StreamEvent
import dev.sandroisu.aimobilelab.core.llm.LlmStreamClient
import dev.sandroisu.aimobilelab.presentation.state.ChatMessageUi
import dev.sandroisu.aimobilelab.presentation.state.ChatScreenState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(private val llmStreamClient: LlmStreamClient) : ViewModel() {

    class ChatViewModelFactory(private val client: LlmStreamClient) : ViewModelProvider
        .Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(client) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private var activeKey: String? = null
    private var job: Job? = null
    private var current: AssistantMessageState? = null
    private val chatHistory = mutableListOf<ChatMessage>()

    private val _screenState = MutableStateFlow<ChatScreenState>(
        ChatScreenState(
            messages = emptyList(),
            input = "",
            canSend = false,
            canCancel = false,
            canRetry = false,
        )
    )
    val screenState: StateFlow<ChatScreenState> = _screenState.asStateFlow()

    fun onInputChange(text: String) {
        val canCancel = current?.isStreaming == true
        val canSend = text.isNotBlank() && !canCancel
        _screenState.update { it.copy(input = text, canSend = canSend) }
    }

    fun send() {
        val text = _screenState.value.input
        if (text.isBlank()) return
        chatHistory.add(ChatMessage(Role.User, text))
        _screenState.update { it.copy(input = "", canSend = false) }
        val id = UUID.randomUUID().toString()
        startRequest(id, 1)
    }

    fun retry() {
        val id = current?.id ?: return
        val attempt = (current?.attempt ?: 0) + 1
        startRequest(id, attempt)
    }

    fun cancel() {
        job?.cancel()
        job = null
        reduceEventAndCommit(StreamEvent.Error("canceled"))
        activeKey = null
        recomputeUi()
    }

    private fun startRequest(id: String, attempt: Int) {
        job?.cancel()
        activeKey = "$id#$attempt"
        reduceEventAndCommit(StreamEvent.Start(id, attempt))
        job = viewModelScope.launch {
            llmStreamClient.stream(id, attempt).collect { env ->
                if (env.key != activeKey) return@collect
                reduceEventAndCommit(env.streamEvent)
            }
        }
    }

    private fun reduceEventAndCommit(event: StreamEvent) {
        val next = AssistantMessageReducer.reduce(current, event)
        if (!next.isStreaming && next.error == null && next.finalText != null) {
            chatHistory.add(ChatMessage(Role.Assistant, next.finalText ?: ""))
            current = null
        } else {
            current = next
        }
        recomputeUi()
    }

    private fun recomputeUi() {
        val list = buildList {
            addAll(chatHistory.mapIndexed { i, m ->
                ChatMessageUi(
                    id = "h$i-${m.role}", role = m.role, text = m.text, isStreaming = false
                )
            })
            if (current != null && current!!.partialText.isNotEmpty()) {
                add(
                    ChatMessageUi(
                        id = "cur", role = Role.Assistant, text = current!!.partialText,
                        isStreaming = true
                    )
                )
            }
        }
        val canCancel = current?.isStreaming == true
        val canRetry = current?.error != null
        val canSend = _screenState.value.input.isNotBlank() && !canCancel
        _screenState.value = _screenState.value.copy(
            messages = list,
            canSend = canSend,
            canCancel = canCancel,
            canRetry = canRetry
        )
    }
}
