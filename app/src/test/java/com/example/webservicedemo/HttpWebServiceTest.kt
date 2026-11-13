package com.example.webservicedemo

import com.example.webservicedemo.data.model.HttpEngineType
import com.example.webservicedemo.data.model.HttpMethod
import com.example.webservicedemo.data.model.HttpRequestConfig
import com.example.webservicedemo.data.model.HttpResponseResult
import com.example.webservicedemo.data.model.PostItem
import com.example.webservicedemo.data.network.HttpServiceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpWebServiceTest {

    @Test
    fun testBuildCurlCommand_get() {
        val config = HttpRequestConfig(
            method = HttpMethod.GET,
            url = "https://jsonplaceholder.typicode.com/posts/1",
            headers = mapOf("Accept" to "application/json")
        )
        val curl = HttpServiceEngine.buildCurlCommand(config)
        assertTrue(curl.contains("curl -X GET"))
        assertTrue(curl.contains("-H \"Accept: application/json\""))
        assertTrue(curl.contains("\"https://jsonplaceholder.typicode.com/posts/1\""))
    }

    @Test
    fun testBuildCurlCommand_postWithBody() {
        val config = HttpRequestConfig(
            method = HttpMethod.POST,
            url = "https://jsonplaceholder.typicode.com/posts",
            headers = mapOf("Content-Type" to "application/json"),
            body = "{\"title\":\"test\",\"body\":\"content\"}"
        )
        val curl = HttpServiceEngine.buildCurlCommand(config)
        assertTrue(curl.contains("curl -X POST"))
        assertTrue(curl.contains("-H \"Content-Type: application/json\""))
        assertTrue(curl.contains("-d \"{\\\"title\\\":\\\"test\\\",\\\"body\\\":\\\"content\\\"}\""))
    }

    @Test
    fun testFormatJson() {
        val unformatted = "{\"id\":1,\"title\":\"test\"}"
        val formatted = HttpServiceEngine.formatJson(unformatted)
        assertTrue(formatted.contains("\n"))
        assertTrue(formatted.contains("\"id\": 1"))
        assertTrue(formatted.contains("\"title\": \"test\""))
    }

    @Test
    fun testHttpResponseResult_statusClassifications() {
        val success200 = HttpResponseResult(statusCode = 200)
        assertTrue(success200.isSuccess)
        assertFalse(success200.isClientError)
        assertFalse(success200.isServerError)

        val created201 = HttpResponseResult(statusCode = 201)
        assertTrue(created201.isSuccess)

        val notFound404 = HttpResponseResult(statusCode = 404)
        assertFalse(notFound404.isSuccess)
        assertTrue(notFound404.isClientError)
        assertFalse(notFound404.isServerError)

        val serverError500 = HttpResponseResult(statusCode = 500)
        assertFalse(serverError500.isSuccess)
        assertFalse(serverError500.isClientError)
        assertTrue(serverError500.isServerError)
    }

    @Test
    fun testPostItemDataClass() {
        val post = PostItem(id = 10, userId = 2, title = "Kotlin HTTP", body = "Demonstrating web services")
        assertEquals(10, post.id)
        assertEquals(2, post.userId)
        assertEquals("Kotlin HTTP", post.title)
        assertEquals("Demonstrating web services", post.body)
    }

    @Test
    fun testEngineDisplayNames() {
        assertEquals("OkHttp 4", HttpEngineType.OKHTTP.displayName)
        assertEquals("HttpURLConnection", HttpEngineType.HTTP_URL_CONNECTION.displayName)
    }
}
