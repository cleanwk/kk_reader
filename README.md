# KK Reader

A free, open-source Android book reader app with text-to-speech support.

## Features

- **Import local books**: EPUB, PDF, and TXT formats
- **Text-to-Speech**: Powered by Sherpa-ONNX with downloadable open-source voice models (Piper, Kokoro, VITS) + Android System TTS fallback
- **Sleep timer**: Auto-stop reading after a set duration
- **Reader customization**: Adjustable font size, line spacing, and theme (Light/Dark/Sepia)
- **Library management**: Sort by title, author, last read, or date added

## Tech Stack

- Kotlin + Jetpack Compose + Material3
- MVVM + Clean Architecture
- Hilt (DI), Room (DB), DataStore (preferences)
- epub4j (EPUB), PdfBox-Android (PDF)
- Sherpa-ONNX (offline TTS)
- Min SDK 26 (Android 8.0)

## Building

```bash
./gradlew assembleDebug
```

## CI/CD

- Push to `master` builds a debug APK
- Push a `v*` tag creates a signed release APK on GitHub Releases
