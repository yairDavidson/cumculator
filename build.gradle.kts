plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    //id("com.google.gms.google-services") apply false  // Firebase plugin
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Optional, older style, you can remove if using plugins {}
        classpath("com.google.gms:google-services:4.4.0")
    }
}
