package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.PortfolioItem
import com.example.util.OperationType
import com.example.util.getFirestoreInstance
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PortfolioRepository(context: Context) {
    private val db: FirebaseFirestore = getFirestoreInstance(context)
    private val portfolioRef = db.collection("portfolio")

    companion object {
        val DEFAULT_PORTFOLIO_ITEMS = listOf(
            PortfolioItem(
                id = "port_1",
                title = "واجهة كلادنج بنك التضامن",
                description = "تنفيذ واجهة كلادنج ألماني مقاوم للحرائق 4 ملم مع حروف بارزة مضيئة ليد كوري عالي الوضوح.",
                imageUrl = "https://images.unsplash.com/photo-1541888946425-d0fbb186f5f7?w=900&auto=format&fit=crop",
                designerName = "سامي البكري",
                createdAt = 1715000000000L
            ),
            PortfolioItem(
                id = "port_2",
                title = "لوحة إعلانية زنكور ثلاثية الأبعاد",
                description = "تصميم وتنفيذ لوحة زنكور ذهبي مصقول 3D لمحلات مجوهرات السعادة بإنارة خفية خلفية (Backlit).",
                imageUrl = "https://images.unsplash.com/photo-1572945553610-826d9e03d79e?w=900&auto=format&fit=crop",
                designerName = "فريق تصميم بصمة",
                createdAt = 1715100000000L
            ),
            PortfolioItem(
                id = "port_3",
                title = "هوية بصرية وطباعة أوفست كبرى",
                description = "طباعة كتالوجات فاخرة، كروت أعمال UV، وفولدرات مونوكروم لمجموعة شركات تجارية.",
                imageUrl = "https://images.unsplash.com/photo-1586075010923-2dd4570fb338?w=900&auto=format&fit=crop",
                designerName = "م. أحمد الشرجبي",
                createdAt = 1715200000000L
            ),
            PortfolioItem(
                id = "port_4",
                title = "شاشات ليد وبروفايل فلكس مضيء",
                description = "تركيب واجهة فلكس كوري 510 جم بمقاس 14×4 متر مع محولات طاقة 400W مقاومة للظروف الجوية.",
                imageUrl = "https://images.unsplash.com/photo-1513151233558-d860c5398176?w=900&auto=format&fit=crop",
                designerName = "سامي البكري",
                createdAt = 1715300000000L
            ),
            PortfolioItem(
                id = "port_5",
                title = "اكريليك ليزر كاست مخصص",
                description = "حروف اكريليك ليزر ملون بسماكة 3 ملم مع قاعدة استيل ذهبي مرايا لمطاعم القصر الفاخرة.",
                imageUrl = "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=900&auto=format&fit=crop",
                designerName = "فريق تصميم بصمة",
                createdAt = 1715400000000L
            ),
            PortfolioItem(
                id = "port_6",
                title = "استكرات فينيل ألماني وزجاج مثلج",
                description = "تغليف واجهات زجاجية باستكرات عازلة مع طباعة فوتوغرافية وتفريغ ليزر دقيق للمكاتب التنفيذية.",
                imageUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=900&auto=format&fit=crop",
                designerName = "م. رامي القدسي",
                createdAt = 1715500000000L
            )
        )
    }

    private val _portfolioState = MutableStateFlow<List<PortfolioItem>>(DEFAULT_PORTFOLIO_ITEMS)
    val portfolioFlow = _portfolioState.asStateFlow()

    init {
        try {
            portfolioRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "portfolio")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val p = doc.toObject(PortfolioItem::class.java)
                        val fallbackUrl = doc.getString("imageUrl") ?: doc.getString("portfolioUrl") ?: ""
                        p?.copy(
                            id = doc.id,
                            imageUrl = if (p.imageUrl.isNotBlank()) p.imageUrl else fallbackUrl,
                            portfolioUrl = if (p.portfolioUrl.isNotBlank()) p.portfolioUrl else fallbackUrl
                        )
                    }
                    if (list.isNotEmpty()) {
                        _portfolioState.value = list
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "Firestore listener safeguard: ${e.message}")
        }
    }

    /**
     * Real-time flow of portfolio items with seamless fallback.
     */
    fun getPortfolioItems(): Flow<List<PortfolioItem>> = portfolioFlow

    /**
     * Adds a new portfolio item to Firestore and immediately reflects in state.
     */
    suspend fun addPortfolioItem(item: PortfolioItem): Boolean {
        val uniqueId = if (item.id.isNotBlank()) item.id else "port_${UUID.randomUUID().toString().take(8)}"
        val finalUrl = item.effectiveImageUrl
        val itemWithId = item.copy(id = uniqueId, imageUrl = finalUrl, portfolioUrl = finalUrl)

        // Instant local update
        _portfolioState.value = listOf(itemWithId) + _portfolioState.value.filter { it.id != uniqueId }

        return try {
            val payload = hashMapOf(
                "title" to itemWithId.title,
                "description" to itemWithId.description,
                "imageUrl" to finalUrl,
                "portfolioUrl" to finalUrl,
                "designerName" to itemWithId.designerName,
                "createdAt" to itemWithId.createdAt
            )
            portfolioRef.document(uniqueId).set(payload).await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "portfolio/$uniqueId")
            true
        }
    }

    /**
     * Deletes a portfolio item from Firestore and updates state immediately.
     */
    suspend fun deletePortfolioItem(itemId: String): Boolean {
        if (itemId.isBlank()) return false

        // Instant local update
        _portfolioState.value = _portfolioState.value.filter { it.id != itemId }

        return try {
            portfolioRef.document(itemId).delete().await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "portfolio/$itemId")
            true
        }
    }

    suspend fun seedInitialPortfolioIfEmpty() {
        try {
            val snapshot = portfolioRef.limit(1).get().await()
            if (snapshot.isEmpty) {
                for (item in DEFAULT_PORTFOLIO_ITEMS) {
                    addPortfolioItem(item)
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "portfolio")
        }
    }
}
