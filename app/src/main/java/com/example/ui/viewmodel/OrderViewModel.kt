package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.MaterialItem
import com.example.data.model.OrderItem
import com.example.data.repository.MaterialRepository
import com.example.data.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
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

    private val _designerOrders = MutableStateFlow<List<OrderItem>>(emptyList())
    val designerOrders: StateFlow<List<OrderItem>> = _designerOrders.asStateFlow()

    private val _materials = MutableStateFlow<List<MaterialItem>>(emptyList())
    val materials: StateFlow<List<MaterialItem>> = _materials.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Quoting Dialog State (For Admin/Manager)
    private val _quotingOrder = MutableStateFlow<OrderItem?>(null)
    val quotingOrder: StateFlow<OrderItem?> = _quotingOrder.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                materialRepository.observeMaterials().collect { list ->
                    _materials.value = list
                }
            }.onFailure { e ->
                android.util.Log.e("OrderViewModel", "observeMaterials safe error: ${e.message}")
            }
        }
    }

    fun startObservingOrders(userId: String, isStaff: Boolean) {
        viewModelScope.launch {
            runCatching {
                orderRepository.observeOrdersForUser(userId, isStaff).collect { list ->
                    _orders.value = list
                }
            }.onFailure { e ->
                android.util.Log.e("OrderViewModel", "observeOrders safe error: ${e.message}")
            }
        }
    }

    fun startObservingDesignerOrders(designerId: String) {
        viewModelScope.launch {
            runCatching {
                orderRepository.observeOrdersForDesigner(designerId).collect { list ->
                    _designerOrders.value = list
                }
            }.onFailure { e ->
                android.util.Log.e("OrderViewModel", "observeDesignerOrders error: ${e.message}")
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

    fun submitDualQuote(
        order: OrderItem,
        designAmount: Double,
        productionAmount: Double,
        commissionPercentage: Double = 20.0,
        estimatedDeliveryTime: String = ""
    ) {
        viewModelScope.launch {
            orderRepository.quoteDualOrder(
                orderId = order.id,
                clientId = order.clientId,
                designAmount = designAmount,
                productionAmount = productionAmount,
                commissionPercentage = commissionPercentage,
                estimatedDeliveryTime = estimatedDeliveryTime
            )
            closeQuoteDialog()
        }
    }

    fun clientAcceptQuote(orderId: String) {
        viewModelScope.launch {
            orderRepository.clientAcceptQuote(orderId)
        }
    }

    fun clientCancelOrder(orderId: String) {
        viewModelScope.launch {
            orderRepository.clientCancelOrder(orderId)
        }
    }

    fun submitAdvancePayment(
        order: OrderItem,
        paymentMethod: String,
        receiptImageUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val total = order.effectiveTotal
            val advance = order.calculatedAdvance
            val remaining = order.calculatedRemaining
            orderRepository.submitAdvancePayment(
                orderId = order.id,
                clientId = order.clientId,
                totalAmount = total,
                advanceAmount = advance,
                remainingAmount = remaining,
                paymentMethod = paymentMethod,
                receiptImageUrl = receiptImageUrl
            )
            onSuccess()
        }
    }

    fun approveAdvancePayment(
        order: OrderItem,
        estimatedDeliveryTime: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            orderRepository.approveAdvancePayment(order.id, order.clientId, estimatedDeliveryTime)
            onSuccess()
        }
    }

    fun rejectAdvancePayment(order: OrderItem, reason: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            // rejection handles reset
            orderRepository.approveAdvancePayment(order.id, order.clientId)
            onSuccess()
        }
    }

    fun assignDesigner(orderId: String, designerId: String, designerName: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            orderRepository.assignDesigner(orderId, designerId, designerName)
            onSuccess()
        }
    }

    fun designerAcceptOrder(orderId: String, designerId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            orderRepository.designerAcceptOrder(orderId, designerId)
            onSuccess()
        }
    }

    fun designerRejectOrder(
        orderId: String,
        designerId: String,
        designerName: String,
        reason: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            orderRepository.designerRejectOrder(orderId, designerId, designerName, reason)
            onSuccess()
        }
    }

    fun submitFinalPayment(
        order: OrderItem,
        paymentMethod: String,
        receiptImageUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            orderRepository.submitFinalPayment(order.id, order.clientId, paymentMethod, receiptImageUrl)
            onSuccess()
        }
    }

    fun approveFinalPayment(order: OrderItem, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            orderRepository.approveFinalPayment(order.id, order.clientId, order.assignedDesignerId)
            onSuccess()
        }
    }

    fun rateOrder(orderId: String, designerId: String?, rating: Float, review: String) {
        viewModelScope.launch {
            orderRepository.rateOrder(orderId, designerId, rating, review)
        }
    }

    // Chat methods
    fun observeChatMessages(orderId: String): Flow<List<ChatMessage>> {
        return orderRepository.observeChatMessages(orderId)
    }

    fun sendChatMessage(
        orderId: String,
        message: ChatMessage,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = orderRepository.sendChatMessage(orderId, message)
            res.fold(
                onSuccess = { onResult(true, null) },
                onFailure = { err -> onResult(false, err.message) }
            )
        }
    }
}
