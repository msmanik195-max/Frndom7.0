package com.example.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.AppSettingsRepository
import com.example.ui.menu.admin.AdminDashboardView
import com.example.ui.menu.admin.AdminDepositRequestsView
import com.example.ui.menu.admin.AdminMonetizationRequestsView
import com.example.ui.menu.admin.AdminPaymentMethodsView
import com.example.ui.menu.admin.AdminVerificationRequestsView
import com.example.ui.menu.admin.AdminWithdrawRequestsView

enum class SettingsSubScreen {
    MAIN,
    ADMIN_DASHBOARD,
    ADMIN_DEPOSIT_REQUESTS,
    ADMIN_WITHDRAW_REQUESTS,
    ADMIN_MONETIZATION_REQUESTS,
    ADMIN_VERIFICATION_REQUESTS,
    ADMIN_PAYMENT_METHODS
}

@Composable
fun SettingsView(
    userProfile: UserProfile?,
    onServerSettingsClick: () -> Unit,
    onVerificationBadgeClick: () -> Unit = {},
    onLogoutClick: () -> Unit,
    onBack: () -> Unit,
    onDepositRequestsClick: (() -> Unit)? = null,
    onWithdrawRequestsClick: (() -> Unit)? = null,
    onMonetizationRequestsClick: (() -> Unit)? = null,
    onVerificationRequestsClick: (() -> Unit)? = null,
    onPaymentMethodsClick: (() -> Unit)? = null,
    onAdminDashboardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appSettingsRepository = remember { AppSettingsRepository.getInstance(context) }
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }

    val autoPlayVideos by appSettingsRepository.autoPlayVideos.collectAsState()
    val notificationsEnabled by appSettingsRepository.notificationsEnabled.collectAsState()
    val dataSaverEnabled by appSettingsRepository.dataSaverEnabled.collectAsState()

    val depositRequests by adminRepo.depositRequestsFlow.collectAsState()
    val withdrawRequests by adminRepo.withdrawRequestsFlow.collectAsState()
    val monetizationRequests by adminRepo.monetizationRequestsFlow.collectAsState()
    val verificationRequests by adminRepo.verificationRequestsFlow.collectAsState()

    val pendingDepositsCount = remember(depositRequests) { depositRequests.count { it.status == "PENDING" } }
    val pendingWithdrawsCount = remember(withdrawRequests) { withdrawRequests.count { it.status == "PENDING" } }
    val pendingMonetizationCount = remember(monetizationRequests) { monetizationRequests.count { it.status == "PENDING" } }
    val pendingVerificationCount = remember(verificationRequests) { verificationRequests.count { it.status == "PENDING" } }
    val totalAdminPendingCount = pendingDepositsCount + pendingWithdrawsCount + pendingMonetizationCount + pendingVerificationCount

    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
    }

    when (currentSubScreen) {
        SettingsSubScreen.ADMIN_DASHBOARD -> {
            AdminDashboardView(
                onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                onServerSettingsClick = onServerSettingsClick,
                modifier = modifier
            )
            return
        }
        SettingsSubScreen.ADMIN_DEPOSIT_REQUESTS -> {
            AdminDepositRequestsView(
                onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
            return
        }
        SettingsSubScreen.ADMIN_WITHDRAW_REQUESTS -> {
            AdminWithdrawRequestsView(
                onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
            return
        }
        SettingsSubScreen.ADMIN_MONETIZATION_REQUESTS -> {
            AdminMonetizationRequestsView(
                onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
            return
        }
        SettingsSubScreen.ADMIN_VERIFICATION_REQUESTS -> {
            AdminVerificationRequestsView(
                onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
            return
        }
        SettingsSubScreen.ADMIN_PAYMENT_METHODS -> {
            AdminPaymentMethodsView(
                onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
            return
        }
        SettingsSubScreen.MAIN -> {
            // Render main settings list
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("settings_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Settings & Privacy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )
            }

            Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. ADMIN DASHBOARD CARD AT THE VERY TOP
                Text(
                    text = "Administration",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (onAdminDashboardClick != null) {
                                onAdminDashboardClick()
                            } else {
                                currentSubScreen = SettingsSubScreen.ADMIN_DASHBOARD
                            }
                        }
                        .testTag("admin_dashboard_settings_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1877F2),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Admin Dashboard",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Admin Dashboard",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (totalAdminPendingCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE53935)
                                    ) {
                                        Text(
                                            text = "$totalAdminPendingCount Pending",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Live metrics, 3 graphs, user management & request controls",
                                fontSize = 12.sp,
                                color = Color(0xFF65676B),
                                lineHeight = 16.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Dashboard",
                            tint = Color(0xFF1877F2)
                        )
                    }
                }

                // 2. Account Center Card
                Text(
                    text = "Account Center",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Account Center",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                        Text(
                            text = "Manage your connected experiences and account settings.",
                            fontSize = 12.sp,
                            color = Color(0xFF65676B)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                        SettingsRow(
                            icon = Icons.Default.Person,
                            title = "Personal Details",
                            subtitle = userProfile?.email?.ifBlank { "Manage contact info" } ?: "Manage contact info",
                            onClick = {}
                        )

                        SettingsRow(
                            icon = Icons.Default.Lock,
                            title = "Password & Security",
                            subtitle = "Change password, two-factor authentication",
                            onClick = {}
                        )

                        Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                        // Verification Badge Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onVerificationBadgeClick)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_verified_badge_green),
                                        contentDescription = "Verification Badge",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Verification Badge",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF050505)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (userProfile?.isVerificationActive() == true) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF00C853).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Active",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF008937),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (userProfile?.isVerificationActive() == true) "Manage or extend your Green Verification Badge" else "Subscribe to get Green Badge & boost credibility",
                                    fontSize = 12.sp,
                                    color = Color(0xFF65676B)
                                )
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFB0B3B8))
                        }
                    }
                }

                // 3. Preferences
                Text(
                    text = "Preferences",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        // Auto-play Videos Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.padding(end = 8.dp)) {
                                    Text(
                                        text = "Auto-play Videos",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF050505)
                                    )
                                    Text(
                                        text = "Automatically play videos on Home feed",
                                        fontSize = 12.sp,
                                        color = Color(0xFF65676B),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            Switch(
                                checked = autoPlayVideos,
                                onCheckedChange = { appSettingsRepository.setAutoPlayVideos(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1877F2)
                                )
                            )
                        }

                        Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF65676B),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Push Notifications",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF050505)
                                    )
                                    Text(
                                        text = "Likes, comments, and follower alerts",
                                        fontSize = 12.sp,
                                        color = Color(0xFF65676B)
                                    )
                                }
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { appSettingsRepository.setNotificationsEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1877F2))
                            )
                        }

                        Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF65676B),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Data Saver",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF050505)
                                    )
                                    Text(
                                        text = "Optimize video and image quality",
                                        fontSize = 12.sp,
                                        color = Color(0xFF65676B)
                                    )
                                }
                            }
                            Switch(
                                checked = dataSaverEnabled,
                                onCheckedChange = { appSettingsRepository.setDataSaverEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1877F2))
                            )
                        }
                    }
                }

                // Log out
                Button(
                    onClick = onLogoutClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFD32F2F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Log Out of Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun AdminSettingsRow(
    icon: ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    badgeCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconBgColor,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF050505)
                )
                if (badgeCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFD32F2F)
                    ) {
                        Text(
                            text = "$badgeCount New",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF65676B), modifier = Modifier.padding(top = 1.dp))
            }
        }

        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFB0B3B8))
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = Color(0xFF65676B)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF050505))
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF65676B))
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFB0B3B8))
    }
}
