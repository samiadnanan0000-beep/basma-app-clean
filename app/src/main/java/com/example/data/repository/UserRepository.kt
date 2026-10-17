package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.AppConfig
import com.example.data.model.AppVersionConfig
import com.example.data.model.DesignerUpgradeRequest
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
    private val settingsRef = db.collection("settings")
    private val designerRequestsRef = db.collection("designer_requests")

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
     * Sends an actual verification email to the user's email address via Firebase Auth.
     */
    suspend fun sendVerificationEmail(email: String): Boolean {
        val authInstance = auth ?: throw IllegalStateException("خدمة المصادقة غير مهيأة")
        val currentUser = authInstance.currentUser
        return if (currentUser != null && currentUser.email.equals(email.trim(), ignoreCase = true)) {
            currentUser.sendEmailVerification().await()
            true
        } else {
            authInstance.sendPasswordResetEmail(email.trim()).await()
            true
        }
    }

    /**
     * Sends a password reset email directly to the user's inbox via Firebase Auth.
     */
    suspend fun sendPasswordResetEmail(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val authInstance = auth ?: throw IllegalStateException("خدمة المصادقة غير متاحة حالياً")
        authInstance.sendPasswordResetEmail(cleanEmail).await()
        return true
    }

    /**
     * Registers a new account with email, password, full name, and phone.
     * Uses official Firebase Auth: createUserWithEmailAndPassword immediately followed by sendEmailVerification.
     * Saves user profile in Firestore (users collection).
     * Role is strictly: ADMIN if matches Super Admin credentials, otherwise CLIENT.
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

        val authInstance = auth ?: throw IllegalStateException("خدمة المصادقة غير مهيأة")

        // 1. Create user in Firebase Auth
        val authResult = authInstance.createUserWithEmailAndPassword(cleanEmail, password).await()
        val firebaseUser = authResult.user ?: throw IllegalStateException("فشل إنشاء المستخدم في Firebase")

        // 2. Immediately send official email verification in background
        try {
            firebaseUser.sendEmailVerification().await()
            Log.d("UserRepository", "Verification email sent successfully to $cleanEmail")
        } catch (e: Exception) {
            Log.w("UserRepository", "sendEmailVerification warning: ${e.message}")
        }

        // 3. Evaluate Super Admin credentials
        val isSuperAdmin = isSuperAdminCredentials(cleanName, cleanEmail, cleanPhone)
        val role = if (isSuperAdmin) ROLE_ADMIN else ROLE_CLIENT
        val isActivated = isSuperAdmin // Super admin is auto-activated; clients activate via WhatsApp

        val userUid = firebaseUser.uid
        val profile = UserProfile(
            uid = userUid,
            name = cleanName,
            phone = cleanPhone,
            email = cleanEmail,
            role = role,
            isAccountActivated = isActivated
        )

        // 4. Save to Firestore users collection
        val docRef = usersRef.document(userUid)
        try {
            val payload = hashMapOf(
                "name" to profile.name,
                "email" to profile.email,
                "phone" to profile.phone,
                "role" to profile.role,
                "isAccountActivated" to profile.isAccountActivated,
                "activeTasksCount" to 0,
                "ratingAverage" to 5.0f,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }

        return profile
    }

    /**
     * Strict Login Verification:
     * Disallows any mock bypass. Executes auth.signInWithEmailAndPassword(email, password) strictly.
     * Only returns UserProfile if authentication succeeds 100%.
     * Any exception is thrown to be mapped to clear UI feedback.
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): UserProfile {
        val cleanEmail = email.trim().lowercase()
        val authInstance = auth ?: throw IllegalStateException("خدمة المصادقة غير متاحة حالياً")

        // Strict authentication call - will throw on wrong password, non-existent user, or network failure
        val authResult = authInstance.signInWithEmailAndPassword(cleanEmail, password).await()
        val firebaseUser = authResult.user ?: throw IllegalStateException("فشل التحقق من حساب المستخدم")

        val uid = firebaseUser.uid
        val docRef = usersRef.document(uid)

        try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                if (profile != null) {
                    return profile.copy(uid = uid)
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.READ, docRef.path)
        }

        // Check if query by email in Firestore finds existing profile
        try {
            val query = usersRef.whereEqualTo("email", cleanEmail).limit(1).get().await()
            if (!query.isEmpty) {
                val doc = query.documents.first()
                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null) return profile.copy(uid = doc.id)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.READ, "users?email=$cleanEmail")
        }

        // Fallback profile creation for newly authenticated user in Firestore
        val isSuperAdmin = cleanEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
        val role = if (isSuperAdmin) ROLE_ADMIN else ROLE_CLIENT
        val newProfile = UserProfile(
            uid = uid,
            name = if (isSuperAdmin) SUPER_ADMIN_NAME else (firebaseUser.displayName ?: "عميل بصمة"),
            email = cleanEmail,
            phone = firebaseUser.phoneNumber.orEmpty(),
            role = role,
            isAccountActivated = isSuperAdmin
        )

        try {
            val payload = hashMapOf(
                "name" to newProfile.name,
                "email" to newProfile.email,
                "phone" to newProfile.phone,
                "role" to newProfile.role,
                "isAccountActivated" to newProfile.isAccountActivated,
                "activeTasksCount" to 0,
                "ratingAverage" to 5.0f,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }

        return newProfile
    }

    /**
     * Resets user password in Firebase Auth.
     */
    suspend fun resetUserPassword(email: String, newPassword: String): Boolean {
        return try {
            auth?.currentUser?.updatePassword(newPassword)?.await()
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "resetUserPassword: ${e.message}")
            false
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
                val activated = isSuperAdmin || existing.isAccountActivated
                val updates = mutableMapOf<String, Any>()
                if (finalRole != existing.role) updates["role"] = finalRole
                if (phone.isNotBlank() && existing.phone.isBlank()) updates["phone"] = phone
                if (activated != existing.isAccountActivated) updates["isAccountActivated"] = activated

                if (updates.isNotEmpty()) {
                    docRef.update(updates).await()
                }
                existing.copy(role = finalRole, phone = if (phone.isNotBlank()) phone else existing.phone, isAccountActivated = activated)
            } else {
                val newRole = if (isSuperAdmin) ROLE_ADMIN else ROLE_CLIENT
                val profile = UserProfile(
                    uid = uid,
                    name = firebaseUser.displayName ?: if (isSuperAdmin) SUPER_ADMIN_NAME else "عميل جديد",
                    email = email,
                    phone = phone,
                    role = newRole,
                    isAccountActivated = isSuperAdmin
                )
                val payload = hashMapOf(
                    "name" to profile.name,
                    "email" to profile.email,
                    "phone" to profile.phone,
                    "role" to profile.role,
                    "isAccountActivated" to profile.isAccountActivated,
                    "activeTasksCount" to 0,
                    "ratingAverage" to 5.0f,
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
                role = if (isSuperAdmin) ROLE_ADMIN else ROLE_CLIENT,
                isAccountActivated = isSuperAdmin
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
                trySend(profile?.copy(uid = snapshot.id))
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

    suspend fun toggleAccountActivation(targetUid: String, isActivated: Boolean) {
        try {
            usersRef.document(targetUid).update("isAccountActivated", isActivated).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
        }
    }

    // --- Designer Requests ---
    suspend fun submitDesignerUpgradeRequest(request: DesignerUpgradeRequest): Boolean {
        return try {
            val payload = hashMapOf(
                "userId" to request.userId,
                "userName" to request.userName,
                "userPhone" to request.userPhone,
                "userEmail" to request.userEmail,
                "experienceAreas" to request.experienceAreas,
                "portfolioUrl" to request.portfolioUrl,
                "notes" to request.notes,
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp()
            )
            designerRequestsRef.add(payload).await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "designer_requests")
            false
        }
    }

    fun observeDesignerUpgradeRequests(): Flow<List<DesignerUpgradeRequest>> = callbackFlow {
        val listener = designerRequestsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "designer_requests")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(DesignerUpgradeRequest::class.java)?.copy(id = doc.id)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun approveDesignerUpgrade(requestId: String, targetUserId: String) {
        try {
            usersRef.document(targetUserId).update("role", ROLE_DESIGNER).await()
            designerRequestsRef.document(requestId).update("status", "APPROVED").await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "designer_requests/$requestId")
        }
    }

    // --- App Config & Settings ---
    fun observeAppConfig(): Flow<AppConfig> = callbackFlow {
        val docRef = settingsRef.document("app_config")
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.READ, "settings/app_config")
                return@addSnapshotListener
            }
            val config = if (snapshot != null && snapshot.exists()) {
                snapshot.toObject(AppConfig::class.java) ?: AppConfig()
            } else {
                AppConfig()
            }
            trySend(config)
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateAppConfig(config: AppConfig) {
        try {
            val payload = hashMapOf(
                "whatsappNumber" to config.whatsappNumber,
                "platformCommissionPercentage" to config.platformCommissionPercentage,
                "beneficiaryName" to config.beneficiaryName,
                "beneficiaryAccount" to config.beneficiaryAccount
            )
            settingsRef.document("app_config").set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "settings/app_config")
        }
    }

    // --- App Version & Update Checker ---
    fun observeAppVersion(): Flow<AppVersionConfig> = callbackFlow {
        val docRef = settingsRef.document("app_version")
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.READ, "settings/app_version")
                return@addSnapshotListener
            }
            val ver = if (snapshot != null && snapshot.exists()) {
                snapshot.toObject(AppVersionConfig::class.java) ?: AppVersionConfig()
            } else {
                AppVersionConfig()
            }
            trySend(ver)
        }
        awaitClose { listener.remove() }
    }
}
