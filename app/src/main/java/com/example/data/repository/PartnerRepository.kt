package com.example.data.repository

import android.content.Context
import com.example.data.model.PartnerPrintShop
import com.example.util.OperationType
import com.example.util.getFirestoreInstance
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PartnerRepository(context: Context) {
    private val db: FirebaseFirestore = getFirestoreInstance(context)
    private val partnersRef = db.collection("partners")

    fun observePartners(): Flow<List<PartnerPrintShop>> = callbackFlow {
        val listener = partnersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "partners")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(PartnerPrintShop::class.java)?.copy(id = doc.id)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addPartner(partner: PartnerPrintShop) {
        try {
            val payload = hashMapOf(
                "name" to partner.name,
                "phone" to partner.phone,
                "printType" to partner.printType,
                "costTotal" to partner.costTotal,
                "currency" to partner.currency,
                "notes" to partner.notes,
                "createdAt" to FieldValue.serverTimestamp()
            )
            partnersRef.add(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "partners")
        }
    }

    suspend fun updatePartner(partner: PartnerPrintShop) {
        if (partner.id.isBlank()) return
        try {
            val payload = mapOf(
                "name" to partner.name,
                "phone" to partner.phone,
                "printType" to partner.printType,
                "costTotal" to partner.costTotal,
                "currency" to partner.currency,
                "notes" to partner.notes
            )
            partnersRef.document(partner.id).update(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "partners/${partner.id}")
        }
    }

    suspend fun deletePartner(partnerId: String) {
        if (partnerId.isBlank()) return
        try {
            partnersRef.document(partnerId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "partners/$partnerId")
        }
    }

    suspend fun seedInitialPartnersIfEmpty() {
        try {
            val snapshot = partnersRef.limit(1).get().await()
            if (snapshot.isEmpty) {
                val defaults = listOf(
                    PartnerPrintShop(name = "مطبعة الصمود للطباعة الرقمية والأوفست", phone = "770000001", printType = "طباعة أوفست وبروشورات", costTotal = 150000.0, currency = "YER", notes = "تسليم خلال 48 ساعة"),
                    PartnerPrintShop(name = "مركز إكستريم لحفر وتقطيع الليزر والراوتر", phone = "770000002", printType = "حفر اكريليك وزنكور 3D", costTotal = 95000.0, currency = "YER", notes = "دقة ليزر عالية لأحرف الصيدليات"),
                    PartnerPrintShop(name = "مؤسسة الأفق لطباعة الفلكس والبنرات الضخمة", phone = "770000003", printType = "فلكس كوري وبنرات يوني بول", costTotal = 230000.0, currency = "YER", notes = "ماكينات إنفينيتي 3.20 متر")
                )
                for (p in defaults) {
                    addPartner(p)
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "partners")
        }
    }
}
