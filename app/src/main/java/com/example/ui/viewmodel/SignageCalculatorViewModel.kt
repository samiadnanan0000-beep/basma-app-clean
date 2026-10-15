package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MaterialItem
import com.example.data.repository.MaterialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class CalculatorResult(
    val area: Double = 0.0,
    val perimeter: Double = 0.0,
    val claddingSheetsCount: Int = 0,
    val ironTubesMeters: Double = 0.0,
    val ledModulesCount: Int = 0,
    val transformersCount: Int = 0,
    val baseMaterialsCost: Double = 0.0,
    val laborAndInstallationCost: Double = 0.0,
    val profitAmount: Double = 0.0,
    val totalCustomerPrice: Double = 0.0,
    val priceInSar: Double = 0.0,
    val priceInUsd: Double = 0.0
)

class SignageCalculatorViewModel(private val materialRepository: MaterialRepository) : ViewModel() {

    private val _materials = MutableStateFlow<List<MaterialItem>>(emptyList())
    val materials: StateFlow<List<MaterialItem>> = _materials.asStateFlow()

    // Form inputs
    val widthInput = MutableStateFlow("4.0")
    val heightInput = MutableStateFlow("1.5")
    val signType = MutableStateFlow("كلادنج وحروف بارزة 3D")
    val profitMarginPercent = MutableStateFlow(30.0) // 30% default
    val selectedCurrency = MutableStateFlow("YER") // "YER", "SAR", "USD"

    // Exchange rates against YER
    val sarRate = 142.0 // 1 SAR = 142 YER
    val usdRate = 535.0 // 1 USD = 535 YER

    private val _result = MutableStateFlow(CalculatorResult())
    val result: StateFlow<CalculatorResult> = _result.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                materialRepository.observeMaterials().collect { list ->
                    _materials.value = list
                    calculate()
                }
            }.onFailure { e ->
                android.util.Log.e("SignageCalcVM", "observeMaterials safe error: ${e.message}")
            }
        }
    }

    fun calculate() {
        val w = widthInput.value.toDoubleOrNull() ?: 0.0
        val h = heightInput.value.toDoubleOrNull() ?: 0.0
        if (w <= 0.0 || h <= 0.0) {
            _result.value = CalculatorResult()
            return
        }

        val area = w * h
        val perimeter = (w + h) * 2

        // Cladding: standard sheet is 1.22m x 2.44m = 2.9768 m2 + 10% cutting waste
        val sheetArea = 2.97
        val rawSheets = (area * 1.10) / sheetArea
        val claddingSheets = ceil(rawSheets).toInt().coerceAtLeast(1)

        // Iron structure: perimeter + internal vertical supports every 60cm + horizontal supports
        val verticalPipes = ceil(w / 0.6) * h
        val horizontalPipes = 3 * w
        val ironMeters = perimeter + verticalPipes + horizontalPipes

        // LEDs: roughly 45 modules per m2 for 3D channel letters
        val ledCount = ceil(area * 45).toInt()
        // Transformer: 400W transformer supports ~250 modules (1.5W each * 0.8 safety factor)
        val transformers = ceil(ledCount / 220.0).toInt().coerceAtLeast(1)

        // Find unit prices from loaded materials or sensible defaults
        val claddingPrice = _materials.value.find { it.name.contains("كلادنج") }?.pricePerUnit ?: 22000.0
        val ironPrice = _materials.value.find { it.name.contains("حديد") }?.pricePerUnit ?: 3200.0
        val ledPrice = _materials.value.find { it.name.contains("ليد") }?.pricePerUnit ?: 450.0
        val transformerPrice = _materials.value.find { it.name.contains("محول") }?.pricePerUnit ?: 19500.0
        val acrylicPrice = _materials.value.find { it.name.contains("اكريليك") }?.pricePerUnit ?: 18500.0

        val claddingCost = area * claddingPrice
        val ironCost = ironMeters * ironPrice
        val ledCost = ledCount * ledPrice
        val transformerCost = transformers * transformerPrice
        val lettersCost = area * 0.4 * acrylicPrice // estimated 40% area of letters

        val baseMaterialsCost = claddingCost + ironCost + ledCost + transformerCost + lettersCost
        // Labor, cutting, framing & crane installation: estimated 25% of materials
        val laborCost = baseMaterialsCost * 0.25

        val totalCost = baseMaterialsCost + laborCost
        val margin = profitMarginPercent.value / 100.0
        val profit = totalCost * margin
        val customerPriceYer = totalCost + profit

        val priceSar = customerPriceYer / sarRate
        val priceUsd = customerPriceYer / usdRate

        _result.value = CalculatorResult(
            area = area,
            perimeter = perimeter,
            claddingSheetsCount = claddingSheets,
            ironTubesMeters = ironMeters,
            ledModulesCount = ledCount,
            transformersCount = transformers,
            baseMaterialsCost = baseMaterialsCost,
            laborAndInstallationCost = laborCost,
            profitAmount = profit,
            totalCustomerPrice = customerPriceYer,
            priceInSar = priceSar,
            priceInUsd = priceUsd
        )
    }
}
