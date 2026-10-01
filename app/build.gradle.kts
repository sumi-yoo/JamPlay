import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.util.Properties
import java.io.FileInputStream

var properties = Properties()
properties.load(FileInputStream("local.properties"))

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.sumi.jamplay"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = file(properties.getProperty("RELEASE_STORE_FILE"))
            storePassword = properties.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = properties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = properties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "com.sumi.jamplay"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val jamendoClientId: String = gradleLocalProperties(rootDir, providers)
            .getProperty("JAMENDO_CLIENT_ID", "")
        buildConfigField("String", "JAMENDO_CLIENT_ID", "\"$jamendoClientId\"")
    }

    buildTypes {
        release {
            getByName("release") {
                isMinifyEnabled = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson 컨버터 (JSON 자동 파싱용)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Preferences DataStore (Key-Value 저장용)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // XML 화면 및 Fragment 내비게이션
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.5")
    implementation("com.google.android.material:material:1.12.0")
    implementation("io.coil-kt:coil:2.4.0")

    // exoplayer
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    // paging
    implementation ("androidx.paging:paging-runtime:3.0.1")
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    // hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    // room
    implementation("androidx.room:room-runtime:2.7.0-alpha07")
    implementation("androidx.room:room-ktx:2.7.0-alpha07")
    kapt("androidx.room:room-compiler:2.7.0-alpha07")
    // media
    implementation("androidx.media:media:1.6.0")
    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
}

kapt {
    correctErrorTypes = true
}