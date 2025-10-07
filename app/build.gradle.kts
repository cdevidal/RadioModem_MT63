plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.radiomodem.mt63"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.radiomodem.mt63"
        minSdk = 29
        targetSdk = 35
        versionCode = 6
        versionName = "1.5.1"
        externalNativeBuild {
            cmake { cppFlags += "" }
        }
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt") }
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}
dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.core:core-ktx:1.15.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
    // Charts kept for statistics
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}

