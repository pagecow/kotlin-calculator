// Top-level build file. Plugins are declared here (without applying them) so
// every module can use the same versions from gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 has built-in Kotlin, so there is no kotlin-android plugin here.
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}
