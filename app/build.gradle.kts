@file:Suppress("DEPRECATION")

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(FileInputStream(file))
}

fun getEnvVariable(key: String, defaultValue: String = ""): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: defaultValue

android {
    namespace = "ru.dzhaparidze.mykct"
    compileSdk = 36

    defaultConfig {
        // НЕ МЕНЯТЬ: идентификатор приложения в RuStore. Смена = новая карточка,
        // потеря установок и отзывов. Пакет исходников (namespace) от него независим.
        applicationId = "ru.dzhaparidze.collegeapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 9
        versionName = "2.0.0"

        buildConfigField("String", "API_BASE_URL", "\"${getEnvVariable("API_BASE_URL", "http://localhost:8500")}\"")
        // Авторизация живёт отдельным сервисом (в iOS это BaseAuthURL). Домен тот же,
        // пока не сказано иное — переопределяется AUTH_BASE_URL в local.properties.
        buildConfigField(
            "String",
            "AUTH_BASE_URL",
            "\"${getEnvVariable("AUTH_BASE_URL", getEnvVariable("API_BASE_URL", "http://localhost:8500"))}\"",
        )
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))

                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time на minSdk 24
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        outputs.all {
            (this as? ApkVariantOutputImpl)?.outputFileName = "МойКЦТ-${versionName}.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
