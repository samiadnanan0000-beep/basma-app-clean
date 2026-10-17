package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class PortfolioItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val portfolioUrl: String = "",
    val designerName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val effectiveImageUrl: String
        get() = imageUrl.ifBlank { portfolioUrl }

    val isWebUrl: Boolean
        get() = effectiveImageUrl.startsWith("http://", ignoreCase = true) ||
                effectiveImageUrl.startsWith("https://", ignoreCase = true)
}
