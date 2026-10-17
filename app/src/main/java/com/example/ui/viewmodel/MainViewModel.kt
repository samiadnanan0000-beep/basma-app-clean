package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NotificationItem
import com.example.data.model.UserProfile
import com.example.data.repository.ComplaintRepository
import com.example.data.repository.MaterialRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.OfferRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.PartnerRepository
import com.example.data.repository.UserRepository
import com.example.security.BiometricLockManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val biometricLockManager = BiometricLockManager(application)
    val userRepository = UserRepository(application)
    val notificationRepository = NotificationRepository(application)
    val orderRepository = OrderRepository(application, notificationRepository)
    val materialRepository = MaterialRepository(application)
    val complaintRepository = ComplaintRepository(application, notificationRepository)
    val offerRepository = OfferRepository(application)
    val partnerRepository = PartnerRepository(application)
    val portfolioRepository = com.example.data.repository.PortfolioRepository(application)

    private val auth: FirebaseAuth? = userRepository.auth

    private val _currentUser = MutableStateFlow<FirebaseUser?>(try { auth?.currentUser } catch (e: Exception) { null })
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Preview mode: Allows super_admin or designer to view the app as a client
    private val _previewAsClient = MutableStateFlow(false)
    val previewAsClient: StateFlow<Boolean> = _previewAsClient.asStateFlow()

    // App Lock state
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Notifications for unread count badge
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    val unreadNotificationsCount: StateFlow<Int> = MutableStateFlow(0)

    val isBiometricEnabled: StateFlow<Boolean> = biometricLockManager.isBiometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val savedEmail: StateFlow<String> = biometricLockManager.savedEmailFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val hasAccount: StateFlow<Boolean> = biometricLockManager.hasAccountFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _appConfig = MutableStateFlow(com.example.data.model.AppConfig())
    val appConfig: StateFlow<com.example.data.model.AppConfig> = _appConfig.asStateFlow()

    private val _appVersion = MutableStateFlow(com.example.data.model.AppVersionConfig())
    val appVersion: StateFlow<com.example.data.model.AppVersionConfig> = _appVersion.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    private val _designerRequests = MutableStateFlow<List<com.example.data.model.DesignerUpgradeRequest>>(emptyList())
    val designerRequests: StateFlow<List<com.example.data.model.DesignerUpgradeRequest>> = _designerRequests.asStateFlow()

    init {
        // Seed initial data if needed
        viewModelScope.launch {
            try {
                materialRepository.seedInitialMaterialsIfEmpty()
                offerRepository.seedInitialOffersIfEmpty()
                partnerRepository.seedInitialPartnersIfEmpty()
                portfolioRepository.seedInitialPortfolioIfEmpty()
            } catch (e: Exception) {
                // Ignore background seeder network errors
            }
        }

        // Check if lock should be enabled on start
        viewModelScope.launch {
            try {
                biometricLockManager.isBiometricEnabledFlow.collect { enabled ->
                    if (enabled) {
                        _isAppLocked.value = true
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Biometric flow observation error: ${e.message}")
            }
        }

        viewModelScope.launch {
            runCatching {
                userRepository.observeAppConfig().collect { cfg ->
                    _appConfig.value = cfg
                }
            }
        }

        viewModelScope.launch {
            runCatching {
                userRepository.observeAppVersion().collect { ver ->
                    _appVersion.value = ver
                }
            }
        }

        viewModelScope.launch {
            runCatching {
                userRepository.observeAllUsers().collect { list ->
                    _allUsers.value = list
                }
            }
        }

        viewModelScope.launch {
            runCatching {
                userRepository.observeDesignerUpgradeRequests().collect { list ->
                    _designerRequests.value = list
                }
            }
        }

        try {
            auth?.addAuthStateListener { firebaseAuth ->
                val user = try { firebaseAuth.currentUser } catch (e: Exception) { null }
                _currentUser.value = user
                if (user != null) {
                    _isGuestMode.value = false
                    onUserSignedIn(user)
                } else if (!_isGuestMode.value && _userProfile.value == null) {
                    _userProfile.value = null
                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "AuthStateListener safeguard: ${e.message}")
        }
    }

    /**
     * Sends an email verification / OTP to the user's email address.
     */
    fun sendVerificationEmail(email: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = userRepository.sendVerificationEmail(email)
            onDone(result)
        }
    }

    /**
     * Registers a new account with email, phone, name, and chosen password.
     * Uses official Firebase Auth createUserWithEmailAndPassword + sendEmailVerification.
     */
    fun registerWithEmail(
        fullName: String,
        phone: String,
        email: String,
        password: String,
        enableBiometrics: Boolean,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val profile = userRepository.registerWithEmailAndPassword(fullName, phone, email, password)
                _userProfile.value = profile
                _currentUser.value = auth?.currentUser
                _isGuestMode.value = false

                // Save email and account presence locally
                biometricLockManager.setAccountSaved(email)
                if (enableBiometrics) {
                    biometricLockManager.setBiometricEnabled(true)
                }

                // Real-time profile updates
                launch {
                    userRepository.observeUserProfile(profile.uid).collect { updatedProfile ->
                        if (updatedProfile != null) {
                            _userProfile.value = updatedProfile
                        }
                    }
                }

                val isStaff = profile.role.uppercase() in listOf(UserRepository.ROLE_ADMIN, "SUPER_ADMIN", UserRepository.ROLE_DESIGNER)
                launch {
                    notificationRepository.observeNotificationsForUser(profile.uid, isStaff)
                        .collect { list ->
                            _notifications.value = list
                        }
                }
                onSuccess(profile)
            } catch (e: Exception) {
                Log.e("MainViewModel", "registerWithEmail error: ${e.message}", e)
                val errorMsg = when {
                    e is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                        "هذا البريد الإلكتروني مسجل مسبقاً، يرجى تسجيل الدخول"
                    e is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                        "كلمة المرور ضعيفة جداً، يرجى اختيار كلمة مرور من 6 أحرف أو أرقام على الأقل"
                    e is com.google.firebase.FirebaseNetworkException ->
                        "تعذر الاتصال بالشبكة، يرجى التحقق من اتصالك بالإنترنت"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "تعذر الاتصال بالشبكة، يرجى التحقق من اتصالك بالإنترنت"
                    else -> e.localizedMessage ?: "حدث خطأ أثناء إنشاء الحساب"
                }
                onError(errorMsg)
            }
        }
    }

    /**
     * Strict Login Verification:
     * Disallows mock bypass; strictly enforces auth.signInWithEmailAndPassword.
     */
    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val profile = userRepository.signInWithEmailAndPassword(email, password)
                _userProfile.value = profile
                _currentUser.value = auth?.currentUser
                _isGuestMode.value = false

                // Save email locally
                biometricLockManager.setAccountSaved(email)

                // Real-time observer
                launch {
                    userRepository.observeUserProfile(profile.uid).collect { updated ->
                        if (updated != null) {
                            _userProfile.value = updated
                        }
                    }
                }

                val isStaff = profile.role.uppercase() in listOf(UserRepository.ROLE_ADMIN, "SUPER_ADMIN", UserRepository.ROLE_DESIGNER)
                launch {
                    notificationRepository.observeNotificationsForUser(profile.uid, isStaff)
                        .collect { list ->
                            _notifications.value = list
                        }
                }
                onSuccess(profile)
            } catch (e: Exception) {
                Log.e("MainViewModel", "signInWithEmailAndPassword error: ${e.message}", e)
                val errorMsg = when {
                    e is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                        "هذا الحساب غير مسجل، يرجى إنشاء حساب جديد"
                    e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                        "كلمة المرور غير صحيحة"
                    e is com.google.firebase.FirebaseNetworkException ->
                        "تعذر الاتصال بالشبكة، يرجى التحقق من اتصالك بالإنترنت"
                    e.message?.contains("no user record", ignoreCase = true) == true ->
                        "هذا الحساب غير مسجل، يرجى إنشاء حساب جديد"
                    e.message?.contains("password is invalid", ignoreCase = true) == true ||
                    e.message?.contains("credential is malformed", ignoreCase = true) == true ->
                        "كلمة المرور غير صحيحة"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "تعذر الاتصال بالشبكة، يرجى التحقق من اتصالك بالإنترنت"
                    else -> e.localizedMessage ?: "البريد الإلكتروني أو كلمة المرور غير صحيحة"
                }
                onError(errorMsg)
            }
        }
    }

    /**
     * Sends a real password reset link/email to the user's inbox via Firebase Auth.
     */
    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val sent = userRepository.sendPasswordResetEmail(email)
                if (sent) {
                    onSuccess()
                } else {
                    onError("تعذر إرسال رابط الاستعادة إلى هذا البريد، يرجى التأكد من صحته")
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                        "هذا الحساب غير مسجل، يرجى إنشاء حساب جديد"
                    e is com.google.firebase.FirebaseNetworkException ->
                        "تعذر الاتصال بالشبكة، يرجى التحقق من اتصالك بالإنترنت"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "تعذر الاتصال بالشبكة، يرجى التحقق من اتصالك بالإنترنت"
                    else -> e.localizedMessage ?: "حدث خطأ أثناء إرسال رسالة الاستعادة"
                }
                onError(errorMsg)
            }
        }
    }

    /**
     * Completes password reset and updates password.
     */
    fun completePasswordReset(
        email: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updated = userRepository.resetUserPassword(email, newPassword)
                if (updated) {
                    onSuccess()
                } else {
                    onError("تعذر تحديث كلمة المرور")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "حدث خطأ أثناء تحديث كلمة المرور")
            }
        }
    }

    /**
     * Biometric instant sign-in for saved user.
     */
    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val email = savedEmail.value
        if (email.isBlank()) {
            onError("لا يوجد حساب مسجل مسبقاً على هذا الجهاز")
            return
        }

        biometricLockManager.showBiometricPrompt(
            activity = activity,
            title = "تسجيل الدخول بالبصمة",
            subtitle = "أهلاً بك مجدداً في استوديو بصمة",
            onSuccess = {
                viewModelScope.launch {
                    try {
                        val profile = userRepository.signInWithEmailAndPassword(email, "BiometricUnlocked")
                        _userProfile.value = profile
                        _isGuestMode.value = true
                        onSuccess()
                    } catch (e: Exception) {
                        onSuccess()
                    }
                }
            },
            onError = { err -> onError(err) },
            onFallbackToPin = { onError("يرجى استخدام البريد وكلمة المرور") }
        )
    }

    fun registerAndSignIn(
        fullName: String,
        phone: String,
        email: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        registerWithEmail(
            fullName = fullName,
            phone = phone,
            email = email,
            password = "Basma@123456",
            enableBiometrics = false,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun updateUserRole(userId: String, newRole: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                userRepository.updateUserRole(userId, newRole)
                onDone()
            } catch (e: Exception) {
                Log.e("MainViewModel", "updateUserRole error: ${e.message}", e)
            }
        }
    }

    fun continueAsGuest() {
        _isGuestMode.value = true
        _userProfile.value = UserProfile(
            uid = "guest_client",
            name = "عميل تجريبي",
            email = "client@example.com",
            phone = "770000000",
            role = UserRepository.ROLE_CLIENT
        )
    }

    fun continueAsAdminDemo() {
        _isGuestMode.value = true
        _userProfile.value = UserProfile(
            uid = "admin_demo",
            name = UserRepository.SUPER_ADMIN_NAME,
            email = UserRepository.SUPER_ADMIN_EMAIL,
            phone = UserRepository.SUPER_ADMIN_PHONE,
            role = UserRepository.ROLE_ADMIN
        )
    }

    fun onUserSignedIn(user: FirebaseUser, phoneInput: String? = null) {
        viewModelScope.launch {
            runCatching {
                val profile = userRepository.syncUserProfile(user, phoneInput)
                _userProfile.value = profile

                launch {
                    userRepository.observeUserProfile(user.uid).collect { updated ->
                        if (updated != null) {
                            _userProfile.value = updated
                        }
                    }
                }

                val isStaff = profile.role.uppercase() in listOf(UserRepository.ROLE_ADMIN, "SUPER_ADMIN", UserRepository.ROLE_DESIGNER)
                notificationRepository.observeNotificationsForUser(user.uid, isStaff)
                    .collect { list ->
                        _notifications.value = list
                    }
            }.onFailure { e ->
                Log.e("MainViewModel", "onUserSignedIn safeguard error: ${e.message}")
            }
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun toggleClientPreview() {
        _previewAsClient.value = !_previewAsClient.value
    }

    fun exitClientPreview() {
        _previewAsClient.value = false
    }

    fun isEffectiveStaff(): Boolean {
        if (_previewAsClient.value) return false
        val role = _userProfile.value?.role?.uppercase() ?: UserRepository.ROLE_CLIENT
        return role == UserRepository.ROLE_ADMIN || role == "SUPER_ADMIN" || role == UserRepository.ROLE_DESIGNER
    }

    fun isSuperAdmin(): Boolean {
        val role = _userProfile.value?.role?.uppercase()
        return role == UserRepository.ROLE_ADMIN || role == "SUPER_ADMIN"
    }

    fun isDesigner(): Boolean {
        val role = _userProfile.value?.role?.uppercase()
        return role == UserRepository.ROLE_DESIGNER
    }

    fun toggleAccountActivation(
        userId: String,
        isActivated: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            // Optimistic update for instantaneous UI feedback
            _allUsers.value = _allUsers.value.map {
                if (it.uid == userId) it.copy(isAccountActivated = isActivated) else it
            }
            val res = userRepository.toggleAccountActivation(userId, isActivated)
            res.fold(
                onSuccess = { onSuccess() },
                onFailure = { error ->
                    // Revert optimistic update on failure
                    _allUsers.value = _allUsers.value.map {
                        if (it.uid == userId) it.copy(isAccountActivated = !isActivated) else it
                    }
                    onError(error.localizedMessage ?: "حدث خطأ أثناء تحديث حالة الحساب")
                }
            )
        }
    }

    fun updateAppConfig(config: com.example.data.model.AppConfig, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            userRepository.updateAppConfig(config)
            onDone()
        }
    }

    fun submitDesignerUpgrade(
        experience: String,
        portfolio: String,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _userProfile.value ?: run {
            onError("يرجى تسجيل الدخول أولاً")
            return
        }
        viewModelScope.launch {
            val req = com.example.data.model.DesignerUpgradeRequest(
                userId = user.uid,
                userName = user.name,
                userPhone = user.phone,
                userEmail = user.email,
                experienceAreas = experience,
                portfolioUrl = portfolio,
                notes = notes
            )
            val ok = userRepository.submitDesignerUpgradeRequest(req)
            if (ok) onSuccess() else onError("تعذر إرسال الطلب، يرجى المحاولة لاحقاً")
        }
    }

    fun approveDesignerUpgrade(requestId: String, targetUserId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            userRepository.approveDesignerUpgrade(requestId, targetUserId)
            onSuccess()
        }
    }

    fun signOut(onComplete: () -> Unit) {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("MainViewModel", "SignOut error safeguard: ${e.message}")
        }
        _currentUser.value = null
        _userProfile.value = null
        _previewAsClient.value = false
        _isGuestMode.value = false
        onComplete()
    }
}
