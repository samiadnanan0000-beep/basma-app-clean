package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserProfile
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN,
    REGISTER,
    FORGOT_PASSWORD
}

@Composable
fun AuthScreen(
    onAuthSuccess: ((FirebaseUser, String) -> Unit)? = null,
    onContinueAsGuest: (() -> Unit)? = null,
    onContinueAsAdminDemo: (() -> Unit)? = null,
    onProfileAuthenticated: ((UserProfile) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val mainViewModel: MainViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val savedEmail by mainViewModel.savedEmail.collectAsState()
    val hasAccount by mainViewModel.hasAccount.collectAsState()
    val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()

    var currentMode by remember { mutableStateOf(if (hasAccount) AuthMode.SIGN_IN else AuthMode.REGISTER) }

    LaunchedEffect(hasAccount, savedEmail) {
        if (hasAccount && savedEmail.isNotBlank()) {
            currentMode = AuthMode.SIGN_IN
        }
    }

    // Sign In inputs
    var loginEmailInput by remember { mutableStateOf("") }
    var loginPasswordInput by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Registration inputs
    var fullNameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var registerEmailInput by remember { mutableStateOf("") }
    var registerPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var showRegisterPassword by remember { mutableStateOf(false) }
    var enableBiometricsCheck by remember { mutableStateOf(true) }

    // Forgot Password inputs
    var forgotEmailInput by remember { mutableStateOf("") }

    // Status feedback
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Pre-populate saved email if available
    LaunchedEffect(savedEmail) {
        if (savedEmail.isNotBlank() && loginEmailInput.isBlank()) {
            loginEmailInput = savedEmail
            forgotEmailInput = savedEmail
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(paddingValues)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo Emblem
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0381FE), Color(0xFF1E3A8A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "شعار بصمة",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "بَصْمَة",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "استوديو الدعاية والإعلان والطباعة الرقمية واللوحات",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Authentication Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Tabs: Sign In / Register (hidden during Forgot Password)
                        if (currentMode != AuthMode.FORGOT_PASSWORD) {
                            TabRow(
                                selectedTabIndex = if (currentMode == AuthMode.SIGN_IN) 0 else 1,
                                containerColor = Color.Transparent,
                                divider = {},
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Tab(
                                    selected = currentMode == AuthMode.SIGN_IN,
                                    onClick = {
                                        currentMode = AuthMode.SIGN_IN
                                        errorMessage = null
                                        infoMessage = null
                                    },
                                    text = {
                                        Text(
                                            text = "تسجيل الدخول",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                )
                                Tab(
                                    selected = currentMode == AuthMode.REGISTER,
                                    onClick = {
                                        currentMode = AuthMode.REGISTER
                                        errorMessage = null
                                        infoMessage = null
                                    },
                                    text = {
                                        Text(
                                            text = "إنشاء حساب جديد",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // ====================================================
                        // MODE 1: STRICT SIGN IN (تسجيل الدخول الصارم)
                        // ====================================================
                        if (currentMode == AuthMode.SIGN_IN) {
                            Text(
                                text = "تسجيل الدخول إلى حسابك 🔐",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "أدخل بريدك الإلكتروني وكلمة المرور للمتابعة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                            )

                            // Email
                            OutlinedTextField(
                                value = loginEmailInput,
                                onValueChange = {
                                    loginEmailInput = it
                                    errorMessage = null
                                },
                                label = { Text("البريد الإلكتروني") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_email_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Password
                            OutlinedTextField(
                                value = loginPasswordInput,
                                onValueChange = {
                                    loginPasswordInput = it
                                    errorMessage = null
                                },
                                label = { Text("كلمة المرور") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                        Icon(
                                            imageVector = if (showLoginPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input")
                            )

                            // Forgot Password Link
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        currentMode = AuthMode.FORGOT_PASSWORD
                                        forgotEmailInput = loginEmailInput
                                        errorMessage = null
                                        infoMessage = null
                                    }
                                ) {
                                    Text(
                                        text = "نسيت كلمة المرور؟",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Strict Sign In Button
                            Button(
                                onClick = {
                                    val cleanEmail = loginEmailInput.trim()
                                    val cleanPassword = loginPasswordInput

                                    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                        errorMessage = "يرجى كتابة بريد إلكتروني صالح"
                                        coroutineScope.launch { snackbarHostState.showSnackbar("يرجى كتابة بريد إلكتروني صالح") }
                                        return@Button
                                    }
                                    if (cleanPassword.isBlank()) {
                                        errorMessage = "يرجى إدخال كلمة المرور"
                                        coroutineScope.launch { snackbarHostState.showSnackbar("يرجى إدخال كلمة المرور") }
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null

                                    mainViewModel.signInWithEmailAndPassword(
                                        email = cleanEmail,
                                        password = cleanPassword,
                                        onSuccess = { profile ->
                                            isLoading = false
                                            Toast.makeText(context, "مرحباً بك ${profile.name}", Toast.LENGTH_SHORT).show()
                                            onProfileAuthenticated?.invoke(profile)
                                            val firebaseUser = mainViewModel.currentUser.value
                                            if (firebaseUser != null && onAuthSuccess != null) {
                                                onAuthSuccess(firebaseUser, profile.phone)
                                            }
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(err)
                                            }
                                        }
                                    )
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("sign_in_submit_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("تسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            // Biometrics Quick Sign In
                            if (activity != null && isBiometricEnabled && mainViewModel.biometricLockManager.canAuthenticateBiometrics()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        mainViewModel.authenticateWithBiometrics(
                                            activity = activity,
                                            onSuccess = {
                                                val profile = mainViewModel.userProfile.value
                                                if (profile != null) {
                                                    onProfileAuthenticated?.invoke(profile)
                                                }
                                            },
                                            onError = { err ->
                                                errorMessage = err
                                                coroutineScope.launch { snackbarHostState.showSnackbar(err) }
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("biometric_quick_login_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("الدخول السريع ببصمة الإصبع", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        // ====================================================
                        // MODE 2: REGISTRATION (إنشاء حساب مع تفعيل الإيميل الرسمي)
                        // ====================================================
                        else if (currentMode == AuthMode.REGISTER) {
                            Text(
                                text = "إنشاء حساب جديد لأول مرة ✨",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "سيتم إرسال رسالة التفعيل الرسمية إلى بريدك الإلكتروني",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                            )

                            // 1. Full Name
                            OutlinedTextField(
                                value = fullNameInput,
                                onValueChange = { fullNameInput = it; errorMessage = null },
                                label = { Text("الاسم الرباعي") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_name_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. Phone
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it; errorMessage = null },
                                label = { Text("رقم الهاتف (للتواصل وتحديث الطلبات)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_phone_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3. Email
                            OutlinedTextField(
                                value = registerEmailInput,
                                onValueChange = { registerEmailInput = it; errorMessage = null },
                                label = { Text("البريد الإلكتروني") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_email_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4. Password
                            OutlinedTextField(
                                value = registerPasswordInput,
                                onValueChange = { registerPasswordInput = it; errorMessage = null },
                                label = { Text("كلمة المرور (6 خانات على الأقل)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showRegisterPassword = !showRegisterPassword }) {
                                        Icon(
                                            imageVector = if (showRegisterPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (showRegisterPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_password_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 5. Confirm Password
                            OutlinedTextField(
                                value = confirmPasswordInput,
                                onValueChange = { confirmPasswordInput = it; errorMessage = null },
                                label = { Text("تأكيد كلمة المرور") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                visualTransformation = if (showRegisterPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_confirm_password_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Biometrics Toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = enableBiometricsCheck,
                                    onCheckedChange = { enableBiometricsCheck = it },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "تفعيل الدخول ببصمة الإصبع أو الوجه في المرات القادمة",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Register Button (Official Firebase createUserWithEmailAndPassword + sendEmailVerification)
                            Button(
                                onClick = {
                                    val cleanName = fullNameInput.trim()
                                    val cleanPhone = phoneInput.trim()
                                    val cleanEmail = registerEmailInput.trim()
                                    val password = registerPasswordInput
                                    val confirmPassword = confirmPasswordInput

                                    if (cleanName.isBlank()) {
                                        errorMessage = "يرجى كتابة الاسم الرباعي كاملاً"
                                        coroutineScope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
                                        return@Button
                                    }
                                    if (cleanPhone.isBlank()) {
                                        errorMessage = "يرجى إدخال رقم الهاتف"
                                        coroutineScope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
                                        return@Button
                                    }
                                    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                        errorMessage = "يرجى إدخال بريد إلكتروني صحيح"
                                        coroutineScope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
                                        return@Button
                                    }
                                    if (password.length < 6) {
                                        errorMessage = "يجب أن تتكون كلمة المرور من 6 خانات على الأقل"
                                        coroutineScope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        errorMessage = "كلمتا المرور غير متطابقتين"
                                        coroutineScope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null

                                    mainViewModel.registerWithEmail(
                                        fullName = cleanName,
                                        phone = cleanPhone,
                                        email = cleanEmail,
                                        password = password,
                                        enableBiometrics = enableBiometricsCheck,
                                        onSuccess = { profile ->
                                            isLoading = false
                                            Toast.makeText(
                                                context,
                                                "تم إنشاء الحساب وإرسال رابط التفعيل إلى بريدك الإلكتروني بنجاح!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            onProfileAuthenticated?.invoke(profile)
                                            val firebaseUser = mainViewModel.currentUser.value
                                            if (firebaseUser != null && onAuthSuccess != null) {
                                                onAuthSuccess(firebaseUser, profile.phone)
                                            }
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(err)
                                            }
                                        }
                                    )
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("confirm_register_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("إنشاء الحساب وتفعيل البريد", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                }
                            }
                        }

                        // ====================================================
                        // MODE 3: FORGOT PASSWORD (استعادة كلمة المرور الرسمية)
                        // ====================================================
                        else if (currentMode == AuthMode.FORGOT_PASSWORD) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "استعادة كلمة المرور",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "أدخل بريدك الإلكتروني المسجل لإرسال رابط إعادة التعيين الرسمي مباشرة إلى بريدك",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                            )

                            // Registered Email Input
                            OutlinedTextField(
                                value = forgotEmailInput,
                                onValueChange = { forgotEmailInput = it; errorMessage = null },
                                label = { Text("البريد الإلكتروني المسجل") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_email_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Send Reset Link Button
                            Button(
                                onClick = {
                                    val cleanEmail = forgotEmailInput.trim()
                                    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                        errorMessage = "يرجى كتابة بريد إلكتروني صالح"
                                        coroutineScope.launch { snackbarHostState.showSnackbar("يرجى كتابة بريد إلكتروني صالح") }
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null

                                    mainViewModel.sendPasswordReset(
                                        email = cleanEmail,
                                        onSuccess = {
                                            isLoading = false
                                            infoMessage = "تم إرسال رابط إعادة تعيين كلمة المرور إلى بريدك الإلكتروني بنجاح! يرجى فتح البريد واتباع الرابط لتعيين كلمة مرور جديدة."
                                            Toast.makeText(context, "تم إرسال رابط إعادة التعيين إلى بريدك الإلكتروني", Toast.LENGTH_LONG).show()
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("تم إرسال رابط الاستعادة إلى بريدك بنجاح")
                                            }
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(err)
                                            }
                                        }
                                    )
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("send_reset_code_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("إرسال رابط إعادة تعيين كلمة المرور", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Return to Sign In
                            TextButton(
                                onClick = {
                                    currentMode = AuthMode.SIGN_IN
                                    errorMessage = null
                                    infoMessage = null
                                }
                            ) {
                                Text("العودة إلى تسجيل الدخول", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Info Feedback Banner
                        if (infoMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = infoMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                            }
                        }

                        // Error Banner
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Security Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مصادقة آمنة ومشفرة عبر Firebase و Android Biometrics",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
