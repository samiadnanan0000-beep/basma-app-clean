package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MaterialItem
import com.example.data.repository.MaterialRepository
import kotlinx.coroutines.launch

@Composable
fun MaterialsScreen(
    materialRepository: MaterialRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val materials by materialRepository.observeMaterials().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var editingMaterial by remember { mutableStateOf<MaterialItem?>(null) }
    var deletingMaterialId by remember { mutableStateOf<String?>(null) }

    var matName by remember { mutableStateOf("") }
    var matUnitType by remember { mutableStateOf("متر مربع") }
    var matPrice by remember { mutableStateOf("") }
    var matCurrency by remember { mutableStateOf("YER") }
    var matInStock by remember { mutableStateOf(true) }

    // Add / Edit Dialog
    if (showAddDialog || editingMaterial != null) {
        val isEditing = editingMaterial != null
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingMaterial = null
            },
            title = {
                Text(
                    text = if (isEditing) "تعديل بيانات الخامة" else "إضافة خامة جديدة للمخزون 📦",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = matName,
                        onValueChange = { matName = it },
                        label = { Text("اسم الخامة أو المادة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = matUnitType,
                        onValueChange = { matUnitType = it },
                        label = { Text("نوع الوحدة (متر مربع، لوح، قطعة...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = matPrice,
                        onValueChange = { matPrice = it },
                        label = { Text("سعر الوحدة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("متوفر في المخزون حالياً", fontSize = 13.sp)
                        Switch(
                            checked = matInStock,
                            onCheckedChange = { matInStock = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = matPrice.toDoubleOrNull() ?: 0.0
                        if (matName.isNotBlank() && price > 0) {
                            coroutineScope.launch {
                                if (isEditing && editingMaterial != null) {
                                    val updated = editingMaterial!!.copy(
                                        name = matName,
                                        unitType = matUnitType,
                                        pricePerUnit = price,
                                        currency = matCurrency,
                                        inStock = matInStock
                                    )
                                    materialRepository.updateMaterial(updated)
                                } else {
                                    val newMat = MaterialItem(
                                        name = matName,
                                        unitType = matUnitType,
                                        pricePerUnit = price,
                                        currency = matCurrency,
                                        inStock = matInStock
                                    )
                                    materialRepository.addMaterial(newMat)
                                }
                                showAddDialog = false
                                editingMaterial = null
                                matName = ""
                                matPrice = ""
                            }
                        }
                    }
                ) {
                    Text(if (isEditing) "حفظ التعديل" else "إضافة الخامة")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAddDialog = false
                        editingMaterial = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingMaterialId != null) {
        AlertDialog(
            onDismissRequest = { deletingMaterialId = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد من حذف هذه الخامة من قائمة المخزون وحاسبة الأسعار؟") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = deletingMaterialId
                        if (id != null) {
                            coroutineScope.launch {
                                materialRepository.deleteMaterial(id)
                                deletingMaterialId = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingMaterialId = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    matName = ""
                    matUnitType = "متر مربع"
                    matPrice = ""
                    matInStock = true
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .testTag("add_material_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة خامة جديدة")
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
                    text = "إدارة الخامات والأسعار 📦",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "أسعار الخامات والمواد الأساسية المعتمدة في التسعير وحاسبة الإنتاج",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            items(materials, key = { it.id }) { material ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = material.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${material.pricePerUnit.toInt()} ${material.currency} / ${material.unitType}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (material.inStock) Color(0xFFD1FAE5) else Color(0xFFFEE2E2))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (material.inStock) "متوفر بالمخزن" else "نفذ المخزون",
                                        color = if (material.inStock) Color(0xFF065F46) else Color(0xFFB91C1C),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    editingMaterial = material
                                    matName = material.name
                                    matUnitType = material.unitType
                                    matPrice = material.pricePerUnit.toInt().toString()
                                    matCurrency = material.currency
                                    matInStock = material.inStock
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(
                                onClick = { deletingMaterialId = material.id }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
