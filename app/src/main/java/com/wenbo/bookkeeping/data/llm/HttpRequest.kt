package com.wenbo.bookkeeping.data.llm

/**
 * 大模型 HTTP 请求描述：协议编解码（纯函数）产出，HttpTransport 消费。
 * 把"构造什么请求"与"怎么发出去"分开，前者可单测。
 */
data class HttpRequest(
    val url: String,
    val body: String,
    val headers: Map<String, String>,
)
