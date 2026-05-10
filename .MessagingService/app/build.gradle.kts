import com.google.protobuf.gradle.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.protobuf")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.messaging.service"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.messaging.service"
        minSdk = 26           // Android 8.0 – BLE GATT reliable baseline
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose exported AIDL so other apps can bind
        buildConfigField("String", "ONLINE_BASE_URL", "\"http://10.0.2.2:8080/\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("boolean", "KPI_VERBOSE", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "KPI_VERBOSE", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        aidl      = true   // Required for IPC with other apps
        buildConfig = true
        viewBinding = true
    }

    // Proto-generated sources must be on the compile classpath
    sourceSets {
        getByName("main") {
            proto {
                srcDir("src/main/proto")
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Protobuf configuration
// IMPORTANT: use protobuf-javalite (not full) + kotlin-lite for Android
// ──────────────────────────────────────────────────────────────────────────────
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.2"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    // ── Core ──────────────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ── Hilt (DI) ─────────────────────────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")

    // ── Embedded HTTP server (exposes REST API to other apps on-device) ───────
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-cio:2.3.7")           // CIO engine – lightweight, no Netty
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-gson:2.3.7")
    implementation("io.ktor:ktor-server-status-pages:2.3.7")  // error handling
    implementation("io.ktor:ktor-server-call-logging:2.3.7")  // request logging

    // ── Networking ────────────────────────────────────────────────────────────
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ── Protobuf (lite – mandatory for Android) ───────────────────────────────
    // protobuf-javalite keeps method count / dex size minimal
    implementation("com.google.protobuf:protobuf-kotlin-lite:3.25.2")

    // ── Room (local message cache) ────────────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── Security (AES encryption for BLE payloads) ────────────────────────────
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ── Swagger / OpenAPI annotations (for inline code documentation) ─────────
    // These are compile-time annotation only; no runtime overhead on device
    compileOnly("io.swagger.core.v3:swagger-annotations:2.2.19")

    // ── WorkManager (background sync scheduling) ──────────────────────────────
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // ── Test ─────────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
