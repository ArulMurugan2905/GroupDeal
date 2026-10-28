package com.example.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.data.AuthUiState
import com.example.core.data.GroupDealRepository
import com.example.core.model.GroupState
import com.example.core.model.PricingTier
import com.example.ui.components.DiscountPill
import com.example.ui.components.MilestoneProgressBar
import com.example.ui.components.StateBadge
import com.example.ui.theme.DealAmber
import com.example.ui.theme.DealOrange
import com.example.ui.theme.Emerald40
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    dealId: String,
    onNavigateBack: () -> Unit,
    onNavigateToOrders: () -> Unit
) {
    val deals by GroupDealRepository.deals.collectAsState()
    val authState by GroupDealRepository.authState.collectAsState()
    val currentUser = (authState as? AuthUiState.Authenticated)?.user

    val deal = deals.firstOrNull { it.id == dealId }
    if (deal == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Deal not found")
        }
        return
    }

    val isJoined = currentUser?.let { deal.joinedUserIds.contains(it.id) } ?: false
    var quantity by remember { mutableIntStateOf(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Group Buy Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share link */ }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share with Neighbors", tint = Slate800)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Group Price",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate600)
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "₹${(deal.currentUnitPrice * quantity).toInt()}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Emerald40
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "₹${(deal.originalMarketPrice * quantity).toInt()}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate400,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isJoined) {
                            Button(
                                onClick = {
                                    GroupDealRepository.placeGroupOrder(deal, quantity)
                                    onNavigateToOrders()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald40),
                                modifier = Modifier.testTag("confirm_order_button")
                            ) {
                                Text(
                                    text = "Confirm & Pay (₹${(deal.currentUnitPrice * quantity).toInt()})",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    GroupDealRepository.joinGroup(deal.id)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald40),
                                modifier = Modifier.testTag("detail_join_button")
                            ) {
                                Text(
                                    text = "Join Group (${deal.currentMembers}/${deal.targetMembers})",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate100)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Image with Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                AsyncImage(
                    model = deal.imageUrl,
                    contentDescription = deal.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentScale = ContentScale.Crop
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StateBadge(state = deal.state)
                    DiscountPill(discountPercent = deal.currentSavingsPercent)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and Locality Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = deal.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Emerald40,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Exclusive to ${deal.locality.localityName} (${deal.locality.city})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Emerald40
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = deal.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Slate700,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }

                // Dynamic Pricing Roadmap
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dynamic Pricing Roadmap",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldContainer
                            ) {
                                Text(
                                    text = "Server Authoritative",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnEmeraldContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        MilestoneProgressBar(
                            tiers = deal.tiers,
                            currentMembers = deal.currentMembers,
                            targetMembers = deal.targetMembers
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tier Breakdown Table
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            deal.tiers.forEachIndexed { index, tier ->
                                val isActive = deal.currentTierIndex == index
                                val isPast = deal.currentTierIndex > index

                                TierRow(
                                    tier = tier,
                                    isActive = isActive,
                                    isPast = isPast,
                                    originalPrice = deal.originalMarketPrice
                                )
                            }
                        }
                    }
                }

                // Verified Supplier Card
                if (deal.winningBid != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Emerald40,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = deal.winningBid.supplierName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Emerald40,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Wholesale Supplier Score: ${deal.winningBid.supplierScore} ★ • ${deal.winningBid.promisedDeliveryDays} Days Delivery",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                )
                            }
                        }
                    }
                }

                // Group Buy How-It-Works Stepper
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "How GroupDeal Works",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        JourneyStepItem(
                            stepNumber = "1",
                            title = "You & Neighbors Join",
                            description = "Every neighbor who joins drives the unit price lower for everyone.",
                            isDone = deal.currentMembers > 0
                        )
                        JourneyStepItem(
                            stepNumber = "2",
                            title = "Wholesale Bidding Triggers",
                            description = "Verified regional suppliers compete to fulfill the locality order.",
                            isDone = deal.state == GroupState.BIDDING || deal.state == GroupState.TARGET_REACHED
                        )
                        JourneyStepItem(
                            stepNumber = "3",
                            title = "Bulk Batch Delivery",
                            description = "Order arrives directly at your locality hub, slashing intermediary costs.",
                            isDone = deal.state == GroupState.DELIVERED
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TierRow(
    tier: PricingTier,
    isActive: Boolean,
    isPast: Boolean,
    originalPrice: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) EmeraldContainer else Slate100,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) Emerald40 else Slate200
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isActive || isPast) Emerald40 else Slate400),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPast || isActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${tier.minMembers}+ Buyers Milestone",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) OnEmeraldContainer else Slate800
                        )
                    )
                    Text(
                        text = "${tier.discountPercent}% Discount",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = if (isActive) Emerald40 else Slate600
                        )
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${tier.unitPrice.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Emerald40 else Slate900
                    )
                )
                Text(
                    text = "Save ₹${(originalPrice - tier.unitPrice).toInt()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = DealOrange,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun JourneyStepItem(
    stepNumber: String,
    title: String,
    description: String,
    isDone: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isDone) Emerald40 else Slate200),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) Color.White else Slate600
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate600,
                    fontSize = 12.sp
                )
            )
        }
    }
}
