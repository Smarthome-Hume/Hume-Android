# Hume Android

Native Android port of the Hume SwiftUI smart-home app for Samsung Galaxy S26 Ultra-class devices.

## Stack
- Kotlin
- Jetpack Compose + Material 3
- MVVM with StateFlow
- OkHttp WebSocket + REST for Home Assistant
- DataStore for local settings
- Coil / Media3 planned for cameras and Frigate

## Current status
This is the first Android skeleton port:
- 5 native tabs: Home, Energy, Security, Profile, AI
- Home Assistant URL/token login
- REST `/api/states`
- WebSocket `/api/websocket` auth + `state_changed` subscription
- Basic entity cache and service calls
- Initial Compose UI matching Hume structure

## Run
Open in Android Studio, let Gradle sync, then run on a connected Samsung device.
