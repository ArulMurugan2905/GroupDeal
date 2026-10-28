package com.example.core.model

enum class OrderStatus(val displayName: String, val badgeColorHex: Long) {
    CONFIRMED("Order Confirmed", 0xFF0F9D58),
    SUPPLIER_ASSIGNED("Supplier Sourced", 0xFF0284C7),
    DISPATCHED("Dispatched in Bulk", 0xFF6D28D9),
    OUT_FOR_DELIVERY("Out for Locality Delivery", 0xFFD97706),
    DELIVERED("Delivered", 0xFF15803D),
    CANCELLED("Refunded / Cancelled", 0xFFDC2626)
}

data class TrackingStep(
    val title: String,
    val description: String,
    val timestampText: String,
    val isCompleted: Boolean
)

data class Order(
    val id: String,
    val orderNumber: String,
    val groupDealId: String,
    val productTitle: String,
    val productImageUrl: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalAmount: Double,
    val originalMarketTotal: Double,
    val savedAmount: Double,
    val status: OrderStatus,
    val orderDateMillis: Long,
    val deliveryLocality: Locality,
    val deliveryAddressLine: String,
    val winningSupplierName: String = "Apex Direct Wholesale",
    val trackingSteps: List<TrackingStep> = emptyList()
)
