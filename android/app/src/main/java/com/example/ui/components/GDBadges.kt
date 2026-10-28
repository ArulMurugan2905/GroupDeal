package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.GroupState
import com.example.core.model.Locality
import com.example.core.model.PricingTier
import com.example.core.model.TrustTier
import com.example.ui.theme.BronzeBadge
import com.example.ui.theme.DealAmber
import com.example.ui.theme.DealOrange
import com.example.ui.theme.Emerald40
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.GoldBadge
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.PlatinumBadge
import com.example.ui.theme.SilverBadge
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800

@Composable
fun StateBadge(state: GroupState, modifier: Modifier = Modifier) {
    val bgColor = Color(state.badgeColorHex).copy(alpha = 0.15f)
    val textColor = Color(state.badgeColorHex)

    Surface(
        modifier = modifier.clip(RoundedCornerShape(6.dp)),
        color = bgColor,
        contentColor = textColor
    ) {
        Text(
            text = state.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun DiscountPill(discountPercent: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(6.dp)),
        color = DealOrange,
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.TrendingDown,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$discountPercent% OFF",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp)
            )
        }
    }
}

@Composable
fun TrustTierBadge(tier: TrustTier, modifier: Modifier = Modifier) {
    val badgeColor = when (tier) {
        TrustTier.PLATINUM -> PlatinumBadge
        TrustTier.GOLD -> GoldBadge
        TrustTier.SILVER -> SilverBadge
        TrustTier.BRONZE -> BronzeBadge
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = badgeColor.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = badgeColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = tier.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            )
        }
    }
}

@Composable
fun MilestoneProgressBar(
    tiers: List<PricingTier>,
    currentMembers: Int,
    targetMembers: Int,
    modifier: Modifier = Modifier
) {
    val maxMembers = targetMembers.coerceAtLeast(1)
    val progressRatio = (currentMembers.toFloat() / maxMembers.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progressRatio, label = "milestone_progress")

    Column(modifier = modifier.fillMaxWidth()) {
        // Bar track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Slate200)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(10.dp)
                    .background(Emerald40)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Milestones Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tiers.forEachIndexed { index, tier ->
                val isReached = currentMembers >= tier.minMembers
                val isNext = !isReached && (index == 0 || currentMembers >= tiers[index - 1].minMembers)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isReached) Emerald40 else if (isNext) DealAmber else Slate200),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isReached) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${tier.minMembers} buyers",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isReached) FontWeight.Bold else FontWeight.Normal,
                                color = if (isReached) Slate800 else Slate600
                            )
                        )
                    }
                    Text(
                        text = "₹${tier.unitPrice.toInt()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReached) Emerald40 else Slate600
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LocalityHeader(
    locality: Locality,
    onLocalityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onLocalityClick)
            .testTag("locality_selector_button"),
        color = EmeraldContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Emerald40),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Locality",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Buying In: ",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate600)
                        )
                        Text(
                            text = locality.localityName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnEmeraldContainer
                            )
                        )
                    }
                    Text(
                        text = "${locality.city} • PIN ${locality.pincode} (${locality.activeGroupsCount} Active Deals)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Change Locality",
                        tint = Slate700,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                    )
                }
            }
        }
    }
}
