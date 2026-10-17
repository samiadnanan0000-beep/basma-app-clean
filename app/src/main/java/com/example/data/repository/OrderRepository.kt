package com.example.data.repository

import android.content.Context
import com.example.data.model.ChatMessage
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
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
    private val usersRef = db.collection("users")

    /**
     * Anti-Bypass protection: checks text for phone numbers and bypass keywords.
     * Returns true if bypass/leak attempt is detected.
     */
    fun isBypassDetected(text: String): Boolean {
        val cleanText = text.replace(" ", "").replace("-", "").replace(".", "")

        // 1. Phone number patterns (Yemen & international sequences)
        val phonePattern = Regex("(77|73|71|70|01)[0-9]{6,7}|[0-9]{8,12}|\\+967[0-9]{8,9}")
        if (phonePattern.containsMatchIn(cleanText)) return true

        // 2. Sensitive contact keywords
        val sensitiveKeywords = listOf(
            "واتساب", "واتس", "whatsapp", "رقمي", "اتصل بي", "اتصلي",
            "تيليجرام", "تليجرام", "telegram", "تواصل معي خارج", "تواصل برع",
            "على الواتس", "على الخاص", "رقم تلفوني", "رقم هاتفي"
        )
        return sensitiveKeywords.any { text.contains(it, ignoreCase = true) }
    }

    /**
     * Observes orders for client or admin.
     */
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

    /**
     * Observes orders assigned to designer with STRICT PRIVACY ISOLATION:
     * Completely masks client PII and production/material costs.
     */
    fun observeOrdersForDesigner(designerId: String): Flow<List<OrderItem>> = callbackFlow {
        val query = ordersRef.whereEqualTo("assignedDesignerId", designerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "orders?designer=$designerId")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                val o = doc.toObject(OrderItem::class.java)?.copy(id = doc.id)
                // Mask client personal data & production cost
                o?.copy(
                    clientName = "عميل المنصة",
                    clientPhone = "محجوب لحماية الخصوصية",
                    clientEmail = "محجوب لحماية الخصوصية",
                    productionAmount = 0.0,
                    totalAmount = 0.0
                )
            }.orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Submits flexible new order with generated orderNumber and PENDING_PRICING status.
     */
    suspend fun submitOrder(order: OrderItem): String? {
        return try {
            val serial = (1000..9999).random()
            val generatedOrderNumber = "#BSM-$serial"

            val payload = hashMapOf(
                "orderNumber" to generatedOrderNumber,
                "clientId" to order.clientId,
                "clientName" to order.clientName,
                "clientPhone" to order.clientPhone,
                "clientEmail" to order.clientEmail,
                "designType" to order.designType.ifBlank { order.category },
                "detailedSpecs" to order.detailedSpecs.ifBlank { order.notes },
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
                "referenceAttachmentUrl" to order.referenceAttachmentUrl,
                "status" to OrderStatus.PENDING_PRICING,
                "designAmount" to 0.0,
                "productionAmount" to 0.0,
                "totalAmount" to 0.0,
                "advanceAmount" to 0.0,
                "remainingAmount" to 0.0,
                "designerFee" to 0.0,
                "platformFee" to 0.0,
                "paymentStatus" to PaymentStatus.NONE,
                "advancePaymentStatus" to PaymentStatus.NONE,
                "finalPaymentStatus" to PaymentStatus.NONE,
                "currency" to "YER",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val doc = ordersRef.add(payload).await()

            // Broadcast alert to admin
            notificationRepository.sendNotification(
                title = "طلب تصميم جديد وارد 📦 $generatedOrderNumber",
                message = "النوع: ${order.designType.ifBlank { order.category }} - العميل: ${order.clientName}، بانتظار التسعير المعتمد من الإدارة.",
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

    /**
     * Dual pricing by Admin:
     * Separates designAmount and productionAmount.
     * Platform commission deducted ONLY from designAmount.
     */
    suspend fun quoteDualOrder(
        orderId: String,
        clientId: String,
        designAmount: Double,
        productionAmount: Double,
        commissionPercentage: Double = 20.0,
        estimatedDeliveryTime: String = ""
    ) {
        if (orderId.isBlank()) return
        try {
            val total = designAmount + productionAmount
            val advance = total * 0.70
            val remaining = total * 0.30
            val platformFee = designAmount * (commissionPercentage / 100.0)
            val designerFee = designAmount - platformFee

            val updates = mutableMapOf<String, Any>(
                "designAmount" to designAmount,
                "productionAmount" to productionAmount,
                "totalAmount" to total,
                "price" to total,
                "advanceAmount" to advance,
                "remainingAmount" to remaining,
                "platformFee" to platformFee,
                "designerFee" to designerFee,
                "status" to OrderStatus.PRICE_OFFERED,
                "paymentStatus" to PaymentStatus.WAITING_FOR_ADVANCE,
                "advancePaymentStatus" to PaymentStatus.WAITING_FOR_ADVANCE,
                "estimatedDeliveryTime" to estimatedDeliveryTime,
                "turnaroundTime" to estimatedDeliveryTime,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ordersRef.document(orderId).update(updates).await()

            notificationRepository.sendNotification(
                title = "تم تحديد تسعيرة طلبك 💰",
                message = "إجمالي التكلفة: ${total.toInt()} ر.ي (الدفعة المقدمة 70%: ${advance.toInt()} ر.ي). يرجى فتح الطلب للاعتماد وبدء السداد.",
                type = "order",
                targetId = clientId,
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    /**
     * Client accepts quote and moves to advance payment.
     */
    suspend fun clientAcceptQuote(orderId: String) {
        if (orderId.isBlank()) return
        try {
            ordersRef.document(orderId).update(
                "status", OrderStatus.WAITING_FOR_ADVANCE,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    /**
     * Client cancels order if quote not accepted.
     */
    suspend fun clientCancelOrder(orderId: String) {
        if (orderId.isBlank()) return
        try {
            ordersRef.document(orderId).update(
                "status", OrderStatus.CANCELLED,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    /**
     * Submits 70% advance payment receipt.
     */
    suspend fun submitAdvancePayment(
        orderId: String,
        clientId: String,
        totalAmount: Double,
        advanceAmount: Double,
        remainingAmount: Double,
        paymentMethod: String,
        receiptImageUrl: String
    ) {
        if (orderId.isBlank()) return
        try {
            val updates = mapOf(
                "totalAmount" to totalAmount,
                "advanceAmount" to advanceAmount,
                "remainingAmount" to remainingAmount,
                "paymentMethod" to paymentMethod,
                "receiptImageUrl" to receiptImageUrl,
                "advanceReceiptUrl" to receiptImageUrl,
                "paymentStatus" to PaymentStatus.UNDER_REVIEW,
                "advancePaymentStatus" to PaymentStatus.UNDER_REVIEW,
                "paymentRejectionReason" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ordersRef.document(orderId).update(updates).await()

            notificationRepository.sendNotification(
                title = "إشعار سداد الدفعة المقدمة (70%) 💳",
                message = "سدد العميل ${advanceAmount.toInt()} ر.ي عبر ($paymentMethod)، بانتظار مراجعة وتأكيد الإدارة.",
                type = "order",
                targetId = "all",
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    /**
     * Admin approves advance payment -> moves to WAITING_FOR_ASSIGNMENT.
     */
    suspend fun approveAdvancePayment(
        orderId: String,
        clientId: String,
        estimatedDeliveryTime: String? = null
    ) {
        if (orderId.isBlank()) return
        try {
            val updates = mutableMapOf<String, Any>(
                "paymentStatus" to PaymentStatus.APPROVED,
                "advancePaymentStatus" to PaymentStatus.APPROVED,
                "isApprovedByDesignerOrAdmin" to true,
                "status" to OrderStatus.WAITING_FOR_ASSIGNMENT,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!estimatedDeliveryTime.isNullOrBlank()) {
                updates["estimatedDeliveryTime"] = estimatedDeliveryTime
                updates["turnaroundTime"] = estimatedDeliveryTime
            }
            ordersRef.document(orderId).update(updates).await()

            notificationRepository.sendNotification(
                title = "تم تأكيد الدفعة المقدمة بنجاح! 🟢",
                message = "تم اعتماد الحوالة بنجاح، والطلب الآن بانتظار إسناده لأحد مصممينا المتفرغين.",
                type = "order",
                targetId = clientId,
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    /**
     * Admin assigns order to a designer -> moves to PENDING_DESIGNER_ACCEPTANCE.
     */
    suspend fun assignDesigner(orderId: String, designerId: String, designerName: String) {
        if (orderId.isBlank()) return
        try {
            val updates = mapOf(
                "assignedDesignerId" to designerId,
                "designerName" to designerName,
                "status" to OrderStatus.PENDING_DESIGNER_ACCEPTANCE,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ordersRef.document(orderId).update(updates).await()

            notificationRepository.sendNotification(
                title = "مهمة تصميم جديدة مسندة إليك 🎨",
                message = "تم إسناد الطلب إليك للموافقة وبدء التنفيذ.",
                type = "order",
                targetId = designerId,
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    /**
     * Designer accepts order -> moves to IN_PROGRESS and increments activeTasksCount.
     */
    suspend fun designerAcceptOrder(orderId: String, designerId: String) {
        if (orderId.isBlank()) return
        try {
            ordersRef.document(orderId).update(
                "status", OrderStatus.IN_PROGRESS,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            // Increment active tasks count for load balancing
            usersRef.document(designerId).update("activeTasksCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    /**
     * Designer rejects/apologizes for order -> reverts to WAITING_FOR_ASSIGNMENT.
     */
    suspend fun designerRejectOrder(
        orderId: String,
        designerId: String,
        designerName: String,
        reason: String
    ) {
        if (orderId.isBlank()) return
        try {
            val logEntry = mapOf(
                "designerId" to designerId,
                "designerName" to designerName,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis()
            )
            val updates = mapOf(
                "assignedDesignerId" to null,
                "designerName" to null,
                "status" to OrderStatus.WAITING_FOR_ASSIGNMENT,
                "rejectionHistory" to FieldValue.arrayUnion(logEntry),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ordersRef.document(orderId).update(updates).await()

            notificationRepository.sendNotification(
                title = "اعتذار مصمم عن طلب ⚠️",
                message = "اعتذر المصمم $designerName عن الطلب (السبب: $reason)، يرجى إعادة الإسناد لمصمم بديل.",
                type = "order",
                targetId = "all",
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    /**
     * Submits 30% final payment.
     */
    suspend fun submitFinalPayment(
        orderId: String,
        clientId: String,
        paymentMethod: String,
        receiptImageUrl: String
    ) {
        if (orderId.isBlank()) return
        try {
            val updates = mapOf(
                "paymentMethod" to paymentMethod,
                "finalReceiptUrl" to receiptImageUrl,
                "finalPaymentStatus" to PaymentStatus.UNDER_REVIEW,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ordersRef.document(orderId).update(updates).await()

            notificationRepository.sendNotification(
                title = "إشعار سداد الدفعة النهائية (30%) 💳",
                message = "قام العميل بسداد الدفعة المتبقية للاستلام، بانتظار المراجعة والاعتماد.",
                type = "order",
                targetId = "all",
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    /**
     * Admin approves final payment -> moves to COMPLETED.
     */
    suspend fun approveFinalPayment(orderId: String, clientId: String, designerId: String?) {
        if (orderId.isBlank()) return
        try {
            val updates = mapOf(
                "finalPaymentStatus" to PaymentStatus.APPROVED,
                "status" to OrderStatus.COMPLETED,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ordersRef.document(orderId).update(updates).await()

            // Decrement designer active tasks count
            if (!designerId.isNullOrBlank()) {
                usersRef.document(designerId).update("activeTasksCount", FieldValue.increment(-1)).await()
            }

            notificationRepository.sendNotification(
                title = "تم اكتمال الطلب بنجاح! 🏁",
                message = "تم اعتماد الدفعة النهائية بنجاح! سيتم تسليم كافة الملفات والتركيبات الأصلية، يرجى تقييم الخدمة.",
                type = "order",
                targetId = clientId,
                relatedId = orderId
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    /**
     * Rates the completed order and updates designer's average rating.
     */
    suspend fun rateOrder(orderId: String, designerId: String?, rating: Float, review: String) {
        if (orderId.isBlank()) return
        try {
            ordersRef.document(orderId).update(
                "clientRating", rating,
                "clientReview", review,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            if (!designerId.isNullOrBlank()) {
                // Update designer's average rating in users
                val designerDoc = usersRef.document(designerId).get().await()
                val currentRating = designerDoc.getDouble("ratingAverage")?.toFloat() ?: 5.0f
                val newRating = ((currentRating + rating) / 2f).coerceIn(1.0f, 5.0f)
                usersRef.document(designerId).update("ratingAverage", newRating).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
        }
    }

    // --- Chat Subcollection with Anti-Bypass Filter ---
    fun observeChatMessages(orderId: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatRef = ordersRef.document(orderId).collection("chat_messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = chatRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "orders/$orderId/chat_messages")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun sendChatMessage(orderId: String, message: ChatMessage): Result<Unit> {
        if (isBypassDetected(message.text)) {
            return Result.failure(
                IllegalStateException("تنبيه أمني: يُمنع تبادل أرقام الهواتف أو وسائل التواصل الخارجي لضمان حقوقك داخل المنصة.")
            )
        }
        return try {
            val chatRef = ordersRef.document(orderId).collection("chat_messages")
            val payload = hashMapOf(
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderRole" to message.senderRole,
                "text" to message.text,
                "timestamp" to FieldValue.serverTimestamp()
            )
            chatRef.add(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "orders/$orderId/chat_messages")
            Result.failure(e)
        }
    }
}
