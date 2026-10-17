package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.OrderViewModel
import com.example.util.WhatsAppHelper

@Composable
fun DesignerHubScreen(
    mainViewModel: MainViewModel,
    orderViewModel: OrderViewModel,
    onNavigateToPortfolio: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val userProfile by mainViewModel.userProfile.collectAsState()
    val designerOrders by orderViewModel.designerOrders.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var chatOrder by remember { mutableStateOf<OrderItem?>(null) }
    var rejectDialogOrder by remember { mutableStateOf<OrderItem?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewImageTitle by remember { mutableStateOf("معاينة المرفق") }

    val designerId = userProfile?.uid.orEmpty()

    if (previewImageUrl != null) {
        com.example.ui.components.FullScreenImagePreviewDialog(
            imageUrl = previewImageUrl!!,
            title = previewImageTitle,
            onDismiss = { previewImageUrl = null }
        )
    }

    LaunchedEffect(designerId) {
        if (designerId.isNotBlank()) {
            orderViewModel.startObservingDesignerOrders(designerId)
        }
    }

    if (chatOrder != null) {
        OrderChatDialog(
            order = chatOrder!!,
            orderViewModel = orderViewModel,
            onDismiss = { chatOrder = null }
        )
    }

    if (rejectDialogOrder != null) {
        DesignerRejectDialog(
            order = rejectDialogOrder!!,
            onConfirmReject = { reason ->
                orderViewModel.designerRejectOrder(
                    orderId = rejectDialogOrder!!.id,
                    designerId = designerId,
                    designerName = userProfile?.name ?: "المصمم",
                    reason = reason,
                    onSuccess = {
                        rejectDialogOrder = null
                        Toast.makeText(context, "تم تسجيل اعتذارك وإعادة إسناد الطلب للإدارة", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onDismiss = { rejectDialogOrder = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Designer Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مساحة المصمم المعتمد 🎨",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = userProfile?.name.orEmpty().ifBlank { "مصمم بصمة" },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Rating & Task Count Badges
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format("%.1f", userProfile?.ratingAverage ?: 5.0f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 3 Safe Tabs: Only designer operational views!
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("مهام التصميم (${designerOrders.filter { it.status != OrderStatus.COMPLETED }.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("محفظتي والأرباح", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("معرض أعمالي", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedTab) {
            0 -> DesignerTasksTab(
                orders = designerOrders.filter { it.status != OrderStatus.COMPLETED },
                designerId = designerId,
                onAcceptOrder = { order ->
                    orderViewModel.designerAcceptOrder(order.id, designerId) {
                        Toast.makeText(context, "تم قبول الطلب وبدء العمل بنجاح 🟢", Toast.LENGTH_SHORT).show()
                    }
                },
                onRequestReject = { order -> rejectDialogOrder = order },
                onOpenChat = { order -> chatOrder = order },
                onSubmitDraftToAdmin = { order ->
                    WhatsAppHelper.openChat(
                        context = context,
                        message = "السلام عليكم إدارة بصمة، تم إنجاز مسودة التصميم للطلب رقم ${order.displayOrderNumber} (${order.designType})، يرجى التكرم بالمراجعة وإرسالها للعميل."
                    )
                },
                onPreviewImage = { url, title ->
                    previewImageUrl = url
                    previewImageTitle = title
                }
            )
            1 -> DesignerEarningsTab(
                orders = designerOrders
            )
            2 -> DesignerPortfolioShortcutTab(
                onNavigateToPortfolio = onNavigateToPortfolio
            )
        }
    }
}

@Composable
fun DesignerTasksTab(
    orders: List<OrderItem>,
    designerId: String,
    onAcceptOrder: (OrderItem) -> Unit,
    onRequestReject: (OrderItem) -> Unit,
    onOpenChat: (OrderItem) -> Unit,
    onSubmitDraftToAdmin: (OrderItem) -> Unit,
    onPreviewImage: (String, String) -> Unit = { _, _ -> }
) {
    if (orders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(54.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "لا توجد مهام نشطة حالياً",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "أنت الآن في وضع (متفرغ / متاح للعمل)، ستظهر لك الطلبات فور إسنادها من الإدارة.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(orders, key = { it.id }) { order ->
                DesignerTaskCard(
                    order = order,
                    onAccept = { onAcceptOrder(order) },
                    onReject = { onRequestReject(order) },
                    onOpenChat = { onOpenChat(order) },
                    onSubmitDraft = { onSubmitDraftToAdmin(order) },
                    onPreviewImage = onPreviewImage
                )
            }
        }
    }
}

@Composable
fun DesignerTaskCard(
    order: OrderItem,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onOpenChat: () -> Unit,
    onSubmitDraft: () -> Unit,
    onPreviewImage: (String, String) -> Unit = { _, _ -> }
) {
    val isPendingAcceptance = order.status == OrderStatus.PENDING_DESIGNER_ACCEPTANCE

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Order Number & Designer Fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "طلب ${order.displayOrderNumber}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = order.designType.ifBlank { "تصميم فني" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // Net designer fee badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "مستحقاتك: ${order.designerFee.toInt()} ر.ي",
                        color = Color(0xFF166534),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Technical Specifications (CLIENT PII IS COMPLETELY REMOVED!)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("المواصفات الفنية والشروط:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = order.detailedSpecs.ifBlank { "لم تُحدد مواصفات إضافية" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    if (order.width > 0 && order.height > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "الأبعاد المطلوبة: ${order.width}م (عرض) × ${order.height}م (ارتفاع)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!order.estimatedDeliveryTime.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "موعد التسليم المتوقع: ${order.estimatedDeliveryTime}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }

            // Reference Attachment preview if available
            if (order.referenceAttachmentUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onPreviewImage(order.referenceAttachmentUrl, "المرفق المرجعي للطلب") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("معاينة المرفق المرجعي للطلب 🖼️", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Accept / Reject Actions (when PENDING_DESIGNER_ACCEPTANCE)
            if (isPendingAcceptance) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تم إسناد هذا الطلب إليك، يرجى القبول أو الاعتذار:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAccept,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("قبول وبدء العمل", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onReject,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("اعتذار / رفض", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                // In Progress Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSubmitDraft,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إرسال المسودة للإدارة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenChat,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("المحادثة", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DesignerEarningsTab(orders: List<OrderItem>) {
    val completedOrders = orders.filter { it.status == OrderStatus.COMPLETED }
    val totalEarnings = completedOrders.sumOf { it.designerFee }
    val pendingEarnings = orders.filter { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }.sumOf { it.designerFee }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("الأرباح المعتمدة والمستلمة", fontSize = 11.sp, color = Color(0xFF166534))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${totalEarnings.toInt()} ر.ي", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF14532D))
                        Text("${completedOrders.size} طلبات منجزة", fontSize = 10.sp, color = Color(0xFF166534))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("أرباح قيد التنفيذ", fontSize = 11.sp, color = Color(0xFF92400E))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${pendingEarnings.toInt()} ر.ي", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF78350F))
                        Text("تُقيد بعد الاستلام النهائي", fontSize = 10.sp, color = Color(0xFF92400E))
                    }
                }
            }
        }

        item {
            Text(
                text = "سجل العمليات والمستحقات الفردية:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "لا توجد عمليات مسجلة حتى الآن",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items(orders, key = { it.id }) { order ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "طلب ${order.displayOrderNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = order.designType.ifBlank { "تصميم" },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (order.status == OrderStatus.COMPLETED) "مكتمل ومعتمد ✓" else "قيد العمل والتنفيذ",
                            fontSize = 11.sp,
                            color = if (order.status == OrderStatus.COMPLETED) Color(0xFF16A34A) else Color(0xFFD97706)
                        )
                    }

                    Text(
                        text = "${order.designerFee.toInt()} ر.ي",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color(0xFF16A34A)
                    )
                }
            }
        }
    }
}

@Composable
fun DesignerPortfolioShortcutTab(onNavigateToPortfolio: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Collections, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "معرض نماذج الأعمال",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "يمكنك إضافة تصاميمك المميزة وإدارتها في المعرض العام لعرضها على العملاء وجذب طلبات جديدة.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = { onNavigateToPortfolio?.invoke() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("الانتقال إلى معرض الأعمال")
            }
        }
    }
}

@Composable
fun DesignerRejectDialog(
    order: OrderItem,
    onConfirmReject: (reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    val reasons = listOf(
        "ضغط عمل حالي وعدم التفرغ",
        "الموعد الزمني للتسليم غير كافٍ",
        "الطلب خارج نطاق التخصص الفني",
        "ظرف شخصي طارئ"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "الاعتذار عن الطلب ${order.displayOrderNumber}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "يرجى تحديد سبب الاعتذار لتوثيقه وإعادة إسناد الطلب لمصمم بديل:",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                reasons.forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = r }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selectedReason == r) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (selectedReason == r) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = r, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReject(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("تأكيد الاعتذار", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("تراجع")
            }
        }
    )
}
