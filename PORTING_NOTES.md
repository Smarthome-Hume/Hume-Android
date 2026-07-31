# Hume SwiftUI → Android Porting Notes

## Samsung native design
Samsung does not have a separate mandatory native UI framework for third-party Android apps comparable to SwiftUI/UIKit. For native Samsung Galaxy apps, use standard Android native UI:

- Jetpack Compose
- Material 3
- Android adaptive layouts
- Android system notifications/widgets

Samsung-specific additions are optional: One UI design inspiration, Samsung Remote Test Lab, SmartThings SDK if needed, and Galaxy Store assets if publishing there.

## iOS to Android mapping
| SwiftUI/iOS | Android |
|---|---|
| SwiftUI Views | Jetpack Compose Composables |
| @Observable / @State | StateFlow + ViewModel + Compose state |
| UserDefaults | DataStore |
| Keychain | Android Keystore / EncryptedSharedPreferences |
| URLSession REST | OkHttp / Retrofit |
| URLSessionWebSocketTask | OkHttp WebSocket |
| ActivityKit Live Activity | Foreground notification / rich notification |
| WidgetKit | Android App Widgets / Glance |
| HomeKit | No direct native Android equivalent; use Home Assistant / Frigate / Matter where possible |
| AVKit | Media3 ExoPlayer |
| PhotosUI | Android Photo Picker |
| Vision QR | ML Kit Barcode Scanner |
| SFSpeechRecognizer | Android SpeechRecognizer |
| AVSpeechSynthesizer | Android TextToSpeech |

## Next phases
1. Add ViewModels and lifecycle-aware reconnect.
2. Port complete entity visibility/rate-limit bucket logic.
3. Port Home UI cards and room bottom sheets.
4. Port Energy charts and flow cards.
5. Port Frigate camera snapshots/clips with Coil + Media3.
6. Port AI providers and tool-calling.
7. Add Android widgets and rich notifications.
