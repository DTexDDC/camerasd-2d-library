plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.datdt.scanningsdk2D"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}
dependencies {

implementation("de.javagl:obj:0.2.1")

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
implementation(libs.core)
implementation(libs.androidx.appcompat)

testImplementation(libs.junit)
androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.espresso.core)
androidTestImplementation(platform(libs.androidx.compose.bom))
androidTestImplementation(libs.androidx.ui.test.junit4)
debugImplementation(libs.androidx.ui.tooling)
debugImplementation(libs.androidx.ui.test.manifest)

val camerax_version = "1.5.0-alpha06"
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.appcompat)
implementation(libs.material)
implementation(libs.androidx.runtime.android)
implementation(libs.androidx.ui.android)
implementation(libs.androidx.navigation.runtime.ktx)
implementation(libs.androidx.material3.android)
implementation(libs.androidx.camera.lifecycle)
implementation(libs.androidx.camera.view)
implementation(libs.navigation.compose)
testImplementation(libs.junit)
androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.espresso.core)

implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")
implementation ("com.google.accompanist:accompanist-permissions:0.24.3-alpha")
implementation("org.tensorflow:tensorflow-lite:2.9.0")
implementation("org.tensorflow:tensorflow-lite-task-vision:0.3.1")
implementation("org.tensorflow:tensorflow-lite-gpu:2.9.0")
//    implementation("com.google.android.gms:play-services-tflite-impl:16.1.0")
//    implementation("com.google.android.gms:play-services-tflite-java:16.1.0")
//    implementation("com.google.android.gms:play-services-tflite-support:16.1.0")
//    implementation("com.google.android.gms:play-services-tflite-gpu:16.2.0")
// CameraX core library using the camera2 implementation
// The following line is optional, as the core library is included indirectly by camera-camera2
implementation ("androidx.appcompat:appcompat:1.6.1")
implementation("androidx.camera:camera-core:${camerax_version}")
implementation("androidx.camera:camera-camera2:${camerax_version}")
// If you want to additionally use the CameraX Lifecycle library
implementation("androidx.camera:camera-lifecycle:${camerax_version}")
// If you want to additionally use the CameraX VideoCapture library
implementation("androidx.camera:camera-video:${camerax_version}")
// If you want to additionally use the CameraX View class
implementation("androidx.camera:camera-view:${camerax_version}")
// If you want to additionally add CameraX ML Kit Vision Integration
//    implementation("androidx.camera:camera-mlkit-vision:${camerax_version}")
// If you want to additionally use the CameraX Extensions library
implementation("androidx.camera:camera-extensions:${camerax_version}")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.DTexDDC"
                artifactId = "camerasdk-2D-library"
                version = "1.0.1" // or the tag you're pushing to GitHub
            }
        }
    }
}