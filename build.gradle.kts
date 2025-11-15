// Top-level build.gradle.kts
// Use aliases for all plugins to ensure versions are managed centrally in libs.versions.toml
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Optional: Keep this task for cleaning the build directory
tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
