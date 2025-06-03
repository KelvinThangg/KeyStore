plugins {
    alias(libs.plugins.android.application)
    // alias(libs.plugins.kotlin.android) // Nếu bạn đang sử dụng Kotlin, bỏ comment dòng này
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.passwordmanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.passwordmanager"
        minSdk = 26
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
    // kotlinOptions { // Nếu bạn đang sử dụng Kotlin, bỏ comment khối này
    //    jvmTarget = "11"
    // }
    buildFeatures { // Thêm nếu bạn sử dụng View Binding hoặc Data Binding
        viewBinding = true // Ví dụ, thay đổi nếu cần
    }
}

dependencies {

    // Android Core
    implementation("androidx.appcompat:appcompat:1.6.1") // Cân nhắc cập nhật nếu có bản vá mới hơn
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity:1.9.0") // Cân nhắc cập nhật (ví dụ activity-ktx cho Kotlin)

    // Firebase - Sử dụng một Firebase BOM duy nhất và mới nhất
    implementation(platform("com.google.firebase:firebase-bom:33.1.1")) // Kiểm tra phiên bản BOM mới nhất từ Firebase
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0") // Kiểm tra phiên bản mới nhất

    // SharedPreferences Encryption
    implementation("androidx.security:security-crypto:1.1.0-alpha06") // Giữ nguyên hoặc kiểm tra bản alpha/beta mới hơn

    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05") // Hoặc 1.1.0 nếu bản alpha không ổn định

    // Material Components - Đã cập nhật!
    implementation("com.google.android.material:material:1.12.0")
    implementation ("androidx.cardview:cardview:1.0.0")

    // Lifecycle Components
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.3") // Kiểm tra phiên bản mới nhất
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.3") // Kiểm tra phiên bản mới nhất

    // Navigation
    implementation("androidx.navigation:navigation-fragment:2.7.7") // Kiểm tra phiên bản mới nhất
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation(libs.androidx.ui.android)     // Kiểm tra phiên bản mới nhất

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1") // Kiểm tra phiên bản mới nhất
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1") // Kiểm tra phiên bản mới nhất
    //
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}