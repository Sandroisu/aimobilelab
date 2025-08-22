package dev.sandroisu.aimobilelab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sandroisu.aimobilelab.core.chat.AssistantMessageReducer
import dev.sandroisu.aimobilelab.core.chat.AssistantMessageState
import dev.sandroisu.aimobilelab.core.chat.StreamEvent
import dev.sandroisu.aimobilelab.core.llm.LLMStreamClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(private val llmStreamClient: LLMStreamClient) : ViewModel() {

    private var activeKey: String? = null
    private var job: Job? = null

    private val _screenState = MutableStateFlow<AssistantMessageState?>(null)
    val screenState: StateFlow<AssistantMessageState?> = _screenState.asStateFlow()

    fun startRequest(id: String, attempt: Int) {
        job?.cancel()
        activeKey = "$id#$attempt"
        _screenState.update { AssistantMessageReducer.reduce(it, StreamEvent.Start(id, attempt)) }
        job = viewModelScope.launch {
            llmStreamClient.stream(
                id = id,
                attempt = attempt,
            ).collect { event ->
                if (event.key != activeKey) return@collect
                _screenState. update { AssistantMessageReducer.reduce(it, event.streamEvent) }
            }
        }
    }

    fun cancelRequest() {
        job?.cancel()
        job = null
        activeKey = null
        _screenState.update { AssistantMessageReducer.reduce(it, StreamEvent.Error("canceled")) }
    }
}
