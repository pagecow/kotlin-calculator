# Project memory

Durable lessons recorded by coding agents working in this project. The engine reads this file at the start of every session and shows it to the next agent — append anything the next session should know.


## 2026-10-19 11:01:14

## Kotlin Calculator (created 2026-10-19)

- Repo: https://github.com/pagecow/kotlin-calculator (public, matching swift-calculator). Package `com.example.kotlincalculator`.
- Toolchain pinned deliberately for compileSdk 34: JDK 17, Gradle 8.9 wrapper, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.06.00. Lint's "newer version available" warnings are expected; do not bump AGP to 9.x without also migrating the build.
- `app/src/main/java/.../calculator/CalculatorState.kt` is the pure engine (Serializable for `rememberSaveable`); keep it free of Android imports. Every button returns a new state. Tests: `./gradlew :app:testDebugUnitTest` (26 tests).
- The test helper `press(vararg keys)` types an Int with more than one digit digit-by-digit; pass 200, not `2,0,0`.
- Launcher icon gotcha: `<adaptive-icon>` MUST stay in `res/mipmap-anydpi-v26/`. Moving it to `res/mipmap-anydpi/` (as lint's ObsoleteSdkInt suggests) makes AAPT fail with "resource mipmap/ic_launcher not found". Leave that lint warning alone; monochrome tag is in both v26 icons.
- This machine's git is 2.23 and does NOT support `git init -b`; use `git init` then `git checkout -b main`.
- The global Gradle is 6.6.1 and cannot run on Java 17, so the wrapper was generated with `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-11.jdk/Contents/Home gradle wrapper --gradle-version 8.9`.
- Emulator verification: a Pixel_XL_API_36 AVD was already running; app installed/launched cleanly, `7 × 8 =` showed 56, dark-mode recreation preserved state. Screenshots: `docs/screenshot-light.png` and `docs/screenshot-dark.png`.
- APK output: `app/build/outputs/apk/debug/app-debug.apk`.
