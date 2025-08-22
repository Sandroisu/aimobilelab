package dev.sandroisu.aimobilelab.core.llm

import kotlinx.coroutines.flow.Flow

interface LLMStreamClient {
    fun stream(id: String, attempt: Int): Flow<StreamEnvelope>
}
