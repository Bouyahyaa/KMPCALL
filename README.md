# KMPCALL - Kotlin Multiplatform Communication App

## 🚀 Quick Start

1. Update your server IP address in:
/composeApp/src/commonMain/kotlin/network/SocketClient.kt


2. Run the backend server first (see [KMPCALL-BACKEND](https://github.com/Bouyahyaa/KMPCALL-BACKEND))

## Project Structure

This is a Kotlin Multiplatform project targeting Android and iOS:

* `/composeApp` - Shared code across platforms:
  - `commonMain`: Common code for all targets
  - Platform-specific folders (e.g., `androidMain`, `iosMain`)

* `/iosApp` - iOS application entry point (contains SwiftUI code if needed)

## Learn More
[Kotlin Multiplatform Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
