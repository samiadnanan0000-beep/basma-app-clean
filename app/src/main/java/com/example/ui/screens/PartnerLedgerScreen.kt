package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PartnerPrintShop
import com.example.data.repository.PartnerRepository
import kotlinx.coroutines.launch

@Composable
fun PartnerLedgerScreen(
    partnerRepository: PartnerRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val partners by partnerRepository.observePartners().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var editingPartner by remember { mutableStateOf<PartnerPrintShop?>(null) }
    var deletingPartnerId by remember { mutableStateOf<String?>(null) }

    var pName by remember { mutableStateOf("") }
    var pPhone by remember { mutableStateOf("") }
    var pType by remember { mutableStateOf("") }
    var pCost by remember { mutableStateOf("") }
    var pNotes by remember { mutableStateOf("") }

    if (showAddDialog || editingPartner != null) {
        val isEditing = editingPartner != null
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingPartner = null
            },
            title = {
                Text(
                    text = if (isEditing) "تعديل بيانات المطبعة الوسيطة" else "إضافة مطبعة وسيطة جديدة 🖨️",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = pName,
                        onValueChange = { pName = it },
                        label = { Text("اسم المطبعة أو الورشة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pPhone,
                        onValueChange = { pPhone = it },
                        label = { Text("رقم الهاتف للتواصل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pType,
                        onValueChange = { pType = it },
                        label = { Text("نوع الطباعة (فلكس، ليزر، أوفست...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pCost,
                        onValueChange = { pCost = it },
                        label = { Text("إجمالي تكلفة الفواتير (YER)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pNotes,
                        onValueChange = { pNotes = it },
                        label = { Text("ملاحظات وشروط التسليم") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = pCost.toDoubleOrNull() ?: 0.0
                        if (pName.isNotBlank()) {
                            coroutineScope.launch {
                                if (isEditing && editingPartner != null) {
                                    val updated = editingPartner!!.copy(
                                        name = pName,
                                        phone = pPhone,
                                        printType = pType,
                                        costTotal = cost,
                                        notes = pNotes
                                    )
                                    partnerRepository.updatePartner(updated)
                                } else {
                                    val newPartner = PartnerPrintShop(
                                        name = pName,
                                        phone = pPhone,
                                        printType = pType,
                                        costTotal = cost,
                                        notes = pNotes
                                    )
                                    partnerRepository.addPartner(newPartner)
                                }
                                showAddDialog = false
                                editingPartner = null
                                pName = ""
                                pPhone = ""
                                pType = ""
                                pCost = ""
                                pNotes = ""
                            }
                        }
                    }
                ) {
                    Text(if (isEditing) "حفظ التعديل" else "إضافة المطبعة")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAddDialog = false
                        editingPartner = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (deletingPartnerId != null) {
        AlertDialog(
            onDismissRequest = { deletingPartnerId = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد من حذف هذه المطبعة من سجل الشركاء؟") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = deletingPartnerId
                        if (id != null) {
                            coroutineScope.launch {
                                partnerRepository.deletePartner(id)
                                deletingPartnerId = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingPartnerId = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pName = ""
                    pPhone = ""
                    pType = ""
                    pCost = ""
                    pNotes = ""
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .testTag("add_partner_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مطبعة شريكة")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
        ) {
            item {
                Text(
                    text = "سجل المطابع الوسيطة 🖨️",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "متابعة ورش ومطابع التنفيذ الخارجية والأسعار وفواتير الإنتاج",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            items(partners, key = { it.id }) { partner ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = partner.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = partner.printType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row {
                                if (partner.phone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val uri = Uri.parse("tel:${partner.phone}")
                                            context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                                        }
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "اتصال", tint = Color(0xFF10B981))
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        editingPartner = partner
                                        pName = partner.name
                                        pPhone = partner.phone
                                        pType = partner.printType
                                        pCost = partner.costTotal.toInt().toString()
                                        pNotes = partner.notes
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { deletingPartnerId = partner.id }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي الفواتير التراكمية:", fontSize = 12.sp)
                            Text(
                                text = "${partner.costTotal.toInt()} ${partner.currency}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }

                        if (partner.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ملاحظات: ${partner.notes}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
