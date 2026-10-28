package com.example.core.engine

import com.example.core.model.PricingTier

object PricingEngine {

    /**
     * Determines current unit price strictly based on active tier matching current member count.
     * If member count is below first tier, falls back to baseUnitPrice.
     */
    fun calculateUnitPrice(
        baseUnitPrice: Double,
        tiers: List<PricingTier>,
        currentMembers: Int
    ): Double {
        if (tiers.isEmpty() || currentMembers <= 0) return baseUnitPrice

        val activeTier = tiers
            .sortedByDescending { it.minMembers }
            .firstOrNull { currentMembers >= it.minMembers }

        return activeTier?.unitPrice ?: baseUnitPrice
    }

    /**
     * Finds current tier index in the ordered tiers list.
     */
    fun calculateTierIndex(tiers: List<PricingTier>, currentMembers: Int): Int {
        val sorted = tiers.sortedBy { it.minMembers }
        var activeIndex = -1
        for (i in sorted.indices) {
            if (currentMembers >= sorted[i].minMembers) {
                activeIndex = i
            }
        }
        return activeIndex
    }

    /**
     * Validates that pricing tiers are strictly monotonic:
     * - Thresholds strictly increase (minMembers)
     * - Unit prices strictly decrease (unitPrice)
     */
    fun validateTiers(tiers: List<PricingTier>): Boolean {
        if (tiers.isEmpty()) return false
        for (i in 0 until tiers.size - 1) {
            if (tiers[i].minMembers >= tiers[i + 1].minMembers) return false
            if (tiers[i].unitPrice <= tiers[i + 1].unitPrice) return false
        }
        return true
    }

    /**
     * Computes the members remaining to unlock the next cheaper tier.
     */
    fun membersToNextTier(tiers: List<PricingTier>, currentMembers: Int): Int? {
        val sorted = tiers.sortedBy { it.minMembers }
        val nextTier = sorted.firstOrNull { it.minMembers > currentMembers }
        return nextTier?.let { it.minMembers - currentMembers }
    }
}
