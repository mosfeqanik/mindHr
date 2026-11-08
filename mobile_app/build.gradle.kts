plugins {
    // AGP 9 compiles Kotlin itself (built-in Kotlin), so the kotlin-android plugin is no longer applied.
    id("com.android.application") version "9.5.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
}
