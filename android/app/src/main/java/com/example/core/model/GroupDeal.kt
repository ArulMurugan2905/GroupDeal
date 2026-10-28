package com.example.core.model

enum class GroupState(val label: String, val badgeColorHex: Long) {
    OPEN("Open for Joining", 0xFF0F9D58),
    FILLING("Filling Fast 🔥", 0xFFE65100),
    TARGET_REACHED("Target Reached 🎉", 0xFF2E7D32),
    BIDDING("Supplier Bidding Active ⚡", 0xFF6D28D9),
    SUPPLIER_SELECTED("Supplier Selected", 0xFF0284C7),
    PAYMENT_PENDING("Payment Window Open", 0xFFD97706),
    PROCESSING("Order In Batching", 0xFF475569),
    DELIVERED("Delivered to Locality", 0xFF15803D),
    EXPIRED("Expired", 0xFF64748B),
    CANCELLED("Cancelled", 0xFFDC2626)
}

data class PricingTier(
    val minMembers: Int,
    val unitPrice: Double,
    val discountPercent: Int,
    val label: String
)

data class SupplierBid(
    val id: String,
    val supplierId: String,
    val supplierName: String,
    val supplierScore: Double,
    val bidAmountPerUnit: Double,
    val promisedDeliveryDays: Int,
    val isSelected: Boolean = false,
    val verifiedBadge: Boolean = true
)

data class GroupDeal(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val imageUrl: String,
    val originalMarketPrice: Double,
    val baseUnitPrice: Double,
    val currentUnitPrice: Double,
    val targetMembers: Int,
    val currentMembers: Int,
    val currentTierIndex: Int,
    val tiers: List<PricingTier>,
    val state: GroupState,
    val locality: Locality,
    val expiresAtMillis: Long,
    val joinedUserIds: List<String> = emptyList(),
    val winningBid: SupplierBid? = null,
    val unitsReserved: Int = 0,
    val brand: String = "",
    val specifications: Map<String, String> = emptyMap()
) {
    val activeTier: PricingTier?
        get() = if (currentTierIndex >= 0 && currentTierIndex < tiers.size) tiers[currentTierIndex] else null

    val nextTier: PricingTier?
        get() = if (currentTierIndex + 1 < tiers.size) tiers[currentTierIndex + 1] else null

    val membersToNextTier: Int
        get() = (nextTier?.minMembers ?: targetMembers) - currentMembers

    val progressToTarget: Float
        get() = (currentMembers.toFloat() / targetMembers.toFloat()).coerceIn(0f, 1f)

    val currentSavingsPercent: Int
        get() = if (originalMarketPrice > 0) {
            (((originalMarketPrice - currentUnitPrice) / originalMarketPrice) * 100).toInt()
        } else 0

    val totalCommunitySavings: Double
        get() = (originalMarketPrice - currentUnitPrice) * currentMembers
}

data class GroupRequest(
    val id: String,
    val productName: String,
    val category: String,
    val targetPrice: Double,
    val referenceMarketPrice: Double,
    val currentVotes: Int,
    val thresholdVotes: Int = 20,
    val requesterName: String,
    val locality: Locality,
    val votedUserIds: List<String> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = (currentVotes.toFloat() / thresholdVotes.toFloat()).coerceIn(0f, 1f)
}
