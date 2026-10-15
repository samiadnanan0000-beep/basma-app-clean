package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.data.model.OrderItem
import com.example.data.model.UserProfile
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.OrderViewModel

@Composable
fun DesignerHubScreen(
    mainViewModel: MainViewModel,
    orderViewModel: OrderViewModel,
    onNavigateToCalculator: () -> Unit,
    onNavigateToMaterials: () -> Unit,
    onNavigateToPartners: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val orders by orderViewModel.orders.collectAsState()
    val userProfile by mainViewModel.userProfile.collectAsState()
    val isSuperAdmin = mainViewModel.isSuperAdmin()

    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }

    var showTeamDialog by remember { mutableStateOf(false) }
    val allUsers by mainViewModel.userRepository.observeAllUsers().collectAsState(initial = emptyList())

    var activeOrderTab by remember { mutableStateOf(0) }
    val tabs = listOf("كل الطلبات (${orders.size})", "بانتظار التسعير (${orders.count { it.status == "pending" }})", "قيد التنفيذ (${orders.count { it.status == "in_production" }})")

    val filteredOrders = when (activeOrderTab) {
        1 -> orders.filter { it.status == "pending" }
        2 -> orders.filter { it.status == "in_production" }
        else -> orders
    }

    // Quoting Dialog
    val quotingOrder by orderViewModel.quotingOrder.collectAsState()
    var quotePriceInput by remember { mutableStateOf("") }
    var quoteCurrency by remember { mutableStateOf("YER") }
    var quoteTurnaround by remember { mutableStateOf("48 ساعة") }
    var quoteProofUrl by remember { mutableStateOf("") }

    if (quotingOrder != null) {
        AlertDialog(
            onDismissRequest = { orderViewModel.closeQuoteDialog() },
            title = {
                Text(
                    text = "تسعير الطلب #${quotingOrder?.id?.take(6)}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "العميل: ${quotingOrder?.clientName} (${quotingOrder?.category})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = quotePriceInput,
                        onValueChange = { quotePriceInput = it },
                        label = { Text("السعر الإجمالي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("YER", "SAR", "USD").forEach { curr ->
                            val selected = quoteCurrency == curr
                            Button(
                                onClick = { quoteCurrency = curr },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(curr, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quoteTurnaround,
                        onValueChange = { quoteTurnaround = it },
                        label = { Text("مدة التسليم والتنفيذ") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quoteProofUrl,
                        onValueChange = { quoteProofUrl = it },
                        label = { Text("رابط صورة بروفة التصميم (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = quotePriceInput.toDoubleOrNull() ?: 0.0
                        if (p > 0) {
                            orderViewModel.submitQuote(p, quoteCurrency, quoteTurnaround, quoteProofUrl)
                            quotePriceInput = ""
                            quoteProofUrl = ""
                        }
                    }
                ) {
                    Text("إرسال التسعير للعميل")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { orderViewModel.closeQuoteDialog() }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Broadcast Announcement Dialog
    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("إذاعة إشعار أو تنبيه عام للعملاء 📢", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("سيتم إرسال هذا التنبيه فوراً إلى شاشات مركز الإشعارات لجميع العملاء.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("عنوان التنبيه") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = broadcastMessage,
                        onValueChange = { broadcastMessage = it },
                        label = { Text("تفاصيل الإشعار") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (broadcastTitle.isNotBlank() && broadcastMessage.isNotBlank()) {
                            coroutineScope.launch {
                                mainViewModel.notificationRepository.sendNotification(
                                    title = broadcastTitle,
                                    message = broadcastMessage,
                                    type = "general",
                                    targetId = "all"
                                )
                                showBroadcastDialog = false
                                broadcastTitle = ""
                                broadcastMessage = ""
                                Toast.makeText(context, "تم إرسال الإشعار لجميع العملاء بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("إرسال التنبيه")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBroadcastDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Team Delegation Dialog (Super Admin only)
    if (showTeamDialog) {
        AlertDialog(
            onDismissRequest = { showTeamDialog = false },
            title = { Text("إدارة فريق المصممين والصلاحيات 👥", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(allUsers) { u ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.name.ifBlank { "مستخدم" }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(u.email.ifBlank { u.phone }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text("الرتبة الحالية: ${u.role}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            if (u.role != "super_admin") {
                                if (u.role == "client") {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                mainViewModel.userRepository.updateUserRole(u.uid, "designer")
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("ترقية لمصمم", fontSize = 11.sp)
                                    }
                                } else if (u.role == "designer") {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                mainViewModel.userRepository.updateUserRole(u.uid, "client")
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("إلغاء الترقية", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTeamDialog = false }) {
                    Text("إغلاق")
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
        // Top Hub Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "لوحة المصمم والإدارة 🎨",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${userProfile?.name} • ${if (isSuperAdmin) "المدير العام والمصمم الأساسي" else "مصمم معتمد"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Switch to Client Mode Button
                Button(
                    onClick = { mainViewModel.toggleClientPreview() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.SwitchAccount, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("وضع العميل", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Quick Hub Tool Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Calculator Tool
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToCalculator() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PriceCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("حاسبة الإنتاج", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Materials Tool
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToMaterials() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("إدارة الخامات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Partners Tool
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToPartners() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF8B5CF6))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("المطابع الشريكة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Broadcast Tool
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showBroadcastDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("إذاعة تنبيه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Super Admin Delegation Banner (if super_admin)
        if (isSuperAdmin) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTeamDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("إدارة وتفويض فريق المصممين", fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                Text("ترقية العملاء المسجلين لمصممين بصلاحيات تسعير", fontSize = 12.sp, color = Color(0xFF3B82F6))
                            }
                        }
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2563EB))
                    }
                }
            }
        }

        // Tab Row for Orders Filter
        item {
            ScrollableTabRow(
                selectedTabIndex = activeOrderTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeOrderTab == index,
                        onClick = { activeOrderTab = index },
                        text = { Text(title, fontWeight = if (activeOrderTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        // Incoming Orders List
        items(filteredOrders, key = { it.id }) { order ->
            DesignerOrderCard(
                order = order,
                onQuote = { orderViewModel.openQuoteDialog(order) },
                onUploadProof = { proofUrl -> orderViewModel.uploadProof(order, proofUrl) },
                onUpdateStatus = { status -> orderViewModel.updateStatus(order, status) }
            )
        }
    }
}

@Composable
fun DesignerOrderCard(
    order: OrderItem,
    onQuote: () -> Unit,
    onUploadProof: (String) -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var showProofUploadDialog by remember { mutableStateOf(false) }
    var proofUrlInput by remember { mutableStateOf("") }

    if (showProofUploadDialog) {
        AlertDialog(
            onDismissRequest = { showProofUploadDialog = false },
            title = { Text("رفع بروفة التصميم للعميل 🎨", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("أدخل رابط صورة البروفة المعدة للطلب:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = proofUrlInput,
                        onValueChange = { proofUrlInput = it },
                        label = { Text("رابط صورة البروفة (URL)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (proofUrlInput.isNotBlank()) {
                            onUploadProof(proofUrlInput)
                            showProofUploadDialog = false
                            proofUrlInput = ""
                        }
                    }
                ) {
                    Text("رفع وإشعار العميل")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showProofUploadDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.brandText.ifBlank { "طلب عمل" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "العميل: ${order.clientName} • ${order.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Copy Row for text and phone
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("النص: ${order.brandText}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (order.clientPhone.isNotBlank()) {
                        Text("الهاتف: ${order.clientPhone}", fontSize = 12.sp)
                    }
                    Text("المقاس: ${order.width}م × ${order.height}م (${"%.2f".format(order.width * order.height)}م²)", fontSize = 12.sp)
                }

                Row {
                    IconButton(
                        onClick = {
                            val copyText = "اسم النشاط: ${order.brandText}\nالمقاسات: ${order.width}x${order.height}m\nالخامة: ${order.materialName}\nبيانات الاتصال: ${order.contactDetails}\nملاحظات: ${order.notes}"
                            clipboard.setPrimaryClip(ClipData.newPlainText("بيانات الطلب", copyText))
                            Toast.makeText(context, "تم نسخ تفاصيل التصميم للحافظة", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ تفاصيل الطلب", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (order.clientPhone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val message = "مرحباً ${order.clientName}، معك مصمم استوديو بصمة بخصوص طلبكم #${order.id.take(6)} (${order.brandText})."
                                val uri = Uri.parse("https://wa.me/967${order.clientPhone}?text=${Uri.encode(message)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "مراسلة واتساب", tint = Color(0xFF25D366))
                        }
                    }
                }
            }

            // Attached Logo & Site Photo preview thumbnails
            if (order.logoUrl.isNotBlank() || order.sitePhotoUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (order.logoUrl.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = order.logoUrl,
                                    contentDescription = "شعار العميل",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(4.dp)
                                ) {
                                    Text("شعار العميل", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    if (order.sitePhotoUrl.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = order.sitePhotoUrl,
                                    contentDescription = "صورة الواجهة",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(4.dp)
                                ) {
                                    Text("صورة الموقع", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Rejection reason note if client requested edits
            if (order.rejectionReason.isNotBlank() && !order.proofApproved) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEE2E2))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "ملاحظات وتعديل العميل: ${order.rejectionReason}",
                        color = Color(0xFFB91C1C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons for Designer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quoting Button
                Button(
                    onClick = onQuote,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (order.price > 0) "تعديل التسعير" else "تسعير الطلب", fontSize = 12.sp)
                }

                // Proof Upload Button
                OutlinedButton(
                    onClick = { showProofUploadDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("رفع بروفة", fontSize = 12.sp)
                }

                // Quick Move status to production / completed
                if (order.status == "in_production") {
                    Button(
                        onClick = { onUpdateStatus("completed") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("اكتمل 🏁", fontSize = 12.sp)
                    }
                } else if (order.status == "quoted" && order.proofApproved) {
                    Button(
                        onClick = { onUpdateStatus("in_production") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("للمطبعة 🟢", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
