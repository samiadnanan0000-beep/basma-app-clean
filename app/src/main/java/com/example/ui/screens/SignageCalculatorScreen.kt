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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.viewmodel.SignageCalculatorViewModel

@Composable
fun SignageCalculatorScreen(
    calculatorViewModel: SignageCalculatorViewModel
) {
    val width by calculatorViewModel.widthInput.collectAsState()
    val height by calculatorViewModel.heightInput.collectAsState()
    val profitPercent by calculatorViewModel.profitMarginPercent.collectAsState()
    val selectedCurrency by calculatorViewModel.selectedCurrency.collectAsState()
    val result by calculatorViewModel.result.collectAsState()

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
                text = "حاسبة إنتاج اللوحات والكلادنج 📐",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "حساب دقيق لمساحات الكلادنج، شاسيهات الحديد، وحدات الليد والمحولات مع هامش الربح",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Currency Toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("YER" to "ريال يمني (YER)", "SAR" to "ريال سعودي (SAR)", "USD" to "دولار أمريكي (USD)").forEach { (code, label) ->
                    val isSelected = selectedCurrency == code
                    Button(
                        onClick = { calculatorViewModel.selectedCurrency.value = code },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(code, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Dimensions Inputs
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "أبعاد اللوحة (متر)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = width,
                            onValueChange = {
                                calculatorViewModel.widthInput.value = it
                                calculatorViewModel.calculate()
                            },
                            label = { Text("العرض (م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calc_width_input")
                        )
                        OutlinedTextField(
                            value = height,
                            onValueChange = {
                                calculatorViewModel.heightInput.value = it
                                calculatorViewModel.calculate()
                            },
                            label = { Text("الارتفاع (م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calc_height_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Profit Margin Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "هامش الربح المطلوب",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${profitPercent.toInt()}%",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Slider(
                        value = profitPercent.toFloat(),
                        onValueChange = {
                            calculatorViewModel.profitMarginPercent.value = it.toDouble()
                            calculatorViewModel.calculate()
                        },
                        valueRange = 10f..60f,
                        steps = 9
                    )
                }
            }
        }

        // Production Material Requirements Cards
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تفصيل استهلاك المواد الخام",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MaterialStatCard(
                            title = "المساحة الإجمالية",
                            value = "${"%.2f".format(result.area)} م²",
                            icon = Icons.Default.Layers,
                            modifier = Modifier.weight(1f)
                        )
                        MaterialStatCard(
                            title = "ألواح الكلادنج",
                            value = "${result.claddingSheetsCount} ألواح",
                            icon = Icons.Default.Layers,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MaterialStatCard(
                            title = "شاسيه الحديد",
                            value = "${result.ironTubesMeters.toInt()} متر طولي",
                            icon = Icons.Default.Engineering,
                            modifier = Modifier.weight(1f)
                        )
                        MaterialStatCard(
                            title = "حقن LED كوري",
                            value = "${result.ledModulesCount} وحدة",
                            icon = Icons.Default.ElectricBolt,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    MaterialStatCard(
                        title = "محولات الطاقة (ترانس 400W)",
                        value = "${result.transformersCount} محول أصلي",
                        icon = Icons.Default.ElectricBolt,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Financial & Pricing Summary
        item {
            val displayPrice = when (selectedCurrency) {
                "SAR" -> "${"%.1f".format(result.priceInSar)} SAR"
                "USD" -> "${"%.1f".format(result.priceInUsd)} USD"
                else -> "${result.totalCustomerPrice.toInt()} YER"
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "السعر المقترح للعميل مع التركيب والربح",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = displayPrice,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cost breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تكلفة المواد الخام:", style = MaterialTheme.typography.bodySmall)
                        Text("${result.baseMaterialsCost.toInt()} YER", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("أجور الورشة والتركيب والرافعة:", style = MaterialTheme.typography.bodySmall)
                        Text("${result.laborAndInstallationCost.toInt()} YER", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("صافي الربح التقديري (${profitPercent.toInt()}%):", style = MaterialTheme.typography.bodySmall)
                        Text("${result.profitAmount.toInt()} YER", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
