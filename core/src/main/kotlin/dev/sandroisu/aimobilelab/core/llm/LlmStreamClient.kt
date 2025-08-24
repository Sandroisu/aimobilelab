package dev.sandroisu.aimobilelab.core.llm

import kotlinx.coroutines.flow.Flow

interface LlmStreamClient {
    fun stream(id: String, attempt: Int): Flow<StreamEnvelope>
}
