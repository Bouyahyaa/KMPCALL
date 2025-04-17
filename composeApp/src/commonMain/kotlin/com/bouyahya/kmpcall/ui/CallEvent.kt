package com.bouyahya.kmpcall.ui

import com.shepeliev.webrtckmp.MediaStream

interface CallEvent {
    data class Connect(val stream: MediaStream) : CallEvent
    data class ToggleAudio(val value: Boolean) : CallEvent
    data class ToggleVideo(val value: Boolean) : CallEvent
    data object LeaveCall : CallEvent
}