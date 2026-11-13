package com.example.webservicedemo.data.model

data class HttpRequestConfig(
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "https://jsonplaceholder.typicode.com/posts/1",
    val headers: Map<String, String> = mapOf(
        "Accept" to "application/json",
        "User-Agent" to "Android-WebServiceDemo/1.0"
    ),
    val body: String? = null,
    val engine: HttpEngineType = HttpEngineType.OKHTTP
)
