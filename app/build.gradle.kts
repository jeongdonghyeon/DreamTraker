plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "kr.co.example.dreamtraker"
    compileSdk = 35

    defaultConfig {
        applicationId = "kr.co.example.dreamtraker"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 🔥 AAR(local) 라이브러리 인식 설정 추가
    sourceSets {
        getByName("main") {
            jniLibs.srcDir("libs")
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(files("libs/samsung-health-data-api-1.0.0.aar"))
}
