package com.example.webservicedemo.data.repository

import com.example.webservicedemo.data.model.HttpEngineType
import com.example.webservicedemo.data.model.HttpMethod
import com.example.webservicedemo.data.model.HttpRequestConfig
import com.example.webservicedemo.data.model.HttpResponseResult
import com.example.webservicedemo.data.model.PostItem
import com.example.webservicedemo.data.network.HttpServiceEngine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebServiceRepository {

    private val gson = Gson()
    private val baseUrl = "https://jsonplaceholder.typicode.com"

    suspend fun fetchPosts(engine: HttpEngineType): Pair<List<PostItem>, HttpResponseResult> = withContext(Dispatchers.IO) {
        val config = HttpRequestConfig(
            method = HttpMethod.GET,
            url = "$baseUrl/posts",
            headers = mapOf("Accept" to "application/json"),
            engine = engine
        )
        val response = HttpServiceEngine.execute(config)
        val posts = if (response.isSuccessful && response.body.isNotBlank()) {
            try {
                val listType = object : TypeToken<List<PostItem>>() {}.type
                gson.fromJson<List<PostItem>>(response.body, listType) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        posts to response
    }

    suspend fun createPost(
        title: String,
        body: String,
        userId: Int = 1,
        engine: HttpEngineType
    ): Pair<PostItem?, HttpResponseResult> = withContext(Dispatchers.IO) {
        val jsonBody = gson.toJson(mapOf("title" to title, "body" to body, "userId" to userId))
        val config = HttpRequestConfig(
            method = HttpMethod.POST,
            url = "$baseUrl/posts",
            headers = mapOf(
                "Content-Type" to "application/json; charset=UTF-8",
                "Accept" to "application/json"
            ),
            body = jsonBody,
            engine = engine
        )
        val response = HttpServiceEngine.execute(config)
        val post = if (response.isSuccessful && response.body.isNotBlank()) {
            try {
                gson.fromJson(response.body, PostItem::class.java)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
        post to response
    }

    suspend fun updatePost(
        id: Int,
        title: String,
        body: String,
        userId: Int = 1,
        engine: HttpEngineType
    ): Pair<PostItem?, HttpResponseResult> = withContext(Dispatchers.IO) {
        val jsonBody = gson.toJson(mapOf("id" to id, "title" to title, "body" to body, "userId" to userId))
        val config = HttpRequestConfig(
            method = HttpMethod.PUT,
            url = "$baseUrl/posts/$id",
            headers = mapOf(
                "Content-Type" to "application/json; charset=UTF-8",
                "Accept" to "application/json"
            ),
            body = jsonBody,
            engine = engine
        )
        val response = HttpServiceEngine.execute(config)
        val post = if (response.isSuccessful && response.body.isNotBlank()) {
            try {
                gson.fromJson(response.body, PostItem::class.java)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
        post to response
    }

    suspend fun deletePost(id: Int, engine: HttpEngineType): HttpResponseResult = withContext(Dispatchers.IO) {
        val config = HttpRequestConfig(
            method = HttpMethod.DELETE,
            url = "$baseUrl/posts/$id",
            headers = mapOf("Accept" to "application/json"),
            engine = engine
        )
        HttpServiceEngine.execute(config)
    }

    suspend fun executeCustom(config: HttpRequestConfig): HttpResponseResult {
        return HttpServiceEngine.execute(config)
    }
}
