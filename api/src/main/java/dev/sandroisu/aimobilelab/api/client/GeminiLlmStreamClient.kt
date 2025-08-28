package dev.sandroisu.aimobilelab.api.client

import dev.sandroisu.aimobilelab.api.responses.GmContent
import dev.sandroisu.aimobilelab.api.responses.GmGenerateRequest
import dev.sandroisu.aimobilelab.api.responses.GmGenerateResponse
import dev.sandroisu.aimobilelab.api.responses.GmPart
import dev.sandroisu.aimobilelab.core.chat.ChatMessage
import dev.sandroisu.aimobilelab.core.chat.Role
import dev.sandroisu.aimobilelab.core.chat.StreamEvent
import dev.sandroisu.aimobilelab.core.llm.LlmStreamClient
import dev.sandroisu.aimobilelab.core.llm.StreamEnvelope
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.headers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class GeminiLlmStreamClient(
    private val apiKeyProvider: () -> String,
    private val model: String = "gemini-2.5-flash",
    private val json: Json = Json { ignoreUnknownKeys = true },
) : LlmStreamClient {
    private val client =
        HttpClient(OkHttp) {
            install(SSE)
            install(Logging)
        }

    override fun stream(
        id: String,
        attempt: Int,
        history: List<ChatMessage>,
    ): Flow<StreamEnvelope> =
        flow {
            val key = "$id#$attempt"
            emit(StreamEnvelope(key, StreamEvent.Start(id, attempt)))
            val request = GmGenerateRequest(contents = history.map { it.toGmContent() })
            val body = json.encodeToString(request)
            val url =
                buildString {
                    append("https://generativelanguage.googleapis.com/v1beta/models/")
                    append(model)
                    append(":streamGenerateContent?alt=sse&key=")
                    append(apiKeyProvider())
                }
            client.sse(
                request = {
                    url(url)
                    method = HttpMethod.Post
                    contentType(ContentType.Application.Json)
                    headers { append("Accept", "text/event-stream") }
                    setBody(body)
                },
            ) {
                incoming.collect { event ->
                    val data = event.data
                    if (data.isNullOrEmpty()) return@collect
                    val resp = runCatching { json.decodeFromString<GmGenerateResponse>(data) }.getOrNull()
                    val delta =
                        resp
                            ?.candidates
                            ?.firstOrNull()
                            ?.content
                            ?.parts
                            ?.mapNotNull { it.text }
                            ?.joinToString("")
                    if (!delta.isNullOrEmpty()) {
                        emit(StreamEnvelope(key, StreamEvent.Delta(delta)))
                    }
                }
            }
            emit(StreamEnvelope(key, StreamEvent.End))
        }.catch { e ->
            val key = "$id#$attempt"
            emit(StreamEnvelope(key, StreamEvent.Error(e.message ?: "network_error")))
        }

    private fun ChatMessage.toGmContent(): GmContent {
        val roleStr =
            when (role) {
                Role.User -> "user"
                Role.Assistant -> "model"
            }
        return GmContent(role = roleStr, parts = listOf(GmPart(text = text)))
    }
}
