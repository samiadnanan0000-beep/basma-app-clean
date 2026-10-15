package com.example.data.repository

import android.content.Context
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.getFirestoreInstance
import com.example.util.handleFirestoreError
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(context: Context) {
    private val db: FirebaseFirestore = getFirestoreInstance(context)
    private val usersRef = db.collection("users")

    companion object {
        const val SUPER_ADMIN_EMAIL = "samialbakri99@gmail.com"
        const val SUPER_ADMIN_PHONE = "771051177"
    }

    suspend fun syncUserProfile(firebaseUser: FirebaseUser, phoneInput: String? = null): UserProfile {
        val uid = firebaseUser.uid
        val docRef = usersRef.document(uid)
        return try {
            val snapshot = docRef.get().await()
            val email = firebaseUser.email.orEmpty()
            val phone = phoneInput ?: firebaseUser.phoneNumber.orEmpty()
            val isSuperAdmin = email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)

            if (snapshot.exists()) {
                val existing = snapshot.toObject(UserProfile::class.java) ?: UserProfile(uid = uid)
                val finalRole = if (isSuperAdmin) "super_admin" else existing.role
                if (finalRole != existing.role || (phone.isNotBlank() && existing.phone.isBlank())) {
                    val updates = mutableMapOf<String, Any>("role" to finalRole)
                    if (phone.isNotBlank()) updates["phone"] = phone
                    docRef.update(updates).await()
                }
                existing.copy(role = finalRole, phone = if (phone.isNotBlank()) phone else existing.phone)
            } else {
                val newRole = if (isSuperAdmin) "super_admin" else "client"
                val profile = UserProfile(
                    uid = uid,
                    name = firebaseUser.displayName ?: if (isSuperAdmin) "سامي البكري (المدير العام)" else "عميل جديد",
                    email = email,
                    phone = phone,
                    role = newRole
                )
                val payload = hashMapOf(
                    "name" to profile.name,
                    "email" to profile.email,
                    "phone" to profile.phone,
                    "role" to profile.role,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                profile
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.READ, docRef.path)
            // Fallback object to not crash app flow
            val isSuperAdmin = (firebaseUser.email.orEmpty()).equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
            UserProfile(
                uid = uid,
                name = firebaseUser.displayName ?: "مستخدم بصمة",
                email = firebaseUser.email.orEmpty(),
                phone = phoneInput.orEmpty(),
                role = if (isSuperAdmin) "super_admin" else "client"
            )
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = usersRef.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.READ, "users/$uid")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = callbackFlow {
        val listener = usersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "users")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(UserProfile::class.java) }.orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateUserRole(targetUid: String, newRole: String) {
        try {
            usersRef.document(targetUid).update("role", newRole).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
        }
    }
}
