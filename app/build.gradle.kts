import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}

android {
    namespace = "com.app.rewardsplanet"

    compileSdk = 35

    signingConfigs {
        create("release") {
            val keyFile = keystoreProperties.getProperty("storeFile")
            if (keyFile != null) {
                storeFile = rootProject.file(keyFile)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.dailykash.app"

        minSdk = 24

        targetSdk = 35

        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // ==============================
    // ANDROIDX
    // ==============================

    implementation(libs.activity.ktx)
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")


    // ==============================
    // NAVIGATION
    // ==============================

    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)


    // ==============================
    // FIREBASE
    // ==============================

    implementation(
        platform(
            "com.google.firebase:firebase-bom:33.7.0"
        )
    )

    implementation(
        "com.google.firebase:firebase-analytics"
    )

    implementation(
        "com.google.firebase:firebase-auth"
    )

    implementation(
        "com.google.firebase:firebase-firestore"
    )

    implementation(
        "com.google.firebase:firebase-messaging"
    )


    // ==============================
    // GOOGLE SIGN-IN
    // ==============================

    implementation(
        "com.google.android.gms:play-services-auth:20.7.0"
    )


    // ==============================
    // ADMOB
    // ==============================

    implementation(
        "com.google.android.gms:play-services-ads:23.6.0"
    )

    // ==============================
    // PLAY INSTALL REFERRER
    // ==============================

    implementation(
        "com.android.installreferrer:installreferrer:2.2"
    )


    // ==============================
    // GLIDE
    // ==============================

    implementation(
        "com.github.bumptech.glide:glide:4.16.0"
    )

    annotationProcessor(
        "com.github.bumptech.glide:compiler:4.16.0"
    )


    // ==============================
    // RETROFIT
    // ==============================

    implementation(
        "com.squareup.retrofit2:retrofit:2.9.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:2.9.0"
    )


    // ==============================
    // SHIMMER
    // ==============================

    implementation(
        "com.facebook.shimmer:shimmer:0.5.0"
    )


    // ==============================
    // TESTING
    // ==============================

    testImplementation(libs.junit)

    androidTestImplementation(libs.ext.junit)

    androidTestImplementation(
        libs.espresso.core
    )
}