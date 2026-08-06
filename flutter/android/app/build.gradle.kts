import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Плейсхолдеры манифеста — из local.properties / -P, не захардкоженные секреты.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun localProp(key: String, default: String) =
    localProperties.getProperty(key) ?: (project.findProperty(key) as? String) ?: default

android {
    namespace = "com.yandex.quickpay.sample"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        applicationId = localProp("APPLICATION_ID", "com.yandex.quickpay.sample")
        minSdk = 24
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        manifestPlaceholders["YANDEX_CLIENT_ID"] =
            localProp("YANDEX_CLIENT_ID", "b3523a6f87854ad4987cee1d0596a151")

        // Required by the native Yandex Pay auth SDK manifest (pulled transitively by
        // com.yandex.pay:quickpay): the deep-link host/scheme and OAuth host placeholders.
        manifestPlaceholders["YANDEX_PAY_CLIENT_ID"] =
            localProp("YANDEX_PAY_CLIENT_ID", "b3523a6f87854ad4987cee1d0596a151")
        manifestPlaceholders["YANDEX_OAUTH_HOST"] =
            localProp("YANDEX_OAUTH_HOST", "oauth.yandex.ru")

        manifestPlaceholders["QUICKPAY_MERCHANT_ID"] =
            localProp("QUICKPAY_MERCHANT_ID", "0df18b44-cb01-4263-b05f-6de81e9b5692")
        manifestPlaceholders["QUICKPAY_ENVIRONMENT"] = localProp("QUICKPAY_ENVIRONMENT", "SANDBOX")
    }

    buildTypes {
        release {
            // TODO: Configure release signing (keystore) before publishing to Play.
            // Do not use debug keys for release builds.
        }
    }
}

flutter {
    source = "../.."
}
