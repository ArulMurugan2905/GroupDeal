package com.example.core.model

enum class UserRole(val displayName: String) {
    CONSUMER("Consumer / Buyer"),
    SUPPLIER("Verified Supplier"),
    ADMIN("Platform Admin")
}

enum class TrustTier(val label: String, val minScore: Int, val badgeColorHex: Long) {
    BRONZE("Bronze Member", 0, 0xFFCD7F32),
    SILVER("Silver Verified", 400, 0xFF94A3B8),
    GOLD("Gold Trusted", 700, 0xFFF59E0B),
    PLATINUM("Platinum VIP", 900, 0xFF06B6D4);

    companion object {
        fun fromScore(score: Int): TrustTier {
            return when {
                score >= 900 -> PLATINUM
                score >= 700 -> GOLD
                score >= 400 -> SILVER
                else -> BRONZE
            }
        }
    }
}

data class Locality(
    val pincode: String,
    val localityName: String,
    val city: String,
    val state: String,
    val activeGroupsCount: Int = 0,
    val activeDemandsCount: Int = 0
) {
    val fullDisplayName: String
        get() = "$localityName, $city ($pincode)"

    val shortDisplayName: String
        get() = "$localityName, $pincode"
}

data class TrustProfile(
    val score: Int = 650,
    val completedPurchases: Int = 8,
    val successfulGroupsInitiated: Int = 3,
    val neighborhoodReferrals: Int = 5,
    val cancellationRatePercent: Double = 0.0,
    val promptPaymentRatePercent: Double = 100.0,
    val trustBadges: List<String> = listOf("Verified Phone", "Prompt Payer", "Locality Anchor")
) {
    val tier: TrustTier
        get() = TrustTier.fromScore(score)
}

data class UserModel(
    val id: String,
    val firebaseUid: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: UserRole = UserRole.CONSUMER,
    val locality: Locality? = null,
    val trustProfile: TrustProfile = TrustProfile(),
    val isOnboarded: Boolean = true,
    val joinedAtMillis: Long = System.currentTimeMillis()
)
