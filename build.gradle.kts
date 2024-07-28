buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.google.services)
        classpath("com.google.gms:google-services:4.3.15")
        classpath("com.google.android.gms:play-services-maps:18.0.0")
        classpath("com.google.android.gms:play-services-location:21.0.0")
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
}