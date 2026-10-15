package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.getFirestoreInstance
import com.example.util.handleFirestoreError
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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
        const val SUPER_ADMIN_NAME = "سامي عدنان حسن سيف البكري"

        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_DESIGNER = "DESIGNER"
        const val ROLE_CLIENT = "CLIENT"

        /**
         * Checks if provided registration or user details match the Super Admin credentials.
         */
        fun isSuperAdminCredentials(name: String, email: String, phone: String): Boolean {
            val cleanEmail = email.trim().lowercase()
            val cleanPhone = phone.trim().replace(" ", "").replace("+967", "").replace("00967", "")
            val cleanName = name.trim()

            val targetEmail = SUPER_ADMIN_EMAIL.lowercase()
            val targetPhone = SUPER_ADMIN_PHONE
            val targetName = SUPER_ADMIN_NAME

            val emailMatch = cleanEmail == targetEmail
            val phoneMatch = cleanPhone == targetPhone || cleanPhone.endsWith(targetPhone)
            val nameMatch = cleanName == targetName || (cleanName.contains("سامي") && (cleanName.contains("البكري") || cleanName.contains("سيف")))

            // Check if matches specified admin credentials
            return (emailMatch && phoneMatch && nameMatch) ||
                   (emailMatch && phoneMatch) ||
                   (emailMatch && nameMatch) ||
                   (phoneMatch && nameMatch) ||
                   emailMatch
        }
    }

    /**
     * Registers a new user or authenticates an existing user.
     * Assigns ROLE_ADMIN if Super Admin credentials match.
     * All new users are registered as ROLE_CLIENT by default.
     * Existing users upgraded to ROLE_DESIGNER retain their role.
     */
    suspend fun registerOrAuthenticate(fullName: String, phone: String, email: String): UserProfile {
        val cleanName = fullName.trim()
        val cleanPhone = phone.trim()
        val cleanEmail = email.trim().lowercase()

        val isSuperAdmin = isSuperAdminCredentials(cleanName, cleanEmail, cleanPhone)
        val defaultRole = if (isSuperAdmin) ROLE_ADMIN else ROLE_CLIENT

        // Generate a stable UID based on phone or email if no Firebase Auth UID is active
        val activeUid = try {
            val app = if (FirebaseApp.getApps(db.app.applicationContext).isNotEmpty()) FirebaseApp.getInstance() else null
            if (app != null) FirebaseAuth.getInstance(app).currentUser?.uid else null
        } catch (_: Exception) {
            null
        }

        val phoneKey = cleanPhone.replace("+", "").replace(" ", "").ifBlank { "unknown" }
        val emailKey = cleanEmail.replace("@", "_at_").replace(".", "_").ifBlank { "unknown" }
        val targetUid = activeUid ?: "user_${phoneKey}_$emailKey"

        val docRef = usersRef.document(targetUid)

        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(UserProfile::class.java) ?: UserProfile(uid = targetUid)
                // Determine final role: Super Admin always gets ADMIN; existing DESIGNER remains DESIGNER; otherwise existing role
                val finalRole = when {
                    isSuperAdmin -> ROLE_ADMIN
                    existing.role.uppercase() in listOf(ROLE_ADMIN, "SUPER_ADMIN") -> ROLE_ADMIN
                    existing.role.uppercase() == ROLE_DESIGNER -> ROLE_DESIGNER
                    else -> ROLE_CLIENT
                }

                val updates = mutableMapOf<String, Any>(
                    "name" to if (cleanName.isNotBlank()) cleanName else existing.name,
                    "phone" to if (cleanPhone.isNotBlank()) cleanPhone else existing.phone,
                    "email" to if (cleanEmail.isNotBlank()) cleanEmail else existing.email,
                    "role" to finalRole
                )
                docRef.update(updates).await()

                existing.copy(
                    name = if (cleanName.isNotBlank()) cleanName else existing.name,
                    phone = if (cleanPhone.isNotBlank()) cleanPhone else existing.phone,
                    email = if (cleanEmail.isNotBlank()) cleanEmail else existing.email,
                    role = finalRole
                )
            } else {
                val profile = UserProfile(
                    uid = targetUid,
                    name = cleanName.ifBlank { if (isSuperAdmin) SUPER_ADMIN_NAME else "عميل جديد" },
                    email = cleanEmail,
                    phone = cleanPhone,
                    role = defaultRole
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
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            UserProfile(
                uid = targetUid,
                name = cleanName.ifBlank { if (isSuperAdmin) SUPER_ADMIN_NAME else "مستخدم بصمة" },
                email = cleanEmail,
                phone = cleanPhone,
                role = defaultRole
            )
        }
    }

    suspend fun syncUserProfile(firebaseUser: FirebaseUser, phoneInput: String? = null): UserProfile {
        val uid = firebaseUser.uid
        val email = firebaseUser.email.orEmpty()
        val phone = phoneInput ?: firebaseUser.phoneNumber.orEmpty()
        val name = firebaseUser.displayName.orEmpty()
        val isSuperAdmin = isSuperAdminCredentials(name, email, phone)

        val docRef = usersRef.document(uid)

        return try {
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val existing = snapshot.toObject(UserProfile::class.java) ?: UserProfile(uid = uid)
                val finalRole = when {
                    isSuperAdmin -> ROLE_ADMIN
                    existing.role.uppercase() in listOf(ROLE_ADMIN, "SUPER_ADMIN") -> ROLE_ADMIN
                    existing.role.uppercase() == ROLE_DESIGNER -> ROLE_DESIGNER
                    else -> ROLE_CLIENT
                }
                if (finalRole != existing.role || (phone.isNotBlank() && existing.phone.isBlank())) {
                    val updates = mutableMapOf<String, Any>("role" to finalRole)
                    if (phone.isNotBlank()) updates["phone"] = phone
                    docRef.update(updates).await()
                }
                existing.copy(role = finalRole, phone = if (phone.isNotBlank()) phone else existing.phone)
            } else {
                val newRole = if (isSuperAdmin) ROLE_ADMIN else ROLE_CLIENT
                val profile = UserProfile(
                    uid = uid,
                    name = firebaseUser.displayName ?: if (isSuperAdmin) SUPER_ADMIN_NAME else "عميل جديد",
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
            UserProfile(
                uid = uid,
                name = firebaseUser.displayName ?: if (isSuperAdmin) SUPER_ADMIN_NAME else "مستخدم بصمة",
                email = email,
                phone = phone,
                role = if (isSuperAdmin) ROLE_ADMIN else ROLE_CLIENT
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
            val list = snapshot?.documents?.mapNotNull { doc ->
                val p = doc.toObject(UserProfile::class.java)
                p?.copy(uid = doc.id)
            }.orEmpty()
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
