import com.android.build.api.dsl.Packaging

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.datdt.scanningapp2D"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.datdt.scanningapp2D"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "1.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // You can exclude this file from packaging entirely:
    // exclude("META-INF/INDEX.LIST")
    // Or use pickFirst if you want to include one of the copies:
    packaging {
        resources {
            // Choose one of the options below:

            // Option 1: pickFirst — safest for INDEX.LIST
            pickFirsts.add("META-INF/INDEX.LIST")
            pickFirsts.add("META-INF/DEPENDENCIES")
            pickFirsts.add("META-INF/io.netty.versions.properties")

            // Option 2: exclude (less common for INDEX.LIST)
            // excludes += ["META-INF/INDEX.LIST"]
        }
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

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}
dependencies {
    implementation(project(":2D Scanning"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation ("androidx.navigation:navigation-runtime-ktx:2.5.0")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.appcompat)
    implementation(libs.firebase.appdistribution.gradle)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // optional
    // Gson core
    implementation ("com.google.code.gson:gson:2.10.1")

// Retrofit Gson converter (if using with Retrofit)
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
}