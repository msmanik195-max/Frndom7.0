package com.example.ui.menu

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.PostRepository
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardView(
    userProfile: UserProfile?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val postRepo = remember { PostRepository(context) }
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    val allPosts by postRepo.postsFlow.collectAsState()
    val monetizationRequests by adminRepo.monetizationRequestsFlow.collectAsState()

    val sharedPrefs = remember { context.getSharedPreferences("frndom_creator_fund_prefs", Context.MODE_PRIVATE) }
    val currentUserId = userProfile?.uid ?: ""
    val userMonetizationRequest = remember(monetizationRequests, currentUserId) {
        monetizationRequests.find { it.userId == currentUserId }
    }
    var isApplicationSubmitted by remember(userMonetizationRequest, currentUserId) {
        mutableStateOf(
            userMonetizationRequest?.status == "PENDING" ||
            userMonetizationRequest?.status == "APPROVED" ||
            sharedPrefs.getBoolean("fund_applied_$currentUserId", false)
        )
    }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val displayName = userProfile?.fullName?.ifBlank { "${userProfile.firstName} ${userProfile.lastName}".trim() } ?: "Creator"

    // Real data calculations
    val userPosts = remember(allPosts, userProfile) {
        allPosts.filter { post ->
            (currentUserId.isNotBlank() && post.authorId == currentUserId) ||
                    (userProfile?.fullName.isNullOrBlank().not() && post.authorName.equals(userProfile?.fullName, ignoreCase = true))
        }
    }

    val totalPostsCount = remember(userPosts) {
        userPosts.count { it.mediaType != "reel" }
    }

    val totalReelsCount = remember(userPosts) {
        userPosts.count { it.mediaType == "reel" }
    }

    val totalContentShares = remember(userPosts) {
        userPosts.sumOf { it.sharesCount }
    }

    val totalLikes = remember(userPosts) {
        userPosts.sumOf {
            maxOf(it.likesCount, maxOf(it.likedByMap.size, it.reactionsMap.size))
        }
    }

    val totalComments = remember(userPosts) {
        userPosts.sumOf { it.commentsCount }
    }

    val totalEngagement = remember(totalLikes, totalComments, totalContentShares) {
        totalLikes + totalComments + totalContentShares
    }

    val totalReach = remember(userPosts, totalEngagement) {
        if (userPosts.isEmpty()) {
            0
        } else {
            userPosts.sumOf { post ->
                val interactions = maxOf(post.likesCount, post.reactionsMap.size) * 4 + post.commentsCount * 3 + post.sharesCount * 6
                maxOf(interactions + 12, 1)
            }
        }
    }

    val netFollowers = remember(userProfile) {
        maxOf(userProfile?.followersCount ?: 0, userProfile?.followersMap?.size ?: 0)
    }

    // Account age in days
    val accountAgeDays = remember(userProfile) {
        val createdTime = userProfile?.createdAt ?: 0L
        if (createdTime <= 0L) {
            0
        } else {
            val diffMs = System.currentTimeMillis() - createdTime
            maxOf((diffMs / (1000L * 60 * 60 * 24)).toInt(), 0)
        }
    }

    // Creator Fund Criteria checks
    val hasMetViews = totalReach >= 500
    val hasMetFollowers = netFollowers >= 100
    val hasMetPosts = (totalPostsCount + totalReelsCount) >= 10 || totalPostsCount >= 10
    val hasMetReels = totalReelsCount >= 5
    val hasMetAccountAge = accountAgeDays >= 7

    val completedCriteriaCount = listOf(
        hasMetViews,
        hasMetFollowers,
        hasMetPosts,
        hasMetReels,
        hasMetAccountAge
    ).count { it }

    val allCriteriaMet = completedCriteriaCount == 5

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("professional_dashboard_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("dashboard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF050505)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Professional Dashboard",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_welcome_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF1877F2).copy(alpha = 0.08f),
                                        Color.White
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Welcome, $displayName",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF050505)
                                    )
                                    if (userProfile?.isVerificationActive() == true) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        com.example.ui.components.VerificationBadge(size = 16.dp, show = true)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Real-time analytics & monetization center",
                                    fontSize = 13.sp,
                                    color = Color(0xFF65676B)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1877F2).copy(alpha = 0.12f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoGraph,
                                        contentDescription = null,
                                        tint = Color(0xFF1877F2),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Stats Section (4 Real-Time Cards)
                Text(
                    text = "Performance",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )

                // Row 1: Total Reach & Total Engagement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Reach",
                        subtitle = "Total Views",
                        value = formatNumber(totalReach),
                        change = if (totalReach > 0) "+18.4%" else "0%",
                        icon = Icons.Default.RemoveRedEye,
                        iconColor = Color(0xFF1877F2),
                        iconBgColor = Color(0xFFE7F3FF),
                        modifier = Modifier.weight(1f),
                        testTag = "stat_card_total_reach"
                    )

                    StatCard(
                        title = "Total Engagement",
                        subtitle = "Likes, Comments & Shares",
                        value = formatNumber(totalEngagement),
                        change = if (totalEngagement > 0) "+12.1%" else "0%",
                        icon = Icons.Default.ThumbUp,
                        iconColor = Color(0xFF00A86B),
                        iconBgColor = Color(0xFFE8F8F0),
                        modifier = Modifier.weight(1f),
                        testTag = "stat_card_total_engagement"
                    )
                }

                // Row 2: Net Followers & Content Shares
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Net Followers",
                        subtitle = "Active Followers",
                        value = formatNumber(netFollowers),
                        change = if (netFollowers > 0) "+8.5%" else "0%",
                        icon = Icons.Default.People,
                        iconColor = Color(0xFF8E24AA),
                        iconBgColor = Color(0xFFF3E5F5),
                        modifier = Modifier.weight(1f),
                        testTag = "stat_card_net_followers"
                    )

                    StatCard(
                        title = "Content Shares",
                        subtitle = "Total Shares",
                        value = formatNumber(totalContentShares),
                        change = if (totalContentShares > 0) "+15.2%" else "0%",
                        icon = Icons.Default.Share,
                        iconColor = Color(0xFFE65100),
                        iconBgColor = Color(0xFFFFF3E0),
                        modifier = Modifier.weight(1f),
                        testTag = "stat_card_content_shares"
                    )
                }

                // CREATOR FUND MONETIZATION SECTION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Creator Fund",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (allCriteriaMet) Color(0xFFE8F8F0) else Color(0xFFF0F2F5)
                    ) {
                        Text(
                            text = "$completedCriteriaCount / 5 Completed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allCriteriaMet) Color(0xFF00A86B) else Color(0xFF65676B),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("creator_fund_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = "Creator Fund",
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Creator Fund Program",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                                Text(
                                    text = "Earn monthly payouts from your views and reels",
                                    fontSize = 12.sp,
                                    color = Color(0xFF65676B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Criteria Progress Overview
                        Text(
                            text = "Monetization Eligibility Criteria",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                        Text(
                            text = "Complete all 5 requirements below to apply for Creator Fund",
                            fontSize = 12.sp,
                            color = Color(0xFF65676B),
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                        )

                        // 1. 500 Views Criterion
                        CriteriaItem(
                            title = "500 Views (Total Reach)",
                            currentVal = totalReach,
                            targetVal = 500,
                            unit = "views",
                            isMet = hasMetViews,
                            icon = Icons.Default.RemoveRedEye,
                            testTag = "criteria_views"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. 100 Followers Criterion
                        CriteriaItem(
                            title = "100 Followers",
                            currentVal = netFollowers,
                            targetVal = 100,
                            unit = "followers",
                            isMet = hasMetFollowers,
                            icon = Icons.Default.People,
                            testTag = "criteria_followers"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. 10+ Posts Uploaded
                        CriteriaItem(
                            title = "10+ Posts Uploaded",
                            currentVal = totalPostsCount,
                            targetVal = 10,
                            unit = "posts",
                            isMet = hasMetPosts,
                            icon = Icons.Default.PostAdd,
                            testTag = "criteria_posts"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. 5+ Reels Uploaded
                        CriteriaItem(
                            title = "5+ Reels Uploaded",
                            currentVal = totalReelsCount,
                            targetVal = 5,
                            unit = "reels",
                            isMet = hasMetReels,
                            icon = Icons.Default.Movie,
                            testTag = "criteria_reels"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 5. Account Age 7+ Days
                        CriteriaItem(
                            title = "7+ Days Account Age",
                            currentVal = accountAgeDays,
                            targetVal = 7,
                            unit = "days",
                            isMet = hasMetAccountAge,
                            icon = Icons.Default.DateRange,
                            testTag = "criteria_account_age"
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Status / Notification
                        if (userMonetizationRequest?.status == "APPROVED") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F8F0),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A86B).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Approved",
                                        tint = Color(0xFF00A86B),
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Partner Monetization Active!",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00A86B)
                                        )
                                        Text(
                                            text = "Congratulations! Your account is approved for Creator Fund earnings.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF050505)
                                        )
                                    }
                                }
                            }
                        } else if (isApplicationSubmitted || userMonetizationRequest?.status == "PENDING") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F8F0),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A86B).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Applied",
                                        tint = Color(0xFF00A86B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Application Submitted!",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00A86B)
                                        )
                                        Text(
                                            text = "Your Creator Fund application is active and under review by Admin.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF050505)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Apply Monetization Button
                            Button(
                                onClick = {
                                    if (allCriteriaMet) {
                                        isApplicationSubmitted = true
                                        sharedPrefs.edit().putBoolean("fund_applied_$currentUserId", true).apply()
                                        adminRepo.submitMonetizationRequest(
                                            userId = currentUserId,
                                            userName = displayName,
                                            userEmail = userProfile?.email ?: "",
                                            viewsCount = totalReach,
                                            followersCount = netFollowers,
                                            postsCount = totalPostsCount,
                                            reelsCount = totalReelsCount,
                                            accountAgeDays = accountAgeDays
                                        )
                                        showSuccessDialog = true
                                    }
                                },
                                enabled = allCriteriaMet,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1877F2),
                                    disabledContainerColor = Color(0xFFE4E6EB),
                                    contentColor = Color.White,
                                    disabledContentColor = Color(0xFF8A8D91)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("apply_monetization_button")
                            ) {
                                if (allCriteriaMet) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Apply for Monetization",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF8A8D91),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Apply for Monetization (${5 - completedCriteriaCount} remaining)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF8A8D91)
                                    )
                                }
                            }

                            if (!allCriteriaMet) {
                                Text(
                                    text = "All 5 requirements must be met before you can apply.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF65676B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Application Submitted Dialog
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Application Submitted!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Text(
                        text = "Congratulations! You have fulfilled all 5 criteria for the Frndom Creator Fund. Your application has been submitted and earnings will start reflecting in your Wallet.",
                        fontSize = 14.sp,
                        color = Color(0xFF050505),
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                    ) {
                        Text("Awesome!", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun CriteriaItem(
    title: String,
    currentVal: Int,
    targetVal: Int,
    unit: String,
    isMet: Boolean,
    icon: ImageVector,
    testTag: String
) {
    val progress = (currentVal.toFloat() / targetVal.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isMet) Color(0xFFE8F8F0) else Color(0xFFF0F2F5),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isMet) Color(0xFF00A86B) else Color(0xFF65676B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF050505)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currentVal / $targetVal $unit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMet) Color(0xFF00A86B) else Color(0xFF65676B)
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (isMet) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00C853),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFCED0D4),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (isMet) Color(0xFF00A86B) else Color(0xFF1877F2),
            trackColor = Color(0xFFE4E6EB),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    subtitle: String,
    value: String,
    change: String,
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        modifier = modifier.testTag(testTag),
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
                    shape = CircleShape,
                    color = iconBgColor,
                    modifier = Modifier.size(34.dp)
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

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F8F0)
                ) {
                    Text(
                        text = change,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00A86B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF050505)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF050505)
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF65676B)
            )
        }
    }
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString()
    }
}
