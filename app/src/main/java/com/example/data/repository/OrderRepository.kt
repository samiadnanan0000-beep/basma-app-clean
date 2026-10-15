package com.example.data.repository

import android.content.Context
import com.example.data.model.OrderItem
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

class OrderRepository(
    context: Context,
    private val notificationRepository: NotificationRepository
) {
    private val db: FirebaseFirestore = getFirestoreInstance(context)
    private val ordersRef = db.collection("orders")

    fun observeOrdersForUser(userId: String, isStaff: Boolean): Flow<List<OrderItem>> = callbackFlow {
        val query = if (isStaff) {
            ordersRef.orderBy("createdAt", Query.Direction.DESCENDING)
        } else {
            ordersRef.whereEqualTo("clientId", userId).orderBy("createdAt", Query.Direction.DESCENDING)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "orders")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(OrderItem::class.java)?.copy(id = doc.id)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun submitOrder(order: OrderItem): String? {
        return try {
            val payload = hashMapOf(
                "clientId" to order.clientId,
                "clientName" to order.clientName,
                "clientPhone" to order.clientPhone,
                "category" to order.category,
                "materialId" to order.materialId,
                "materialName" to order.materialName,
                "width" to order.width,
                "height" to order.height,
                "brandText" to order.brandText,
                "contactDetails" to order.contactDetails,
                "notes" to order.notes,
                "logoUrl" to order.logoUrl,
                "sitePhotoUrl" to order.sitePhotoUrl,
                "status" to "pending",
                "price" to 0.0,
                "currency" to order.currency,
                "turnaroundTime" to "",
                "proofUrl" to "",
                "proofApproved" to false,
                "rejectionReason" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val doc = ordersRef.add(payload).await()

            // Broadcast alert to staff
            notificationRepository.sendNotification(
                title = "طلب عمل جديد وارد 📦",
                message = "العميل: ${order.clientName} - الفئة: ${order.category} (${order.width}م × ${order.height}م)",
                type = "order",
                targetId = "all",
                relatedId = doc.id
            )
            doc.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "orders")
            null
        }
    }

    suspend fun quoteOrder(
        orderId: String,
        clientId: String,
        price: Double,
        currency: String,
        turnaroundTime: String,
        proofUrl: String? = null
    ) {
        if (orderId.isBlank()) return
        try {
            val updates = mutableMapOf<String, Any>(
                "status" to "quoted",
                "price" to price,
                "currency" to currency,
                "turnaroundTime" to turnaroundTime,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!proofUrl.isNullOrBlank()) {
                updates["proofUrl"] = proofUrl
            }
            ordersRef.document(orderId).update(updates).await()

            // Notify client directly
            notificationRepository.sendNotification(
                title = "تم تسعير طلبك بنجاح 💰",
                message = "السعر: $price $currency - موعد التسليم: $turnaroundTime. يرجى مراجعة تفاصيل الطلب واعتماد البروفة.",
                type = "order",
                targetId = clientId,
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    suspend fun updateOrderStatus(orderId: String, clientId: String, newStatus: String) {
        if (orderId.isBlank()) return
        try {
            ordersRef.document(orderId).update(
                "status", newStatus,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            val statusArabic = when (newStatus) {
                "in_production" -> "قيد التنفيذ بالمطبعة 🟢"
                "completed" -> "جاهز للتسليم والتركيب 🏁"
                else -> newStatus
            }

            notificationRepository.sendNotification(
                title = "تحديث في حالة طلبك 📦",
                message = "أصبح طلبك الآن: $statusArabic",
                type = "order",
                targetId = clientId,
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    suspend fun uploadProof(orderId: String, clientId: String, proofUrl: String) {
        if (orderId.isBlank()) return
        try {
            ordersRef.document(orderId).update(
                "proofUrl", proofUrl,
                "proofApproved", false,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            notificationRepository.sendNotification(
                title = "تم رفع بروفة التصميم للمعاينة 🎨",
                message = "قام المصمم برفع بروفة جديدة لطلبك. تفقدها الآن واعتمد التصميم لبدء الطباعة.",
                type = "order",
                targetId = clientId,
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    suspend fun clientApproveProof(orderId: String) {
        if (orderId.isBlank()) return
        try {
            ordersRef.document(orderId).update(
                "proofApproved", true,
                "status", "in_production",
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            notificationRepository.sendNotification(
                title = "تم اعتماد البروفة من قبل العميل ✅",
                message = "الطلب رقم: ${orderId.take(6)} معتمد وجاهز للبدء فوراً بالطباعة والتركيب.",
                type = "order",
                targetId = "all",
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    suspend fun clientRejectProof(orderId: String, reason: String) {
        if (orderId.isBlank()) return
        try {
            ordersRef.document(orderId).update(
                "proofApproved", false,
                "rejectionReason", reason,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            notificationRepository.sendNotification(
                title = "طلب تعديل على البروفة من العميل ✏️",
                message = "ملاحظات العميل: $reason",
                type = "order",
                targetId = "all",
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }
}
