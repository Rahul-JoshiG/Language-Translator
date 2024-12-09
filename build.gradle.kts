// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false

}
buildscript {
    repositories {
        google() // Required for ML Kit
        mavenCentral()
    }
    dependencies {
        classpath ("com.android.tools.build:gradle:8.7.3")
        classpath ("com.android.tools.build:gradle-api:8.7.2")
        classpath ("com.google.gms:google-services:4.4.0")

    }
}

