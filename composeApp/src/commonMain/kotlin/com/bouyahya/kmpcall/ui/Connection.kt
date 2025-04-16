package com.bouyahya.kmpcall.ui

import com.shepeliev.webrtckmp.AudioStreamTrack
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.VideoStreamTrack

data class Connection(
    val userId: String,
    val name: String,
    val peerConnection: PeerConnection?,
    val remoteVideoTrack: VideoStreamTrack? = null,
    val remoteAudioTrack: AudioStreamTrack? = null,
    val videoEnabled: Boolean? = true,
    val audioEnabled: Boolean? = true,
)