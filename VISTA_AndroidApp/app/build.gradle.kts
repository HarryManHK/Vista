plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.vista"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.vista"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Libraries from version catalogs, for example:
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
    implementation(libs.cardview)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.okhttp3)

    // Socket.IO client (not in catalog)
    implementation("io.socket:socket.io-client:2.1.0") {
        exclude(group = "org.json", module = "json") // if needed
    }

    implementation ("androidx.core:core:1.12.0")

    implementation(libs.osmdroid)
}