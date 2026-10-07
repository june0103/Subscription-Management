import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
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
    compileSdk = 36

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
    // 릴리스는 항상 사용 통계를 보낸다. 디버그는 테스트 데이터가 섞이지 않게 기본으로 끄고,
    // DebugView로 확인할 때만
    // ./gradlew assembleDebug -PENABLE_DEBUG_ANALYTICS=true
    val enableDebugAnalytics = providers.gradleProperty("ENABLE_DEBUG_ANALYTICS")
        .map(String::toBoolean)
        .orElse(false)
        .get()
    // 인앱 업데이트 흐름을 에뮬레이터에서 확인할 때만: ./gradlew assembleDebug -PFAKE_APP_UPDATE=true
    val fakeAppUpdate = providers.gradleProperty("FAKE_APP_UPDATE")
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
        targetSdk = 36
        versionCode = 5
        versionName = "1.1.2"

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
            buildConfigField("boolean", "ANALYTICS_ENABLED", enableDebugAnalytics.toString())
            buildConfigField("boolean", "FAKE_APP_UPDATE", fakeAppUpdate.toString())
        }
        release {
            // R8로 쓰지 않는 코드를 지우고 난독화한다. Play Console "DEX 코드 최적화" 기준을 맞추고,
            // 라이브러리에 남아 있던 지원 중단 창 색 API(쓰지 않는 사이드시트 등)도 함께 빠진다.
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField(
                "String",
                "ADMOB_HOME_BANNER_AD_UNIT_ID",
                "\"$admobHomeBannerAdUnitId\""
            )
            buildConfigField("boolean", "USE_TEST_ADS", "false")
            buildConfigField("boolean", "ANALYTICS_ENABLED", "true")
            buildConfigField("boolean", "FAKE_APP_UPDATE", "false")
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
    sourceSets {
        // Room이 내보낸 DB 스키마로 마이그레이션을 테스트한다.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
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
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation("com.google.android.material:material:1.14.0")
    // Play 스토어 새 버전을 앱 안에서 받는 인앱 업데이트
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
}
