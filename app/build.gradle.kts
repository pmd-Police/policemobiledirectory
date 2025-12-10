plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    alias(libs.plugins.hilt)
    id("org.jetbrains.kotlin.kapt")

    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.policemobiledirectory"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.policemobiledirectory"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
        
        // ⚠️ SECURITY: Add secret token via gradle.properties or CI/CD secrets
        // In gradle.properties: APPS_SCRIPT_SECRET_TOKEN=your_secret_here
        val secretToken = project.findProperty("APPS_SCRIPT_SECRET_TOKEN") as? String ?: ""
        buildConfigField("String", "APPS_SCRIPT_SECRET_TOKEN", "\"$secretToken\"")
        
        // App signature hash for verification (set via CI/CD)
        val signatureHash = project.findProperty("EXPECTED_SIGNATURE_HASH") as? String ?: ""
        buildConfigField("String", "EXPECTED_SIGNATURE_HASH", "\"$signatureHash\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xjvm-default=all",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"

            // FIX DUPLICATE CLASSES AND STAX PROVIDERS
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"

            // Critical: Exclude standard Java StAX providers so Android uses Aalto XML
            excludes += "META-INF/services/javax.xml.stream.XMLEventFactory"
            excludes += "META-INF/services/javax.xml.stream.XMLInputFactory"
            excludes += "META-INF/services/javax.xml.stream.XMLOutputFactory"

            // FIX: Use 'pickFirsts +=' instead of 'pickFirst()'
            pickFirsts += "org/apache/xmlbeans/impl/regex/message_*.properties"
            pickFirsts += "schemaorg_apache_xmlbeans/**"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { 
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

dependencies {

    // Required for POI DOCX on minSdk 24+
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Core + Lifecycle
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.navigation.compose)
    
    // Material3 window size class (from BOM)
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation(libs.compose.material.icons)
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")


    implementation("androidx.multidex:multidex:2.0.1")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil
    implementation(libs.coil.compose)
    implementation("io.coil-kt:coil-svg:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Google Sign-In
    implementation(libs.play.identity)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    implementation("commons-io:commons-io:2.15.1")

    // uCrop
    implementation("com.github.yalantis:ucrop:2.2.8")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")

    // PDF
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // --- POI / DOCX SUPPORT (UPDATED) ---

    // 1. Aalto XML: Faster StAX parser, required for Android
    implementation("com.fasterxml:aalto-xml:1.3.2")

    // 2. Standard Apache POI (Latest stable)
    implementation("org.apache.poi:poi-ooxml:5.2.5") {
        // Exclude standard XML APIs that conflict with Android
        exclude(group = "xml-apis", module = "xml-apis")
        // Exclude Log4j API if not used, to save size
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }

    // 3. Logging bridge (Prevents crashes related to Log4j)
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.20.0")

    // --- END POI SECTION ---

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
