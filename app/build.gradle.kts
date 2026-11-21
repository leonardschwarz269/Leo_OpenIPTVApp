import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("com.google.devtools.ksp")
}

// Release signing: keystore.properties (gitignored) или env KEYSTORE_*; без ключа — unsigned APK
val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("keystore.properties")
if (keystorePropsFile.exists()) {
    keystorePropsFile.inputStream().use { keystoreProps.load(it) }
}
val hasReleaseKey = keystoreProps.getProperty("storeFile") != null ||
    System.getenv("KEYSTORE_FILE") != null

android {
    namespace = "com.kraftplay.openiptv"
    compileSdk = 35

    if (hasReleaseKey) {
        signingConfigs {
            create("release") {
                storeFile = file(
                    keystoreProps.getProperty("storeFile") ?: System.getenv("KEYSTORE_FILE")!!
                )
                storePassword = keystoreProps.getProperty("storePassword")
                    ?: System.getenv("KEYSTORE_PASSWORD")
                keyAlias = keystoreProps.getProperty("keyAlias")
                    ?: System.getenv("KEY_ALIAS")
                keyPassword = keystoreProps.getProperty("keyPassword")
                    ?: System.getenv("KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "com.kraftplay.openiptv"
        minSdk = 24
        targetSdk = 35
        versionCode = 18
        versionName = "6.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Тестовая сборка ставится РЯДОМ с оригинальным OpenIPTV, не поверх него
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        // OpenIptvCarActivity.java:149 — pattern matching instanceof (Java 16+)
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // ponytail: Instantiatable — false positive для legacy aauto.aar CarActivity (суперкласс
    // из старого Android Auto SDK не виден lint'у). Отключён точечно; вернуть при переходе на androidx.car.app.
    lint {
        disable += "Instantiatable"
        abortOnError = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Media3 (ExoPlayer)
    val media3Version = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")

    // Room (EPG cache) — используется androidx.room.* в model/
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Android Auto: официальный car-app (OpenIptvCarServiceFS) +
    // bundled legacy AA SDK (aauto.aar, flatDir app/src/main/lib из settings)
    implementation("androidx.car.app:app:1.4.0")
    implementation(mapOf("name" to "aauto", "ext" to "aar"))

    // Xposed API — только компиляция LSPosedHook, в APK не включается
    compileOnly(files("src/main/lib/xposed-api-82.jar"))

    // com.google.common.* (ListenableFuture) в legacy car-коде
    implementation("com.google.guava:guava:33.0.0-android")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // OkHttp for networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.6")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.01.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
