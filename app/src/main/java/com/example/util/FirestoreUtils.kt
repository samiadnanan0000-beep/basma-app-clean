package com.example.util

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

enum class OperationType {
    CREATE, READ, UPDATE, DELETE, LIST
}

fun handleFirestoreError(error: Exception, operation: OperationType, path: String) {
    Log.e("FirestoreError", "Firestore ${operation.name} failed at path '$path': ${error.message}", error)
}

fun getFirestoreInstance(context: Context): FirebaseFirestore? {
    val app: FirebaseApp? = try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            try {
                FirebaseApp.initializeApp(context)
            } catch (e: Throwable) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:905620934158:android:05826bb346af1a4ede365f")
                        .setApiKey("AIzaSyCjw19CzFmqXszycXKWXBwc39-44bKwil0")
                        .setProjectId("gen-lang-client-0749483247")
                        .setStorageBucket("gen-lang-client-0749483247.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                } catch (e2: Throwable) {
                    Log.e("FirestoreUtils", "FirebaseApp initializeApp options failed: ${e2.message}")
                    null
                }
            }
        } else {
            FirebaseApp.getInstance()
        }
    } catch (e: Throwable) {
        Log.e("FirestoreUtils", "FirebaseApp initialization safeguard: ${e.message}")
        null
    }

    if (app == null) {
        Log.w("FirestoreUtils", "FirebaseApp is not initialized; returning null safely")
        return null
    }

    val databaseId = try {
        val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
        if (resId != 0) {
            context.getString(resId)
        } else {
            "ai-studio-android-cd523880-1221-4f24-9331-a62d41dced51"
        }
    } catch (e: Throwable) {
        "ai-studio-android-cd523880-1221-4f24-9331-a62d41dced51"
    }

    return try {
        if (databaseId.isNotBlank()) {
            try {
                FirebaseFirestore.getInstance(app, databaseId)
            } catch (eNamed: Throwable) {
                FirebaseFirestore.getInstance(app)
            }
        } else {
            FirebaseFirestore.getInstance(app)
        }
    } catch (e: Throwable) {
        Log.e("FirestoreUtils", "Firestore unavailable on this device/configuration: ${e.message}")
        null
    }
}
