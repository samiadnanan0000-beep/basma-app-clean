package com.example.ui.viewmodel

import android.app.Application
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
        null
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

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
            biometricLockManager.isBiometricEnabledFlow.collect { enabled ->
                if (enabled) {
                    _isAppLocked.value = true
                }
            }
        }

        auth?.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                onUserSignedIn(user)
            } else {
                _userProfile.value = null
            }
        }
    }

    fun onUserSignedIn(user: FirebaseUser, phoneInput: String? = null) {
        viewModelScope.launch {
            val profile = userRepository.syncUserProfile(user, phoneInput)
            _userProfile.value = profile

            val isStaff = profile.role in listOf("super_admin", "designer")
            notificationRepository.observeNotificationsForUser(user.uid, isStaff)
                .collect { list ->
                    _notifications.value = list
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
        val role = _userProfile.value?.role ?: "client"
        return role == "super_admin" || role == "designer"
    }

    fun isSuperAdmin(): Boolean {
        return _userProfile.value?.role == "super_admin"
    }

    fun signOut(onComplete: () -> Unit) {
        auth?.signOut()
        _currentUser.value = null
        _userProfile.value = null
        _previewAsClient.value = false
        onComplete()
    }
}
