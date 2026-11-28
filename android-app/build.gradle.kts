plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

val profileRelease = providers.gradleProperty("howlProfileRelease").orNull == "true"

android {
    namespace = "com.howl.music"
    compileSdk = 35

    if (profileRelease) testBuildType = "release"

    defaultConfig {
        applicationId = "com.howl.music"
        minSdk = 26
        targetSdk = 35
        // Every packaged ABI must contain the playback engine, including transitive native libraries.
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        versionCode = 1
        versionName = "0.1.0-dev"

        testInstrumentationRunner = "com.howl.music.EngineApiInstrumentation"
    }

    buildTypes {
        release {
            if (profileRelease) {
                applicationIdSuffix = ".profile"
                signingConfig = signingConfigs.getByName("debug")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":android-media"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation.compose)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.ui.tooling)

    constraints {
        androidTestImplementation("androidx.collection:collection:1.4.4") {
            because("Match the app runtime's AndroidX Collection version")
        }
    }

    testImplementation(libs.junit)
}
