plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.lifemate"
    compileSdk = 36
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "com.lifemate"
        minSdk = 26
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64") }
        targetSdk = 35
        versionCode = providers.environmentVariable("LIFEMATE_VERSION_CODE").orNull?.toInt() ?: 100000
        versionName = providers.environmentVariable("LIFEMATE_VERSION_NAME").orNull ?: "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val releaseKeystore = providers.environmentVariable("LIFEMATE_KEYSTORE_PATH").orNull
    if (!releaseKeystore.isNullOrBlank()) {
        signingConfigs.create("privateRelease") {
            storeFile = file(releaseKeystore)
            storeType = "JKS"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = false
            storePassword = requireNotNull(providers.environmentVariable("LIFEMATE_STORE_PASSWORD").orNull)
            keyAlias = requireNotNull(providers.environmentVariable("LIFEMATE_KEY_ALIAS").orNull)
            keyPassword = requireNotNull(providers.environmentVariable("LIFEMATE_KEY_PASSWORD").orNull)
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
            if (!releaseKeystore.isNullOrBlank()) signingConfig = signingConfigs.getByName("privateRelease")
            // No fallback to a debug or disposable key. Private signing material is never in source.
        }
    }
    buildFeatures { buildConfig = true }
    compileOptions { isCoreLibraryDesugaringEnabled = true; sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // Direct APK delivery: compress native libraries for a smaller download.
        // Android extracts them once at install; normal launches use those files.
        jniLibs.useLegacyPackaging = providers.gradleProperty("lifemate.compressNative").map { it.toBoolean() }.orElse(true).get()
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation(project(":flutter"))
    // Read-only compatibility with the existing user's encrypted profile.
    implementation("net.zetetic:sqlcipher-android:4.9.0")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
