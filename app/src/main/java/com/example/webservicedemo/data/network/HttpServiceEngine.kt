package com.example.webservicedemo.data.network

import com.example.webservicedemo.data.model.HttpEngineType
import com.example.webservicedemo.data.model.HttpMethod
import com.example.webservicedemo.data.model.HttpRequestConfig
import com.example.webservicedemo.data.model.HttpResponseResult
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object HttpServiceEngine {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val prettyGson by lazy {
        GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    }

    suspend fun execute(config: HttpRequestConfig): HttpResponseResult = withContext(Dispatchers.IO) {
        val curl = buildCurlCommand(config)
        try {
            when (config.engine) {
                HttpEngineType.OKHTTP -> executeOkHttp(config, curl)
                HttpEngineType.HTTP_URL_CONNECTION -> executeHttpURLConnection(config, curl)
            }
        } catch (e: Exception) {
            HttpResponseResult(
                statusCode = 0,
                statusMessage = "Network Request Failed",
                durationMs = 0L,
                headers = emptyMap(),
                body = "",
                isSuccessful = false,
                errorReason = e.localizedMessage ?: e.javaClass.simpleName,
                responseSizeBytes = 0L,
                engineUsed = config.engine,
                curlEquivalent = curl
            )
        }
    }

    private fun executeOkHttp(config: HttpRequestConfig, curl: String): HttpResponseResult {
        val startTime = System.currentTimeMillis()
        val requestBuilder = Request.Builder().url(config.url)

        // Headers
        config.headers.forEach { (key, value) ->
            if (key.isNotBlank()) {
                requestBuilder.header(key.trim(), value.trim())
            }
        }

        // Body
        val mediaType = (config.headers["Content-Type"] ?: "application/json; charset=utf-8").toMediaTypeOrNull()
        val requestBody = config.body?.toRequestBody(mediaType)

        when (config.method) {
            HttpMethod.GET -> requestBuilder.get()
            HttpMethod.POST -> requestBuilder.post(requestBody ?: ByteArray(0).toRequestBody(mediaType))
            HttpMethod.PUT -> requestBuilder.put(requestBody ?: ByteArray(0).toRequestBody(mediaType))
            HttpMethod.PATCH -> requestBuilder.patch(requestBody ?: ByteArray(0).toRequestBody(mediaType))
            HttpMethod.DELETE -> if (requestBody != null) requestBuilder.delete(requestBody) else requestBuilder.delete()
            HttpMethod.HEAD -> requestBuilder.head()
        }

        val request = requestBuilder.build()
        val response = okHttpClient.newCall(request).execute()
        val duration = System.currentTimeMillis() - startTime

        response.use { resp ->
            val responseBody = resp.body?.string() ?: ""
            val headersMap = mutableMapOf<String, String>()
            for (i in 0 until resp.headers.size) {
                headersMap[resp.headers.name(i)] = resp.headers.value(i)
            }

            return HttpResponseResult(
                statusCode = resp.code,
                statusMessage = resp.message.ifBlank { getStandardStatusMessage(resp.code) },
                durationMs = duration,
                headers = headersMap,
                body = responseBody,
                isSuccessful = resp.isSuccessful,
                errorReason = if (!resp.isSuccessful) "HTTP Error ${resp.code}: ${resp.message}" else null,
                responseSizeBytes = responseBody.toByteArray(Charsets.UTF_8).size.toLong(),
                protocol = resp.protocol.toString().uppercase(),
                engineUsed = HttpEngineType.OKHTTP,
                curlEquivalent = curl
            )
        }
    }

    private fun executeHttpURLConnection(config: HttpRequestConfig, curl: String): HttpResponseResult {
        val startTime = System.currentTimeMillis()
        val url = URL(config.url)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            instanceFollowRedirects = true
            requestMethod = config.method.name

            // Headers
            config.headers.forEach { (k, v) ->
                if (k.isNotBlank()) {
                    setRequestProperty(k.trim(), v.trim())
                }
            }
        }

        try {
            // Write request body if applicable
            if (config.method.supportsBody && !config.body.isNullOrEmpty()) {
                connection.doOutput = true
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(config.body)
                    writer.flush()
                }
            }

            val statusCode = connection.responseCode
            val statusMessage = connection.responseMessage ?: getStandardStatusMessage(statusCode)
            val duration = System.currentTimeMillis() - startTime

            // Extract headers
            val headersMap = mutableMapOf<String, String>()
            connection.headerFields?.forEach { (key, values) ->
                if (key != null) {
                    headersMap[key] = values.joinToString(", ")
                }
            }

            // Read response body stream (either inputStream or errorStream)
            val stream = if (statusCode in 200..399) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseBody = stream?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: ""

            return HttpResponseResult(
                statusCode = statusCode,
                statusMessage = statusMessage,
                durationMs = duration,
                headers = headersMap,
                body = responseBody,
                isSuccessful = statusCode in 200..299,
                errorReason = if (statusCode !in 200..299) "HTTP Error $statusCode: $statusMessage" else null,
                responseSizeBytes = responseBody.toByteArray(Charsets.UTF_8).size.toLong(),
                protocol = "HTTP/1.1",
                engineUsed = HttpEngineType.HTTP_URL_CONNECTION,
                curlEquivalent = curl
            )
        } finally {
            connection.disconnect()
        }
    }

    fun formatJson(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            val element = JsonParser.parseString(raw)
            prettyGson.toJson(element)
        } catch (_: Exception) {
            raw // Not valid JSON, return raw
        }
    }

    fun buildCurlCommand(config: HttpRequestConfig): String {
        val sb = StringBuilder("curl -X ").append(config.method.name)
        config.headers.forEach { (k, v) ->
            if (k.isNotBlank()) {
                sb.append(" -H \"").append(k).append(": ").append(v).append("\"")
            }
        }
        if (config.method.supportsBody && !config.body.isNullOrBlank()) {
            val escapedBody = config.body.replace("\"", "\\\"").replace("\n", " ")
            sb.append(" -d \"").append(escapedBody).append("\"")
        }
        sb.append(" \"").append(config.url).append("\"")
        return sb.toString()
    }

    fun getStandardStatusMessage(code: Int): String = when (code) {
        200 -> "OK"
        201 -> "Created"
        202 -> "Accepted"
        204 -> "No Content"
        301 -> "Moved Permanently"
        302 -> "Found"
        304 -> "Not Modified"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        405 -> "Method Not Allowed"
        408 -> "Request Timeout"
        429 -> "Too Many Requests"
        500 -> "Internal Server Error"
        502 -> "Bad Gateway"
        503 -> "Service Unavailable"
        504 -> "Gateway Timeout"
        else -> if (code in 200..299) "Success" else "HTTP Error"
    }
}
