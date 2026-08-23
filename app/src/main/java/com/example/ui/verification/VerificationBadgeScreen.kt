package com.example.ui.verification

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.ui.components.VerificationBadge
import com.example.ui.menu.DepositScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VerificationPlan(
    val id: String,
    val title: String,
    val durationText: String,
    val durationDays: Int,
    val price: Double,
    val tag: String? = null,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationBadgeScreen(
    userProfile: UserProfile?,
    userRepository: UserRepository,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit = onBack,
    onNavigateToDeposit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val walletRepo = remember { WalletRepository.getInstance(context) }
    val walletBalance by walletRepo.balanceFlow.collectAsState()

    var showDepositScreen by remember { mutableStateOf(false) }
    var showInsufficientBalanceDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showDepositScreen) {
        showDepositScreen = false
    }

    if (showDepositScreen) {
        DepositScreen(
            onBack = { showDepositScreen = false },
            onDepositSuccess = { showDepositScreen = false },
            modifier = modifier
        )
        return
    }

    // Realtime live profile flow if available
    val profileFlow = remember(userProfile?.uid) {
        userProfile?.uid?.let { userRepository.getUserProfileFlow(it) }
    }
    val liveProfile by (profileFlow?.collectAsState(initial = userProfile) ?: remember { mutableStateOf(userProfile) })
    val activeProfile = liveProfile ?: userProfile ?: UserProfile()

    val isBadgeActive = activeProfile.isVerificationActive()

    val plans = remember {
        listOf(
            VerificationPlan(
                id = "plan_1_month",
                title = "1 Month",
                durationText = "30 Days Validity",
                durationDays = 30,
                price = 99.0,
                tag = "Starter",
                description = "Perfect to try out green badge benefits & trust"
            ),
            VerificationPlan(
                id = "plan_6_months",
                title = "6 Months",
                durationText = "180 Days Validity",
                durationDays = 180,
                price = 499.0,
                tag = "Popular • Save 16%",
                description = "Great value for active creators & sellers"
            ),
            VerificationPlan(
                id = "plan_12_months",
                title = "12 Months (1 Year)",
                durationText = "365 Days Validity",
                durationDays = 365,
                price = 999.0,
                tag = "Best Value • Save 20%",
                description = "Maximum savings with full year peace of mind"
            )
        )
    }

    var selectedPlanIndex by remember { mutableIntStateOf(1) } // Default 6 months
    var isProcessingPurchase by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showThankYouDialog by remember { mutableStateOf(false) }
    var purchasedPlan by remember { mutableStateOf<VerificationPlan?>(null) }
    var newExpiryDateString by remember { mutableStateOf("") }

    val displayName = when {
        activeProfile.fullName.isNotBlank() -> activeProfile.fullName
        activeProfile.firstName.isNotBlank() || activeProfile.lastName.isNotBlank() ->
            "${activeProfile.firstName} ${activeProfile.lastName}".trim()
        else -> "User"
    }

    val primaryGreen = Color(0xFF00C853)
    val darkGreen = Color(0xFF008937)
    val lightGreenBg = Color(0xFFE8F5E9)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("verification_badge_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF050505)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verification Badge",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Header Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFE8F5E9), Color.White)
                                    )
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Large Animated Verified Badge Icon
                                Surface(
                                    shape = CircleShape,
                                    color = primaryGreen.copy(alpha = 0.12f),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_verified_badge_green),
                                            contentDescription = "Green Verified Badge",
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Get Verified with Green Badge",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505),
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Build instant credibility, stand out in conversations, and unlock priority visibility across feed, marketplace, and comments.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF65676B),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )

                                if (isBadgeActive) {
                                    val sdf = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
                                    val dateStr = if (activeProfile.verifiedUntil > 0) {
                                        sdf.format(Date(activeProfile.verifiedUntil))
                                    } else "Lifetime"
                                    val remainingDays = activeProfile.getRemainingDays()
                                    val planName = if (activeProfile.verificationPlanTitle.isNotBlank()) {
                                        activeProfile.verificationPlanTitle
                                    } else {
                                        "Green Badge Subscription"
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryGreen)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = darkGreen,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Active Subscription",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = darkGreen
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = primaryGreen
                                                ) {
                                                    Text(
                                                        text = "$remainingDays Days Left",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            Divider(color = primaryGreen.copy(alpha = 0.3f), thickness = 0.8.dp)

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Plan:",
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF4A4D50),
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = planName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF050505)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Validity Remaining:",
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF4A4D50),
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "$remainingDays days",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = darkGreen
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Expires On:",
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF4A4D50),
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = dateStr,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF050505)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. LIVE PROFILE PREVIEW CARD (With Cover, Photo & Green Badge)
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = primaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Profile Preview",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505)
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Cover + Avatar Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                ) {
                                    // Cover Photo
                                    if (activeProfile.coverPictureUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = activeProfile.coverPictureUrl,
                                            contentDescription = "Cover",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(125.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(125.dp)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFF2E7D32), Color(0xFF00C853))
                                                    )
                                                )
                                        )
                                    }

                                    // Preview Badge Overlay Tag
                                    Surface(
                                        shape = RoundedCornerShape(bottomStart = 8.dp),
                                        color = Color(0xCC000000),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text(
                                            text = "Preview with Badge",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Profile Avatar with Border
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 0.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .size(86.dp)
                                                .border(3.dp, Color.White, CircleShape)
                                                .shadow(4.dp, CircleShape),
                                            shape = CircleShape,
                                            color = Color(0xFFE4E6EB)
                                        ) {
                                            if (activeProfile.profilePictureUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = activeProfile.profilePictureUrl,
                                                    contentDescription = displayName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = displayName.firstOrNull()?.uppercase() ?: "U",
                                                        fontSize = 32.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0866FF)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Name with Green Badge Beside it
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = displayName,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF050505),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        // Green Verified Badge
                                        VerificationBadge(
                                            size = 20.dp,
                                            show = true
                                        )
                                    }

                                    if (activeProfile.bio.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = activeProfile.bio,
                                            fontSize = 13.sp,
                                            color = Color(0xFF65676B),
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${activeProfile.followersCount} followers",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF65676B)
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 13.sp,
                                            color = Color(0xFF65676B)
                                        )
                                        Text(
                                            text = "${activeProfile.followingCount} following",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF65676B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Verification Benefits List
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "What's Included",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                BenefitItem(
                                    icon = Icons.Default.VerifiedUser,
                                    iconColor = primaryGreen,
                                    title = "Exclusive Green Verification Badge",
                                    description = "Your audience knows that you are the real, authentic you with a verified seal on your profile, posts, and comments."
                                )

                                Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                                BenefitItem(
                                    icon = Icons.Default.TrendingUp,
                                    iconColor = Color(0xFF1877F2),
                                    title = "Prioritized Search & Feed Prominence",
                                    description = "Your posts, reels, marketplace listings, and comments are ranked higher and displayed with high visibility."
                                )

                                Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                                BenefitItem(
                                    icon = Icons.Default.Security,
                                    iconColor = Color(0xFFE65100),
                                    title = "Proactive Account Protection",
                                    description = "Advanced identity defense and impersonation monitoring designed to protect your creator reputation."
                                )

                                Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                                BenefitItem(
                                    icon = Icons.Default.Star,
                                    iconColor = Color(0xFF8E24AA),
                                    title = "Increased Trust on Marketplace",
                                    description = "Buyers and sellers trust verified accounts much more, boosting transaction speed and inquiry response rates."
                                )
                            }
                        }
                    }
                }

                // 3.5 WALLET BALANCE STATUS CARD
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE8F1FD),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color(0xFF0B5ED7),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Wallet Balance",
                                        fontSize = 12.sp,
                                        color = Color(0xFF65676B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "৳ ${String.format(Locale.US, "%.2f", walletBalance)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF050505)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (onNavigateToDeposit != null) {
                                        onNavigateToDeposit()
                                    } else {
                                        showDepositScreen = true
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0B5ED7),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("verification_deposit_shortcut_button")
                            ) {
                                Text(
                                    text = "Deposit Funds",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 4. CHOOSE YOUR PACKAGE (3 Packages strictly in ৳ BDT)
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Select a Verification Plan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                        Text(
                            text = "Special Promotional Offer • Instant Activation",
                            fontSize = 12.sp,
                            color = primaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        plans.forEachIndexed { index, plan ->
                            val isSelected = selectedPlanIndex == index

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlanIndex = index }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) primaryGreen else Color(0xFFE4E6EB),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFF1F8E9) else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Radio Indicator
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) primaryGreen else Color.Transparent,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = 2.dp,
                                                    color = if (isSelected) primaryGreen else Color(0xFFB0B3B8)
                                                ),
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                if (isSelected) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(
                                                    text = plan.title,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF050505)
                                                )
                                                Text(
                                                    text = plan.durationText,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF65676B)
                                                )
                                            }
                                        }

                                        // Price in BDT (৳)
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "৳ ${plan.price.toInt()}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) darkGreen else Color(0xFF050505)
                                            )
                                            if (plan.tag != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = primaryGreen.copy(alpha = 0.15f),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    Text(
                                                        text = plan.tag,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = darkGreen,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = plan.description,
                                        fontSize = 12.sp,
                                        color = Color(0xFF65676B),
                                        modifier = Modifier.padding(start = 32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // 5. BUY / ACTIVATE BUTTON
                item {
                    val currentSelectedPlan = plans.getOrNull(selectedPlanIndex) ?: plans[0]

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (activeProfile.uid.isBlank()) {
                                    Toast.makeText(context, "Please log in first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (walletBalance < currentSelectedPlan.price) {
                                    showInsufficientBalanceDialog = true
                                } else {
                                    showConfirmDialog = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("buy_verification_badge_button"),
                            enabled = !isProcessingPurchase
                        ) {
                            if (isProcessingPurchase) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Activating...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_verified_badge_green),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBadgeActive) {
                                        "Extend Verification (৳ ${currentSelectedPlan.price.toInt()})"
                                    } else {
                                        "Buy Verification Badge (৳ ${currentSelectedPlan.price.toInt()})"
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Instant Badge Activation • Green Badge will appear next to your name everywhere",
                            fontSize = 11.sp,
                            color = Color(0xFF65676B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // 5.4 INSUFFICIENT BALANCE DIALOG
    if (showInsufficientBalanceDialog) {
        val selectedPlan = plans.getOrNull(selectedPlanIndex) ?: plans[0]
        val deficit = selectedPlan.price - walletBalance

        AlertDialog(
            onDismissRequest = { showInsufficientBalanceDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Insufficient Wallet Balance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF050505)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "You do not have enough funds in your wallet to activate the Verification Badge. Please deposit funds.",
                        fontSize = 14.sp,
                        color = Color(0xFF65676B),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0F2F5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Package Price:", fontSize = 13.sp, color = Color(0xFF65676B))
                                Text(
                                    text = "৳ ${selectedPlan.price.toInt()}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Current Balance:", fontSize = 13.sp, color = Color(0xFF65676B))
                                Text(
                                    text = "৳ ${String.format(Locale.US, "%.2f", walletBalance)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                            Divider(thickness = 0.5.dp, color = Color(0xFFD0D2D6))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Required Deposit:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    text = "৳ ${String.format(Locale.US, "%.2f", if (deficit > 0) deficit else 0.0)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInsufficientBalanceDialog = false
                        if (onNavigateToDeposit != null) {
                            onNavigateToDeposit()
                        } else {
                            showDepositScreen = true
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B5ED7),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("insufficient_balance_deposit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Deposit Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showInsufficientBalanceDialog = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("insufficient_balance_cancel_button")
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF65676B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    // 5.5 PURCHASE CONFIRMATION DIALOG
    if (showConfirmDialog) {
        val selectedPlan = plans.getOrNull(selectedPlanIndex) ?: plans[0]
        AlertDialog(
            onDismissRequest = {
                if (!isProcessingPurchase) showConfirmDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_verified_badge_green),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBadgeActive) "Confirm Extension" else "Confirm Verification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF050505)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "৳ ${selectedPlan.price.toInt()} will be deducted from your wallet to activate the Green Verification Badge.",
                        fontSize = 14.sp,
                        color = Color(0xFF050505),
                        lineHeight = 20.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Package:", fontSize = 13.sp, color = Color(0xFF008937), fontWeight = FontWeight.Medium)
                                Text(text = selectedPlan.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF008937))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Price to Deduct:", fontSize = 13.sp, color = Color(0xFF008937), fontWeight = FontWeight.Medium)
                                Text(text = "৳ ${selectedPlan.price.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF008937))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Validity:", fontSize = 13.sp, color = Color(0xFF008937), fontWeight = FontWeight.Medium)
                                Text(text = selectedPlan.durationText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF008937))
                            }
                            Divider(thickness = 0.5.dp, color = Color(0xFFA5D6A7), modifier = Modifier.padding(vertical = 2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Wallet Balance After:", fontSize = 12.sp, color = Color(0xFF2E7D32))
                                Text(
                                    text = "৳ ${String.format(Locale.US, "%.2f", walletBalance - selectedPlan.price)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Once confirmed, the amount will be deducted and sent for verification. Upon admin approval, the Green Badge will be activated beside your profile name. If rejected, the full amount will be refunded to your wallet.",
                        fontSize = 12.sp,
                        color = Color(0xFF65676B),
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (walletBalance < selectedPlan.price) {
                            showConfirmDialog = false
                            showInsufficientBalanceDialog = true
                            return@Button
                        }

                        isProcessingPurchase = true
                        coroutineScope.launch {
                            val deducted = walletRepo.deduct(
                                amount = selectedPlan.price,
                                title = "Verification Badge Request",
                                subtitle = "Green Badge - ${selectedPlan.title} (${selectedPlan.durationDays} Days)"
                            )

                            if (!deducted) {
                                isProcessingPurchase = false
                                showConfirmDialog = false
                                showInsufficientBalanceDialog = true
                                return@launch
                            }

                            val estimatedExpiryTimestamp = System.currentTimeMillis() + (selectedPlan.durationDays.toLong() * 24L * 60L * 60L * 1000L)

                            try {
                                AdminRequestRepository.getInstance(context).submitVerificationRequest(
                                    userId = activeProfile.uid,
                                    userName = activeProfile.fullName.ifBlank { "${activeProfile.firstName} ${activeProfile.lastName}".trim() },
                                    userEmail = activeProfile.email,
                                    userPhone = activeProfile.phoneNumber,
                                    planTitle = selectedPlan.title,
                                    durationDays = selectedPlan.durationDays,
                                    price = selectedPlan.price
                                )

                                val sdf = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
                                newExpiryDateString = sdf.format(Date(estimatedExpiryTimestamp))
                                purchasedPlan = selectedPlan
                                isProcessingPurchase = false
                                showConfirmDialog = false
                                showThankYouDialog = true
                            } catch (e: Exception) {
                                isProcessingPurchase = false
                                showConfirmDialog = false
                                // Refund in case of submission failure
                                walletRepo.recharge(selectedPlan.price, "Refund - Verification Submission Failed")
                                Toast.makeText(context, "Failed to submit verification request: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryGreen,
                        contentColor = Color.White
                    ),
                    enabled = !isProcessingPurchase,
                    modifier = Modifier.testTag("confirm_purchase_dialog_button")
                ) {
                    if (isProcessingPurchase) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Processing...", fontSize = 13.sp)
                    } else {
                        Text(text = "Confirm & Deduct", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false },
                    enabled = !isProcessingPurchase,
                    modifier = Modifier.testTag("cancel_purchase_dialog_button")
                ) {
                    Text(text = "Cancel", color = Color(0xFF65676B), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    // 6. CELEBRATION / THANK YOU MODAL DIALOG
    if (showThankYouDialog && purchasedPlan != null) {
        Dialog(onDismissRequest = { showThankYouDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Big Badge Animation Card
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_verified_badge_green),
                                contentDescription = "Green Badge",
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Text(
                        text = "Request Submitted!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF050505)
                    )

                    Text(
                        text = "Your verification request has been submitted to Admin. Once approved, your Green Badge will be activated and displayed beside your name. If rejected, your balance will be automatically refunded.",
                        fontSize = 13.sp,
                        color = Color(0xFF65676B),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    // Details Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0F2F5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Plan Selected:", fontSize = 13.sp, color = Color(0xFF65676B))
                                Text(
                                    text = purchasedPlan!!.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Price:", fontSize = 13.sp, color = Color(0xFF65676B))
                                Text(
                                    text = "৳ ${purchasedPlan!!.price.toInt()} BDT",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = darkGreen
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Valid Until:", fontSize = 13.sp, color = Color(0xFF65676B))
                                Text(
                                    text = newExpiryDateString,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = darkGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            showThankYouDialog = false
                            onNavigateToProfile()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("view_verified_profile_button")
                    ) {
                        Text(text = "View Your Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showThankYouDialog = false },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE4E6EB),
                            contentColor = Color(0xFF050505)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(text = "Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF050505)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF65676B),
                lineHeight = 16.sp
            )
        }
    }
}
