plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqlDelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            
            // Disable Bitcode (deprecated in iOS 26)
            binaryOption("bundleId", "com.eunio.healthapp.shared")
            
            // Export dependencies that are used in public API
            export(libs.kotlinx.coroutines.core)
            export(libs.kotlinx.datetime)
            export(libs.kotlinx.serialization.json)
        }
        
        // Configure compiler args for iOS 26 compatibility
        iosTarget.compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    // Support for arm64 and x86_64 simulator architectures
                    freeCompilerArgs.add("-Xbinary=bundleId=com.eunio.healthapp.shared")
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Coroutines - exported for iOS
            api(libs.kotlinx.coroutines.core)
            
            // Serialization - exported for iOS
            api(libs.kotlinx.serialization.json)
            
            // DateTime - exported for iOS
            api(libs.kotlinx.datetime)
            
            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            
            // Koin
            implementation(libs.koin.core)
            
            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
        }
        
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(libs.sqldelight.android.driver)
            
            // Firebase - specify versions explicitly for now
            implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
            implementation("com.google.firebase:firebase-firestore-ktx:24.10.0")
            implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
            implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.0")
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation("io.insert-koin:koin-test:4.0.0")
        }
        
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.mockk)
                implementation(libs.mockk.android)
                implementation("androidx.arch.core:core-testing:2.2.0")
                implementation("org.robolectric:robolectric:4.11.1")
            }
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
        

    }
}

android {
    namespace = "com.eunio.healthapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

sqldelight {
    databases {
        create("EunioDatabase") {
            packageName.set("com.eunio.healthapp.database")
        }
    }
}