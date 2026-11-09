package com.wenbo.bookkeeping.data.llm

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 最小 HTTP 传输层：HttpURLConnection 零依赖。仅收编网络 IO，不做业务解析。 */
internal object HttpTransport {

    fun post(request: HttpRequest): String {
        val conn = (URL(request.url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 90_000 // 视觉模型响应较慢
            doOutput = true
            request.headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            conn.outputStream.use { it.write(request.body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            if (code !in 200..299) {
                throw IOException("HTTP $code：${text.take(200)}")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }
}
