# KotlinCalculator — project notes for agents

## What this is

A Kotlin + Jetpack Compose Android calculator app, the counterpart to
`../swift-calculator`. One Gradle module (`:app`), no third-party dependencies
beyond AndroidX/Compose, no code generation, no backend.

- Package / application id: `com.example.kotlincalculator`
- Namespace: `com.example.kotlincalculator`
- compileSdk / targetSdk 34, minSdk 26
- Toolchain: JDK 17, Android Gradle Plugin 8.7.3, Gradle 8.9 (wrapper),
  Kotlin 2.0.21, Compose BOM 2024.06.00
- Versions live in `gradle/libs.versions.toml`; use the version catalog when
  adding a dependency.

## Layout

| File | Role |
| --- | --- |
| `app/src/main/java/com/example/kotlincalculator/calculator/CalculatorState.kt` | Immutable calculator state machine + `Operation` enum. No Android imports. |
| `app/src/main/java/com/example/kotlincalculator/ui/CalculatorScreen.kt` | Display, 4-column keypad, `Key` button, keypad colors, previews |
| `app/src/main/java/com/example/kotlincalculator/ui/theme/Theme.kt` | Material 3 light/dark schemes and the calculator orange |
| `app/src/main/java/com/example/kotlincalculator/MainActivity.kt` | Entry point, edge-to-edge, Compose content |
| `app/src/main/res/` | Manifest, themes, strings, adaptive launcher icon |
| `app/src/test/.../CalculatorStateTest.kt` | JVM tests for every engine transition |

## Build, test, run

`local.properties` holds the machine's `sdk.dir` and is gitignored. If Gradle
cannot find the Android SDK, create it with the right path.

```bash
./gradlew :app:assembleDebug        # compile + package
./gradlew :app:testDebugUnitTest    # JVM tests, no emulator
./gradlew :app:installDebug         # install on a running device/emulator
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

The emulator used for screenshots is a Pixel 5 API 34 AVD:

```bash
~/Library/Android/sdk/emulator/emulator -avd Pixel_5_API_34 &
~/Library/Android/sdk/platform-tools/adb wait-for-device
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
~/Library/Android/sdk/platform-tools/adb shell am start -n com.example.kotlincalculator/.MainActivity
~/Library/Android/sdk/platform-tools/adb exec-out screencap -p > tmp/scratch/calculator.png
```

## Conventions

- Put all arithmetic and state transitions in `CalculatorState` and return a
  **new** state from every button function. Keep it free of Android/Compose
  imports so it stays unit-testable.
- Add a JVM test in `CalculatorStateTest` for every new engine behavior.
  `press(vararg keys)` in that file maps button names to engine calls.
- The display string is formatted in one place: the private `format` /
  `formatTyped` helpers in `CalculatorState`. Do not format numbers in the UI.
- Colors: use `MaterialTheme.colorScheme` for surfaces/text, but keypad colors
  come from `keyColors()` in `CalculatorScreen` so the layout matches iOS.
- `rememberSaveable` requires `CalculatorState` to stay `Serializable` (and its
  fields to remain saveable). Keep that in mind when adding state.
- Keep `tmp/` for scratch files — it is gitignored.
