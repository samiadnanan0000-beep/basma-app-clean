package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "client", // "super_admin" / "admin", "designer", "client"
    @get:PropertyName("isAccountActivated")
    @set:PropertyName("isAccountActivated")
    var isAccountActivated: Boolean = false,
    val activeTasksCount: Int = 0,
    val ratingAverage: Float = 5.0f,
    val portfolioLink: String = "",
    val bio: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

object PaymentStatus {
    const val NONE = "NONE"
    const val WAITING_FOR_ADVANCE = "WAITING_FOR_ADVANCE"
    const val WAITING_FOR_FINAL = "WAITING_FOR_FINAL"
    const val UNDER_REVIEW = "UNDER_REVIEW"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

object OrderStatus {
    const val PENDING_PRICING = "PENDING_PRICING"
    const val PRICE_OFFERED = "PRICE_OFFERED"
    const val WAITING_FOR_ADVANCE = "WAITING_FOR_ADVANCE"
    const val WAITING_FOR_ASSIGNMENT = "WAITING_FOR_ASSIGNMENT"
    const val PENDING_DESIGNER_ACCEPTANCE = "PENDING_DESIGNER_ACCEPTANCE"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val SAMPLE_REVIEW = "SAMPLE_REVIEW"
    const val WAITING_FOR_FINAL_PAYMENT = "WAITING_FOR_FINAL_PAYMENT"
    const val COMPLETED = "COMPLETED"
    const val CANCELLED = "CANCELLED"
}

@IgnoreExtraProperties
data class OrderItem(
    @DocumentId val id: String = "",
    val orderNumber: String = "", // e.g. #BSM-7716
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientEmail: String = "",
    val designType: String = "", // عنوان / نوع التصميم المكتوب بحرية
    val detailedSpecs: String = "", // تفاصيل وشروط التصميم المطلوبة
    val category: String = "أعمال فنية وتصميم",
    val materialId: String = "",
    val materialName: String = "",
    val width: Double = 0.0,
    val height: Double = 0.0,
    val brandText: String = "",
    val contactDetails: String = "",
    val notes: String = "",
    val logoUrl: String = "",
    val sitePhotoUrl: String = "",
    val referenceAttachmentUrl: String = "",
    val status: String = OrderStatus.PENDING_PRICING,
    val price: Double = 0.0,
    // Dual pricing model:
    val designAmount: Double = 0.0, // أتعاب التصميم
    val productionAmount: Double = 0.0, // تكاليف التنفيذ والطباعة والمواد (0 للتصاميم الرقمية)
    val totalAmount: Double = 0.0, // designAmount + productionAmount
    val advanceAmount: Double = 0.0, // 70%
    val remainingAmount: Double = 0.0, // 30%
    val designerFee: Double = 0.0, // صافي مستحقات المصمم من التصميم
    val platformFee: Double = 0.0, // عمولة المنصة من التصميم
    val paymentMethod: String = "", // "محفظة جيب", "محفظة جوالي", "محفظة ون كاش"
    val receiptImageUrl: String? = null,
    val advanceReceiptUrl: String = "",
    val finalReceiptUrl: String = "",
    val paymentStatus: String = PaymentStatus.NONE,
    val advancePaymentStatus: String = PaymentStatus.NONE,
    val finalPaymentStatus: String = PaymentStatus.NONE,
    val isApprovedByDesignerOrAdmin: Boolean = false,
    val paymentRejectionReason: String = "",
    val currency: String = "YER",
    val turnaroundTime: String = "",
    val estimatedDeliveryTime: String? = null,
    val proofUrl: String = "",
    val proofApproved: Boolean = false,
    val rejectionReason: String = "",
    val assignedDesignerId: String? = null,
    val designerName: String? = null,
    val clientRating: Float = 0f,
    val clientReview: String? = null,
    val rejectionHistory: List<Map<String, Any>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val displayOrderNumber: String
        get() = orderNumber.ifBlank { "#BSM-${id.takeLast(4).uppercase()}" }

    val effectiveTotal: Double
        get() = if (totalAmount > 0.0) totalAmount else if (price > 0.0) price else (designAmount + productionAmount)

    val calculatedAdvance: Double
        get() = if (advanceAmount > 0.0) advanceAmount else (effectiveTotal * 0.70)

    val calculatedRemaining: Double
        get() = if (remainingAmount > 0.0) remainingAmount else (effectiveTotal * 0.30)
}

@IgnoreExtraProperties
data class MaterialItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val unitType: String = "متر مربع", // "متر مربع", "لوح", "قطعة", "متر طولي"
    val pricePerUnit: Double = 0.0,
    val quantity: Double = 1.0,
    val currency: String = "YER",
    val inStock: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "general", // "offer", "order", "general", "complaint"
    val targetId: String = "all", // "all" or specific client uid
    val isRead: Boolean = false,
    val relatedId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ComplaintTicket(
    @DocumentId val id: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val type: String = "complaint", // "complaint", "suggestion", "inquiry"
    val subject: String = "",
    val message: String = "",
    val orderId: String = "",
    val status: String = "pending", // "pending", "resolved"
    val replyMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val resolvedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OfferItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val discountBadge: String = "",
    val category: String = "",
    val discountPercentage: Double = 0.0,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PartnerPrintShop(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val printType: String = "", // "فلكس وبنر", "أوفست ومجلات", "سلك سكرين", "حفر ليزر"
    val costTotal: Double = 0.0,
    val currency: String = "YER",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "app_config",
    val whatsappNumber: String = "967771051177",
    val platformCommissionPercentage: Double = 20.0,
    val beneficiaryName: String = "سامي عدنان حسن البكري",
    val beneficiaryAccount: String = "771051177"
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class AppVersionConfig(
    @DocumentId val id: String = "app_version",
    val latest_version_code: Int = 1,
    val apk_download_url: String = "",
    val is_mandatory: Boolean = false,
    val release_notes: String = ""
)

@IgnoreExtraProperties
data class DesignerUpgradeRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val specialties: String = "",
    val experienceAreas: String = "",
    val portfolioUrl: String = "",
    val notes: String = "",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val displaySpecialties: String
        get() = specialties.ifBlank { experienceAreas }
}
