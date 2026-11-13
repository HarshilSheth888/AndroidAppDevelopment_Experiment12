package com.example.webservicedemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.webservicedemo.data.model.HttpEngineType
import com.example.webservicedemo.data.model.HttpMethod
import com.example.webservicedemo.data.model.HttpRequestConfig
import com.example.webservicedemo.data.model.HttpResponseResult
import com.example.webservicedemo.data.model.PostItem
import com.example.webservicedemo.data.repository.WebServiceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FeedUiState {
    object Idle : FeedUiState()
    object Loading : FeedUiState()
    data class Success(val allPosts: List<PostItem>, val displayedPosts: List<PostItem>, val response: HttpResponseResult) : FeedUiState()
    data class Error(val message: String, val response: HttpResponseResult?) : FeedUiState()
}

sealed class RequestUiState {
    object Idle : RequestUiState()
    object Loading : RequestUiState()
    data class Result(val response: HttpResponseResult) : RequestUiState()
}

class MainViewModel(
    private val repository: WebServiceRepository = WebServiceRepository()
) : ViewModel() {

    private val _engineType = MutableStateFlow(HttpEngineType.OKHTTP)
    val engineType: StateFlow<HttpEngineType> = _engineType.asStateFlow()

    private val _feedState = MutableStateFlow<FeedUiState>(FeedUiState.Idle)
    val feedState: StateFlow<FeedUiState> = _feedState.asStateFlow()

    private val _requestState = MutableStateFlow<RequestUiState>(RequestUiState.Idle)
    val requestState: StateFlow<RequestUiState> = _requestState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var allCachedPosts: List<PostItem> = emptyList()
    private var lastFeedResponse: HttpResponseResult? = null
    var currentSearchQuery: String = ""
        private set

    init {
        loadFeed()
    }

    fun setEngineType(engine: HttpEngineType) {
        _engineType.value = engine
    }

    fun toggleEngine() {
        _engineType.value = if (_engineType.value == HttpEngineType.OKHTTP) {
            HttpEngineType.HTTP_URL_CONNECTION
        } else {
            HttpEngineType.OKHTTP
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            _feedState.value = FeedUiState.Loading
            val (posts, response) = repository.fetchPosts(_engineType.value)
            lastFeedResponse = response
            if (response.isSuccessful) {
                allCachedPosts = posts
                filterPosts(currentSearchQuery)
            } else {
                _feedState.value = FeedUiState.Error(
                    response.errorReason ?: "Failed to fetch posts (${response.statusCode})",
                    response
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query
        filterPosts(query)
    }

    private fun filterPosts(query: String) {
        val filtered = if (query.isBlank()) {
            allCachedPosts
        } else {
            allCachedPosts.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.body.contains(query, ignoreCase = true) ||
                it.id.toString() == query.trim()
            }
        }
        val response = lastFeedResponse ?: HttpResponseResult()
        _feedState.value = FeedUiState.Success(allCachedPosts, filtered, response)
    }

    fun createPost(title: String, body: String, onResult: (Boolean, HttpResponseResult) -> Unit) {
        viewModelScope.launch {
            val (newPost, response) = repository.createPost(title, body, 1, _engineType.value)
            if (response.isSuccessful && newPost != null) {
                // Add to start of list
                allCachedPosts = listOf(newPost) + allCachedPosts
                filterPosts(currentSearchQuery)
                _toastEvent.emit("HTTP POST Succeeded! Created item #${newPost.id}")
                onResult(true, response)
            } else {
                _toastEvent.emit("HTTP POST Failed: ${response.statusCode} ${response.statusMessage}")
                onResult(false, response)
            }
        }
    }

    fun updatePost(id: Int, title: String, body: String, onResult: (Boolean, HttpResponseResult) -> Unit) {
        viewModelScope.launch {
            val (updatedPost, response) = repository.updatePost(id, title, body, 1, _engineType.value)
            if (response.isSuccessful && updatedPost != null) {
                allCachedPosts = allCachedPosts.map { if (it.id == id) updatedPost else it }
                filterPosts(currentSearchQuery)
                _toastEvent.emit("HTTP PUT Succeeded! Updated item #$id")
                onResult(true, response)
            } else {
                _toastEvent.emit("HTTP PUT Failed: ${response.statusCode}")
                onResult(false, response)
            }
        }
    }

    fun deletePost(id: Int, onResult: (Boolean, HttpResponseResult) -> Unit) {
        viewModelScope.launch {
            val response = repository.deletePost(id, _engineType.value)
            if (response.isSuccessful) {
                allCachedPosts = allCachedPosts.filter { it.id != id }
                filterPosts(currentSearchQuery)
                _toastEvent.emit("HTTP DELETE Succeeded for item #$id")
                onResult(true, response)
            } else {
                _toastEvent.emit("HTTP DELETE Failed: ${response.statusCode}")
                onResult(false, response)
            }
        }
    }

    fun sendCustomRequest(config: HttpRequestConfig) {
        viewModelScope.launch {
            _requestState.value = RequestUiState.Loading
            val result = repository.executeCustom(config)
            _requestState.value = RequestUiState.Result(result)
        }
    }
}
