plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Firma de release: si CI provee un keystore (secrets), se usa; si no (forks, builds locales),
// se firma con la llave de debug para que siempre se pueda generar un APK instalable.
val keystorePath: String? = System.getenv("KEYSTORE_FILE")

android {
    namespace = "com.openkiosk.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.openkiosk.tv"
        minSdk = 22
        targetSdk = 34
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "0.1.0"
    }

    signingConfigs {
        if (keystorePath != null && file(keystorePath).exists()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
        buildConfig = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}
