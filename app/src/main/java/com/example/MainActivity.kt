package com.example

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BiometricLockOverlay
import com.example.ui.navigation.AppNavigation
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.BasmaTheme
import com.example.ui.viewmodel.MainViewModel
import androidx.activity.compose.setContent

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "FirebaseApp init safeguard: ${e.message}")
        }
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            BasmaTheme(darkTheme = isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    BasmaAppRoot(
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }
}

@Composable
fun BasmaAppRoot(
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    mainViewModel: MainViewModel = viewModel()
) {
    val currentUser by mainViewModel.currentUser.collectAsState()
    val isAppLocked by mainViewModel.isAppLocked.collectAsState()

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { user, phone ->
                mainViewModel.onUserSignedIn(user, phone)
            }
        )
    } else if (isAppLocked) {
        BiometricLockOverlay(
            biometricLockManager = mainViewModel.biometricLockManager,
            onUnlocked = { mainViewModel.unlockApp() }
        )
    } else {
        AppNavigation(
            mainViewModel = mainViewModel,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme
        )
    }
}
