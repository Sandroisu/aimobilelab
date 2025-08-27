package dev.sandroisu.aimobilelab.api.responses

import kotlinx.serialization.Serializable

@Serializable
data class GmPart(
    val text: String? = null,
)

@Serializable
data class GmContent(
    val role: String? = null,
    val parts: List<GmPart>,
)

@Serializable
data class GmGenerateRequest(
    val contents: List<GmContent>,
)

@Serializable
data class GmTextPart(
    val text: String? = null,
)

@Serializable
data class GmContentResp(
    val parts: List<GmTextPart>? = null,
)

@Serializable
data class GmCandidate(
    val content: GmContentResp? = null,
)

@Serializable
data class GmGenerateResponse(
    val candidates: List<GmCandidate>? = null,
)
