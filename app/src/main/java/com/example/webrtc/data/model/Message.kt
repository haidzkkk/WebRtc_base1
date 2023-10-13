package com.example.webrtc.data.model

data class Message(
    val type: String? = null,
    val name: String? = null,
    val target: String? = null,
    val data: Any? = null
)