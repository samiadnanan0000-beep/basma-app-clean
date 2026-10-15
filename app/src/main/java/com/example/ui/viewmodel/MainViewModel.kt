package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
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

    private val auth: FirebaseAuth? = try {
        if (com.google.firebase.FirebaseApp.getApps(application).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(application)
        }
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.e("MainViewModel", "FirebaseAuth init safeguard: ${e.message}")
        null
    }

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

    init {
        // Seed initial data if needed
        viewModelScope.launch {
            try {
                materialRepository.seedInitialMaterialsIfEmpty()
                offerRepository.seedInitialOffersIfEmpty()
                partnerRepository.seedInitialPartnersIfEmpty()
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
     * Authenticates user via Name, Phone, and Email after OTP verification.
     * Evaluates role automatically in background (ADMIN if matches super admin, otherwise CLIENT).
     */
    fun registerAndSignIn(
        fullName: String,
        phone: String,
        email: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val profile = userRepository.registerOrAuthenticate(fullName, phone, email)
                _userProfile.value = profile
                _isGuestMode.value = true // Flags user as authenticated in session

                // Start observing real-time profile updates (e.g. if admin changes role in background)
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
                Log.e("MainViewModel", "registerAndSignIn error: ${e.message}", e)
                onError(e.localizedMessage ?: "حدث خطأ أثناء حفظ وتفعيل الحساب")
            }
        }
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

                // Listen to live changes to user's role and profile
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
