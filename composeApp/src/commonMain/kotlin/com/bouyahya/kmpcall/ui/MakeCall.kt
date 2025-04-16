package com.bouyahya.kmpcall.ui

import com.shepeliev.webrtckmp.AudioStreamTrack
import com.shepeliev.webrtckmp.IceCandidate
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.MediaStreamTrackKind
import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.SignalingState
import com.shepeliev.webrtckmp.VideoStreamTrack
import com.shepeliev.webrtckmp.onConnectionStateChange
import com.shepeliev.webrtckmp.onIceCandidate
import com.shepeliev.webrtckmp.onIceConnectionStateChange
import com.shepeliev.webrtckmp.onSignalingStateChange
import com.shepeliev.webrtckmp.onTrack
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

suspend fun makeCall(
    peerConnections: Pair<PeerConnection, PeerConnection>,
    localStream: MediaStream,
    onRemoteVideoTrack: (VideoStreamTrack) -> Unit,
    onRemoteAudioTrack: (AudioStreamTrack) -> Unit = {},
): Nothing = coroutineScope {
    val (pc1, pc2) = peerConnections
    localStream.tracks.forEach { pc1.addTrack(it) }
    val pc1IceCandidates = mutableListOf<IceCandidate>()
    val pc2IceCandidates = mutableListOf<IceCandidate>()
    pc1.onIceCandidate
        .onEach {
            if (pc2.signalingState == SignalingState.HaveRemoteOffer) {
                pc2.addIceCandidate(it)
            } else {
                pc1IceCandidates.add(it)
            }
        }
        .launchIn(this)
    pc2.onIceCandidate
        .onEach {
            if (pc1.signalingState == SignalingState.HaveRemoteOffer) {
                pc1.addIceCandidate(it)
            } else {
                pc2IceCandidates.add(it)
            }
        }
        .launchIn(this)
    pc1.onSignalingStateChange
        .onEach { signalingState ->
            if (signalingState == SignalingState.HaveRemoteOffer) {
                pc2IceCandidates.forEach { pc1.addIceCandidate(it) }
                pc2IceCandidates.clear()
            }
        }
        .launchIn(this)
    pc2.onSignalingStateChange
        .onEach { signalingState ->
            if (signalingState == SignalingState.HaveRemoteOffer) {
                pc1IceCandidates.forEach { pc2.addIceCandidate(it) }
                pc1IceCandidates.clear()
            }
        }
        .launchIn(this)
    pc1.onIceConnectionStateChange
        .launchIn(this)
    pc2.onIceConnectionStateChange
        .launchIn(this)
    pc1.onConnectionStateChange
        .launchIn(this)
    pc2.onConnectionStateChange
        .launchIn(this)
    pc1.onTrack
        .launchIn(this)
    pc2.onTrack
        .map { it.track }
        .filterNotNull()
        .onEach {
            if (it.kind == MediaStreamTrackKind.Audio) {
                onRemoteAudioTrack(it as AudioStreamTrack)
            } else if (it.kind == MediaStreamTrackKind.Video) {
                onRemoteVideoTrack(it as VideoStreamTrack)
            }
        }
        .launchIn(this)
    val offer = pc1.createOffer(OfferAnswerOptions(offerToReceiveVideo = true, offerToReceiveAudio = true))
    pc1.setLocalDescription(offer)
    pc2.setRemoteDescription(offer)
    val answer = pc2.createAnswer(options = OfferAnswerOptions())
    pc2.setLocalDescription(answer)
    pc1.setRemoteDescription(answer)

    awaitCancellation()
}