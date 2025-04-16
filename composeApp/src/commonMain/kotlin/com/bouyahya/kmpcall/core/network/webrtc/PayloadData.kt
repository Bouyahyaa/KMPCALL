package com.bouyahya.kmpcall.core.network.webrtc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Config(
    val videoEnabled: Boolean,
    val audioEnabled: Boolean,
)

@Serializable
data class UserJoinedData(
    val userID: String,
    val name: String,
    val config: Config,
)

@Serializable
data class OfferSdpData(
    val userID: String,
    val name: String,
    val sdp: RTCSessionDescription
)

@Serializable
data class AnswerSdpData(
    val userID: String,
    val name: String,
    val sdp: RTCSessionDescription
)

@Serializable
data class RTCSessionDescription(
    val sdp: String,
    val type: String,
)

@Serializable
data class UserLeftData(
    val userID: String,
    val name: String,
)

@Serializable
data class IceCandidateData(
    val userID: String,
    val name: String,
    val candidate: RTCIceCandidate
)

@Serializable
data class RTCIceCandidate(
    val candidate: String,
    val sdpMid: String,
    val sdpMLineIndex: Int,
)

@Serializable
data class VideoToggleData(
    val userID: String,
    val videoEnabled: Boolean,
)

@Serializable
data class AudioToggleData(
    val userID: String,
    val audioEnabled: Boolean,
)

@Serializable
data class ConnectSocket(
    val userID: String,
    val audioEnabled: Boolean,
)

@Serializable
data class MessagePayload(
    val type: String,
    val data: JsonObject,
)