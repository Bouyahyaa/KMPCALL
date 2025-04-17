package com.bouyahya.kmpcall.ui

import com.bouyahya.kmpcall.domain.Connection

data class CallState(
    val connectionList: List<Connection> = emptyList(),
)
