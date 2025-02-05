import org.gradle.kotlin.dsl.android
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")

}

android {
    val properties = Properties()
    val file = FileInputStream("local.properties")
    properties.load(file)

    namespace = "com.languagetranslator.languagetranslator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.languagetranslator.languagetranslator"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0.0"


        buildConfigField("String", "GEMINI_API_KEY", properties.getProperty("GEMINI_API_KEY"))

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    dataBinding {
        enable = true
    }

    buildFeatures {
        buildConfig = true // Ensure BuildConfig generation is enabled
    }
}

dependencies {
    // AndroidX and UI dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Firebase dependencies
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)

    // ML Kit dependencies (latest stable versions)
    implementation("com.google.mlkit:translate:17.0.1")
    implementation("com.google.mlkit:language-id:17.0.4")
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Play Services ML Kit dependencies
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
    implementation("com.google.android.gms:play-services-mlkit-language-id:17.0.0")

    // Coil for image loading (latest stable version)
    implementation(libs.coil)

    // Google AI generative library
    implementation(libs.generativeai)

    //dagger hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    //size
    implementation (libs.sdp.android)
    implementation (libs.ssp.android)


    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
// Allow references to generated code
kapt {
    correctErrorTypes = true
}