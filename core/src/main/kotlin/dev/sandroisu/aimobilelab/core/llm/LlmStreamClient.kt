package dev.sandroisu.aimobilelab.core.llm

import dev.sandroisu.aimobilelab.core.chat.ChatMessage
import kotlinx.coroutines.flow.Flow

interface LlmStreamClient {
    fun stream(
        id: String,
        attempt: Int,
        history: List<ChatMessage>,
    ): Flow<StreamEnvelope>
}
