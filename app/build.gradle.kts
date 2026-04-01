import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}


val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists() &&
    listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all {
        !keystoreProperties.getProperty(it).isNullOrBlank()
    }

android {
    namespace = "com.management.subscription"
    compileSdk = 35

    val admobAppId = providers.gradleProperty("ADMOB_APP_ID")
        .orElse("ca-app-pub-3940256099942544~3347511713")
        .get()
    val admobHomeBannerAdUnitId = providers.gradleProperty("ADMOB_HOME_BANNER_AD_UNIT_ID")
        .orElse("ca-app-pub-3940256099942544/9214589741")
        .get()
    val useRealDebugAds = providers.gradleProperty("USE_REAL_DEBUG_ADS")
        .map(String::toBoolean)
        .orElse(false)
        .get()
    val admobTestDeviceIds = providers.gradleProperty("ADMOB_TEST_DEVICE_IDS")
        .orElse("")
        .get()
    val debugBannerAdUnitId = if (useRealDebugAds) {
        admobHomeBannerAdUnitId
    } else {
        "ca-app-pub-3940256099942544/9214589741"
    }

    defaultConfig {
        applicationId = "com.management.subscription"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["ADMOB_APP_ID"] = admobAppId
        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        buildConfigField("String", "ADMOB_TEST_DEVICE_IDS", "\"$admobTestDeviceIds\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                val resolvedStoreFile = rootProject.file(
                    keystoreProperties.getProperty("storeFile")
                )
                storeFile = resolvedStoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "ADMOB_HOME_BANNER_AD_UNIT_ID",
                "\"$debugBannerAdUnitId\""
            )
            buildConfigField("boolean", "USE_TEST_ADS", (!useRealDebugAds).toString())
        }
        release {
            isMinifyEnabled = false
            buildConfigField(
                "String",
                "ADMOB_HOME_BANNER_AD_UNIT_ID",
                "\"$admobHomeBannerAdUnitId\""
            )
            buildConfigField("boolean", "USE_TEST_ADS", "false")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.google.play.services.ads)
    implementation("com.google.android.material:material:1.12.0")
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
}
