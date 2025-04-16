package com.bouyahya.kmpcall.core.network

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(): HttpClient
