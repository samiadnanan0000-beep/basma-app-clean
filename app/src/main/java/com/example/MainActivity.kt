package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BiometricLockOverlay
import com.example.ui.navigation.AppNavigation
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.StartupCrashScreen
import com.example.ui.theme.BasmaTheme
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_CRASH_INFO = "EXTRA_CRASH_INFO"
        private const val PREFS_CRASH = "basma_crash_prefs"
        private const val KEY_LAST_CRASH = "key_last_crash"
        var memoryCrashLog: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Explicitly initialize Firebase first before anything else
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("App", "Firebase init error", e)
        }

        // Install global uncaught exception handler to prevent silent force-close
        setupGlobalExceptionHandler()

        // Check if there is an intercepted crash to display
        val startupCrash: String? = intent.getStringExtra(EXTRA_CRASH_INFO)
            ?: memoryCrashLog
            ?: try {
                getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE).getString(KEY_LAST_CRASH, null)
            } catch (_: Exception) {
                null
            }

        // Fallback initialization if FirebaseApp is still not ready
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:905620934158:android:05826bb346af1a4ede365f")
                    .setApiKey("AIzaSyCjw19CzFmqXszycXKWXBwc39-44bKwil0")
                    .setProjectId("gen-lang-client-0749483247")
                    .setStorageBucket("gen-lang-client-0749483247.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "FirebaseApp fallback init safeguard: ${e.message}")
        }

        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            Log.w("MainActivity", "enableEdgeToEdge safeguard: ${e.message}")
        }

        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }
            var currentCrashLog by remember { mutableStateOf(startupCrash) }

            BasmaTheme(darkTheme = isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    if (currentCrashLog != null) {
                        StartupCrashScreen(
                            errorDetails = currentCrashLog ?: "",
                            onBypassAndOpen = {
                                clearCrashRecord()
                                currentCrashLog = null
                            },
                            onRestartApp = {
                                clearCrashRecord()
                                restartActivity()
                            }
                        )
                    } else {
                        BasmaAppRoot(
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = { isDarkTheme = !isDarkTheme }
                        )
                    }
                }
            }
        }
    }

    private fun setupGlobalExceptionHandler() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GlobalCrashHandler", "Uncaught exception in ${thread.name}: ${throwable.message}", throwable)
            val fullError = buildString {
                append("نوع الاستثناء: ${throwable.javaClass.name}\n")
                append("الرسالة: ${throwable.localizedMessage ?: throwable.message ?: "بدون نص توضيحي"}\n")
                append("اسم المعالج (Thread): ${thread.name}\n\n")
                append("مسار الاستدعاءات (Stack Trace):\n")
                append(throwable.stackTraceToString())
            }

            memoryCrashLog = fullError
            try {
                getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CRASH, fullError)
                    .commit()
            } catch (_: Throwable) {}

            // Prevent background thread errors (network, Firestore gRPC, coroutines) from terminating the app
            if (thread.name != "main") {
                Log.w("GlobalCrashHandler", "Prevented app kill from background thread: ${thread.name}")
                return@setDefaultUncaughtExceptionHandler
            }

            try {
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra(EXTRA_CRASH_INFO, fullError)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                Thread.sleep(300)
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (e: Throwable) {
                originalHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun clearCrashRecord() {
        memoryCrashLog = null
        try {
            getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_LAST_CRASH)
                .apply()
        } catch (_: Exception) {}
    }

    private fun restartActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun BasmaAppRoot(
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    mainViewModel: MainViewModel = viewModel()
) {
    val currentUser by mainViewModel.currentUser.collectAsState()
    val userProfile by mainViewModel.userProfile.collectAsState()
    val isGuestMode by mainViewModel.isGuestMode.collectAsState()
    val isAppLocked by mainViewModel.isAppLocked.collectAsState()

    val appVersion by mainViewModel.appVersion.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (appVersion.latest_version_code > 1 && appVersion.apk_download_url.isNotBlank()) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!appVersion.is_mandatory) { /* can dismiss */ } },
            title = {
                androidx.compose.material3.Text("تحديث جديد متاح 🚀", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            },
            text = {
                androidx.compose.material3.Text(
                    if (appVersion.release_notes.isNotBlank()) appVersion.release_notes
                    else "يتوفر إصدار جديد ومحسن من تطبيق بصمة. يرجى التحديث للمتابعة."
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(appVersion.apk_download_url))
                            context.startActivity(browserIntent)
                        } catch (_: Exception) {}
                    }
                ) {
                    androidx.compose.material3.Text("تحديث التطبيق الآن", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        )
    }

    if (currentUser == null && userProfile == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = { user, phone ->
                mainViewModel.onUserSignedIn(user, phone)
            },
            onContinueAsGuest = { mainViewModel.continueAsGuest() },
            onContinueAsAdminDemo = { mainViewModel.continueAsAdminDemo() }
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
