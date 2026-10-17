package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by mainViewModel.userProfile.collectAsState()
    val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
    val designerRequests by mainViewModel.designerRequests.collectAsState()

    val isClientRole = userProfile?.role?.uppercase() !in listOf("ADMIN", "SUPER_ADMIN", "DESIGNER")
    val userPendingRequest = designerRequests.firstOrNull { it.userId == userProfile?.uid && it.status == "PENDING" }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }

    var showUpgradeDialog by remember { mutableStateOf(false) }
    var experienceInput by remember { mutableStateOf("") }
    var portfolioUrlInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var isSubmittingUpgrade by remember { mutableStateOf(false) }

    // Designer Upgrade Request Dialog
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmittingUpgrade) showUpgradeDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "طلب الانضمام كشريك تصميم 🎨",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "انضم إلى شبكة مصممي بصمة المعتمدين لتنفيذ الطلبات وحساب مستحقاتك لكل تصميم بدقة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = experienceInput,
                        onValueChange = { experienceInput = it },
                        label = { Text("مجالات الخبرة والتخصص *") },
                        placeholder = { Text("مثال: هويات بصرية، بنرات، سوشيال ميديا، لوحات...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("upgrade_experience_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = portfolioUrlInput,
                        onValueChange = { portfolioUrlInput = it },
                        label = { Text("رابط معرض الأعمال (Portfolio URL) *") },
                        placeholder = { Text("Behance / Google Drive / موقعك الشخصي") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("upgrade_portfolio_url_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("ملاحظات إضافية أو برامج التصميم المتقنة") },
                        placeholder = { Text("Photoshop, Illustrator, 3Ds Max...") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("upgrade_notes_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val exp = experienceInput.trim()
                        val port = portfolioUrlInput.trim()
                        if (exp.isBlank()) {
                            Toast.makeText(context, "يرجى توضيح مجالات التخصص والخبرة", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (port.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال رابط معرض أعمالك", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSubmittingUpgrade = true
                        mainViewModel.submitDesignerUpgrade(
                            experience = exp,
                            portfolio = port,
                            notes = notesInput.trim(),
                            onSuccess = {
                                isSubmittingUpgrade = false
                                showUpgradeDialog = false
                                experienceInput = ""
                                portfolioUrlInput = ""
                                notesInput = ""
                                Toast.makeText(context, "تم إرسال طلب الترقية، ستتم مراجعته من الإدارة قريباً", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                isSubmittingUpgrade = false
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isSubmittingUpgrade,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("submit_upgrade_request_button")
                ) {
                    Text("إرسال الطلب", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showUpgradeDialog = false },
                    enabled = !isSubmittingUpgrade,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("تغيير رمز المرور PIN 🔒", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("أدخل رمز PIN جديد مكون من 4 أرقام:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4) newPinInput = it },
                        label = { Text("رمز PIN الجديد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length == 4) {
                            coroutineScope.launch {
                                mainViewModel.biometricLockManager.setUserPin(newPinInput)
                                showChangePinDialog = false
                                newPinInput = ""
                                Toast.makeText(context, "تم تحديث رمز المرور بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showChangePinDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        item {
            Text(
                text = "الإعدادات والحساب ⚙️",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Profile Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (userProfile?.name?.take(1) ?: "ب"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.name ?: "مستخدم بصمة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userProfile?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = when (userProfile?.role) {
                                "super_admin" -> "👑 المدير العام والمصمم الأساسي"
                                "designer" -> "🎨 مصمم معتمد بالاستوديو"
                                else -> "👤 حساب عميل"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Designer Role Upgrade Card for Clients
        if (isClientRole) {
            item {
                if (userPendingRequest != null) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        modifier = Modifier.fillMaxWidth().testTag("designer_pending_request_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD97706).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color(0xFFD97706))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "طلبك للانضمام كمصمم قيد المراجعة ⏳",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "تم استلام بياناتك ومعرض أعمالك بنجاح، وطلبك حالياً قيد التدقيق والاعتماد من قِبل الإدارة.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("designer_upgrade_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "الانضمام كشريك تصميم / طلب ترقية لمصمم 🎨",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "هل تمتلك مهارات التصميم الإعلاني؟ انضم لفريق بصمة واستلم طلبات التصميم.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { showUpgradeDialog = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("open_designer_upgrade_dialog_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("تقديم طلب ترقية لمصمم معتمد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Security & Biometric Lock Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "الأمان والقفل بالبصمة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Biometrics Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("قفل التطبيق بالبصمة (Biometrics)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("تأكيد الهوية ببصمة الإصبع أو الوجه عند الفتح", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }

                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    mainViewModel.biometricLockManager.setBiometricEnabled(checked)
                                }
                            },
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Change PIN Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("رمز المرور الاحتياطي (PIN)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("يستخدم عند تعذر قراءة البصمة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }

                        Button(
                            onClick = { showChangePinDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("تغيير", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Appearance & Theme Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "المظهر ونظام العرض",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("الوضع الداكن (Dark Mode)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("مظهر فخم عالي التباين One UI Luxury", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }

                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleDarkTheme() }
                        )
                    }
                }
            }
        }

        // About Basma Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("حول تطبيق بَصْمَة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "الإصدار 1.0 (Samsung One UI Standard)\nمنظومة متكاملة لربط العملاء باستوديو وورش تصنيع الدعاية والإعلان والطباعة الرقمية واللوحات.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Sign Out Button
        item {
            Button(
                onClick = { mainViewModel.signOut {} },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
            }
        }

        // Developer documentation & copyright footer
        item {
            com.example.ui.components.DeveloperFooter(
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
