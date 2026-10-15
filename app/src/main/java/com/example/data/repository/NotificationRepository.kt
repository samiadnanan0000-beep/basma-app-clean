package com.example.data.repository

import android.content.Context
import com.example.data.model.NotificationItem
import com.example.util.OperationType
import com.example.util.getFirestoreInstance
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository(context: Context) {
    private val db: FirebaseFirestore = getFirestoreInstance(context)
    private val notifsRef = db.collection("notifications")

    fun observeNotificationsForUser(userId: String, isStaff: Boolean): Flow<List<NotificationItem>> = callbackFlow {
        val query = if (isStaff) {
            notifsRef.orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
        } else {
            notifsRef.whereIn("targetId", listOf("all", userId))
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "notifications")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun sendNotification(
        title: String,
        message: String,
        type: String, // "offer", "order", "general", "complaint"
        targetId: String = "all",
        relatedId: String = ""
    ) {
        try {
            val payload = hashMapOf(
                "title" to title,
                "message" to message,
                "type" to type,
                "targetId" to targetId,
                "isRead" to false,
                "relatedId" to relatedId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            notifsRef.add(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "notifications")
        }
    }

    suspend fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) return
        try {
            notifsRef.document(notificationId).update("isRead", true).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
        }
    }

    suspend fun clearNotification(notificationId: String) {
        if (notificationId.isBlank()) return
        try {
            notifsRef.document(notificationId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "notifications/$notificationId")
        }
    }
}
