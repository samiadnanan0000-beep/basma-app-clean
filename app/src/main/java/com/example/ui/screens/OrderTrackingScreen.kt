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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.ui.components.AdvancePaymentDialog
import com.example.ui.viewmodel.OrderViewModel
import com.example.util.WhatsAppHelper

@Composable
fun OrderTrackingScreen(
    orderViewModel: OrderViewModel,
    onNavigateToNewOrder: () -> Unit
) {
    val context = LocalContext.current
    val orders by orderViewModel.orders.collectAsState()

    var paymentOrder by remember { mutableStateOf<OrderItem?>(null) }
    var isFinalPaymentTarget by remember { mutableStateOf(false) }

    var chatOrder by remember { mutableStateOf<OrderItem?>(null) }
    var ratingOrder by remember { mutableStateOf<OrderItem?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewImageTitle by remember { mutableStateOf("معاينة الصورة") }

    if (previewImageUrl != null) {
        com.example.ui.components.FullScreenImagePreviewDialog(
            imageUrl = previewImageUrl!!,
            title = previewImageTitle,
            onDismiss = { previewImageUrl = null }
        )
    }

    if (paymentOrder != null) {
        AdvancePaymentDialog(
            order = paymentOrder!!,
            orderViewModel = orderViewModel,
            isFinalPayment = isFinalPaymentTarget,
            onDismiss = { paymentOrder = null },
            onPaymentSubmitted = { paymentOrder = null }
        )
    }

    if (chatOrder != null) {
        OrderChatDialog(
            order = chatOrder!!,
            orderViewModel = orderViewModel,
            onDismiss = { chatOrder = null }
        )
    }

    if (ratingOrder != null) {
        OrderRatingDialog(
            order = ratingOrder!!,
            onSubmit = { rating, review ->
                orderViewModel.rateOrder(ratingOrder!!.id, ratingOrder!!.assignedDesignerId, rating, review)
                ratingOrder = null
                Toast.makeText(context, "شكراً لتقييمك! نسعى دائماً لتقديم الأفضل.", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { ratingOrder = null }
        )
    }

    if (orders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "لا توجد طلبات حالية",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "يمكنك تقديم طلب تصميم جديد للبدء بالتسعير والمتابعة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onNavigateToNewOrder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("طلب عمل جديد الآن")
                }
            }
        }
    } else {
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
                    text = "متابعة طلباتي 📦",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تتبع مراحل التسعير، الدفعات المالية، وتنفيذ التصاميم لحظة بلحظة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            items(orders, key = { it.id }) { order ->
                OrderTrackingCard(
                    order = order,
                    onOpenPayment = { isFinal ->
                        isFinalPaymentTarget = isFinal
                        paymentOrder = order
                    },
                    onAcceptQuote = {
                        orderViewModel.clientAcceptQuote(order.id)
                        isFinalPaymentTarget = false
                        paymentOrder = order
                    },
                    onCancelOrder = {
                        orderViewModel.clientCancelOrder(order.id)
                    },
                    onOpenChat = { chatOrder = order },
                    onOpenRating = { ratingOrder = order },
                    onContactWhatsApp = {
                        WhatsAppHelper.openChat(
                            context = context,
                            message = "مرحباً إدارة بصمة، بخصوص طلبي ${order.displayOrderNumber} (${order.designType}):"
                        )
                    },
                    onPreviewImage = { url, title ->
                        previewImageUrl = url
                        previewImageTitle = title
                    }
                )
            }
        }
    }
}

@Composable
fun OrderTrackingCard(
    order: OrderItem,
    onOpenPayment: (isFinal: Boolean) -> Unit,
    onAcceptQuote: () -> Unit,
    onCancelOrder: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenRating: () -> Unit,
    onContactWhatsApp: () -> Unit,
    onPreviewImage: (imageUrl: String, title: String) -> Unit = { _, _ -> }
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Order Number & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.designType.ifBlank { order.category },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "طلب ${order.displayOrderNumber}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                StatusBadge(status = order.status, paymentStatus = order.advancePaymentStatus)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Technical Specs Summary
            if (order.detailedSpecs.isNotBlank()) {
                Text(
                    text = order.detailedSpecs,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Dimensions and deadline banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (order.width > 0 && order.height > 0) {
                    Text(
                        text = "المقاس: ${order.width}م × ${order.height}م",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text("المقاس: حسب التصميم", fontSize = 12.sp)
                }

                if (!order.estimatedDeliveryTime.isNullOrBlank()) {
                    Text(
                        text = "التسليم: ${order.estimatedDeliveryTime}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Attachments & Receipts preview buttons
            if (order.referenceAttachmentUrl.isNotBlank() || order.advanceReceiptUrl.isNotBlank() || order.finalReceiptUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (order.referenceAttachmentUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onPreviewImage(order.referenceAttachmentUrl, "المرفق المرجعي للطلب") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("المرفق 🖼️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (order.advanceReceiptUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onPreviewImage(order.advanceReceiptUrl, "إشعار الدفعة المقدمة (70%)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("إشعار 70% 👁️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (order.finalReceiptUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onPreviewImage(order.finalReceiptUrl, "إشعار الدفعة النهائية (30%)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("إشعار 30% 👁️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 1. Quoting Card if status is PRICE_OFFERED
            if (order.status == OrderStatus.PRICE_OFFERED) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "عرض السعر المعتمد من الإدارة 💰",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي تكلفة الطلب:", fontSize = 13.sp)
                            Text("${order.effectiveTotal.toInt()} ر.ي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Advance 70% highlight
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الدفعة المقدمة المطلوبة (70%):",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${order.calculatedAdvance.toInt()} ر.ي",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1D4ED8),
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المبلغ المتبقي عند التسليم (30%):", fontSize = 12.sp, color = Color.Gray)
                            Text("${order.calculatedRemaining.toInt()} ر.ي", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAcceptQuote,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("الموافقة وبدء السداد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = onCancelOrder,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("إلغاء الطلب", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. Button for WAITING_FOR_ADVANCE (if advance not submitted yet or rejected)
            if (order.status == OrderStatus.WAITING_FOR_ADVANCE && order.advancePaymentStatus != PaymentStatus.UNDER_REVIEW) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onOpenPayment(false) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("سداد الدفعة المقدمة (70%): ${order.calculatedAdvance.toInt()} ر.ي", fontWeight = FontWeight.Bold)
                }
            }

            // 3. Notice if advance payment under review
            if (order.advancePaymentStatus == PaymentStatus.UNDER_REVIEW) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                ) {
                    Text(
                        text = "تم إرسال إشعار الدفعة المقدمة (70%)، سيتم مراجعة التحويل وتأكيد بدء العمل في أقرب وقت.",
                        fontSize = 12.sp,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 4. Sample Review via WhatsApp Badge
            if (order.status == OrderStatus.SAMPLE_REVIEW || order.status == OrderStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الطلب قيد إعداد ومراجعة العينة عبر واتساب المنصة مع الإدارة",
                            fontSize = 12.sp,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 5. Button for final 30% payment
            if (order.status == OrderStatus.WAITING_FOR_FINAL_PAYMENT && order.finalPaymentStatus != PaymentStatus.UNDER_REVIEW) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onOpenPayment(true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("سداد الدفعة النهائية المتبقية (30%): ${order.calculatedRemaining.toInt()} ر.ي", fontWeight = FontWeight.Bold)
                }
            }

            // 6. Rating prompt if completed
            if (order.status == OrderStatus.COMPLETED && order.clientRating == 0f) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenRating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تقييم جودة الخدمة والمصمم", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row: In-App Chat & WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenChat,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("محادثة الطلب", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onContactWhatsApp,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("واتساب الإدارة", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, paymentStatus: String) {
    val (label, bg, fg) = when (status) {
        OrderStatus.PENDING_PRICING -> Triple("بانتظار التسعير 🟡", Color(0xFFFEF3C7), Color(0xFF92400E))
        OrderStatus.PRICE_OFFERED -> Triple("تم تحديد السعر 🔵", Color(0xFFDBEAFE), Color(0xFF1E40AF))
        OrderStatus.WAITING_FOR_ADVANCE -> Triple("بانتظار سداد الـ 70% 💳", Color(0xFFEDE9FE), Color(0xFF6D28D9))
        OrderStatus.WAITING_FOR_ASSIGNMENT -> Triple("بانتظار الإسناد ⏳", Color(0xFFE0E7FF), Color(0xFF3730A3))
        OrderStatus.PENDING_DESIGNER_ACCEPTANCE -> Triple("بانتظار قبول المصمم 🎨", Color(0xFFFCE7F3), Color(0xFF9D174D))
        OrderStatus.IN_PROGRESS -> Triple("قيد التنفيذ 🟢", Color(0xFFD1FAE5), Color(0xFF065F46))
        OrderStatus.SAMPLE_REVIEW -> Triple("مراجعة العينة 🔍", Color(0xFFCCFBF1), Color(0xFF115E59))
        OrderStatus.WAITING_FOR_FINAL_PAYMENT -> Triple("سداد الـ 30% 🏁", Color(0xFFFEF08A), Color(0xFF854D0E))
        OrderStatus.COMPLETED -> Triple("مكتمل ومسلّم ✅", Color(0xFFDCFCE7), Color(0xFF166534))
        OrderStatus.CANCELLED -> Triple("ملغي ❌", Color(0xFFFEE2E2), Color(0xFF991B1B))
        else -> Triple(status, Color.LightGray, Color.DarkGray)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = fg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
fun OrderChatDialog(
    order: OrderItem,
    orderViewModel: OrderViewModel,
    onDismiss: () -> Unit
) {
    val chatMessages by orderViewModel.observeChatMessages(order.id).collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }
    var warningMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "محادثة الطلب ${order.displayOrderNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "محادثة نصية آمنة ومراقبة (يُمنع تبادل أرقام الهواتف)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                // Warning if bypass detected
                if (warningMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = warningMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Messages list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (chatMessages.isEmpty()) {
                        item {
                            Text(
                                text = "لا توجد رسائل سابقة. يمكنك كتابة استفسارك الفني هنا.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    items(chatMessages) { msg ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = msg.senderName.ifBlank { "مستخدم" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(text = msg.text, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            warningMessage = null
                        },
                        placeholder = { Text("اكتب رسالتك هنا...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val msg = ChatMessage(
                                    senderId = order.clientId,
                                    senderName = order.clientName,
                                    senderRole = "CLIENT",
                                    text = messageText.trim()
                                )
                                orderViewModel.sendChatMessage(order.id, msg) { success, err ->
                                    if (success) {
                                        messageText = ""
                                        warningMessage = null
                                    } else {
                                        warningMessage = err ?: "تعذر إرسال الرسالة"
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun OrderRatingDialog(
    order: OrderItem,
    onSubmit: (rating: Float, review: String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var reviewText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تقييم الخدمة والتصميم ⭐",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "طلب ${order.displayOrderNumber} - ${order.designType}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 5 Stars row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    placeholder = { Text("أضف تعليقك حول جودة وسرعة التنفيذ (اختياري)...") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating.toFloat(), reviewText.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إرسال التقييم", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("تخطي")
            }
        }
    )
}
