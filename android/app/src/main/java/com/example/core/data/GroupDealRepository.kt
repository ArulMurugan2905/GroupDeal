package com.example.core.data

import com.example.core.engine.PricingEngine
import com.example.core.model.GroupDeal
import com.example.core.model.GroupRequest
import com.example.core.model.GroupState
import com.example.core.model.Locality
import com.example.core.model.Order
import com.example.core.model.OrderStatus
import com.example.core.model.PricingTier
import com.example.core.model.SupplierBid
import com.example.core.model.TrackingStep
import com.example.core.model.TrustProfile
import com.example.core.model.TrustTier
import com.example.core.model.UserModel
import com.example.core.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object Unauthenticated : AuthUiState
    data class OnboardingRequired(val firebaseUid: String, val email: String) : AuthUiState
    data class Authenticated(val user: UserModel) : AuthUiState
}

data class LocalityEvent(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val localityName: String,
    val timestamp: Long = System.currentTimeMillis()
)

object GroupDealRepository {

    private val scope = CoroutineScope(Dispatchers.Default)

    // Current Auth State
    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    // Current Active Locality for Discovery
    private val _currentLocality = MutableStateFlow(LocalityService.POPULAR_LOCALITIES[0])
    val currentLocality: StateFlow<Locality> = _currentLocality.asStateFlow()

    // All Group Deals in Memory / Store
    private val _deals = MutableStateFlow<List<GroupDeal>>(emptyList())
    val deals: StateFlow<List<GroupDeal>> = _deals.asStateFlow()

    // Group Requests in Locality
    private val _requests = MutableStateFlow<List<GroupRequest>>(emptyList())
    val requests: StateFlow<List<GroupRequest>> = _requests.asStateFlow()

    // User's Placed Orders
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    // Live Socket Simulation Feed
    private val _liveEvents = MutableSharedFlow<LocalityEvent>(extraBufferCapacity = 10)
    val liveEvents: SharedFlow<LocalityEvent> = _liveEvents.asSharedFlow()

    init {
        seedInitialDeals()
        seedInitialRequests()
        seedInitialOrders()
        // Initialize default authenticated user
        seedDefaultUser()
    }

    private fun seedDefaultUser() {
        val defaultLocality = LocalityService.POPULAR_LOCALITIES[0] // Koramangala
        val user = UserModel(
            id = "user_demo_01",
            firebaseUid = "fb_uid_koramangala_101",
            fullName = "Ananya Sharma",
            email = "ananya.sharma@example.com",
            phone = "+91 98765 43210",
            role = UserRole.CONSUMER,
            locality = defaultLocality,
            trustProfile = TrustProfile(
                score = 780,
                completedPurchases = 12,
                successfulGroupsInitiated = 4,
                neighborhoodReferrals = 9,
                cancellationRatePercent = 0.0,
                promptPaymentRatePercent = 100.0,
                trustBadges = listOf("Verified Resident", "Top Saver", "Prompt Payer", "Locality Anchor")
            ),
            isOnboarded = true
        )
        _authState.value = AuthUiState.Authenticated(user)
    }

    // --- Authentication & Session Management ---

    fun loginWithEmail(email: String, pass: String) {
        scope.launch {
            _authState.value = AuthUiState.Loading
            delay(400) // Simulated auth roundtrip
            if (email.contains("new", ignoreCase = true)) {
                _authState.value = AuthUiState.OnboardingRequired(
                    firebaseUid = "uid_${System.currentTimeMillis()}",
                    email = email
                )
            } else {
                val existing = (authState.value as? AuthUiState.Authenticated)?.user
                val locality = _currentLocality.value
                val user = existing?.copy(email = email) ?: UserModel(
                    id = "user_${System.currentTimeMillis()}",
                    firebaseUid = "uid_${System.currentTimeMillis()}",
                    fullName = "Ananya Sharma",
                    email = email,
                    phone = "+91 98765 43210",
                    role = UserRole.CONSUMER,
                    locality = locality,
                    isOnboarded = true
                )
                _authState.value = AuthUiState.Authenticated(user)
            }
        }
    }

    fun completeOnboarding(
        fullName: String,
        phone: String,
        locality: Locality,
        role: UserRole
    ) {
        val state = _authState.value
        val email = when (state) {
            is AuthUiState.OnboardingRequired -> state.email
            is AuthUiState.Authenticated -> state.user.email
            else -> "user@groupdeal.in"
        }
        val newUser = UserModel(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            firebaseUid = "uid_${System.currentTimeMillis()}",
            fullName = fullName.ifBlank { "GroupDeal Member" },
            email = email,
            phone = phone.ifBlank { "+91 98765 00000" },
            role = role,
            locality = locality,
            trustProfile = TrustProfile(score = 500),
            isOnboarded = true
        )
        _currentLocality.value = locality
        _authState.value = AuthUiState.Authenticated(newUser)
    }

    fun switchDemoRole(newRole: UserRole) {
        val current = (_authState.value as? AuthUiState.Authenticated)?.user ?: return
        _authState.value = AuthUiState.Authenticated(current.copy(role = newRole))
    }

    fun updateProfile(
        fullName: String,
        phone: String,
        locality: Locality
    ) {
        val current = (_authState.value as? AuthUiState.Authenticated)?.user ?: return
        val updated = current.copy(
            fullName = fullName,
            phone = phone,
            locality = locality
        )
        _currentLocality.value = locality
        _authState.value = AuthUiState.Authenticated(updated)
    }

    fun logout() {
        _authState.value = AuthUiState.Unauthenticated
    }

    fun setLocality(locality: Locality) {
        _currentLocality.value = locality
        // Update user profile locality if logged in
        val current = (_authState.value as? AuthUiState.Authenticated)?.user
        if (current != null) {
            _authState.value = AuthUiState.Authenticated(current.copy(locality = locality))
        }
        scope.launch {
            _liveEvents.emit(
                LocalityEvent(
                    message = "Switched browsing locality to ${locality.localityName}",
                    localityName = locality.localityName
                )
            )
        }
    }

    // --- Deals & Dynamic Pricing ---

    fun getDealById(dealId: String): GroupDeal? {
        return _deals.value.firstOrNull { it.id == dealId }
    }

    fun joinGroup(dealId: String) {
        val currentUser = (_authState.value as? AuthUiState.Authenticated)?.user ?: return
        val currentList = _deals.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == dealId }
        if (index == -1) return

        val deal = currentList[index]
        if (deal.joinedUserIds.contains(currentUser.id)) return

        val newMemberCount = deal.currentMembers + 1
        val newJoinedList = deal.joinedUserIds + currentUser.id
        val newTierIndex = PricingEngine.calculateTierIndex(deal.tiers, newMemberCount)
        val newPrice = PricingEngine.calculateUnitPrice(deal.baseUnitPrice, deal.tiers, newMemberCount)

        val newState = when {
            newMemberCount >= deal.targetMembers -> GroupState.TARGET_REACHED
            newMemberCount >= (deal.targetMembers * 0.6) -> GroupState.FILLING
            else -> GroupState.OPEN
        }

        val updatedDeal = deal.copy(
            currentMembers = newMemberCount,
            joinedUserIds = newJoinedList,
            currentTierIndex = newTierIndex,
            currentUnitPrice = newPrice,
            state = newState,
            unitsReserved = deal.unitsReserved + 1
        )
        currentList[index] = updatedDeal
        _deals.value = currentList

        // Emit live activity
        scope.launch {
            _liveEvents.emit(
                LocalityEvent(
                    message = "🎉 You joined '${deal.title}' in ${deal.locality.localityName}! Price is now ₹${newPrice.toInt()}",
                    localityName = deal.locality.localityName
                )
            )
        }
    }

    fun leaveGroup(dealId: String) {
        val currentUser = (_authState.value as? AuthUiState.Authenticated)?.user ?: return
        val currentList = _deals.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == dealId }
        if (index == -1) return

        val deal = currentList[index]
        if (!deal.joinedUserIds.contains(currentUser.id)) return

        val newMemberCount = (deal.currentMembers - 1).coerceAtLeast(0)
        val newJoinedList = deal.joinedUserIds - currentUser.id
        val newTierIndex = PricingEngine.calculateTierIndex(deal.tiers, newMemberCount)
        val newPrice = PricingEngine.calculateUnitPrice(deal.baseUnitPrice, deal.tiers, newMemberCount)

        currentList[index] = deal.copy(
            currentMembers = newMemberCount,
            joinedUserIds = newJoinedList,
            currentTierIndex = newTierIndex,
            currentUnitPrice = newPrice,
            unitsReserved = (deal.unitsReserved - 1).coerceAtLeast(0)
        )
        _deals.value = currentList
    }

    fun placeGroupOrder(deal: GroupDeal, quantity: Int = 1) {
        val user = (_authState.value as? AuthUiState.Authenticated)?.user ?: return
        val locality = deal.locality
        val total = deal.currentUnitPrice * quantity
        val originalTotal = deal.originalMarketPrice * quantity
        val saved = (originalTotal - total).coerceAtLeast(0.0)

        val newOrder = Order(
            id = "ord_${UUID.randomUUID().toString().take(8)}",
            orderNumber = "GD-${(100000..999999).random()}",
            groupDealId = deal.id,
            productTitle = deal.title,
            productImageUrl = deal.imageUrl,
            quantity = quantity,
            unitPrice = deal.currentUnitPrice,
            totalAmount = total,
            originalMarketTotal = originalTotal,
            savedAmount = saved,
            status = OrderStatus.CONFIRMED,
            orderDateMillis = System.currentTimeMillis(),
            deliveryLocality = locality,
            deliveryAddressLine = "Flat 402, Green Glen Layout, ${locality.localityName}, ${locality.city}",
            winningSupplierName = deal.winningBid?.supplierName ?: "Apex Wholesale Direct",
            trackingSteps = listOf(
                TrackingStep("Group Target Reached", "All ${deal.targetMembers} neighborhood buyers confirmed", "Just now", true),
                TrackingStep("Order Confirmed", "Payment secured and batch sent to supplier", "Just now", true),
                TrackingStep("Supplier Dispatched", "Dispatched from regional warehouse hub", "Expected Tomorrow", false),
                TrackingStep("Locality Delivery", "Arriving at ${locality.localityName} distribution point", "In 2 days", false)
            )
        )
        _orders.value = listOf(newOrder) + _orders.value
    }

    fun voteForRequest(requestId: String) {
        val currentUser = (_authState.value as? AuthUiState.Authenticated)?.user ?: return
        val list = _requests.value.toMutableList()
        val index = list.indexOfFirst { it.id == requestId }
        if (index == -1) return

        val req = list[index]
        if (req.votedUserIds.contains(currentUser.id)) return

        val updated = req.copy(
            currentVotes = req.currentVotes + 1,
            votedUserIds = req.votedUserIds + currentUser.id
        )
        list[index] = updated
        _requests.value = list

        scope.launch {
            _liveEvents.emit(
                LocalityEvent(
                    message = "👍 Voted for '${req.productName}'! (${updated.currentVotes}/${req.thresholdVotes} neighbors interested)",
                    localityName = req.locality.localityName
                )
            )
        }
    }

    fun createRequest(
        productName: String,
        category: String,
        targetPrice: Double,
        referenceMarketPrice: Double
    ) {
        val currentUser = (_authState.value as? AuthUiState.Authenticated)?.user ?: return
        val locality = _currentLocality.value
        val newReq = GroupRequest(
            id = "req_${UUID.randomUUID().toString().take(8)}",
            productName = productName,
            category = category,
            targetPrice = targetPrice,
            referenceMarketPrice = referenceMarketPrice,
            currentVotes = 1,
            thresholdVotes = 15,
            requesterName = currentUser.fullName,
            locality = locality,
            votedUserIds = listOf(currentUser.id)
        )
        _requests.value = listOf(newReq) + _requests.value

        scope.launch {
            _liveEvents.emit(
                LocalityEvent(
                    message = "🚀 New demand requested: '$productName' in ${locality.localityName}!",
                    localityName = locality.localityName
                )
            )
        }
    }

    // --- Seeding Methods ---

    private fun seedInitialDeals() {
        val koramangala = LocalityService.POPULAR_LOCALITIES[0]

        val deal1Tiers = listOf(
            PricingTier(minMembers = 5, unitPrice = 1099.0, discountPercent = 21, label = "Tier 1: 5 Buyers"),
            PricingTier(minMembers = 15, unitPrice = 949.0, discountPercent = 32, label = "Tier 2: 15 Buyers"),
            PricingTier(minMembers = 30, unitPrice = 799.0, discountPercent = 43, label = "Tier 3 (Max): 30 Buyers")
        )

        val deal2Tiers = listOf(
            PricingTier(minMembers = 10, unitPrice = 1850.0, discountPercent = 26, label = "Tier 1: 10 Boxes"),
            PricingTier(minMembers = 25, unitPrice = 1599.0, discountPercent = 36, label = "Tier 2: 25 Boxes"),
            PricingTier(minMembers = 50, unitPrice = 1349.0, discountPercent = 46, label = "Tier 3: 50 Boxes")
        )

        val deal3Tiers = listOf(
            PricingTier(minMembers = 8, unitPrice = 449.0, discountPercent = 25, label = "Tier 1: 8 Tins"),
            PricingTier(minMembers = 20, unitPrice = 379.0, discountPercent = 37, label = "Tier 2: 20 Tins"),
            PricingTier(minMembers = 40, unitPrice = 319.0, discountPercent = 47, label = "Tier 3: 40 Tins")
        )

        val deal4Tiers = listOf(
            PricingTier(minMembers = 5, unitPrice = 3200.0, discountPercent = 20, label = "Tier 1: 5 Sets"),
            PricingTier(minMembers = 12, unitPrice = 2750.0, discountPercent = 31, label = "Tier 2: 12 Sets"),
            PricingTier(minMembers = 20, unitPrice = 2299.0, discountPercent = 42, label = "Tier 3: 20 Sets")
        )

        _deals.value = listOf(
            GroupDeal(
                id = "deal_01",
                title = "Royal Organic Royal A2 Gir Cow Ghee (1L Pet Jar)",
                description = "100% pure traditional Bilona method curd churned A2 desi cow ghee sourced directly from Gujarat organic dairy co-ops.",
                category = "Groceries & Farm",
                imageUrl = "https://images.unsplash.com/photo-1589927986089-35812388d1f4?auto=format&fit=crop&w=600&q=80",
                originalMarketPrice = 1400.0,
                baseUnitPrice = 1250.0,
                currentUnitPrice = 949.0,
                targetMembers = 30,
                currentMembers = 18,
                currentTierIndex = 1,
                tiers = deal1Tiers,
                state = GroupState.FILLING,
                locality = koramangala,
                expiresAtMillis = System.currentTimeMillis() + 86400000L * 2,
                joinedUserIds = listOf("user_demo_01"),
                winningBid = SupplierBid(
                    id = "bid_101",
                    supplierId = "sup_01",
                    supplierName = "Vedic Farms Co-op",
                    supplierScore = 4.9,
                    bidAmountPerUnit = 949.0,
                    promisedDeliveryDays = 2,
                    isSelected = true
                ),
                unitsReserved = 18,
                brand = "Vedic Organics",
                specifications = mapOf(
                    "Volume" to "1 Litre",
                    "Method" to "Traditional Bilona",
                    "Certification" to "FSSAI & Jaivik Bharat Certified"
                )
            ),
            GroupDeal(
                id = "deal_02",
                title = "Alphonso Mangoes Devgad Grade-A (Dozen Box, 3.2kg)",
                description = "GI-tagged naturally ripened handpicked Devgad Alphonso mangoes direct from Konkan orchards. Zero carbide chemicals.",
                category = "Fresh Produce",
                imageUrl = "https://images.unsplash.com/photo-1553279768-865429fa0078?auto=format&fit=crop&w=600&q=80",
                originalMarketPrice = 2500.0,
                baseUnitPrice = 2100.0,
                currentUnitPrice = 1599.0,
                targetMembers = 50,
                currentMembers = 34,
                currentTierIndex = 1,
                tiers = deal2Tiers,
                state = GroupState.FILLING,
                locality = koramangala,
                expiresAtMillis = System.currentTimeMillis() + 86400000L * 1,
                joinedUserIds = emptyList(),
                brand = "Konkan Growers Collective",
                specifications = mapOf(
                    "Weight" to "3.2 kg (12 Jumbo pieces)",
                    "Origin" to "Devgad, Maharashtra",
                    "Harvest" to "Tree Ripened"
                )
            ),
            GroupDeal(
                id = "deal_03",
                title = "Cold Pressed Wood-Churned Extra Virgin Groundnut Oil (5L)",
                description = "Unrefined nutrient-rich wood pressed Chekku / Ghani groundnut cooking oil, zero chemicals and zero preservatives.",
                category = "Pantry Essentials",
                imageUrl = "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=600&q=80",
                originalMarketPrice = 600.0,
                baseUnitPrice = 520.0,
                currentUnitPrice = 319.0,
                targetMembers = 40,
                currentMembers = 40,
                currentTierIndex = 2,
                tiers = deal3Tiers,
                state = GroupState.TARGET_REACHED,
                locality = koramangala,
                expiresAtMillis = System.currentTimeMillis() + 3600000L * 12,
                joinedUserIds = listOf("user_demo_01"),
                winningBid = SupplierBid(
                    id = "bid_102",
                    supplierId = "sup_02",
                    supplierName = "PurePress Natural Oils",
                    supplierScore = 4.8,
                    bidAmountPerUnit = 319.0,
                    promisedDeliveryDays = 1,
                    isSelected = true
                ),
                unitsReserved = 40,
                brand = "Ghani Gramin",
                specifications = mapOf(
                    "Capacity" to "5 Litre Can",
                    "Extraction" to "Cold Wood Ghani",
                    "Smoke Point" to "232°C"
                )
            ),
            GroupDeal(
                id = "deal_04",
                title = "Kashmir Saffron Pure Mongra Threads (5g Certified)",
                description = "Grade A++ Kashmiri Mongra saffron with deep red stigmas, rich aroma and unmatched coloring strength.",
                category = "Gourmet & Spices",
                imageUrl = "https://images.unsplash.com/photo-1599940824399-b87987ceb72a?auto=format&fit=crop&w=600&q=80",
                originalMarketPrice = 4000.0,
                baseUnitPrice = 3600.0,
                currentUnitPrice = 2750.0,
                targetMembers = 20,
                currentMembers = 14,
                currentTierIndex = 1,
                tiers = deal4Tiers,
                state = GroupState.BIDDING,
                locality = koramangala,
                expiresAtMillis = System.currentTimeMillis() + 86400000L * 3,
                joinedUserIds = emptyList(),
                brand = "Pampore Valley Saffron",
                specifications = mapOf(
                    "Grade" to "Mongra Grade A++",
                    "Net Weight" to "5 Grams",
                    "Purity" to "100% Tested ISO 3632"
                )
            )
        )
    }

    private fun seedInitialRequests() {
        val koramangala = LocalityService.POPULAR_LOCALITIES[0]
        _requests.value = listOf(
            GroupRequest(
                id = "req_01",
                productName = "Daawat Super Basmati Rice (25kg Bag)",
                category = "Pantry Essentials",
                targetPrice = 2800.0,
                referenceMarketPrice = 3600.0,
                currentVotes = 17,
                thresholdVotes = 20,
                requesterName = "Rohan Mehta",
                locality = koramangala,
                votedUserIds = listOf("user_demo_01")
            ),
            GroupRequest(
                id = "req_02",
                productName = "Organic Kashmiri Walnuts Giri (1kg Vacuum Pack)",
                category = "Dry Fruits",
                targetPrice = 850.0,
                referenceMarketPrice = 1250.0,
                currentVotes = 12,
                thresholdVotes = 15,
                requesterName = "Sneha Kulkarni",
                locality = koramangala
            ),
            GroupRequest(
                id = "req_03",
                productName = "Cold Pressed Virgin Coconut Oil (5L Can)",
                category = "Pantry Essentials",
                targetPrice = 1100.0,
                referenceMarketPrice = 1600.0,
                currentVotes = 9,
                thresholdVotes = 15,
                requesterName = "Vikas Rao",
                locality = koramangala
            )
        )
    }

    private fun seedInitialOrders() {
        val koramangala = LocalityService.POPULAR_LOCALITIES[0]
        _orders.value = listOf(
            Order(
                id = "ord_past_01",
                orderNumber = "GD-984210",
                groupDealId = "deal_03",
                productTitle = "Cold Pressed Wood-Churned Extra Virgin Groundnut Oil (5L)",
                productImageUrl = "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=600&q=80",
                quantity = 1,
                unitPrice = 319.0,
                totalAmount = 319.0,
                originalMarketTotal = 600.0,
                savedAmount = 281.0,
                status = OrderStatus.CONFIRMED,
                orderDateMillis = System.currentTimeMillis() - 7200000L,
                deliveryLocality = koramangala,
                deliveryAddressLine = "Apartment 304, Green Palms, 4th Block, Koramangala",
                winningSupplierName = "PurePress Natural Oils",
                trackingSteps = listOf(
                    TrackingStep("Group Target Reached", "Target 40 members met. Final tier discount unlocked!", "Today, 10:15 AM", true),
                    TrackingStep("Order Confirmed", "Payment verified via UPI batch settlement", "Today, 10:30 AM", true),
                    TrackingStep("Supplier Dispatched", "Packed and leaving regional warehouse", "Today, 4:00 PM", true),
                    TrackingStep("Locality Delivery", "Arriving at Koramangala Hub for last-mile delivery", "Tomorrow by 2:00 PM", false)
                )
            )
        )
    }
}
