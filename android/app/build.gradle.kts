import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.yandex.pay.kit.sample"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.yandex.pay.kit.sample"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val yandexClientId = "b8f475015d0640fd8b870b5838e2e623"
        val yandexPayClientId = "b8f475015d0640fd8b870b5838e2e623"
        val merchantId = "a5f49c84-0baa-41e1-814f-6f99746a6987"
        val paymentBaseUrl = "https://sandbox.pay.yandex.ru/api"
        val merchantApiKey = "a5f49c84-0baa-41e1-814f-6f99746a6987"

        manifestPlaceholders["YANDEX_CLIENT_ID"] = yandexClientId
        manifestPlaceholders["YANDEX_PAY_CLIENT_ID"] = yandexPayClientId
        manifestPlaceholders["YANDEX_OAUTH_HOST"] = "oauth.yandex.ru"
        buildConfigField("String", "MERCHANT_ID", "\"$merchantId\"")
        buildConfigField("String", "YPAY_VERSION", "\"${libs.versions.ypay.get()}\"")
        buildConfigField("String", "PAYMENT_BASE_URL", "\"$paymentBaseUrl\"")
        buildConfigField("String", "MERCHANT_API_KEY", "\"$merchantApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "debug"
            keyPassword = "android"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.ypay.flows.assistant)
    implementation(libs.ypay.flows.auth)
    implementation(libs.ypay.flows.pay.with.redirect)
    implementation(libs.ypay.flows.inapp)
    implementation(libs.ypay.flows.quickpay)
    implementation(libs.ypay.flows.inventory)

    implementation(libs.yandex.login)
    implementation(libs.material)
    implementation(libs.zxing.android.embedded)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.timber)
}
