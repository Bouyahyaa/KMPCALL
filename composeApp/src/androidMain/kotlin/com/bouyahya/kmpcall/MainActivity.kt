package com.bouyahya.kmpcall

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bouyahya.kmpcall.di.initKoin
import com.shepeliev.webrtckmp.WebRtc
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.stopKoin
import org.webrtc.Loggable
import org.webrtc.Logging

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initKoin {
            androidContext(applicationContext)
            androidLogger()
        }

        val initializationOptionsBuilder = WebRtc.createInitializationOptionsBuilder()
            .setInjectableLogger(WebRtcLogger, Logging.Severity.LS_ERROR)
        val peerConnectionFactoryBuilder = WebRtc.createPeerConnectionFactoryBuilder(initializationOptionsBuilder)
        WebRtc.configure(peerConnectionFactoryBuilder = peerConnectionFactoryBuilder)

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopKoin()
    }
}



private object WebRtcLogger : Loggable {
    override fun onLogMessage(message: String, severity: Logging.Severity, tag: String) {
        when (severity) {
            Logging.Severity.LS_ERROR -> Log.e(tag, message)
            Logging.Severity.LS_WARNING -> Log.w(tag, message)
            Logging.Severity.LS_INFO -> Log.i(tag, message)
            Logging.Severity.LS_VERBOSE -> Log.v(tag, message)
            Logging.Severity.LS_NONE -> {}
        }
    }
}