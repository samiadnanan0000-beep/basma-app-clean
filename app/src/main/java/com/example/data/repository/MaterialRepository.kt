package com.example.data.repository

import android.content.Context
import com.example.data.model.MaterialItem
import com.example.util.OperationType
import com.example.util.getFirestoreInstance
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MaterialRepository(context: Context) {
    private val db: FirebaseFirestore = getFirestoreInstance(context)
    private val materialsRef = db.collection("materials")

    fun observeMaterials(): Flow<List<MaterialItem>> = callbackFlow {
        val listener = materialsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "materials")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(MaterialItem::class.java)?.copy(id = doc.id)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addMaterial(material: MaterialItem) {
        try {
            val payload = hashMapOf(
                "name" to material.name,
                "unitType" to material.unitType,
                "pricePerUnit" to material.pricePerUnit,
                "currency" to material.currency,
                "inStock" to material.inStock,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            materialsRef.add(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "materials")
        }
    }

    suspend fun updateMaterial(material: MaterialItem) {
        if (material.id.isBlank()) return
        try {
            val payload = mapOf(
                "name" to material.name,
                "unitType" to material.unitType,
                "pricePerUnit" to material.pricePerUnit,
                "currency" to material.currency,
                "inStock" to material.inStock,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            materialsRef.document(material.id).update(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "materials/${material.id}")
        }
    }

    suspend fun deleteMaterial(materialId: String) {
        if (materialId.isBlank()) return
        try {
            materialsRef.document(materialId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "materials/$materialId")
        }
    }

    suspend fun seedInitialMaterialsIfEmpty() {
        try {
            val snapshot = materialsRef.limit(1).get().await()
            if (snapshot.isEmpty) {
                val defaults = listOf(
                    MaterialItem(name = "كلادنج الكبوند مقاوم للحريق 4ملم", unitType = "متر مربع", pricePerUnit = 22000.0, currency = "YER"),
                    MaterialItem(name = "اكريليك كاست ملون نقي 3ملم", unitType = "متر مربع", pricePerUnit = 18500.0, currency = "YER"),
                    MaterialItem(name = "فلكس كوري مضيء عالي الكثافة 510 جم", unitType = "متر مربع", pricePerUnit = 6500.0, currency = "YER"),
                    MaterialItem(name = "استكر فينيل ألماني عازل للأشعة", unitType = "متر مربع", pricePerUnit = 4800.0, currency = "YER"),
                    MaterialItem(name = "شاسيه حديد هيكلي تيوبات 2.5×2.5", unitType = "متر طولي", pricePerUnit = 3200.0, currency = "YER"),
                    MaterialItem(name = "زنكور مجلفن مقاوم للصدأ سماكة 0.8ملم", unitType = "متر مربع", pricePerUnit = 11000.0, currency = "YER"),
                    MaterialItem(name = "حقن ليد كوري عدسة عريضة 1.5W مقاوم للماء", unitType = "قطعة", pricePerUnit = 450.0, currency = "YER"),
                    MaterialItem(name = "محول طاقة ترانس معالج 12V 400W أصلي", unitType = "قطعة", pricePerUnit = 19500.0, currency = "YER")
                )
                for (item in defaults) {
                    addMaterial(item)
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "materials")
        }
    }
}
