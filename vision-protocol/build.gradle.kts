// DS-F4 — first extracted module. `vision-protocol` holds the pure, Android-free wire/graph
// contract (`core/graph`: VisionMessage, ContentPart, GraphState, GraphEvent, Node, the runtime
// + Checkpointer interface). Package names are preserved (`com.kianirani.jarvis.core.graph`) so
// every existing import in :app keeps working — this is the non-breaking first step of the
// gradual monorepo split. KMP-ready (no Android/Hilt deps, only kotlinx).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.kianirani.jarvis.protocol"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)
}
