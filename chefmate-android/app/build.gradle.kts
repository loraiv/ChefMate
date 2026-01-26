plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // id("kotlin-kapt")
    // id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.chefmate"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.chefmate"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Base URL for API
        // For Android Emulator: use "http://10.0.2.2:8090/"
        // For real device: use your computer's IP address, e.g., "http://192.168.1.11:8090/"
        // To find your IP: Windows (ipconfig), Linux/Mac (ifconfig)
        // Make sure your phone and computer are on the same WiFi network!
        // NOTE: Change this to 10.0.2.2 if using Android Emulator
        buildConfigField("String", "BASE_URL", "\"http://192.168.1.11:8090/\"")
    }
    
    testOptions {
        unitTests {
            all {
                // Allow reflection access for testing
                it.jvmArgs = listOf(
                    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                    "--add-opens=java.base/java.lang=ALL-UNNAMED"
                )
            }
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.4")
    
    // ViewPager2 for image swiping
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    
    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Hilt
    // implementation("com.google.dagger:hilt-android:2.48")
    // kapt("com.google.dagger:hilt-compiler:2.48")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // kapt("com.github.bumptech.glide:compiler:4.16.0")

    // UCrop for image cropping
    implementation("com.github.yalantis:ucrop:2.2.8")

    // ML Kit Language Identification (for detecting language from text)
    implementation("com.google.mlkit:language-id:17.0.0")
    
    // Note: ML Kit doesn't have direct Speech Recognition API
    // We use Android's built-in SpeechRecognizer (already available)
    // For better accuracy, consider Google Cloud Speech-to-Text API

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.0")
    testImplementation("org.robolectric:robolectric:4.11.1")

    // Android UI Testing (Espresso)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.fragment:fragment-testing:1.6.2")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.4")
}

// kapt {
//     correctErrorTypes = true
// }