package com.example.ui.menu.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

enum class AdminActiveScreen {
    DASHBOARD_MAIN,
    USER_MANAGEMENT,
    DEPOSIT_REQUESTS,
    WITHDRAW_REQUESTS,
    MONETIZATION_REQUESTS,
    VERIFICATION_REQUESTS,
    PAYMENT_METHODS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardView(
    onBack: () -> Unit,
    onServerSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository(context) }
    val postRepository = remember { PostRepository(context) }
    val chatRepository = remember { ChatRepository(context) }
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    // Real-time Data Sources
    val users by userRepository.getAllUsersFlow().collectAsState(initial = emptyList())
    val posts by postRepository.postsFlow.collectAsState()
    val totalMessages by chatRepository.getTotalMessagesCountFlow().collectAsState(initial = 0)

    val depositRequests by adminRepo.depositRequestsFlow.collectAsState()
    val withdrawRequests by adminRepo.withdrawRequestsFlow.collectAsState()
    val monetizationRequests by adminRepo.monetizationRequestsFlow.collectAsState()
    val verificationRequests by adminRepo.verificationRequestsFlow.collectAsState()
    val paymentMethods by adminRepo.paymentMethodsFlow.collectAsState()

    // Real metrics calculations
    val totalUsersCount = users.size
    val activeUsersCount = users.count { it.isUserOnline() }
    val blockedUsersCount = users.count { it.isBlocked }
    val verifiedUsersCount = users.count { it.isVerificationActive() }
    val monetizedUsersCount = users.count { it.isMonetized }

    val totalPostsCount = posts.size
    val totalCommentsCount = posts.sumOf { it.commentsCount }
    val totalImagePostsCount = posts.count { it.mediaType == "photo" || it.mediaUrls.isNotEmpty() || (it.mediaUrl.isNotBlank() && !it.mediaUrl.endsWith(".mp4")) }
    val totalVideoPostsCount = posts.count { it.mediaType == "video" || it.mediaType == "reel" || it.mediaUrl.endsWith(".mp4") }
    val totalTextPostsCount = posts.count { it.mediaType == "text" && it.mediaUrl.isBlank() && it.mediaUrls.isEmpty() }

    val pendingDepositsCount = depositRequests.count { it.status == "PENDING" }
    val pendingWithdrawsCount = withdrawRequests.count { it.status == "PENDING" }
    val pendingMonetizationsCount = monetizationRequests.count { it.status == "PENDING" }
    val pendingVerificationsCount = verificationRequests.count { it.status == "PENDING" }

    val totalPendingDepositAmount = depositRequests.filter { it.status == "PENDING" }.sumOf { it.amount }
    val totalPendingWithdrawAmount = withdrawRequests.filter { it.status == "PENDING" }.sumOf { it.amount }

    // Drawer state & Navigation within Admin module
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentAdminScreen by remember { mutableStateOf(AdminActiveScreen.DASHBOARD_MAIN) }

    when (currentAdminScreen) {
        AdminActiveScreen.USER_MANAGEMENT -> {
            AdminUserManagementView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.DEPOSIT_REQUESTS -> {
            AdminDepositRequestsView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.WITHDRAW_REQUESTS -> {
            AdminWithdrawRequestsView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.MONETIZATION_REQUESTS -> {
            AdminMonetizationRequestsView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.VERIFICATION_REQUESTS -> {
            AdminVerificationRequestsView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.PAYMENT_METHODS -> {
            AdminPaymentMethodsView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.DASHBOARD_MAIN -> {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(310.dp),
                        drawerContainerColor = Color.White
                    ) {
                        // Admin Drawer Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF1877F2), Color(0xFF0056B3))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = "Admin",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Admin Control Menu",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Platform Management System",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 1. Dashboard Main
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null, tint = Color(0xFF1877F2)) },
                            label = { Text("Dashboard Overview", fontWeight = FontWeight.SemiBold) },
                            selected = true,
                            onClick = {
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFFE7F3FF),
                                selectedTextColor = Color(0xFF1877F2)
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 2. User Management
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF008937)) },
                            label = { Text("User Management", fontWeight = FontWeight.Medium) },
                            badge = {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "$totalUsersCount",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF008937),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color(0xFFE4E6EB))

                        // 3. Deposit Requests
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF1976D2)) },
                            label = { Text("Deposit Requests", fontWeight = FontWeight.Medium) },
                            badge = {
                                if (pendingDepositsCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE53935)
                                    ) {
                                        Text(
                                            text = "$pendingDepositsCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.DEPOSIT_REQUESTS
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 4. Withdraw Requests
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFFE65100)) },
                            label = { Text("Withdraw Requests", fontWeight = FontWeight.Medium) },
                            badge = {
                                if (pendingWithdrawsCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE53935)
                                    ) {
                                        Text(
                                            text = "$pendingWithdrawsCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.WITHDRAW_REQUESTS
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 5. Verification Requests
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF00C853)) },
                            label = { Text("Verification Requests", fontWeight = FontWeight.Medium) },
                            badge = {
                                if (pendingVerificationsCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE53935)
                                    ) {
                                        Text(
                                            text = "$pendingVerificationsCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.VERIFICATION_REQUESTS
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 6. Monetization Requests
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFA000)) },
                            label = { Text("Monetization Requests", fontWeight = FontWeight.Medium) },
                            badge = {
                                if (pendingMonetizationsCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE53935)
                                    ) {
                                        Text(
                                            text = "$pendingMonetizationsCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.MONETIZATION_REQUESTS
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color(0xFFE4E6EB))

                        // 7. Payment Methods
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF7B1FA2)) },
                            label = { Text("Payment Methods Setup", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.PAYMENT_METHODS
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 8. Server / Storage Settings
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color(0xFF0097A7)) },
                            label = { Text("Cloudflare R2 Storage", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onServerSettingsClick()
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "Admin Dashboard",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = Color(0xFF050505)
                                    )
                                    Text(
                                        text = "Live Real-Time Monitoring",
                                        fontSize = 12.sp,
                                        color = Color(0xFF008937),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            navigationIcon = {
                                // 3-line hamburger menu icon on the left
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("admin_hamburger_menu_btn")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            val totalPending = pendingDepositsCount + pendingWithdrawsCount + pendingMonetizationsCount + pendingVerificationsCount
                                            if (totalPending > 0) {
                                                Badge(
                                                    containerColor = Color(0xFFE53935),
                                                    contentColor = Color.White
                                                ) {
                                                    Text("$totalPending")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Admin Menu",
                                            tint = Color(0xFF050505),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.testTag("admin_dashboard_close_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF050505)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                        )
                    },
                    containerColor = Color(0xFFF0F2F5),
                    modifier = modifier.testTag("admin_dashboard_screen")
                ) { innerPadding ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quick Action Banner: Jump to User Management (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT }
                                    .testTag("admin_user_mgmt_banner"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1877F2)),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.People,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "User Management",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Block, unblock, verify & manage accounts",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Section 1: User & Community Live Stats (Cards)
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "User & Community Metrics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505),
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }

                        // 1. Total Users
                        item {
                            AdminMetricCard(
                                title = "Total Users",
                                count = "$totalUsersCount",
                                subtitle = "Registered users",
                                icon = Icons.Default.People,
                                iconBg = Color(0xFFE7F3FF),
                                iconTint = Color(0xFF1877F2),
                                onClick = { currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT }
                            )
                        }

                        // 2. Active Users (Online)
                        item {
                            AdminMetricCard(
                                title = "Active Users",
                                count = "$activeUsersCount",
                                subtitle = "Online currently",
                                icon = Icons.Default.TrendingUp,
                                iconBg = Color(0xFFE8F5E9),
                                iconTint = Color(0xFF00C853),
                                onClick = { currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT }
                            )
                        }

                        // 3. Blocked Users
                        item {
                            AdminMetricCard(
                                title = "Blocked Users",
                                count = "$blockedUsersCount",
                                subtitle = "Restricted accounts",
                                icon = Icons.Default.Block,
                                iconBg = Color(0xFFFFEBEE),
                                iconTint = Color(0xFFD32F2F),
                                onClick = { currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT }
                            )
                        }

                        // 4. Verified Users (Green Badge)
                        item {
                            AdminMetricCard(
                                title = "Verified Users",
                                count = "$verifiedUsersCount",
                                subtitle = "Active Green Badges",
                                icon = Icons.Default.Verified,
                                iconBg = Color(0xFFE8F5E9),
                                iconTint = Color(0xFF008937),
                                onClick = { currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT }
                            )
                        }

                        // Section 2: Posts & Social Engagement
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "Engagement & Content Metrics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505),
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }

                        // 5. Total Posts
                        item {
                            AdminMetricCard(
                                title = "Total Posts",
                                count = "$totalPostsCount",
                                subtitle = "Feed publications",
                                icon = Icons.Default.PostAdd,
                                iconBg = Color(0xFFEDE7F6),
                                iconTint = Color(0xFF673AB7)
                            )
                        }

                        // 6. Total Comments
                        item {
                            AdminMetricCard(
                                title = "Total Comments",
                                count = "$totalCommentsCount",
                                subtitle = "Post discussions",
                                icon = Icons.Default.Comment,
                                iconBg = Color(0xFFFFF3E0),
                                iconTint = Color(0xFFFF9800)
                            )
                        }

                        // 7. Total Messages Exchanged
                        item {
                            AdminMetricCard(
                                title = "Total Messages",
                                count = "$totalMessages",
                                subtitle = "Chats exchanged",
                                icon = Icons.Default.Chat,
                                iconBg = Color(0xFFE0F7FA),
                                iconTint = Color(0xFF00ACC1)
                            )
                        }

                        // 8. Total Image Posts
                        item {
                            AdminMetricCard(
                                title = "Image Posts",
                                count = "$totalImagePostsCount",
                                subtitle = "Photos published",
                                icon = Icons.Default.Image,
                                iconBg = Color(0xFFFCE4EC),
                                iconTint = Color(0xFFE91E63)
                            )
                        }

                        // 9. Total Video Posts
                        item {
                            AdminMetricCard(
                                title = "Video Posts",
                                count = "$totalVideoPostsCount",
                                subtitle = "Videos & Reels",
                                icon = Icons.Default.Videocam,
                                iconBg = Color(0xFFE1F5FE),
                                iconTint = Color(0xFF03A9F4)
                            )
                        }

                        // 10. Monetized Users
                        item {
                            AdminMetricCard(
                                title = "Monetized Users",
                                count = "$monetizedUsersCount",
                                subtitle = "Creator Fund active",
                                icon = Icons.Default.MonetizationOn,
                                iconBg = Color(0xFFFFF8E1),
                                iconTint = Color(0xFFFFA000),
                                onClick = { currentAdminScreen = AdminActiveScreen.MONETIZATION_REQUESTS }
                            )
                        }

                        // Section 3: Financial & Pending Requests
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "Requests & Wallet Pipelines",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505),
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }

                        // 11. Pending Deposits
                        item {
                            AdminMetricCard(
                                title = "Pending Deposits",
                                count = "$pendingDepositsCount",
                                subtitle = "৳ ${String.format(Locale.US, "%.0f", totalPendingDepositAmount)} BDT",
                                icon = Icons.Default.AccountBalanceWallet,
                                iconBg = Color(0xFFE8F5E9),
                                iconTint = Color(0xFF2E7D32),
                                badgeAlert = pendingDepositsCount > 0,
                                onClick = { currentAdminScreen = AdminActiveScreen.DEPOSIT_REQUESTS }
                            )
                        }

                        // 12. Pending Withdraws
                        item {
                            AdminMetricCard(
                                title = "Pending Withdraws",
                                count = "$pendingWithdrawsCount",
                                subtitle = "৳ ${String.format(Locale.US, "%.0f", totalPendingWithdrawAmount)} BDT",
                                icon = Icons.Default.AccountBalance,
                                iconBg = Color(0xFFFFEBEE),
                                iconTint = Color(0xFFC62828),
                                badgeAlert = pendingWithdrawsCount > 0,
                                onClick = { currentAdminScreen = AdminActiveScreen.WITHDRAW_REQUESTS }
                            )
                        }

                        // 13. Pending Verifications
                        item {
                            AdminMetricCard(
                                title = "Badge Requests",
                                count = "$pendingVerificationsCount",
                                subtitle = "Pending review",
                                icon = Icons.Default.Verified,
                                iconBg = Color(0xFFE8F5E9),
                                iconTint = Color(0xFF008937),
                                badgeAlert = pendingVerificationsCount > 0,
                                onClick = { currentAdminScreen = AdminActiveScreen.VERIFICATION_REQUESTS }
                            )
                        }

                        // 14. Pending Monetization
                        item {
                            AdminMetricCard(
                                title = "Monetization Req",
                                count = "$pendingMonetizationsCount",
                                subtitle = "Pending creator apps",
                                icon = Icons.Default.MonetizationOn,
                                iconBg = Color(0xFFFFF8E1),
                                iconTint = Color(0xFFE65100),
                                badgeAlert = pendingMonetizationsCount > 0,
                                onClick = { currentAdminScreen = AdminActiveScreen.MONETIZATION_REQUESTS }
                            )
                        }

                        // ==========================================
                        // 3 DISTINCT REAL DATA GRAPHS / CHARTS
                        // ==========================================
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "Real-Time Performance Graphs",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF050505),
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }

                        // GRAPH 1: User Base & Community Distribution (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            AdminUserDistributionGraph(
                                totalUsers = totalUsersCount,
                                activeUsers = activeUsersCount,
                                verifiedUsers = verifiedUsersCount,
                                blockedUsers = blockedUsersCount,
                                monetizedUsers = monetizedUsersCount
                            )
                        }

                        // GRAPH 2: Content & Media Breakdown (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            AdminContentMediaGraph(
                                imagePosts = totalImagePostsCount,
                                videoPosts = totalVideoPostsCount,
                                textPosts = totalTextPostsCount,
                                comments = totalCommentsCount,
                                messages = totalMessages
                            )
                        }

                        // GRAPH 3: Financial & Pipeline Activity (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            AdminFinancialPipelineGraph(
                                pendingDeposits = pendingDepositsCount,
                                pendingWithdraws = pendingWithdrawsCount,
                                pendingVerifications = pendingVerificationsCount,
                                pendingMonetizations = pendingMonetizationsCount,
                                totalDeposits = depositRequests.size,
                                totalWithdraws = withdrawRequests.size
                            )
                        }

                        item(span = { GridItemSpan(2) }) {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    count: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    badgeAlert: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconBg,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (badgeAlert) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE53935),
                        modifier = Modifier.size(10.dp)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = count,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF050505)
            )

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF050505),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF65676B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// GRAPH 1: USER COMMUNITY DISTRIBUTION
// ==========================================
@Composable
fun AdminUserDistributionGraph(
    totalUsers: Int,
    activeUsers: Int,
    verifiedUsers: Int,
    blockedUsers: Int,
    monetizedUsers: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "1. User Community Breakdown",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                    Text(
                        text = "Distribution across total $totalUsers registered users",
                        fontSize = 12.sp,
                        color = Color(0xFF65676B)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE7F3FF)
                ) {
                    Text(
                        text = "LIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1877F2),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = maxOf(totalUsers, 1).toFloat()

            // Custom Canvas Chart with rounded bars
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val barSpacing = size.width / 5f
                val barWidth = barSpacing * 0.5f

                val items = listOf(
                    Triple("Total", totalUsers, Color(0xFF1877F2)),
                    Triple("Active", activeUsers, Color(0xFF00C853)),
                    Triple("Verified", verifiedUsers, Color(0xFF008937)),
                    Triple("Monetized", monetizedUsers, Color(0xFFFFA000)),
                    Triple("Blocked", blockedUsers, Color(0xFFD32F2F))
                )

                // Background horizontal guide lines
                val lines = 3
                for (i in 0..lines) {
                    val y = size.height * (i.toFloat() / lines)
                    drawLine(
                        color = Color(0xFFF0F2F5),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                items.forEachIndexed { index, (label, value, color) ->
                    val x = index * barSpacing + (barSpacing - barWidth) / 2f
                    val heightRatio = if (maxVal > 0) (value.toFloat() / maxVal).coerceIn(0.08f, 1f) else 0.08f
                    val barHeight = (size.height - 20.dp.toPx()) * heightRatio
                    val y = size.height - barHeight

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend & Values Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatBadgeItem(label = "Total", value = "$totalUsers", color = Color(0xFF1877F2))
                StatBadgeItem(label = "Active", value = "$activeUsers", color = Color(0xFF00C853))
                StatBadgeItem(label = "Verified", value = "$verifiedUsers", color = Color(0xFF008937))
                StatBadgeItem(label = "Monetized", value = "$monetizedUsers", color = Color(0xFFFFA000))
                StatBadgeItem(label = "Blocked", value = "$blockedUsers", color = Color(0xFFD32F2F))
            }
        }
    }
}

// ==========================================
// GRAPH 2: CONTENT & MEDIA ENGAGEMENT GRAPH
// ==========================================
@Composable
fun AdminContentMediaGraph(
    imagePosts: Int,
    videoPosts: Int,
    textPosts: Int,
    comments: Int,
    messages: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "2. Media & Social Traffic",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                    Text(
                        text = "Images, Videos, Texts, Comments & Messages",
                        fontSize = 12.sp,
                        color = Color(0xFF65676B)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEDE7F6)
                ) {
                    Text(
                        text = "REAL-TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF673AB7),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxCount = maxOf(imagePosts, videoPosts, textPosts, comments, messages, 1).toFloat()

            val rows = listOf(
                Pair("Photos ($imagePosts)", Triple(imagePosts, Color(0xFFE91E63), Color(0xFFFCE4EC))),
                Pair("Videos ($videoPosts)", Triple(videoPosts, Color(0xFF03A9F4), Color(0xFFE1F5FE))),
                Pair("Texts ($textPosts)", Triple(textPosts, Color(0xFF9C27B0), Color(0xFFF3E5F5))),
                Pair("Comments ($comments)", Triple(comments, Color(0xFFFF9800), Color(0xFFFFF3E0))),
                Pair("Messages ($messages)", Triple(messages, Color(0xFF00BCD4), Color(0xFFE0F7FA)))
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { (label, data) ->
                    val (valCount, color, bg) = data
                    val ratio = (valCount.toFloat() / maxCount).coerceIn(0.04f, 1f)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF050505))
                            Text(text = "$valCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(bg)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// GRAPH 3: FINANCIAL & VERIFICATION PIPELINE
// ==========================================
@Composable
fun AdminFinancialPipelineGraph(
    pendingDeposits: Int,
    pendingWithdraws: Int,
    pendingVerifications: Int,
    pendingMonetizations: Int,
    totalDeposits: Int,
    totalWithdraws: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "3. Financial & Request Pipelines",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                    Text(
                        text = "Pending deposits, withdrawals & verification queues",
                        fontSize = 12.sp,
                        color = Color(0xFF65676B)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = "PIPELINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxRequests = maxOf(pendingDeposits, pendingWithdraws, pendingVerifications, pendingMonetizations, 1).toFloat()

            // Custom multi-column pipeline canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                val colWidth = size.width / 4f
                val barW = colWidth * 0.55f

                val pipelines = listOf(
                    Triple("Deposits", pendingDeposits, Color(0xFF2E7D32)),
                    Triple("Withdraws", pendingWithdraws, Color(0xFFC62828)),
                    Triple("Badge Req", pendingVerifications, Color(0xFF008937)),
                    Triple("Monetize", pendingMonetizations, Color(0xFFFFA000))
                )

                pipelines.forEachIndexed { i, (name, valCount, color) ->
                    val x = i * colWidth + (colWidth - barW) / 2f
                    val heightRatio = if (maxRequests > 0) (valCount.toFloat() / maxRequests).coerceIn(0.1f, 1f) else 0.1f
                    val barH = (size.height - 15.dp.toPx()) * heightRatio
                    val y = size.height - barH

                    // Draw bar
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatBadgeItem(label = "Deposits", value = "$pendingDeposits Pending", color = Color(0xFF2E7D32))
                StatBadgeItem(label = "Withdraws", value = "$pendingWithdraws Pending", color = Color(0xFFC62828))
                StatBadgeItem(label = "Badge", value = "$pendingVerifications Pending", color = Color(0xFF008937))
                StatBadgeItem(label = "Monetize", value = "$pendingMonetizations Pending", color = Color(0xFFFFA000))
            }
        }
    }
}

@Composable
fun StatBadgeItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(8.dp)
        ) {}
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF050505))
        Text(text = label, fontSize = 10.sp, color = Color(0xFF65676B))
    }
}
