import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val keyPropertiesFile = rootProject.file("key.properties")
val keyProperties = Properties().apply {
    if (keyPropertiesFile.exists()) keyPropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "de.j4velin.calendarWidget"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.j4velin.calendarWidget"
        minSdk = 26
        targetSdk = 37
        versionCode = 300
        versionName = "3.0.0"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (keyPropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keyProperties.getProperty("keyStore"))
                storePassword = keyProperties.getProperty("keyStorePassword")
                keyAlias = keyProperties.getProperty("keyAlias")
                keyPassword = keyProperties.getProperty("keyAliasPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keyPropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-project.txt"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
}
