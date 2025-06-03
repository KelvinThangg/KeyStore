package com.example.passwordmanager;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        // Thêm log để kiểm tra
        android.util.Log.d("MyApplication", "Firebase initialized");
    }
}