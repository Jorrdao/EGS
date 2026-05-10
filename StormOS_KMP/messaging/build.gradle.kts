import com.google.protobuf.gradle.*

plugins {
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")              // Room annotation processing
    alias(libs.plugins.protobuf)
    // NOTE: No Hilt, no KSP — the messaging module uses manual DI.
    // Hilt lives only in :composeApp which has @HiltAndroidApp.
    // Room annotation processing is handled via kapt in the app module.
}

android {
    namespace  = "com.messaging.service"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        buildConfigField("String", "ONLINE_BASE_URL", "\"http://10.0.2.2:8080/\"")
    }

    buildTypes {
        debug   { buildConfigField("boolean", "KPI_VERBOSE", "true") }
        release { buildConfigField("boolean", "KPI_VERBOSE", "false") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions { jvmTarget = "11" }
    buildFeatures { buildConfig = true }

    sourceSets {
        getByName("main") {
            proto { srcDir("src/main/proto") }
            // Explicitly register generated proto sources so Android Studio
            // resolves them without needing a full Gradle sync first.
            java.srcDirs(
                "build/generated/source/proto/debug/java",
                "build/generated/source/proto/debug/kotlin"
            )
        }
    }
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.25.2" }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java")   { option("lite") }
                create("kotlin") { option("lite") }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)       // generates MessageDatabase_Impl at build time

    // Protobuf
    implementation(libs.protobuf.kotlin.lite)

    // Embedded HTTP server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.gson)

    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.security.crypto)
}

// Ensure generated proto sources are registered on the compile classpath
// for all build variants. Needed with AGP 8.x + protobuf plugin 0.9.x.
afterEvaluate {
    android.libraryVariants.forEach { variant ->
        val capitalize = variant.name.replaceFirstChar { it.uppercase() }
        val protoTask  = tasks.findByName("generate${capitalize}Proto")
            ?: return@forEach
        variant.registerJavaGeneratingTask(
            protoTask,
            file("build/generated/source/proto/${variant.name}/java"),
            file("build/generated/source/proto/${variant.name}/kotlin")
        )
    }
}