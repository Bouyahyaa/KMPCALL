package com.bouyahya.kmpcall.core.network

import com.bouyahya.kmpcall.core.network.webrtc.AnswerSdpData
import com.bouyahya.kmpcall.core.network.webrtc.IceCandidateData
import com.bouyahya.kmpcall.core.network.webrtc.MessagePayload
import com.bouyahya.kmpcall.core.network.webrtc.OfferSdpData
import com.bouyahya.kmpcall.core.network.webrtc.RTCSessionDescription
import com.bouyahya.kmpcall.core.network.webrtc.UserJoinedData
import com.bouyahya.kmpcall.ui.Connection
import com.piasy.kmp.socketio.socketio.IO
import com.piasy.kmp.socketio.socketio.Socket
import com.shepeliev.webrtckmp.AudioStreamTrack
import com.shepeliev.webrtckmp.IceCandidate
import com.shepeliev.webrtckmp.IceServer
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.MediaStreamTrackKind
import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.RtcConfiguration
import com.shepeliev.webrtckmp.SessionDescription
import com.shepeliev.webrtckmp.SessionDescriptionType
import com.shepeliev.webrtckmp.VideoStreamTrack
import com.shepeliev.webrtckmp.onIceCandidate
import com.shepeliev.webrtckmp.onTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SocketClient {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val callId: String = "b0d1ee04-caed-456d-9a61-398f1e0764d4"
    private val userId: String = Uuid.random().toString()

    private var localStream: MediaStream? = null

    private var connections = mutableListOf<Connection>()
    private val connectionsEvent = MutableSharedFlow<List<Connection>>()
    val connectionsEventFlow = connectionsEvent.asSharedFlow()

    private val _remoteVideoTrack = MutableSharedFlow<Pair<String, VideoStreamTrack>>()
    val remoteVideoTrack = _remoteVideoTrack.asSharedFlow()

    private val _remoteAudioTrack = MutableSharedFlow<Pair<String, AudioStreamTrack>>()
    val remoteAudioTrack = _remoteAudioTrack.asSharedFlow()

    val json = Json { ignoreUnknownKeys = true }

    fun connect(stream: MediaStream) = scope.launch {
        IO.socket(
            uri = "http://192.168.3.62:4000",
            opt = IO.Options(),
        ) { socket ->
            with(socket) { listenEvents(socket) }
            socket.open()
        }

        localStream = stream
    }

    fun listenEvents(socket: Socket) {
        socket.on(Socket.EVENT_CONNECT) {
            socket.emit(
                "CONNECT", JsonObject(
                    mapOf(
                        "userID" to JsonPrimitive(userId),
                        "SOCKET_ID" to JsonPrimitive(socket.id),
                    )
                )
            )

            socket.emit(
                "JOIN_ROOM", JsonObject(
                    mapOf(
                        "groupID" to JsonPrimitive(callId),
                    )
                )
            )

            socket.on("new message") { args ->
                val payloadData = json.decodeFromString<MessagePayload>(
                    args.firstOrNull().toString()
                )

                handleMessage(payloadData, socket)
            }

            socket.on("new message solo") { args ->
                val payloadData = json.decodeFromString<MessagePayload>(
                    args.firstOrNull().toString()
                )

                handleMessage(payloadData, socket)
            }

            // Join the call
            join(socket)
        }
    }

    fun join(socket: Socket) =
        sendMessage(
            "user-joined",
            JsonObject(
                mapOf(
                    "userID" to JsonPrimitive(userId),
                    "name" to JsonPrimitive(userId),
                    "config" to JsonObject(
                        mapOf(
                            "audioEnabled" to JsonPrimitive(true),
                            "videoEnabled" to JsonPrimitive(true),
                        )
                    )
                )
            ),
            socket,
        )

    fun handleMessage(payload: MessagePayload, socket: Socket) {
        if (payload.data["userID"]?.jsonPrimitive?.content == userId) return
        when (payload.type) {
            "user-joined" -> {
                val userJoinedData = json.decodeFromString<UserJoinedData>(payload.data.toString())
                userJoined(userJoinedData, socket)
            }

            "connection-request" -> {
                val userJoinedData = json.decodeFromString<UserJoinedData>(payload.data.toString())
                receivedConnectionRequest(userJoinedData, socket)
            }

            "offer-sdp" -> {
                val offerSdpData = json.decodeFromString<OfferSdpData>(payload.data.toString())
                receivedOfferSdp(offerSdpData, socket)
            }

            "answer-sdp" -> {
                val answerSdpData = json.decodeFromString<AnswerSdpData>(payload.data.toString())
                receivedAnswerSdp(answerSdpData)
            }

            "icecandidate" -> {
                val icaCandidateData = json.decodeFromString<IceCandidateData>(payload.data.toString())
                setIceCandidate(icaCandidateData)
            }

//            "user-left" -> handleUserLeft(payload.data)
//            "video-toggle" -> handleVideoToggle(payload.data)
//            "audio-toggle" -> handleAudioToggle(payload.data)
        }
    }

    fun userJoined(data: UserJoinedData, socket: Socket) {
        val connection = createConnection(data, socket)
        sendConnectionRequest(connection.userId, socket)
    }

    fun sendConnectionRequest(
        otherUserId: String,
        socket: Socket
    ) = sendMessage(
        type = "connection-request",
        data = JsonObject(
            mapOf(
                "userID" to JsonPrimitive(userId),
                "name" to JsonPrimitive(userId),
                "otherUserId" to JsonPrimitive(otherUserId),
                "config" to JsonObject(
                    mapOf(
                        "audioEnabled" to JsonPrimitive(true),
                        "videoEnabled" to JsonPrimitive(true),
                    )
                )
            )
        ),
        socket = socket,
        userId = otherUserId
    )

    fun receivedConnectionRequest(data: UserJoinedData, socket: Socket) {
        val previousConnectionIndex = connections
            .indexOfFirst { connection -> connection.userId == data.userID }
        if (previousConnectionIndex == -1) {
            createConnection(data, socket)
            sendOfferSdp(data.userID, socket)
        }
    }

    fun sendOfferSdp(otherUserId: String, socket: Socket) =
        scope.launch {
            val connection = getConnection(otherUserId)
            val sdp = connection.peerConnection?.createOffer(
                options = OfferAnswerOptions(
                    offerToReceiveAudio = true,
                    offerToReceiveVideo = true,
                )
            )

            sdp?.let { d ->
                connection.peerConnection.setLocalDescription(d)
                sendMessage(
                    type = "offer-sdp",
                    data = JsonObject(
                        mapOf(
                            "userID" to JsonPrimitive(userId),
                            "name" to JsonPrimitive(userId),
                            "otherUserId" to JsonPrimitive(otherUserId),
                            "sdp" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive(d.type.name),
                                    "sdp" to JsonPrimitive(d.sdp),
                                )
                            )
                        )
                    ),
                    socket = socket,
                    userId = otherUserId
                )
            }
        }

    fun receivedOfferSdp(data: OfferSdpData, socket: Socket) =
        sendAnswerSdp(
            data.userID,
            data.sdp,
            socket
        )

    fun sendAnswerSdp(
        otherUserId: String,
        sdp: RTCSessionDescription,
        socket: Socket
    ) = scope.launch {
        val connection = getConnection(otherUserId)
        connection.peerConnection?.setRemoteDescription(
            SessionDescription(
                type = SessionDescriptionType.entries.first { it.name == sdp.type },
                sdp = sdp.sdp,
            )
        )
        val answerSdp = connection.peerConnection?.createAnswer(
            options = OfferAnswerOptions(
                offerToReceiveAudio = true,
                offerToReceiveVideo = true,
            )
        )

        answerSdp?.let { s ->
            connection.peerConnection.setLocalDescription(s)
            sendMessage(
                type = "answer-sdp",
                data = JsonObject(
                    mapOf(
                        "userID" to JsonPrimitive(userId),
                        "name" to JsonPrimitive(userId),
                        "otherUserId" to JsonPrimitive(otherUserId),
                        "sdp" to JsonObject(
                            mapOf(
                                "type" to JsonPrimitive(s.type.name),
                                "sdp" to JsonPrimitive(s.sdp),
                            )
                        )
                    )
                ),
                socket = socket,
                userId = otherUserId
            )
        }
    }

    fun receivedAnswerSdp(data: AnswerSdpData) =
        scope.launch {
            val connection = getConnection(data.userID)
            connection.peerConnection?.setRemoteDescription(
                SessionDescription(
                    type = SessionDescriptionType.entries.first { it.name == data.sdp.type },
                    sdp = data.sdp.sdp,
                )
            )
        }

    fun setIceCandidate(data: IceCandidateData) =
        scope.launch {
            val connection = getConnection(data.userID)
            connection.peerConnection?.addIceCandidate(
                IceCandidate(
                    candidate = data.candidate.candidate,
                    sdpMid = data.candidate.sdpMid,
                    sdpMLineIndex = data.candidate.sdpMLineIndex,
                )
            )
        }

    fun sendIceCandidate(
        otherUserId: String,
        iceCandidate: IceCandidate,
        socket: Socket
    ) = sendMessage(
        type = "icecandidate",
        data = JsonObject(
            mapOf(
                "userID" to JsonPrimitive(userId),
                "name" to JsonPrimitive(userId),
                "otherUserId" to JsonPrimitive(otherUserId),
                "candidate" to JsonObject(
                    mapOf(
                        "candidate" to JsonPrimitive(iceCandidate.candidate),
                        "sdpMid" to JsonPrimitive(iceCandidate.sdpMid),
                        "sdpMLineIndex" to JsonPrimitive(iceCandidate.sdpMLineIndex),
                    )
                )
            )
        ),
        socket = socket,
        userId = otherUserId
    )

    private fun createConnection(
        userJoinedData: UserJoinedData,
        socket: Socket
    ): Connection {
        val configuration = RtcConfiguration(
            iceServers = listOf(
                IceServer(
                    urls = listOf("turn:turn.linkefoot.fr"),
                    username = "admin",
                    password = "1NJKj8TQntReoZ3",
                )
            )
        )

        val peerConnection = PeerConnection(configuration)

        // Add local media tracks if available
        localStream?.tracks?.forEach { track ->
            peerConnection.addTrack(track)
        }

        // Handle ICE Candidates
        peerConnection.onIceCandidate
            .onEach { iceCandidate ->
                sendIceCandidate(userJoinedData.userID, iceCandidate, socket)
            }
            .launchIn(scope)

        // Handle incoming remote tracks
        peerConnection.onTrack
            .map { it.track }
            .filterNotNull()
            .onEach { track ->
                when (track.kind) {
                    MediaStreamTrackKind.Audio -> {
                        connections = connections.map {
                            if (it.userId == userJoinedData.userID) {
                                it.copy(remoteAudioTrack = track as AudioStreamTrack)
                            } else {
                                it
                            }
                        }.toMutableList()

                        _remoteAudioTrack.emit(Pair(userJoinedData.userID, track as AudioStreamTrack))
                    }

                    MediaStreamTrackKind.Video -> {
                        connections = connections.map {
                            if (it.userId == userJoinedData.userID) {
                                it.copy(remoteVideoTrack = track as VideoStreamTrack)
                            } else {
                                it
                            }
                        }.toMutableList()

                        _remoteVideoTrack.emit(Pair(userJoinedData.userID, track as VideoStreamTrack))
                    }
                }
            }
            .launchIn(scope)

        val connection = Connection(
            userId = userJoinedData.userID,
            name = userJoinedData.name,
            peerConnection = peerConnection,
            videoEnabled = userJoinedData.config.videoEnabled,
            audioEnabled = userJoinedData.config.audioEnabled,
        )

        connections.add(connection)


        scope.launch { connectionsEvent.emit(connections.toList()) }

        return connection
    }

    fun getConnection(userId: String): Connection = connections.first { connection -> connection.userId == userId }

    fun sendMessage(
        type: String,
        data: JsonObject,
        socket: Socket,
        userId: String = "",
    ) {
        val requestMap = mutableMapOf<String, JsonElement>()
        requestMap["type"] = JsonPrimitive(type)
        requestMap["data"] = data

        if (userId.isNotEmpty())
            requestMap["userID"] = JsonPrimitive(userId) else
            requestMap["groupID"] = JsonPrimitive(callId)

        val payload = JsonObject(requestMap)

        socket.emit("MESSAGE", payload)
    }
}