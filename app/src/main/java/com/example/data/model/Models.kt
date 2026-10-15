package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "client", // "super_admin", "designer", "client"
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OrderItem(
    @DocumentId val id: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val category: String = "كلادنج وفلكس",
    val materialId: String = "",
    val materialName: String = "",
    val width: Double = 0.0,
    val height: Double = 0.0,
    val brandText: String = "",
    val contactDetails: String = "",
    val notes: String = "",
    val logoUrl: String = "",
    val sitePhotoUrl: String = "",
    val status: String = "pending", // "pending", "quoted", "in_production", "completed"
    val price: Double = 0.0,
    val currency: String = "YER",
    val turnaroundTime: String = "",
    val proofUrl: String = "",
    val proofApproved: Boolean = false,
    val rejectionReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MaterialItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val unitType: String = "متر مربع", // "متر مربع", "لوح", "قطعة", "متر طولي"
    val pricePerUnit: Double = 0.0,
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
