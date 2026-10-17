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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppConfig
import com.example.data.model.DesignerUpgradeRequest
import com.example.data.model.NotificationItem
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.OrderViewModel

@Composable
fun AdminDashboardScreen(
    mainViewModel: MainViewModel,
    orderViewModel: OrderViewModel,
    onNavigateToClientView: () -> Unit = {}
) {
    val context = LocalContext.current
    val allUsers by mainViewModel.allUsers.collectAsState()
    val orders by orderViewModel.orders.collectAsState()
    val appConfig by mainViewModel.appConfig.collectAsState()
    val designerRequests by mainViewModel.designerRequests.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog states
    var dualPricingOrder by remember { mutableStateOf<OrderItem?>(null) }
    var assignDesignerOrder by remember { mutableStateOf<OrderItem?>(null) }
    var previewReceiptUrl by remember { mutableStateOf<String?>(null) }

    val tabs = listOf(
        "طلبات التسعير والسداد",
        "الإحصائيات والأرباح",
        "المستخدمين والتفعيل",
        "الحاسبة القياسية",
        "إعدادات المنصة",
        "الإشعارات العامة"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Admin Top Banner with Preview as Client button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("لوحة إدارة المنصة • المدير سامي البكري", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("إدارة الأسعار المعتمدة، الحوالات، وتوزيع المهام", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Switch to Client View Mode Button
                Button(
                    onClick = {
                        mainViewModel.toggleClientPreview()
                        onNavigateToClientView()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("admin_preview_as_client_button")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("معاينة كعميل", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Horizontal scrollable tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AdminOrdersManagementTab(
                    orders = orders,
                    allUsers = allUsers,
                    onOpenPricing = { order -> dualPricingOrder = order },
                    onOpenAssign = { order -> assignDesignerOrder = order },
                    onPreviewReceipt = { url -> previewReceiptUrl = url },
                    onApproveAdvance = { order, time ->
                        orderViewModel.approveAdvancePayment(order, time) {
                            Toast.makeText(context, "تم تأكيد الدفعة (70%) ونقل الطلب للإسناد ✓", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onApproveFinal = { order ->
                        orderViewModel.approveFinalPayment(order) {
                            Toast.makeText(context, "تم اعتماد الدفعة النهائية (30%) واكتمال الطلب ✓", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                1 -> AdminFinancialStatsTab(orders = orders)
                2 -> AdminUsersManagementTab(
                    allUsers = allUsers,
                    designerRequests = designerRequests,
                    onToggleActivation = { uid, act ->
                        mainViewModel.toggleAccountActivation(
                            userId = uid,
                            isActivated = act,
                            onSuccess = {
                                Toast.makeText(context, "تم تحديث حالة تفعيل الحساب بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "فشل تحديث حالة الحساب: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    onUpdateRole = { uid, role -> mainViewModel.updateUserRole(uid, role) },
                    onApproveDesigner = { reqId, uid -> mainViewModel.approveDesignerUpgrade(reqId, uid) }
                )
                3 -> AdminStandardCalculatorTab()
                4 -> AdminSettingsConfigTab(
                    config = appConfig,
                    onSaveConfig = { cfg ->
                        mainViewModel.updateAppConfig(cfg) {
                            Toast.makeText(context, "تم حفظ إعدادات المنصة بنجاح ✓", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                5 -> AdminBroadcastNotificationsTab(
                    notifications = notifications,
                    onSendBroadcast = { title, msg, type ->
                        coroutineScope.launch {
                            mainViewModel.notificationRepository.sendNotification(
                                title = title,
                                message = msg,
                                type = type,
                                targetId = "all"
                            )
                            Toast.makeText(context, "تم نشر الإشعار العام للمستخدمين ✓", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteNotification = { id ->
                        coroutineScope.launch {
                            mainViewModel.notificationRepository.deleteNotification(id)
                        }
                    }
                )
            }
        }
    }

    // 1. Dual Pricing Dialog
    if (dualPricingOrder != null) {
        DualPricingDialog(
            order = dualPricingOrder!!,
            commissionPercentage = appConfig.platformCommissionPercentage,
            onSubmit = { designAmt, prodAmt, deliveryTime ->
                orderViewModel.submitDualQuote(
                    order = dualPricingOrder!!,
                    designAmount = designAmt,
                    productionAmount = prodAmt,
                    commissionPercentage = appConfig.platformCommissionPercentage,
                    estimatedDeliveryTime = deliveryTime
                )
                dualPricingOrder = null
                Toast.makeText(context, "تم إرسال التسعيرة للعميل بنجاح 💰", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { dualPricingOrder = null }
        )
    }

    // 2. Assign Designer with Load Balancing Dialog
    if (assignDesignerOrder != null) {
        AssignDesignerDialog(
            order = assignDesignerOrder!!,
            designers = allUsers.filter { it.role.uppercase() == UserRepository.ROLE_DESIGNER },
            onAssign = { designer ->
                orderViewModel.assignDesigner(
                    orderId = assignDesignerOrder!!.id,
                    designerId = designer.uid,
                    designerName = designer.name
                ) {
                    assignDesignerOrder = null
                    Toast.makeText(context, "تم إسناد الطلب للمصمم ${designer.name} بنجاح", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { assignDesignerOrder = null }
        )
    }

    // 3. Receipt Fullscreen Preview Dialog
    if (previewReceiptUrl != null) {
        ReceiptPreviewDialog(
            imageUrl = previewReceiptUrl!!,
            onDismiss = { previewReceiptUrl = null }
        )
    }
}

// -------------------------------------------------------------
// TAB 0: Admin Orders, Dual Pricing & Payments
// -------------------------------------------------------------
@Composable
fun AdminOrdersManagementTab(
    orders: List<OrderItem>,
    allUsers: List<UserProfile>,
    onOpenPricing: (OrderItem) -> Unit,
    onOpenAssign: (OrderItem) -> Unit,
    onPreviewReceipt: (String) -> Unit,
    onApproveAdvance: (OrderItem, String) -> Unit,
    onApproveFinal: (OrderItem) -> Unit
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد طلبات واردة حالياً", color = MaterialTheme.colorScheme.outline)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(orders, key = { it.id }) { order ->
                AdminOrderCard(
                    order = order,
                    onOpenPricing = { onOpenPricing(order) },
                    onOpenAssign = { onOpenAssign(order) },
                    onPreviewReceipt = onPreviewReceipt,
                    onApproveAdvance = onApproveAdvance,
                    onApproveFinal = { onApproveFinal(order) }
                )
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: OrderItem,
    onOpenPricing: () -> Unit,
    onOpenAssign: () -> Unit,
    onPreviewReceipt: (String) -> Unit,
    onApproveAdvance: (OrderItem, String) -> Unit,
    onApproveFinal: () -> Unit
) {
    var deliveryTimeInput by remember { mutableStateOf(order.estimatedDeliveryTime ?: "خلال 48 ساعة") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "طلب ${order.displayOrderNumber}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    Text(text = order.designType.ifBlank { "طلب عمل" }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                StatusBadge(status = order.status, paymentStatus = order.advancePaymentStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Client Info (Admin CAN see client info!)
            Text(
                text = "العميل: ${order.clientName} • هاتف: ${order.clientPhone}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )

            // Specs
            if (order.detailedSpecs.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "المواصفات: ${order.detailedSpecs}",
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            // Designer Rejection Warning (if any)
            if (order.rejectionHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val lastRejection = order.rejectionHistory.lastOrNull()
                val designerName = lastRejection?.get("designerName")?.toString() ?: "المصمم"
                val reason = lastRejection?.get("reason")?.toString() ?: "ضغط عمل"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "اعتذر $designerName عن الطلب (السبب: $reason). يرجى إعادة الإسناد لمصمم بديل.",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons based on status:
            when (order.status) {
                OrderStatus.PENDING_PRICING -> {
                    Button(
                        onClick = onOpenPricing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.PriceCheck, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسعير الطلب وتحديد التكاليف 💰", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                OrderStatus.WAITING_FOR_ADVANCE -> {
                    if (order.advancePaymentStatus == PaymentStatus.UNDER_REVIEW) {
                        Column {
                            Text("إشعار سداد الـ 70% وارد للمراجعة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E40AF))
                            Spacer(modifier = Modifier.height(6.dp))

                            if (order.advanceReceiptUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onPreviewReceipt(order.advanceReceiptUrl) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("معاينة صورة إشعار الحوالة 👁️", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            OutlinedTextField(
                                value = deliveryTimeInput,
                                onValueChange = { deliveryTimeInput = it },
                                label = { Text("موعد التسليم المتوقع للعميل") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { onApproveAdvance(order, deliveryTimeInput) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("تأكيد استلام الدفعة (70%) ونقل للإسناد ✓", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Text("بانتظار سداد العميل للدفعة المقدمة (70%): ${order.calculatedAdvance.toInt()} ر.ي", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                OrderStatus.WAITING_FOR_ASSIGNMENT -> {
                    Button(
                        onClick = onOpenAssign,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text("إسناد لمصمم متفرغ 🎨 (توزيع المهام)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                OrderStatus.WAITING_FOR_FINAL_PAYMENT -> {
                    if (order.finalPaymentStatus == PaymentStatus.UNDER_REVIEW) {
                        Column {
                            Text("إشعار سداد الدفعة النهائية (30%) وارد:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF15803D))
                            Spacer(modifier = Modifier.height(6.dp))
                            if (order.finalReceiptUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onPreviewReceipt(order.finalReceiptUrl) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("معاينة إشعار الـ 30% 👁️", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Button(
                                onClick = onApproveFinal,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("تأكيد استلام الـ 30% وإكمال الطلب 🏁", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Text("بانتظار سداد العميل للدفعة المتبقية (30%): ${order.calculatedRemaining.toInt()} ر.ي", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                OrderStatus.COMPLETED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مكتمل ومسلّم للعميل • مستحقات المصمم: ${order.designerFee.toInt()} ر.ي", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                }

                else -> {
                    Text("الحالة الحالية: ${order.status}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DUAL PRICING DIALOG
// -------------------------------------------------------------
@Composable
fun DualPricingDialog(
    order: OrderItem,
    commissionPercentage: Double,
    onSubmit: (designAmount: Double, productionAmount: Double, deliveryTime: String) -> Unit,
    onDismiss: () -> Unit
) {
    var designInput by remember { mutableStateOf("") }
    var productionInput by remember { mutableStateOf("") }
    var deliveryTimeInput by remember { mutableStateOf("خلال 48 ساعة") }

    val designVal = designInput.toDoubleOrNull() ?: 0.0
    val prodVal = productionInput.toDoubleOrNull() ?: 0.0
    val totalVal = designVal + prodVal
    val advanceVal = totalVal * 0.70
    val remainingVal = totalVal * 0.30

    // Commission from design only
    val platformFeeVal = designVal * (commissionPercentage / 100.0)
    val designerFeeVal = designVal - platformFeeVal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسعير الطلب ${order.displayOrderNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                Text(
                    text = "فصل أتعاب التصميم عن تكاليف الطباعة والمواد:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = designInput,
                    onValueChange = { designInput = it },
                    label = { Text("أتعاب التصميم (ريال) *") },
                    placeholder = { Text("مثال: 15000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = productionInput,
                    onValueChange = { productionInput = it },
                    label = { Text("تكاليف الطباعة والمواد (ريال)") },
                    placeholder = { Text("0 للتصاميم الرقمية") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = deliveryTimeInput,
                    onValueChange = { deliveryTimeInput = it },
                    label = { Text("موعد التسليم المتوقع") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Calculations Breakdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("الإجمالي للعميل: ${totalVal.toInt()} ر.ي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("• الدفعة المقدمة 70%: ${advanceVal.toInt()} ر.ي", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Text("• الدفعة المتبقية 30%: ${remainingVal.toInt()} ر.ي", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• عمولة المنصة (${commissionPercentage.toInt()}% من التصميم): ${platformFeeVal.toInt()} ر.ي", fontSize = 11.sp, color = Color(0xFF1E40AF))
                        Text("• صافي مستحقات المصمم: ${designerFeeVal.toInt()} ر.ي", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (totalVal > 0) {
                        onSubmit(designVal, prodVal, deliveryTimeInput.trim())
                    }
                },
                enabled = totalVal > 0,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إرسال التسعيرة للعميل", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("إلغاء")
            }
        }
    )
}

// -------------------------------------------------------------
// ASSIGN DESIGNER WITH LOAD BALANCING DIALOG
// -------------------------------------------------------------
@Composable
fun AssignDesignerDialog(
    order: OrderItem,
    designers: List<UserProfile>,
    onAssign: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار مصمم للطلب ${order.displayOrderNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("توزيع المهام الذكي: الأولوية للمصممين المتفرغين (0 مهام نشطة)", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(10.dp))

                if (designers.isEmpty()) {
                    Text("لا يوجد مصممون معتمدون مسجلون حالياً", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                } else {
                    // Sort free designers first
                    val sorted = designers.sortedWith(
                        compareBy<UserProfile> { it.activeTasksCount }.thenByDescending { it.ratingAverage }
                    )

                    LazyColumn(
                        modifier = Modifier.height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sorted) { designer ->
                            val isFree = designer.activeTasksCount == 0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAssign(designer) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isFree) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isFree) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(designer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(String.format("%.1f", designer.ratingAverage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("• ${designer.phone}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }

                                    if (isFree) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF22C55E)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("متفرغ / متاح للعمل", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                        }
                                    } else {
                                        Text("${designer.activeTasksCount} مهام نشطة", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("إغلاق")
            }
        }
    )
}

// -------------------------------------------------------------
// RECEIPT PREVIEW DIALOG
// -------------------------------------------------------------
@Composable
fun ReceiptPreviewDialog(imageUrl: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("معاينة إشعار التحويل البنكي", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "صورة الإشعار",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("إغلاق")
            }
        }
    )
}

// -------------------------------------------------------------
// TAB 1: Financial Stats & Audit Table
// -------------------------------------------------------------
@Composable
fun AdminFinancialStatsTab(orders: List<OrderItem>) {
    val completed = orders.filter { it.status == OrderStatus.COMPLETED }
    val totalRevenue = completed.sumOf { it.effectiveTotal }
    val totalDesignersFee = completed.sumOf { it.designerFee }
    val totalPlatformProfit = completed.sumOf { it.productionAmount + it.platformFee }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("صافي دخل المنصة", fontSize = 11.sp, color = Color(0xFF166534))
                        Text("${totalPlatformProfit.toInt()} ر.ي", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF14532D))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDBEAFE)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("مستحقات المصممين", fontSize = 11.sp, color = Color(0xFF1E40AF))
                        Text("${totalDesignersFee.toInt()} ر.ي", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1E3A8A))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("الطلبات المكتملة", fontSize = 11.sp, color = Color(0xFF92400E))
                        Text("${completed.size} طلب", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF78350F))
                    }
                }
            }
        }

        item {
            Text("جدول تفصيلي لعمليات المنصة:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(orders) { o ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("طلب ${o.displayOrderNumber} • ${o.designType}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${o.effectiveTotal.toInt()} ر.ي", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("العميل: ${o.clientName} (${o.clientPhone})", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("عمولة المنصة: ${o.platformFee.toInt()} ر.ي", fontSize = 11.sp, color = Color(0xFF1E40AF))
                        Text("أتعاب المصمم: ${o.designerFee.toInt()} ر.ي", fontSize = 11.sp, color = Color(0xFF16A34A))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: Users Management & Activation
// -------------------------------------------------------------
@Composable
fun AdminUsersManagementTab(
    allUsers: List<UserProfile>,
    designerRequests: List<DesignerUpgradeRequest>,
    onToggleActivation: (uid: String, isActivated: Boolean) -> Unit,
    onUpdateRole: (uid: String, newRole: String) -> Unit,
    onApproveDesigner: (reqId: String, targetUid: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = allUsers.filter { u ->
        searchQuery.isBlank() || u.name.contains(searchQuery, true) || u.phone.contains(searchQuery)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Designer Requests section (if any pending)
        val pendingRequests = designerRequests.filter { it.status == "PENDING" }
        if (pendingRequests.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("طلبات الترقية إلى مصمم معتمد (${pendingRequests.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                        Spacer(modifier = Modifier.height(8.dp))
                        pendingRequests.forEach { req ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(req.userName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("معرض الأعمال: ${req.portfolioUrl}", fontSize = 10.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = { onApproveDesigner(req.id, req.userId) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    Text("ترقية كمصمم", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث بالاسم أو رقم الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(filtered, key = { it.uid }) { u ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(u.name.ifBlank { "مستخدم بدون اسم" }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(u.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text("الرتبة: ${u.role}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (u.isAccountActivated) "مفعّل" else "معطّل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = u.isAccountActivated,
                                onCheckedChange = { onToggleActivation(u.uid, it) }
                            )
                        }

                        // Role toggle dropdown/buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { onUpdateRole(u.uid, UserRepository.ROLE_CLIENT) },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("عميل", fontSize = 9.sp)
                            }
                            OutlinedButton(
                                onClick = { onUpdateRole(u.uid, UserRepository.ROLE_DESIGNER) },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("مصمم", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: Standard Basic Calculator (آلة حاسبة قياسية بسيطة جداً)
// -------------------------------------------------------------
@Composable
fun AdminStandardCalculatorTab() {
    var display by remember { mutableStateOf("0") }
    var operand1 by remember { mutableDoubleStateOf(0.0) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var shouldClearOnNextDigit by remember { mutableStateOf(false) }

    fun onDigit(d: String) {
        if (display == "0" || shouldClearOnNextDigit) {
            display = d
            shouldClearOnNextDigit = false
        } else {
            display += d
        }
    }

    fun onOp(op: String) {
        operand1 = display.toDoubleOrNull() ?: 0.0
        pendingOp = op
        shouldClearOnNextDigit = true
    }

    fun onEqual() {
        val operand2 = display.toDoubleOrNull() ?: 0.0
        val res = when (pendingOp) {
            "+" -> operand1 + operand2
            "-" -> operand1 - operand2
            "×" -> operand1 * operand2
            "÷" -> if (operand2 != 0.0) operand1 / operand2 else 0.0
            else -> operand2
        }
        display = if (res % 1.0 == 0.0) res.toLong().toString() else String.format("%.2f", res)
        pendingOp = null
        shouldClearOnNextDigit = true
    }

    fun onClear() {
        display = "0"
        operand1 = 0.0
        pendingOp = null
        shouldClearOnNextDigit = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("آلة حاسبة قياسية بسيطة للإدارة", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        // Calculator Screen
        Card(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = display,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            }
        }

        // Keypad Grid
        val buttons = listOf(
            listOf("C", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )

        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { label ->
                    val isOp = label in listOf("+", "-", "×", "÷", "=")
                    val isClear = label == "C"

                    Button(
                        onClick = {
                            when (label) {
                                "C" -> onClear()
                                "=" -> onEqual()
                                "+", "-", "×", "÷" -> onOp(label)
                                "." -> if (!display.contains(".")) display += "."
                                else -> onDigit(label)
                            }
                        },
                        modifier = Modifier.weight(if (label == "C" || label == "=") 1.5f else 1f).height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isClear -> MaterialTheme.colorScheme.error
                                isOp -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOp || isClear) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: Settings & Config
// -------------------------------------------------------------
@Composable
fun AdminSettingsConfigTab(
    config: AppConfig,
    onSaveConfig: (AppConfig) -> Unit
) {
    var whatsappInput by remember { mutableStateOf(config.whatsappNumber) }
    var commissionInput by remember { mutableStateOf(config.platformCommissionPercentage.toString()) }
    var nameInput by remember { mutableStateOf(config.beneficiaryName) }
    var accountInput by remember { mutableStateOf(config.beneficiaryAccount) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Text("إعدادات المنصة والحسابات المعتمدة:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        item {
            OutlinedTextField(
                value = whatsappInput,
                onValueChange = { whatsappInput = it },
                label = { Text("رقم واتساب التفعيل المعتمد") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = commissionInput,
                onValueChange = { commissionInput = it },
                label = { Text("نسبة عمولة المنصة من أتعاب التصميم (%)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("اسم المستلم المعتمد للحوالات") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = accountInput,
                onValueChange = { accountInput = it },
                label = { Text("رقم حساب المستلم (جيب / جوالي / ون كاش)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    val comm = commissionInput.toDoubleOrNull() ?: 20.0
                    onSaveConfig(
                        config.copy(
                            whatsappNumber = whatsappInput.trim(),
                            platformCommissionPercentage = comm,
                            beneficiaryName = nameInput.trim(),
                            beneficiaryAccount = accountInput.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ التعديلات في السحابة ✓", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 5: Broadcast Notifications
// -------------------------------------------------------------
@Composable
fun AdminBroadcastNotificationsTab(
    notifications: List<NotificationItem>,
    onSendBroadcast: (title: String, msg: String, type: String) -> Unit,
    onDeleteNotification: (id: String) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("عام") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("إرسال إشعار عام للمستخدمين 📢", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("عنوان الإشعار") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text("نص الإشعار") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (titleInput.isNotBlank() && messageInput.isNotBlank()) {
                                onSendBroadcast(titleInput.trim(), messageInput.trim(), selectedType)
                                titleInput = ""
                                messageInput = ""
                            }
                        },
                        enabled = titleInput.isNotBlank() && messageInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("نشر الإشعار الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("الإشعارات السابقة المنشورة (حذف فوري):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        items(notifications, key = { it.id }) { notif ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(notif.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = { onDeleteNotification(notif.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
