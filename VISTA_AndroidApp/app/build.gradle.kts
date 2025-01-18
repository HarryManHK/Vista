plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.vista"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.vista"
        minSdk = 28
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Libraries from version catalogs, for example:
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Socket.IO client
    implementation("io.socket:socket.io-client:2.1.0") {
        exclude(group = "org.json", module = "json") // if needed
    }
}