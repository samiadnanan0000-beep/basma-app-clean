package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.data.model.MaterialItem
import com.example.data.repository.MaterialRepository
import kotlinx.coroutines.launch

@Composable
fun MaterialsScreen(
    materialRepository: MaterialRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Instant, real-time Flow collection for immediate recomposition
    val materials by materialRepository.observeMaterials().collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMaterial by remember { mutableStateOf<MaterialItem?>(null) }
    var deletingMaterialId by remember { mutableStateOf<String?>(null) }

    // Form fields
    var matName by remember { mutableStateOf("") }
    var matUnitType by remember { mutableStateOf("متر مربع") }
    var matPrice by remember { mutableStateOf("") }
    var matQuantity by remember { mutableStateOf("100") }
    var matCurrency by remember { mutableStateOf("YER") }
    var matInStock by remember { mutableStateOf(true) }
    var formError by remember { mutableStateOf<String?>(null) }

    val filteredMaterials = remember(materials, searchQuery) {
        if (searchQuery.isBlank()) {
            materials
        } else {
            materials.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.unitType.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Add / Edit Dialog with full field editing (Name, Price, Quantity, Unit, Stock)
    if (showAddDialog || editingMaterial != null) {
        val isEditing = editingMaterial != null
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingMaterial = null
                formError = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "تعديل بيانات الخامة" else "إضافة خامة جديدة للمخزون",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    // Material Name
                    OutlinedTextField(
                        value = matName,
                        onValueChange = { matName = it; formError = null },
                        label = { Text("اسم الخامة أو المادة") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("material_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Unit Type
                    OutlinedTextField(
                        value = matUnitType,
                        onValueChange = { matUnitType = it; formError = null },
                        label = { Text("نوع الوحدة (متر مربع، لوح، متر طولي، قطعة...)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("material_unit_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Price Per Unit
                        OutlinedTextField(
                            value = matPrice,
                            onValueChange = { matPrice = it; formError = null },
                            label = { Text("سعر الوحدة (ر.ي)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("material_price_input")
                        )

                        // Quantity in Stock
                        OutlinedTextField(
                            value = matQuantity,
                            onValueChange = { matQuantity = it; formError = null },
                            label = { Text("الكمية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("material_quantity_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // In Stock switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("متوفر في المخزون حالياً", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = matInStock,
                            onCheckedChange = { matInStock = it }
                        )
                    }

                    if (formError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = matPrice.toDoubleOrNull() ?: 0.0
                        val qty = matQuantity.toDoubleOrNull() ?: 0.0

                        if (matName.isBlank()) {
                            formError = "يرجى كتابة اسم الخامة"
                            return@Button
                        }
                        if (price <= 0) {
                            formError = "يرجى إدخال سعر وحدة صحيح"
                            return@Button
                        }

                        coroutineScope.launch {
                            try {
                                if (isEditing && editingMaterial != null) {
                                    val updated = editingMaterial!!.copy(
                                        name = matName.trim(),
                                        unitType = matUnitType.trim().ifBlank { "متر مربع" },
                                        pricePerUnit = price,
                                        quantity = qty,
                                        currency = matCurrency,
                                        inStock = matInStock
                                    )
                                    materialRepository.updateMaterial(updated)
                                    snackbarHostState.showSnackbar("تم التعديل بنجاح ✏️")
                                    Toast.makeText(context, "تم تعديل بيانات الخامة بنجاح", Toast.LENGTH_SHORT).show()
                                } else {
                                    val newMat = MaterialItem(
                                        name = matName.trim(),
                                        unitType = matUnitType.trim().ifBlank { "متر مربع" },
                                        pricePerUnit = price,
                                        quantity = qty,
                                        currency = matCurrency,
                                        inStock = matInStock
                                    )
                                    materialRepository.addMaterial(newMat)
                                    snackbarHostState.showSnackbar("تمت الإضافة بنجاح 📦")
                                    Toast.makeText(context, "تمت إضافة الخامة إلى المخزون بنجاح", Toast.LENGTH_SHORT).show()
                                }
                                showAddDialog = false
                                editingMaterial = null
                                matName = ""
                                matPrice = ""
                                matQuantity = "100"
                                formError = null
                            } catch (e: Exception) {
                                formError = "حدث خطأ: ${e.message}"
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("save_material_button")
                ) {
                    Text(if (isEditing) "حفظ التعديل" else "إضافة الخامة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAddDialog = false
                        editingMaterial = null
                        formError = null
                    },
                    shape = RoundedCornerShape(12.dp)
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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد حذف الخامة", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("هل أنت متأكد من حذف هذه الخامة من قائمة المخزون وحاسبة الأسعار؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = deletingMaterialId
                        if (id != null) {
                            coroutineScope.launch {
                                try {
                                    materialRepository.deleteMaterial(id)
                                    deletingMaterialId = null
                                    snackbarHostState.showSnackbar("تم الحذف بنجاح 🗑️")
                                    Toast.makeText(context, "تم حذف الخامة من المخزون بنجاح", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "حدث خطأ أثناء الحذف: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_material_button")
                ) {
                    Text("حذف نهائي", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deletingMaterialId = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    matName = ""
                    matUnitType = "متر مربع"
                    matPrice = ""
                    matQuantity = "100"
                    matCurrency = "YER"
                    matInStock = true
                    formError = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .testTag("add_material_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة خامة جديدة")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة خامة", fontWeight = FontWeight.Bold)
                }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إدارة الخامات والأسعار 📦",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "التحكم بأسعار الخامات، المخزون، والكميات المعتمدة للإنتاج",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("البحث في الخامات والمواد...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("materials_search_input")
                )
            }

            if (filteredMaterials.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("لا توجد خامات مطابقة", fontWeight = FontWeight.Bold)
                            Text("اضغط على زر (إضافة خامة) بالأسفل لإضافة مادة جديدة للمخزون", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            items(filteredMaterials, key = { it.id }) { material ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("material_card_${material.id}")
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
                                        text = if (material.inStock) "متوفر (${material.quantity.toInt()} ${material.unitType})" else "نفذ المخزون",
                                        color = if (material.inStock) Color(0xFF065F46) else Color(0xFFB91C1C),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
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
                                    matQuantity = material.quantity.toInt().toString()
                                    matCurrency = material.currency
                                    matInStock = material.inStock
                                    formError = null
                                },
                                modifier = Modifier.testTag("edit_material_${material.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(
                                onClick = { deletingMaterialId = material.id },
                                modifier = Modifier.testTag("delete_material_${material.id}")
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
