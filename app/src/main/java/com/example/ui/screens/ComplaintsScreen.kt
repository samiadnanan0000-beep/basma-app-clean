package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ComplaintTicket
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun ComplaintsScreen(
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by mainViewModel.userProfile.collectAsState()
    val isStaff = mainViewModel.isEffectiveStaff()

    val tickets by mainViewModel.complaintRepository
        .observeComplaintsForUser(userProfile?.uid ?: "", isStaff)
        .collectAsState(initial = emptyList())

    var activeTab by remember { mutableStateOf(if (isStaff) 1 else 0) }
    val tabTitles = if (isStaff) {
        listOf("تقديم رسالة جديدة", "صندوق الشكاوى الواردة (${tickets.size})")
    } else {
        listOf("تقديم شكوى أو اقتراح", "سجل تذاكري ومقترحاتي (${tickets.size})")
    }

    // Form inputs
    val ticketTypes = listOf("شكوى" to "complaint", "اقتراح تطوير" to "suggestion", "استفسار عام" to "inquiry")
    var selectedType by remember { mutableStateOf("complaint") }
    var subjectInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var orderIdInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Admin Reply Dialog
    var replyingTicket by remember { mutableStateOf<ComplaintTicket?>(null) }
    var adminReplyText by remember { mutableStateOf("") }

    if (replyingTicket != null) {
        AlertDialog(
            onDismissRequest = { replyingTicket = null },
            title = { Text("الرد على التذكرة ومعالجتها 💬", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("المرسل: ${replyingTicket?.clientName} (${replyingTicket?.clientPhone})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("الموضوع: ${replyingTicket?.subject}", fontSize = 12.sp)
                    Text("الرسالة: ${replyingTicket?.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = adminReplyText,
                        onValueChange = { adminReplyText = it },
                        label = { Text("نص الرد والتوجيه للعميل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ticket = replyingTicket
                        if (ticket != null && adminReplyText.isNotBlank()) {
                            coroutineScope.launch {
                                mainViewModel.complaintRepository.replyAndResolveTicket(
                                    ticketId = ticket.id,
                                    clientId = ticket.clientId,
                                    replyMessage = adminReplyText,
                                    subject = ticket.subject
                                )
                                replyingTicket = null
                                adminReplyText = ""
                                Toast.makeText(context, "تم الرد على العميل وإرسال إشعار فوري", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("إرسال الرد وإغلاق التذكرة")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { replyingTicket = null }) {
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
                text = "الشكاوى والاقتراحات 💬",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "نسعد باستقبال آرائكم وملاحظاتكم لتطوير خدمات بصمة باستمرار",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        item {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title, fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        if (activeTab == 0) {
            // New Ticket Form
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "نوع المراسلة",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ticketTypes.forEach { (label, code) ->
                                val isSelected = selectedType == code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedType = code },
                                    label = { Text(label, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = subjectInput,
                            onValueChange = { subjectInput = it },
                            label = { Text("عنوان الشكوى أو المقترح") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("complaint_subject_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = orderIdInput,
                            onValueChange = { orderIdInput = it },
                            label = { Text("رقم الطلب المرتبط (اختياري)") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            label = { Text("التفاصيل والملاحظات الكاملة") },
                            minLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("complaint_message_input")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (subjectInput.isNotBlank() && messageInput.isNotBlank()) {
                                    isSending = true
                                    val ticket = ComplaintTicket(
                                        clientId = userProfile?.uid ?: "",
                                        clientName = userProfile?.name ?: "عميل",
                                        clientPhone = userProfile?.phone ?: "",
                                        type = selectedType,
                                        subject = subjectInput,
                                        message = messageInput,
                                        orderId = orderIdInput
                                    )
                                    coroutineScope.launch {
                                        mainViewModel.complaintRepository.submitTicket(ticket)
                                        isSending = false
                                        subjectInput = ""
                                        messageInput = ""
                                        orderIdInput = ""
                                        activeTab = 1
                                        Toast.makeText(context, "تم إرسال التذكرة بنجاح وستتم مراجعتها", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSending,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_complaint_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("إرسال التذكرة للإدارة والمصممين", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Tickets History / Inbox
            if (tickets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد تذاكر مسجلة حالياً", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                    }
                }
            } else {
                items(tickets, key = { it.id }) { ticket ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = ticket.subject,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isStaff) {
                                        Text(
                                            text = "من: ${ticket.clientName} (${ticket.clientPhone})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                val isResolved = ticket.status == "resolved"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isResolved) Color(0xFFD1FAE5) else Color(0xFFFEF3C7))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isResolved) "تم الرد والمعالجة 🟢" else "قيد المراجعة 🟡",
                                        color = if (isResolved) Color(0xFF065F46) else Color(0xFF92400E),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = ticket.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            if (ticket.orderId.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("رقم الطلب المرتبط: #${ticket.orderId.take(6)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }

                            // Designer Reply Box (if replied)
                            if (ticket.replyMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Reply, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("رد إدارة واستوديو بصمة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E40AF))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(ticket.replyMessage, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                                    }
                                }
                            }

                            // Staff Reply Action Button
                            if (isStaff && ticket.status != "resolved") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        replyingTicket = ticket
                                        adminReplyText = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("كتابة رد للعميل وإغلاق التذكرة", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
