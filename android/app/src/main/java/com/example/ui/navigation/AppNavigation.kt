package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.AuthUiState
import com.example.core.data.GroupDealRepository
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.OnboardingScreen
import com.example.ui.detail.GroupDetailScreen
import com.example.ui.discovery.DiscoverHomeScreen
import com.example.ui.mygroups.MyGroupsScreen
import com.example.ui.orders.OrdersScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.requests.RequestGroupScreen
import com.example.ui.theme.Emerald40
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DISCOVER("Deals", Icons.Filled.Storefront, Icons.Outlined.Storefront, "tab_discover"),
    REQUESTS("Demands", Icons.Filled.Campaign, Icons.Outlined.Campaign, "tab_requests"),
    MY_GROUPS("My Groups", Icons.Filled.Group, Icons.Outlined.Group, "tab_my_groups"),
    ORDERS("Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_orders"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
}

@Composable
fun AppNavigation() {
    val authState by GroupDealRepository.authState.collectAsState()
    var isManualOnboardingFlow by remember { mutableStateOf(false) }

    when (val state = authState) {
        is AuthUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Emerald40,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        is AuthUiState.Unauthenticated -> {
            if (isManualOnboardingFlow) {
                OnboardingScreen(onNavigateBack = { isManualOnboardingFlow = false })
            } else {
                LoginScreen(onNavigateToOnboarding = { isManualOnboardingFlow = true })
            }
        }

        is AuthUiState.OnboardingRequired -> {
            OnboardingScreen(onNavigateBack = { GroupDealRepository.logout() })
        }

        is AuthUiState.Authenticated -> {
            MainAppShell()
        }
    }
}

@Composable
fun MainAppShell() {
    var selectedTab by remember { mutableStateOf(MainTab.DISCOVER) }
    var selectedDetailDealId by remember { mutableStateOf<String?>(null) }

    if (selectedDetailDealId != null) {
        GroupDetailScreen(
            dealId = selectedDetailDealId!!,
            onNavigateBack = { selectedDetailDealId = null },
            onNavigateToOrders = {
                selectedDetailDealId = null
                selectedTab = MainTab.ORDERS
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 6.dp
                ) {
                    MainTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OnEmeraldContainer,
                                selectedTextColor = Emerald40,
                                indicatorColor = EmeraldContainer,
                                unselectedIconColor = Slate600,
                                unselectedTextColor = Slate600
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    MainTab.DISCOVER -> DiscoverHomeScreen(
                        onNavigateToDetail = { dealId -> selectedDetailDealId = dealId },
                        onNavigateToRequests = { selectedTab = MainTab.REQUESTS }
                    )
                    MainTab.REQUESTS -> RequestGroupScreen()
                    MainTab.MY_GROUPS -> MyGroupsScreen(
                        onNavigateToDetail = { dealId -> selectedDetailDealId = dealId },
                        onNavigateToOrders = { selectedTab = MainTab.ORDERS }
                    )
                    MainTab.ORDERS -> OrdersScreen()
                    MainTab.PROFILE -> ProfileScreen()
                }
            }
        }
    }
}
