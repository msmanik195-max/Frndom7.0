package com.example.ui.menu

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PageItem
import com.example.data.model.UserProfile
import com.example.data.repository.GroupPageRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    currentProfile: UserProfile?,
    savedAccounts: List<UserProfile>,
    groupPageRepository: GroupPageRepository? = null,
    onSelectAccount: (UserProfile) -> Unit,
    onSelectPage: ((PageItem) -> Unit)? = null,
    onAddNewAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pages by groupPageRepository?.pagesFlow?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList<PageItem>()) }

    // User's owned pages or pages where user is creator
    val currentUid = currentProfile?.uid ?: ""
    val userPages = pages.filter { it.creatorId == currentUid || it.creatorId.isBlank() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("account_switcher_sheet")
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Switch Profiles & Pages",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

            val allAccounts = if (savedAccounts.none { it.uid == currentProfile?.uid } && currentProfile != null) {
                listOf(currentProfile) + savedAccounts
            } else {
                savedAccounts.ifEmpty { currentProfile?.let { listOf(it) } ?: emptyList() }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section 1: Personal Profiles
                item {
                    Text(
                        text = "Personal Accounts",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF65676B),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                items(allAccounts, key = { it.uid.ifBlank { it.email } }) { profile ->
                    val isCurrent = profile.uid == currentProfile?.uid && !profile.uid.startsWith("page_profile_")
                    val name = profile.fullName.ifBlank { "${profile.firstName} ${profile.lastName}".trim().ifBlank { "User" } }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectAccount(profile)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) Color(0xFFEBF5FF) else Color(0xFFF7F8FA)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (profile.profilePictureUrl.isNotBlank()) {
                                AsyncImage(
                                    model = profile.profilePictureUrl,
                                    contentDescription = name,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF1877F2).copy(alpha = 0.15f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = name.take(1).uppercase(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1877F2)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF050505)
                                    )
                                    if (profile.isVerificationActive()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        com.example.ui.components.VerificationBadge(size = 14.dp, show = true)
                                    }
                                }
                                Text(
                                    text = if (isCurrent) "Active Personal Profile" else profile.email.ifBlank { "Personal Profile" },
                                    fontSize = 12.sp,
                                    color = if (isCurrent) Color(0xFF1877F2) else Color(0xFF65676B),
                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }

                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Section 2: Switch to Pages
                if (userPages.isNotEmpty()) {
                    item {
                        Text(
                            text = "Your Pages (Switch to post as Page)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF65676B),
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(userPages, key = { it.id }) { page ->
                        val isPageActive = currentProfile?.uid == "page_profile_${page.id}" || currentProfile?.fullName == page.name

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (onSelectPage != null) {
                                        onSelectPage(page)
                                    } else {
                                        // Default fallback page profile conversion
                                        val pageUserProfile = UserProfile(
                                            uid = "page_profile_${page.id}",
                                            firstName = page.name,
                                            lastName = "",
                                            fullName = page.name,
                                            email = "${page.name.lowercase().replace(" ", "")}@page.frndom.app",
                                            profilePictureUrl = page.avatarUrl,
                                            coverPictureUrl = page.coverUrl,
                                            bio = page.description
                                        )
                                        onSelectAccount(pageUserProfile)
                                    }
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPageActive) Color(0xFFEBF5FF) else Color(0xFFF7F8FA)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (page.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = page.avatarUrl,
                                        contentDescription = page.name,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.size(44.dp),
                                        shape = CircleShape,
                                        color = Color(0xFF1877F2)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = page.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF050505)
                                    )
                                    Text(
                                        text = if (isPageActive) "Active Page Profile" else "Page • ${page.category}",
                                        fontSize = 12.sp,
                                        color = if (isPageActive) Color(0xFF1877F2) else Color(0xFF65676B),
                                        fontWeight = if (isPageActive) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }

                                if (isPageActive) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = Color(0xFF1877F2),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Add account
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAddNewAccount()
                                onDismiss()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = Color(0xFFE4E6EB)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add account",
                                        tint = Color(0xFF050505),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Log into another personal account",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                                Text(
                                    text = "Switch or add a new personal profile",
                                    fontSize = 12.sp,
                                    color = Color(0xFF65676B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
