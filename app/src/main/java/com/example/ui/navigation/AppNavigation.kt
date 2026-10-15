package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserRepository
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ClientHomeScreen
import com.example.ui.screens.ComplaintsScreen
import com.example.ui.screens.DesignerHubScreen
import com.example.ui.screens.MaterialsScreen
import com.example.ui.screens.NewOrderScreen
import com.example.ui.screens.NotificationsCenterScreen
import com.example.ui.screens.OrderTrackingScreen
import com.example.ui.screens.PartnerLedgerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignageCalculatorScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.OrderViewModel
import com.example.ui.viewmodel.SignageCalculatorViewModel

enum class Screen {
    HOME,
    NEW_ORDER,
    MY_ORDERS,
    NOTIFICATIONS,
    COMPLAINTS,
    DESIGNER_HUB,
    ADMIN_DASHBOARD,
    CALCULATOR,
    MATERIALS,
    PARTNERS,
    SETTINGS
}

data class NavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
)

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit
) {
    val userProfile by mainViewModel.userProfile.collectAsState()
    val isPreviewingAsClient by mainViewModel.previewAsClient.collectAsState()

    val currentRole = userProfile?.role?.uppercase() ?: UserRepository.ROLE_CLIENT
    val isSuperAdmin = mainViewModel.isSuperAdmin()
    val isDesigner = mainViewModel.isDesigner()
    val isStaff = mainViewModel.isEffectiveStaff()

    val orderViewModel = remember {
        OrderViewModel(mainViewModel.orderRepository, mainViewModel.materialRepository)
    }

    val calculatorViewModel = remember {
        SignageCalculatorViewModel(mainViewModel.materialRepository)
    }

    // Default screen based on role
    var currentScreen by remember(isStaff, isSuperAdmin) {
        mutableStateOf(if (isStaff) Screen.DESIGNER_HUB else Screen.HOME)
    }

    // Navigation Guard (Requirement 4):
    // Users with CLIENT role can only view client services and orders.
    // Designer/Admin screens are locked strictly for verified designers & admin.
    LaunchedEffect(isStaff, isSuperAdmin, currentScreen) {
        if (!isStaff) {
            val forbiddenForClient = setOf(
                Screen.DESIGNER_HUB,
                Screen.ADMIN_DASHBOARD,
                Screen.CALCULATOR,
                Screen.MATERIALS,
                Screen.PARTNERS
            )
            if (currentScreen in forbiddenForClient) {
                currentScreen = Screen.HOME
            }
        } else if (!isSuperAdmin && currentScreen == Screen.ADMIN_DASHBOARD) {
            // Only Super Admin can access the Admin Dashboard
            currentScreen = Screen.DESIGNER_HUB
        }
    }

    // Start observing orders when user is signed in
    LaunchedEffect(userProfile, isStaff) {
        val uid = userProfile?.uid
        if (!uid.isNullOrBlank()) {
            orderViewModel.startObservingOrders(uid, isStaff)
        }
    }

    // Handle back button for sub-screens
    BackHandler(enabled = currentScreen != Screen.HOME && currentScreen != Screen.DESIGNER_HUB) {
        currentScreen = if (isStaff) Screen.DESIGNER_HUB else Screen.HOME
    }

    // Navigation items tailored strictly by role
    val clientNavItems = listOf(
        NavItem(Screen.HOME, "الرئيسية", Icons.Default.Home),
        NavItem(Screen.NEW_ORDER, "طلب جديد", Icons.Default.AddCircle),
        NavItem(Screen.MY_ORDERS, "طلباتي", Icons.Default.Assignment),
        NavItem(Screen.COMPLAINTS, "الشكاوى", Icons.Default.ChatBubble),
        NavItem(Screen.SETTINGS, "الإعدادات", Icons.Default.Settings)
    )

    val designerNavItems = listOf(
        NavItem(Screen.DESIGNER_HUB, "الطلبات", Icons.Default.Dashboard),
        NavItem(Screen.CALCULATOR, "الحاسبة", Icons.Default.Calculate),
        NavItem(Screen.MATERIALS, "الخامات", Icons.Default.Inventory2),
        NavItem(Screen.COMPLAINTS, "الشكاوى", Icons.Default.ChatBubble),
        NavItem(Screen.SETTINGS, "الإعدادات", Icons.Default.Settings)
    )

    val adminNavItems = listOf(
        NavItem(Screen.DESIGNER_HUB, "الطلبات", Icons.Default.Dashboard),
        NavItem(Screen.ADMIN_DASHBOARD, "الإدارة", Icons.Default.AdminPanelSettings),
        NavItem(Screen.CALCULATOR, "الحاسبة", Icons.Default.Calculate),
        NavItem(Screen.MATERIALS, "الخامات", Icons.Default.Inventory2),
        NavItem(Screen.SETTINGS, "الإعدادات", Icons.Default.Settings)
    )

    val currentNavItems = when {
        !isStaff -> clientNavItems
        isSuperAdmin -> adminNavItems
        else -> designerNavItems
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                currentNavItems.forEach { item ->
                    val selected = currentScreen == item.screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = item.screen },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            // Floating Action Button allowing return to Designer Hub when previewing as client
            AnimatedVisibility(visible = isPreviewingAsClient) {
                ExtendedFloatingActionButton(
                    onClick = {
                        mainViewModel.exitClientPreview()
                        currentScreen = Screen.DESIGNER_HUB
                    },
                    icon = { Icon(Icons.Default.DesignServices, contentDescription = null, tint = Color.White) },
                    text = { Text("العودة إلى لوحة المصمم", color = Color.White, fontSize = 13.sp) },
                    containerColor = Color(0xFFD97706),
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("back_to_designer_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.HOME -> ClientHomeScreen(
                    mainViewModel = mainViewModel,
                    onNavigateToNewOrder = { currentScreen = Screen.NEW_ORDER },
                    onNavigateToCalculator = {
                        if (isStaff) currentScreen = Screen.CALCULATOR
                    },
                    onNavigateToNotifications = { currentScreen = Screen.NOTIFICATIONS },
                    onNavigateToComplaints = { currentScreen = Screen.COMPLAINTS }
                )
                Screen.NEW_ORDER -> NewOrderScreen(
                    mainViewModel = mainViewModel,
                    orderViewModel = orderViewModel,
                    onOrderSubmitted = { currentScreen = Screen.MY_ORDERS }
                )
                Screen.MY_ORDERS -> OrderTrackingScreen(
                    orderViewModel = orderViewModel,
                    onNavigateToNewOrder = { currentScreen = Screen.NEW_ORDER }
                )
                Screen.NOTIFICATIONS -> NotificationsCenterScreen(
                    mainViewModel = mainViewModel
                )
                Screen.COMPLAINTS -> ComplaintsScreen(
                    mainViewModel = mainViewModel
                )
                Screen.DESIGNER_HUB -> DesignerHubScreen(
                    mainViewModel = mainViewModel,
                    orderViewModel = orderViewModel,
                    onNavigateToCalculator = { currentScreen = Screen.CALCULATOR },
                    onNavigateToMaterials = { currentScreen = Screen.MATERIALS },
                    onNavigateToPartners = { currentScreen = Screen.PARTNERS },
                    onNavigateToAdminDashboard = { currentScreen = Screen.ADMIN_DASHBOARD }
                )
                Screen.ADMIN_DASHBOARD -> AdminDashboardScreen(
                    mainViewModel = mainViewModel
                )
                Screen.CALCULATOR -> SignageCalculatorScreen(
                    calculatorViewModel = calculatorViewModel
                )
                Screen.MATERIALS -> MaterialsScreen(
                    materialRepository = mainViewModel.materialRepository
                )
                Screen.PARTNERS -> PartnerLedgerScreen(
                    partnerRepository = mainViewModel.partnerRepository
                )
                Screen.SETTINGS -> SettingsScreen(
                    mainViewModel = mainViewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme
                )
            }
        }
    }
}
