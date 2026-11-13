package com.example.webservicedemo.data.model

enum class HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD;

    val supportsBody: Boolean
        get() = this == POST || this == PUT || this == PATCH
}
