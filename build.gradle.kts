plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
buildscript {
    repositories {
        google() // Required for ML Kit
        mavenCentral()
    }
    dependencies {
        classpath (libs.gradle)
        classpath (libs.gradle.api)
        classpath (libs.google.services)
    }
}