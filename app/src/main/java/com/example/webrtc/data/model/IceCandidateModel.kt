package com.example.webrtc.data.model

data class IceCandidateModel(
    val sdpMid: String,
    val sdpCandidate: String,
    val sdpMLineIndex: Double
)