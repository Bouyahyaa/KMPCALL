package com.bouyahya.kmpcall.ui

import com.shepeliev.webrtckmp.MediaStream

interface CallEvent {
    data class Connect(val stream: MediaStream) : CallEvent
}