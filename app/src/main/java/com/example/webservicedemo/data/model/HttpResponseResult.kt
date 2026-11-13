package com.example.webservicedemo.data.model

data class HttpResponseResult(
    val statusCode: Int = 0,
    val statusMessage: String = "",
    val durationMs: Long = 0L,
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
    val isSuccessful: Boolean = false,
    val errorReason: String? = null,
    val responseSizeBytes: Long = 0L,
    val protocol: String = "HTTP/1.1",
    val engineUsed: HttpEngineType = HttpEngineType.OKHTTP,
    val curlEquivalent: String = ""
) {
    val formattedSize: String
        get() = when {
            responseSizeBytes < 1024 -> "$responseSizeBytes B"
            responseSizeBytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", responseSizeBytes / 1024.0)
            else -> String.format(java.util.Locale.US, "%.2f MB", responseSizeBytes / (1024.0 * 1024.0))
        }

    val isClientError: Boolean
        get() = statusCode in 400..499

    val isServerError: Boolean
        get() = statusCode in 500..599

    val isSuccess: Boolean
        get() = statusCode in 200..299

    val isRedirect: Boolean
        get() = statusCode in 300..399
}
