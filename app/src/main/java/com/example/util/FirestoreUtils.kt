package com.example.util

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

enum class OperationType {
    CREATE, READ, UPDATE, DELETE, LIST
}

fun handleFirestoreError(error: Exception, operation: OperationType, path: String) {
    Log.e("FirestoreError", "Firestore ${operation.name} failed at path '$path': ${error.message}", error)
}

fun getFirestoreInstance(context: Context): FirebaseFirestore {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    } catch (e: Exception) {
        Log.e("FirestoreUtils", "FirebaseApp init safety: ${e.message}")
    }

    val databaseId = try {
        context.getString(R.string.firestore_database_id)
    } catch (e: Exception) {
        ""
    }

    return try {
        if (databaseId.isNotBlank()) {
            FirebaseFirestore.getInstance(databaseId)
        } else {
            @Suppress("DEPRECATION")
            FirebaseFirestore.getInstance()
        }
    } catch (e: Exception) {
        Log.w("FirestoreUtils", "Could not get named firestore instance, falling back to default: ${e.message}")
        @Suppress("DEPRECATION")
        FirebaseFirestore.getInstance()
    }
}
