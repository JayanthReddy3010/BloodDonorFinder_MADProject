package com.example.blooddonorfinder

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Application class for Blood Donor Finder.
 * Ensures proper and reliable Firebase initialization at application startup.
 */
class BloodDonorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            val app = FirebaseApp.initializeApp(this)
            if (app != null) {
                Log.d("BloodDonorApp", "Firebase successfully initialized: ${app.name}")
            } else {
                Log.w("BloodDonorApp", "FirebaseApp.initializeApp returned null (may be auto-initialized)")
            }
        } catch (e: Exception) {
            Log.e("BloodDonorApp", "Error during Firebase initialization", e)
        }
    }
}
