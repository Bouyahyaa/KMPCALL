package com.bouyahya.kmpcall.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bouyahya.kmpcall.core.network.SocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallViewModel(
    private val socketClient: SocketClient,
    val state: MutableStateFlow<CallState> = MutableStateFlow(CallState()),
) : ViewModel() {

    init {
        collectConnections()
        collectVideoTracks()
        collectAudioTracks()
    }

    fun onEvent(event: CallEvent) {
        when (event) {
            is CallEvent.Connect -> connect(event)
            is CallEvent.ToggleAudio -> toggleAudio(event)
            is CallEvent.ToggleVideo -> toggleVideo(event)
        }
    }

    fun connect(event: CallEvent.Connect) =
        viewModelScope.launch {
            socketClient.connect(event.stream)
        }

    fun toggleAudio(event: CallEvent.ToggleAudio) =
        viewModelScope.launch {
            socketClient.toggleAudio(event.value)
        }

    fun toggleVideo(event: CallEvent.ToggleVideo) =
        viewModelScope.launch {
            socketClient.toggleVideo(event.value)
        }

    private fun collectConnections() =
        viewModelScope.launch {
            socketClient.connectionsEventFlow.collect { connections ->
                state.update {
                    it.copy(
                        connectionList = connections
                    )
                }
            }
        }

    private fun collectVideoTracks() =
        viewModelScope.launch {
            socketClient.remoteVideoTrack.collect { v ->
                state.update {
                    it.copy(
                        connectionList = it.connectionList.map { c ->
                            if (c.userId == v.first)
                                c.copy(remoteVideoTrack = v.second) else
                                c
                        }
                    )
                }
            }
        }

    private fun collectAudioTracks() =
        viewModelScope.launch {
            socketClient.remoteAudioTrack.collect { a ->
                state.update {
                    it.copy(
                        connectionList = it.connectionList.map { c ->
                            if (c.userId == a.first)
                                c.copy(remoteAudioTrack = a.second) else
                                c
                        }
                    )
                }
            }
        }
}