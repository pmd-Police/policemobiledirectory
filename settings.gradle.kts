pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // ✅ Added for uCrop and other GitHub libs
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // ✅ Added for uCrop and POI
    }
}

rootProject.name = "PoliceMobileDirectory" // I inferred the name from your file path
include(":app")