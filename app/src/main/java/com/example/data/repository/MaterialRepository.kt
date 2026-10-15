package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.MaterialItem
import com.example.util.OperationType
import com.example.util.getFirestoreInstance
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MaterialRepository(context: Context) {
    private val db: FirebaseFirestore = getFirestoreInstance(context)
    private val materialsRef = db.collection("materials")
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        val DEFAULT_MATERIALS = listOf(
            MaterialItem(id = "m1", name = "كلادنج الكبوند مقاوم للحريق 4ملم", unitType = "متر مربع", pricePerUnit = 22000.0, quantity = 100.0, currency = "YER"),
            MaterialItem(id = "m2", name = "اكريليك كاست ملون نقي 3ملم", unitType = "متر مربع", pricePerUnit = 18500.0, quantity = 50.0, currency = "YER"),
            MaterialItem(id = "m3", name = "فلكس كوري مضيء عالي الكثافة 510 جم", unitType = "متر مربع", pricePerUnit = 6500.0, quantity = 200.0, currency = "YER"),
            MaterialItem(id = "m4", name = "استكر فينيل ألماني عازل للأشعة", unitType = "متر مربع", pricePerUnit = 4800.0, quantity = 150.0, currency = "YER"),
            MaterialItem(id = "m5", name = "شاسيه حديد هيكلي تيوبات 2.5×2.5", unitType = "متر طولي", pricePerUnit = 3200.0, quantity = 80.0, currency = "YER"),
            MaterialItem(id = "m6", name = "زنكور مجلفن مقاوم للصدأ سماكة 0.8ملم", unitType = "متر مربع", pricePerUnit = 11000.0, quantity = 40.0, currency = "YER"),
            MaterialItem(id = "m7", name = "حقن ليد كوري عدسة عريضة 1.5W مقاوم للماء", unitType = "قطعة", pricePerUnit = 450.0, quantity = 500.0, currency = "YER"),
            MaterialItem(id = "m8", name = "محول طاقة ترانس معالج 12V 400W أصلي", unitType = "قطعة", pricePerUnit = 19500.0, quantity = 25.0, currency = "YER")
        )
    }

    private val _materialsState = MutableStateFlow<List<MaterialItem>>(DEFAULT_MATERIALS)
    val materialsFlow: StateFlow<List<MaterialItem>> = _materialsState.asStateFlow()

    init {
        // Real-time Firestore sync
        try {
            materialsRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "materials")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val firestoreList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(MaterialItem::class.java)?.copy(id = doc.id)
                    }
                    if (firestoreList.isNotEmpty()) {
                        _materialsState.value = firestoreList
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MaterialRepository", "Snapshot listener safeguard: ${e.message}")
        }
    }

    fun observeMaterials(): StateFlow<List<MaterialItem>> = materialsFlow

    suspend fun addMaterial(material: MaterialItem): Boolean {
        val uniqueId = if (material.id.isNotBlank()) material.id else "mat_${UUID.randomUUID().toString().take(8)}"
        val itemWithId = material.copy(id = uniqueId)

        // Instant local update for seamless immediate UI recomposition
        _materialsState.value = listOf(itemWithId) + _materialsState.value.filter { it.id != uniqueId }

        return try {
            val payload = hashMapOf(
                "name" to itemWithId.name,
                "unitType" to itemWithId.unitType,
                "pricePerUnit" to itemWithId.pricePerUnit,
                "quantity" to itemWithId.quantity,
                "currency" to itemWithId.currency,
                "inStock" to itemWithId.inStock,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            materialsRef.document(uniqueId).set(payload).await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "materials/$uniqueId")
            // Remains active locally even if offline
            true
        }
    }

    suspend fun updateMaterial(material: MaterialItem): Boolean {
        if (material.id.isBlank()) return false

        // Instant local update
        _materialsState.value = _materialsState.value.map {
            if (it.id == material.id) material else it
        }

        return try {
            val payload = mapOf(
                "name" to material.name,
                "unitType" to material.unitType,
                "pricePerUnit" to material.pricePerUnit,
                "quantity" to material.quantity,
                "currency" to material.currency,
                "inStock" to material.inStock,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            materialsRef.document(material.id).update(payload).await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "materials/${material.id}")
            true
        }
    }

    suspend fun deleteMaterial(materialId: String): Boolean {
        if (materialId.isBlank()) return false

        // Instant local update
        _materialsState.value = _materialsState.value.filter { it.id != materialId }

        return try {
            materialsRef.document(materialId).delete().await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "materials/$materialId")
            true
        }
    }

    suspend fun seedInitialMaterialsIfEmpty() {
        try {
            val snapshot = materialsRef.limit(1).get().await()
            if (snapshot.isEmpty) {
                for (item in DEFAULT_MATERIALS) {
                    addMaterial(item)
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "materials")
        }
    }
}
