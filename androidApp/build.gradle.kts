plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    id("com.google.firebase.crashlytics") version "2.9.9"
    id("com.google.firebase.firebase-perf") version "1.4.2"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.material3)
            implementation("androidx.compose.material:material-icons-extended:1.7.6")
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            
            // DateTime
            implementation(libs.kotlinx.datetime)
            
            // Firebase - specify versions explicitly for now
            implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
            implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
            implementation("com.google.firebase:firebase-firestore-ktx:24.10.0")
            implementation("com.google.firebase:firebase-functions-ktx:20.4.0")
            implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.0")
            implementation("com.google.firebase:firebase-perf-ktx:20.5.1")
        }
        
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlin.test)
                implementation(libs.koin.core)
                implementation(libs.koin.android)
                implementation(libs.mockk)
                implementation("androidx.test:core:1.5.0")
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("androidx.arch.core:core-testing:2.2.0")
                implementation(project(":shared"))
            }
        }
        
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.espresso.core)
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.compose.ui.test.manifest)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.eunio.healthapp.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.eunio.healthapp.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}