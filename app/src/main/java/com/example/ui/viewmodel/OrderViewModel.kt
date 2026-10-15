package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MaterialItem
import com.example.data.model.OrderItem
import com.example.data.repository.MaterialRepository
import com.example.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderViewModel(
    private val orderRepository: OrderRepository,
    private val materialRepository: MaterialRepository
) : ViewModel() {

    private val _orders = MutableStateFlow<List<OrderItem>>(emptyList())
    val orders: StateFlow<List<OrderItem>> = _orders.asStateFlow()

    private val _materials = MutableStateFlow<List<MaterialItem>>(emptyList())
    val materials: StateFlow<List<MaterialItem>> = _materials.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Quoting Dialog State (For Designer)
    private val _quotingOrder = MutableStateFlow<OrderItem?>(null)
    val quotingOrder: StateFlow<OrderItem?> = _quotingOrder.asStateFlow()

    init {
        viewModelScope.launch {
            materialRepository.observeMaterials().collect { list ->
                _materials.value = list
            }
        }
    }

    fun startObservingOrders(userId: String, isStaff: Boolean) {
        viewModelScope.launch {
            orderRepository.observeOrdersForUser(userId, isStaff).collect { list ->
                _orders.value = list
            }
        }
    }

    fun submitNewOrder(order: OrderItem, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            val id = orderRepository.submitOrder(order)
            _isSubmitting.value = false
            if (id != null) {
                _submitSuccess.value = true
                onSuccess()
            } else {
                _errorMessage.value = "تعذر إرسال الطلب، يرجى التحقق من الاتصال بالإنترنت"
            }
        }
    }

    fun openQuoteDialog(order: OrderItem) {
        _quotingOrder.value = order
    }

    fun closeQuoteDialog() {
        _quotingOrder.value = null
    }

    fun submitQuote(price: Double, currency: String, turnaround: String, proofUrl: String? = null) {
        val order = _quotingOrder.value ?: return
        viewModelScope.launch {
            orderRepository.quoteOrder(order.id, order.clientId, price, currency, turnaround, proofUrl)
            closeQuoteDialog()
        }
    }

    fun updateStatus(order: OrderItem, newStatus: String) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(order.id, order.clientId, newStatus)
        }
    }

    fun uploadProof(order: OrderItem, proofUrl: String) {
        viewModelScope.launch {
            orderRepository.uploadProof(order.id, order.clientId, proofUrl)
        }
    }

    fun approveProof(order: OrderItem) {
        viewModelScope.launch {
            orderRepository.clientApproveProof(order.id)
        }
    }

    fun rejectProof(order: OrderItem, reason: String) {
        viewModelScope.launch {
            orderRepository.clientRejectProof(order.id, reason)
        }
    }
}
