package com.example.data.repository

import android.content.Context
import com.example.data.model.OfferItem
import com.example.util.OperationType
import com.example.util.getFirestoreInstance
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OfferRepository(context: Context) {
    private val db: FirebaseFirestore = getFirestoreInstance(context)
    private val offersRef = db.collection("offers")

    fun observeOffers(): Flow<List<OfferItem>> = callbackFlow {
        val listener = offersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "offers")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(OfferItem::class.java)?.copy(id = doc.id)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addOffer(offer: OfferItem) {
        try {
            val payload = hashMapOf(
                "title" to offer.title,
                "description" to offer.description,
                "imageUrl" to offer.imageUrl,
                "discountBadge" to offer.discountBadge,
                "category" to offer.category,
                "isActive" to offer.isActive,
                "createdAt" to FieldValue.serverTimestamp()
            )
            offersRef.add(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "offers")
        }
    }

    suspend fun deleteOffer(offerId: String) {
        if (offerId.isBlank()) return
        try {
            offersRef.document(offerId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "offers/$offerId")
        }
    }

    suspend fun seedInitialOffersIfEmpty() {
        try {
            val snapshot = offersRef.limit(1).get().await()
            if (snapshot.isEmpty) {
                val defaults = listOf(
                    OfferItem(
                        title = "باقة واجهات الكلادنج الفندقية والتجارية",
                        description = "تصميم 3D احترافي + تنفيذ كلادنج الكبوند ألماني مقاوم للحريق مع ضمان 10 سنوات",
                        imageUrl = "https://images.unsplash.com/photo-1541888946425-d0fbb186f5f7?w=800",
                        discountBadge = "خصم 20% لفترة محدودة",
                        category = "كلادنج وواجهات",
                        isActive = true
                    ),
                    OfferItem(
                        title = "عرض الحروف البارزة 3D الإضاءة المخفية",
                        description = "اكريليك ليزر كاست نقي مع حقن ليد كوري ضد الماء وهيكل ستانلس ستيل ذهبي/فضي",
                        imageUrl = "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=800",
                        discountBadge = "عرض حصري للمحلات",
                        category = "حروف مضيئة",
                        isActive = true
                    ),
                    OfferItem(
                        title = "باقة الهوية البصرية والمطبوعات الورقية الكاملة",
                        description = "تصميم شعار معتمد + فولدرات + بروشورات وبطاقات أعمال فاخرة سبوت يو في",
                        imageUrl = "https://images.unsplash.com/photo-1626785774573-4b799315345d?w=800",
                        discountBadge = "الأكثر طلباً",
                        category = "هوية بصرية",
                        isActive = true
                    )
                )
                for (item in defaults) {
                    addOffer(item)
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "offers")
        }
    }
}
