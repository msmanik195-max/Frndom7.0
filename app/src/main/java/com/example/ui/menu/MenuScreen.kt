package com.example.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostItem
import com.example.data.model.UserProfile
import com.example.data.repository.GroupPageRepository
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WatchHistoryRepository
import com.example.data.service.MediaUploadService
import com.example.ui.menu.admin.AdminDashboardView
import com.example.ui.menu.admin.AdminDepositRequestsView
import com.example.ui.menu.admin.AdminMonetizationRequestsView
import com.example.ui.menu.admin.AdminPaymentMethodsView
import com.example.ui.menu.admin.AdminUserManagementView
import com.example.ui.menu.admin.AdminVerificationRequestsView
import com.example.ui.menu.admin.AdminWithdrawRequestsView
import com.example.ui.verification.VerificationBadgeScreen

enum class MenuSubScreen {
    MAIN,
    MARKETPLACE,
    SAVED,
    DASHBOARD,
    WALLET,
    GROUPS,
    PAGES,
    WATCH_HISTORY,
    SETTINGS,
    ADMIN_DASHBOARD,
    ADMIN_USER_MANAGEMENT,
    VERIFICATION,
    DEPOSIT,
    WITHDRAW,
    ADMIN_DEPOSITS,
    ADMIN_WITHDRAWS,
    ADMIN_MONETIZATION,
    ADMIN_VERIFICATIONS,
    ADMIN_PAYMENT_METHODS
}

@Composable
fun MenuScreen(
    userProfile: UserProfile?,
    onProfileClick: () -> Unit,
    onServerSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onPostClick: (PostItem) -> Unit = {},
    onAccountSwitched: (UserProfile) -> Unit = {},
    onAddNewAccount: () -> Unit = {},
    onSubScreenChanged: (Boolean) -> Unit = {},
    mediaUploadService: MediaUploadService? = null,
    onOpenChat: (UserProfile, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val groupPageRepository = remember { GroupPageRepository(context) }
    val postRepository = remember { PostRepository(context) }
    val watchHistoryRepository = remember { WatchHistoryRepository(context) }
    val userRepository = remember { UserRepository(context) }
    val marketplaceRepository = remember { MarketplaceRepository(context) }

    var currentSubScreen by remember { mutableStateOf(MenuSubScreen.MAIN) }
    var showAccountSwitcher by remember { mutableStateOf(false) }

    LaunchedEffect(currentSubScreen) {
        onSubScreenChanged(currentSubScreen != MenuSubScreen.MAIN)
    }

    BackHandler(enabled = currentSubScreen != MenuSubScreen.MAIN) {
        currentSubScreen = MenuSubScreen.MAIN
    }

    when (currentSubScreen) {
        MenuSubScreen.MARKETPLACE -> {
            MarketplaceView(
                userProfile = userProfile,
                marketplaceRepository = marketplaceRepository,
                mediaUploadService = mediaUploadService,
                onBack = { currentSubScreen = MenuSubScreen.MAIN },
                onOpenChat = onOpenChat,
                modifier = modifier
            )
        }
        MenuSubScreen.SAVED -> {
            SavedItemsView(
                currentUserId = userProfile?.uid ?: "",
                postRepository = postRepository,
                marketplaceRepository = marketplaceRepository,
                onPostClick = { post ->
                    onPostClick(post)
                },
                onOpenChat = onOpenChat,
                onBack = { currentSubScreen = MenuSubScreen.MAIN },
                modifier = modifier
            )
        }
        MenuSubScreen.DASHBOARD -> {
            DashboardView(
                userProfile = userProfile,
                onBack = { currentSubScreen = MenuSubScreen.MAIN },
                modifier = modifier
            )
        }
        MenuSubScreen.WALLET -> {
            WalletView(
                userProfile = userProfile,
                onBack = { currentSubScreen = MenuSubScreen.MAIN },
                modifier = modifier
            )
        }
        MenuSubScreen.GROUPS -> {
            GroupsView(
                userProfile = userProfile,
                groupPageRepository = groupPageRepository,
                postRepository = postRepository,
                onBack = { currentSubScreen = MenuSubScreen.MAIN },
                modifier = modifier
            )
        }
        MenuSubScreen.PAGES -> {
            PagesView(
                userProfile = userProfile,
                groupPageRepository = groupPageRepository,
                postRepository = postRepository,
                onBack = { currentSubScreen = MenuSubScreen.MAIN },
                onSwitchToPage = { selectedPage ->
                    val pageUserProfile = UserProfile(
                        uid = "page_profile_${selectedPage.id}",
                        firstName = selectedPage.name,
                        lastName = "",
                        fullName = selectedPage.name,
                        email = "${selectedPage.name.lowercase().replace(" ", "")}@page.frndom.app",
                        profilePictureUrl = selectedPage.avatarUrl,
                        coverPictureUrl = selectedPage.coverUrl,
                        bio = selectedPage.description
                    )
                    userRepository.saveLocalUserProfile(pageUserProfile)
                    onAccountSwitched(pageUserProfile)
                },
                modifier = modifier
            )
        }
        MenuSubScreen.WATCH_HISTORY -> {
            WatchHistoryView(
                watchHistoryRepository = watchHistoryRepository,
                onPostClick = onPostClick,
                onBack = { currentSubScreen = MenuSubScreen.MAIN },
                modifier = modifier
            )
        }
        MenuSubScreen.SETTINGS -> {
            SettingsView(
                userProfile = userProfile,
                onServerSettingsClick = onServerSettingsClick,
                onVerificationBadgeClick = { currentSubScreen = MenuSubScreen.VERIFICATION },
                onLogoutClick = onLogoutClick,
                onBack = { currentSubScreen = MenuSubScreen.MAIN },
                onDepositRequestsClick = { currentSubScreen = MenuSubScreen.ADMIN_DEPOSITS },
                onWithdrawRequestsClick = { currentSubScreen = MenuSubScreen.ADMIN_WITHDRAWS },
                onMonetizationRequestsClick = { currentSubScreen = MenuSubScreen.ADMIN_MONETIZATION },
                onVerificationRequestsClick = { currentSubScreen = MenuSubScreen.ADMIN_VERIFICATIONS },
                onPaymentMethodsClick = { currentSubScreen = MenuSubScreen.ADMIN_PAYMENT_METHODS },
                onAdminDashboardClick = { currentSubScreen = MenuSubScreen.ADMIN_DASHBOARD },
                modifier = modifier
            )
        }
        MenuSubScreen.ADMIN_DASHBOARD -> {
            AdminDashboardView(
                onBack = { currentSubScreen = MenuSubScreen.SETTINGS },
                onServerSettingsClick = onServerSettingsClick,
                modifier = modifier
            )
        }
        MenuSubScreen.ADMIN_USER_MANAGEMENT -> {
            AdminUserManagementView(
                onBack = { currentSubScreen = MenuSubScreen.ADMIN_DASHBOARD },
                modifier = modifier
            )
        }
        MenuSubScreen.ADMIN_DEPOSITS -> {
            AdminDepositRequestsView(
                onBack = { currentSubScreen = MenuSubScreen.SETTINGS },
                modifier = modifier
            )
        }
        MenuSubScreen.ADMIN_WITHDRAWS -> {
            AdminWithdrawRequestsView(
                onBack = { currentSubScreen = MenuSubScreen.SETTINGS },
                modifier = modifier
            )
        }
        MenuSubScreen.ADMIN_MONETIZATION -> {
            AdminMonetizationRequestsView(
                onBack = { currentSubScreen = MenuSubScreen.SETTINGS },
                modifier = modifier
            )
        }
        MenuSubScreen.ADMIN_VERIFICATIONS -> {
            AdminVerificationRequestsView(
                onBack = { currentSubScreen = MenuSubScreen.SETTINGS },
                modifier = modifier
            )
        }
        MenuSubScreen.ADMIN_PAYMENT_METHODS -> {
            AdminPaymentMethodsView(
                onBack = { currentSubScreen = MenuSubScreen.SETTINGS },
                modifier = modifier
            )
        }
        MenuSubScreen.VERIFICATION -> {
            VerificationBadgeScreen(
                userProfile = userProfile,
                userRepository = userRepository,
                onBack = { currentSubScreen = MenuSubScreen.MAIN },
                onNavigateToProfile = onProfileClick,
                onNavigateToDeposit = { currentSubScreen = MenuSubScreen.DEPOSIT },
                modifier = modifier
            )
        }
        MenuSubScreen.DEPOSIT -> {
            DepositScreen(
                onBack = { currentSubScreen = MenuSubScreen.MAIN },
                onDepositSuccess = { currentSubScreen = MenuSubScreen.MAIN },
                modifier = modifier
            )
        }
        MenuSubScreen.WITHDRAW -> {
            WithdrawScreen(
                onBack = { currentSubScreen = MenuSubScreen.MAIN },
                onWithdrawSuccess = { currentSubScreen = MenuSubScreen.MAIN },
                modifier = modifier
            )
        }
        MenuSubScreen.MAIN -> {
            val displayName = when {
                !userProfile?.fullName.isNullOrBlank() -> userProfile?.fullName ?: "User"
                !userProfile?.firstName.isNullOrBlank() -> "${userProfile?.firstName} ${userProfile?.lastName}".trim()
                else -> "User"
            }
            val initial = displayName.firstOrNull()?.uppercase() ?: "U"

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFFF0F2F5))
                    .testTag("menu_screen")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Menu",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF050505)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE4E6EB),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable(onClick = onSearchClick)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color(0xFF050505),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE4E6EB),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { currentSubScreen = MenuSubScreen.SETTINGS }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = Color(0xFF050505),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top Profile & Account Switcher Card (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onProfileClick)
                                    .testTag("menu_profile_card"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (!userProfile?.profilePictureUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = userProfile?.profilePictureUrl,
                                                contentDescription = displayName,
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Surface(
                                                modifier = Modifier.size(52.dp),
                                                shape = CircleShape,
                                                color = Color(0xFF1877F2).copy(alpha = 0.15f)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = initial,
                                                        fontSize = 22.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1877F2)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = displayName,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF050505)
                                                )
                                                if (userProfile?.isVerificationActive() == true) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    com.example.ui.components.VerificationBadge(size = 16.dp, show = true)
                                                }
                                            }
                                            Text(
                                                text = "See your profile",
                                                fontSize = 13.sp,
                                                color = Color(0xFF65676B)
                                            )
                                        }
                                    }

                                    // Switch Account Button
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFFE4E6EB),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { showAccountSwitcher = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SwapHoriz,
                                                contentDescription = "Switch Account",
                                                tint = Color(0xFF050505),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Switch",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF050505)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section Title: Shortcuts
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "All Shortcuts",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF65676B),
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }

                        // 1. Marketplace (Facebook style buy & sell)
                        item {
                            FacebookMenuCard(
                                title = "Marketplace",
                                subtitle = "Buy and sell items",
                                icon = Icons.Default.Storefront,
                                iconColor = Color(0xFF1877F2),
                                iconBgColor = Color(0xFFEBF5FF),
                                onClick = { currentSubScreen = MenuSubScreen.MARKETPLACE },
                                tag = "menu_card_marketplace"
                            )
                        }

                        // 2. Dashboard
                        item {
                            FacebookMenuCard(
                                title = "Dashboard",
                                subtitle = "Professional insights",
                                icon = Icons.Default.AutoGraph,
                                iconColor = Color(0xFF009688),
                                iconBgColor = Color(0xFFE0F2F1),
                                onClick = { currentSubScreen = MenuSubScreen.DASHBOARD },
                                tag = "menu_card_dashboard"
                            )
                        }

                        // 3. Wallet
                        item {
                            FacebookMenuCard(
                                title = "Wallet",
                                subtitle = "Earnings & stars (৳ BDT)",
                                icon = Icons.Default.AccountBalanceWallet,
                                iconColor = Color(0xFF2E7D32),
                                iconBgColor = Color(0xFFE8F5E9),
                                onClick = { currentSubScreen = MenuSubScreen.WALLET },
                                tag = "menu_card_wallet"
                            )
                        }

                        // 4. Groups
                        item {
                            FacebookMenuCard(
                                title = "Groups",
                                subtitle = "Find & join groups",
                                icon = Icons.Default.Group,
                                iconColor = Color(0xFF0288D1),
                                iconBgColor = Color(0xFFE1F5FE),
                                onClick = { currentSubScreen = MenuSubScreen.GROUPS },
                                tag = "menu_card_groups"
                            )
                        }

                        // 5. Pages
                        item {
                            FacebookMenuCard(
                                title = "Pages",
                                subtitle = "Manage your pages",
                                icon = Icons.Default.Flag,
                                iconColor = Color(0xFFE91E63),
                                iconBgColor = Color(0xFFFCE4EC),
                                onClick = { currentSubScreen = MenuSubScreen.PAGES },
                                tag = "menu_card_pages"
                            )
                        }

                        // 6. Saved (Posts & Videos)
                        item {
                            FacebookMenuCard(
                                title = "Saved",
                                subtitle = "Saved posts & reels",
                                icon = Icons.Default.Bookmark,
                                iconColor = Color(0xFF8E24AA),
                                iconBgColor = Color(0xFFF3E5F5),
                                onClick = { currentSubScreen = MenuSubScreen.SAVED },
                                tag = "menu_card_saved"
                            )
                        }

                        // 6. History
                        item {
                            FacebookMenuCard(
                                title = "History",
                                subtitle = "Watched videos & reels",
                                icon = Icons.Default.History,
                                iconColor = Color(0xFFE65100),
                                iconBgColor = Color(0xFFFFF3E0),
                                onClick = { currentSubScreen = MenuSubScreen.WATCH_HISTORY },
                                tag = "menu_card_watch_history"
                            )
                        }

                        // 7. Verification Badge
                        item {
                            FacebookMenuCard(
                                title = "Verification Badge",
                                subtitle = "Get Green Badge & trust",
                                icon = Icons.Default.VerifiedUser,
                                iconColor = Color(0xFF00C853),
                                iconBgColor = Color(0xFFE8F5E9),
                                onClick = { currentSubScreen = MenuSubScreen.VERIFICATION },
                                tag = "menu_card_verification"
                            )
                        }

                        // 8. Settings
                        item {
                            FacebookMenuCard(
                                title = "Settings",
                                subtitle = "Privacy & R2 cloud",
                                icon = Icons.Default.Settings,
                                iconColor = Color(0xFF5E35B1),
                                iconBgColor = Color(0xFFEDE7F6),
                                onClick = { currentSubScreen = MenuSubScreen.SETTINGS },
                                tag = "menu_card_settings"
                            )
                        }

                        // Log Out Card (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onLogoutClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE4E6EB)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("menu_logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Log Out",
                                    tint = Color(0xFF050505),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Log Out",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505),
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Account Switcher Bottom Sheet
            if (showAccountSwitcher) {
                val savedAccounts = remember { userRepository.getSavedAccounts() }
                AccountSwitcherSheet(
                    currentProfile = userProfile,
                    savedAccounts = savedAccounts,
                    groupPageRepository = groupPageRepository,
                    onSelectAccount = { selectedProfile ->
                        userRepository.saveLocalUserProfile(selectedProfile)
                        onAccountSwitched(selectedProfile)
                    },
                    onSelectPage = { selectedPage ->
                        val pageUserProfile = UserProfile(
                            uid = "page_profile_${selectedPage.id}",
                            firstName = selectedPage.name,
                            lastName = "",
                            fullName = selectedPage.name,
                            email = "${selectedPage.name.lowercase().replace(" ", "")}@page.frndom.app",
                            profilePictureUrl = selectedPage.avatarUrl,
                            coverPictureUrl = selectedPage.coverUrl,
                            bio = selectedPage.description
                        )
                        userRepository.saveLocalUserProfile(pageUserProfile)
                        onAccountSwitched(pageUserProfile)
                    },
                    onAddNewAccount = onAddNewAccount,
                    onDismiss = { showAccountSwitcher = false }
                )
            }
        }
    }
}

@Composable
private fun FacebookMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconBgColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF050505)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF65676B),
                lineHeight = 16.sp
            )
        }
    }
}
