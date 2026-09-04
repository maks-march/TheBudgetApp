plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

import java.util.Properties

android {
    namespace = "ru.budget.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.budget.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"

        // Проверка обновлений: version.json и APK в репозитории
        // (создайте репозиторий и поправьте ссылки при необходимости)
        buildConfigField(
            "String",
            "VERSION_URL",
            "\"https://raw.githubusercontent.com/maks-march/BudgetApp/main/version.json\"",
        )
        buildConfigField(
            "String",
            "APK_URL",
            "\"https://github.com/maks-march/BudgetApp/raw/main/apk/Budget.apk\"",
        )
        buildConfigField(
            "String",
            "GITHUB_URL",
            "\"https://github.com/maks-march/BudgetApp\"",
        )
    }

    signingConfigs {
        create("release") {
            // Постоянный ключ: обновления ставятся поверх только при совпадении подписи.
            // Пароли можно вынести в keystore.properties (вне git).
            val props = Properties()
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) props.load(propsFile.inputStream())

            storeFile = rootProject.file(
                props.getProperty("storeFile") ?: "keystore/budget.jks"
            )
            storePassword = props.getProperty("storePassword") ?: "budgetapp"
            keyAlias = props.getProperty("keyAlias") ?: "release"
            keyPassword = props.getProperty("keyPassword") ?: "budgetapp"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // ТЕМ ЖЕ ключом, что и release — иначе сборки не ставятся поверх друг друга
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")

    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
