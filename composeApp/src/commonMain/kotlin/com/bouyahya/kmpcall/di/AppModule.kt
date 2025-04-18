package com.bouyahya.kmpcall.di

import org.koin.core.module.dsl.viewModel
import com.bouyahya.kmpcall.core.webrtc.SocketClient
import com.bouyahya.kmpcall.core.network.createHttpClient
import com.bouyahya.kmpcall.ui.CallViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module


val appModule
    get() =
        module {
            single { createHttpClient() }
            single { SocketClient() }
            viewModel { CallViewModel(get()) }
        }

fun initKoin(enableNetworkLogs: Boolean = false, appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(
            appModule,
        )
    }