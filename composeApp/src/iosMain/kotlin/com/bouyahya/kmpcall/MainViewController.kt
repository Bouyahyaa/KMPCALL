package com.bouyahya.kmpcall

import androidx.compose.ui.window.ComposeUIViewController
import com.bouyahya.kmpcall.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}