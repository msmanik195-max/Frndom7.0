package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class FrndomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                // Try standard initialization
                try {
                    FirebaseApp.initializeApp(this)
                    Log.d("FrndomApplication", "Firebase initialized successfully via default provider.")
                } catch (initErr: Throwable) {
                    Log.w("FrndomApplication", "Default FirebaseApp.initializeApp failed, attempting manual options fallback: ${initErr.message}")
                }

                // If still not initialized, manually build FirebaseOptions from google-services.json credentials
                if (FirebaseApp.getApps(this).isEmpty()) {
                    val apiKey = BuildConfig.FIREBASE_API_KEY.ifBlank { "AIzaSyDIyVBiQKM9sFaOie1Mabvx6uWIq_5G2g4" }
                    val appId = BuildConfig.FIREBASE_APP_ID.ifBlank { "1:811952393925:android:4755f7334040c07c702aac" }
                    val projectId = BuildConfig.FIREBASE_PROJECT_ID.ifBlank { "frndom-871ec" }
                    val databaseUrl = BuildConfig.FIREBASE_DATABASE_URL.ifBlank { "https://frndom-871ec-default-rtdb.firebaseio.com" }

                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setApplicationId(appId)
                        .setProjectId(projectId)
                        .setDatabaseUrl(databaseUrl)
                        .setStorageBucket("frndom-871ec.firebasestorage.app")
                        .build()

                    FirebaseApp.initializeApp(this, options)
                    Log.d("FrndomApplication", "Firebase successfully initialized manually with project credentials.")
                }
            }
        } catch (e: Throwable) {
            Log.e("FrndomApplication", "Firebase initialization error: ${e.message}", e)
        }
    }
}

