package dev.sandroisu.aimobilelab.api.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GmPart(
    @SerialName("text")
    val text: String? = null,
)

@Serializable
data class GmContent(
    @SerialName("role")
    val role: String? = null,
    @SerialName("parts")
    val parts: List<GmPart>,
)

@Serializable
data class GmGenerateRequest(
    @SerialName("contents")
    val contents: List<GmContent>,
)

@Serializable
data class GmTextPart(
    @SerialName("text")
    val text: String? = null,
)

@Serializable
data class GmContentResp(
    @SerialName("parts")
    val parts: List<GmTextPart>? = null,
)

@Serializable
data class GmCandidate(
    @SerialName("content")
    val content: GmContentResp? = null,
)

@Serializable
data class GmGenerateResponse(
    @SerialName("candidates")
    val candidates: List<GmCandidate>? = null,
)
