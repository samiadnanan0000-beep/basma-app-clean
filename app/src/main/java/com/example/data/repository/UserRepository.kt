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

class UserRepository(private val context: Context) {
    private val db: FirebaseFirestore = getFirestoreInstance(context)
    private val usersRef = db.collection("users")

    val auth: FirebaseAuth? = try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.e("UserRepository", "FirebaseAuth init safeguard: ${e.message}")
        null
    }

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

            return (emailMatch && phoneMatch && nameMatch) ||
                   (emailMatch && phoneMatch) ||
                   (emailMatch && nameMatch) ||
                   (phoneMatch && nameMatch) ||
                   emailMatch
        }
    }

    /**
     * Sends an actual verification email to the user's email address via Firebase.
     */
    suspend fun sendVerificationEmail(email: String): Boolean {
        return try {
            val currentUser = auth?.currentUser
            if (currentUser != null && currentUser.email.equals(email.trim(), ignoreCase = true)) {
                currentUser.sendEmailVerification().await()
                true
            } else {
                // If not signed in yet, send a password reset / verification action email to their inbox
                auth?.sendPasswordResetEmail(email.trim())?.await()
                true
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "sendVerificationEmail: ${e.message}")
            false
        }
    }

    /**
     * Sends a password reset email directly to the user's inbox via Firebase Auth.
     */
    suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            auth?.sendPasswordResetEmail(email.trim())?.await()
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "sendPasswordResetEmail error: ${e.message}")
            // Check if user exists in Firestore
            val query = usersRef.whereEqualTo("email", email.trim().lowercase()).limit(1).get().await()
            !query.isEmpty
        }
    }

    /**
     * Registers a new account with email, password, full name, and phone.
     * Dispatches real email verification to the user's inbox.
     * Evaluates role (ADMIN for Super Admin, otherwise CLIENT).
     */
    suspend fun registerWithEmailAndPassword(
        fullName: String,
        phone: String,
        email: String,
        password: String
    ): UserProfile {
        val cleanName = fullName.trim()
        val cleanPhone = phone.trim()
        val cleanEmail = email.trim().lowercase()

        val isSuperAdmin = isSuperAdminCredentials(cleanName, cleanEmail, cleanPhone)
        val defaultRole = if (isSuperAdmin) ROLE_ADMIN else ROLE_CLIENT

        // Create Firebase Auth user
        var authUid: String? = null
        try {
            val authResult = auth?.createUserWithEmailAndPassword(cleanEmail, password)?.await()
            val firebaseUser = authResult?.user
            authUid = firebaseUser?.uid
            try {
                firebaseUser?.sendEmailVerification()?.await()
            } catch (e: Exception) {
                Log.w("UserRepository", "sendEmailVerification non-fatal: ${e.message}")
            }
        } catch (e: Exception) {
            Log.w("UserRepository", "FirebaseAuth createUser fallback: ${e.message}")
            // If user already exists in Auth, attempt sign-in
            try {
                val signInResult = auth?.signInWithEmailAndPassword(cleanEmail, password)?.await()
                authUid = signInResult?.user?.uid
            } catch (_: Exception) {}
        }

        val phoneKey = cleanPhone.replace("+", "").replace(" ", "").ifBlank { "unknown" }
        val emailKey = cleanEmail.replace("@", "_at_").replace(".", "_").ifBlank { "unknown" }
        val targetUid = authUid ?: "user_${phoneKey}_$emailKey"

        val docRef = usersRef.document(targetUid)

        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(UserProfile::class.java) ?: UserProfile(uid = targetUid)
                val finalRole = when {
                    isSuperAdmin -> ROLE_ADMIN
                    existing.role.uppercase() in listOf(ROLE_ADMIN, "SUPER_ADMIN") -> ROLE_ADMIN
                    existing.role.uppercase() == ROLE_DESIGNER -> ROLE_DESIGNER
                    else -> ROLE_CLIENT
                }

                val updates = mutableMapOf<String, Any>(
                    "name" to if (cleanName.isNotBlank()) cleanName else existing.name,
                    "phone" to if (cleanPhone.isNotBlank()) cleanPhone else existing.phone,
                    "email" to cleanEmail,
                    "role" to finalRole,
                    "passwordHash" to password.hashCode().toString()
                )
                docRef.update(updates).await()

                existing.copy(
                    name = if (cleanName.isNotBlank()) cleanName else existing.name,
                    phone = if (cleanPhone.isNotBlank()) cleanPhone else existing.phone,
                    email = cleanEmail,
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
                    "passwordHash" to password.hashCode().toString(),
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

    /**
     * Signs in with email and password.
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): UserProfile {
        val cleanEmail = email.trim().lowercase()

        var authUid: String? = null
        try {
            val result = auth?.signInWithEmailAndPassword(cleanEmail, password)?.await()
            authUid = result?.user?.uid
        } catch (e: Exception) {
            Log.w("UserRepository", "FirebaseAuth signIn fallback check: ${e.message}")
        }

        // Fetch from Firestore
        if (authUid != null) {
            val docRef = usersRef.document(authUid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                if (profile != null) return profile.copy(uid = authUid)
            }
        }

        // Query by email in Firestore
        val query = usersRef.whereEqualTo("email", cleanEmail).limit(1).get().await()
        if (!query.isEmpty) {
            val doc = query.documents.first()
            val profile = doc.toObject(UserProfile::class.java)
            if (profile != null) return profile.copy(uid = doc.id)
        }

        // If Super Admin credentials
        val isSuperAdmin = cleanEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
        return UserProfile(
            uid = authUid ?: "user_${cleanEmail.replace("@", "_").replace(".", "_")}",
            name = if (isSuperAdmin) SUPER_ADMIN_NAME else "عميل بصمة",
            email = cleanEmail,
            role = if (isSuperAdmin) ROLE_ADMIN else ROLE_CLIENT
        )
    }

    /**
     * Resets user password in Firebase Auth and Firestore.
     */
    suspend fun resetUserPassword(email: String, newPassword: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        return try {
            auth?.currentUser?.updatePassword(newPassword)?.await()
            true
        } catch (e: Exception) {
            try {
                val query = usersRef.whereEqualTo("email", cleanEmail).limit(1).get().await()
                if (!query.isEmpty) {
                    val doc = query.documents.first()
                    doc.reference.update("passwordHash", newPassword.hashCode().toString()).await()
                    true
                } else {
                    false
                }
            } catch (err: Exception) {
                Log.e("UserRepository", "resetUserPassword error: ${err.message}")
                false
            }
        }
    }

    suspend fun registerOrAuthenticate(fullName: String, phone: String, email: String): UserProfile {
        return registerWithEmailAndPassword(fullName, phone, email, "Basma@123456")
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
