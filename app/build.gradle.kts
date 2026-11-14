import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(keystorePropertiesFile.inputStream())
    }
}

android {
    namespace = "com.questterm"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.questterm"
        minSdk = 29
        targetSdk = 34
        versionCode = 5
        versionName = "1.0.4"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            val storeFilePath = keystoreProperties["storeFile"] as? String
            if (!storeFilePath.isNullOrEmpty()) {
                create("release") {
                    storeFile = file(storeFilePath)
                    storePassword = keystoreProperties["storePassword"] as? String ?: ""
                    keyAlias = keystoreProperties["keyAlias"] as? String ?: ""
                    keyPassword = keystoreProperties["keyPassword"] as? String ?: ""
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // SSH - using ConnectBot's sshlib (battle-tested on Android)
    implementation("org.connectbot:sshlib:2.2.25")

    // Terminal emulation (Termux terminal-emulator, GPL v3)
    implementation("com.github.termux.termux-app:terminal-emulator:0.118.1")

    // AndroidX appcompat (needed by forked Termux terminal-view)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Navigation
    implementation(libs.navigation.compose)

    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Core Android
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
