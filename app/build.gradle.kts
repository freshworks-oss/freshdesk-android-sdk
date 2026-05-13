plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.compose)
}

// Google Services / Firebase are optional for this sample: the public repo does not ship
// google-services.json. When that file is absent, we skip the plugin so CI and clones
// still assemble; push (FCM) is not configured in that case.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
} else {
    logger.lifecycle(
        "${project.path}: app/google-services.json not found — skipping com.google.gms.google-services " +
            "(FCM / push notifications are not configured for this build).",
    )
}

android {
    namespace = "com.freshdesk.southwest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.freshdesk.southwest"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
        create("snapshot") {
            initWith(getByName("release"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packagingOptions {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

dependencies {
    releaseImplementation(libs.freshdesk)
    "snapshotImplementation"(libs.freshdesk.ss)
    debugImplementation(libs.freshdesk)

    implementation(libs.compose.runtime)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.firebase.messaging)
    implementation(libs.gson)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    debugImplementation(libs.compose.ui.tooling)
}