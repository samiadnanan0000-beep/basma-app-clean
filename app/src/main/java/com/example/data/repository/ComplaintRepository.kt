package com.example.data.repository

import android.content.Context
import com.example.data.model.ComplaintTicket
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

class ComplaintRepository(
    context: Context,
    private val notificationRepository: NotificationRepository
) {
    private val db: FirebaseFirestore = getFirestoreInstance(context)
    private val complaintsRef = db.collection("complaints")

    fun observeComplaintsForUser(userId: String, isStaff: Boolean): Flow<List<ComplaintTicket>> = callbackFlow {
        val query = if (isStaff) {
            complaintsRef.orderBy("createdAt", Query.Direction.DESCENDING)
        } else {
            complaintsRef.whereEqualTo("clientId", userId).orderBy("createdAt", Query.Direction.DESCENDING)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "complaints")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ComplaintTicket::class.java)?.copy(id = doc.id)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun submitTicket(ticket: ComplaintTicket): Boolean {
        return try {
            val payload = hashMapOf(
                "clientId" to ticket.clientId,
                "clientName" to ticket.clientName,
                "clientPhone" to ticket.clientPhone,
                "type" to ticket.type,
                "subject" to ticket.subject,
                "message" to ticket.message,
                "orderId" to ticket.orderId,
                "status" to "pending",
                "replyMessage" to "",
                "createdAt" to FieldValue.serverTimestamp()
            )
            complaintsRef.add(payload).await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "complaints")
            false
        }
    }

    suspend fun replyAndResolveTicket(ticketId: String, clientId: String, replyMessage: String, subject: String) {
        if (ticketId.isBlank()) return
        try {
            val payload = mapOf(
                "status" to "resolved",
                "replyMessage" to replyMessage,
                "resolvedAt" to FieldValue.serverTimestamp()
            )
            complaintsRef.document(ticketId).update(payload).await()

            // Trigger real-time client notification
            notificationRepository.sendNotification(
                title = "تم الرد على استفسارك / مقترحك 💬",
                message = "بخصوص: $subject - $replyMessage",
                type = "complaint",
                targetId = clientId,
                relatedId = ticketId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "complaints/$ticketId")
        }
    }
}
