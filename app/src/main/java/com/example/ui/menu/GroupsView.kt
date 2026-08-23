package com.example.ui.menu

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GroupItem
import com.example.data.model.UserProfile
import com.example.data.repository.GroupPageRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.StorageRepository
import com.example.data.service.MediaUploadService

@Composable
fun GroupsView(
    userProfile: UserProfile?,
    groupPageRepository: GroupPageRepository,
    postRepository: PostRepository,
    mediaUploadService: MediaUploadService? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveMediaUploadService = mediaUploadService ?: remember { MediaUploadService(context, StorageRepository(context)) }

    val groups by groupPageRepository.groupsFlow.collectAsState()
    val joinedGroups = remember { mutableStateMapOf<String, Boolean>() }
    var selectedGroupForDetail by remember { mutableStateOf<GroupItem?>(null) }
    var showCreateScreen by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Discover", "Your Groups")

    if (showCreateScreen) {
        CreateGroupScreen(
            userProfile = userProfile,
            groupPageRepository = groupPageRepository,
            mediaUploadService = effectiveMediaUploadService,
            onGroupCreated = { createdGroup ->
                showCreateScreen = false
                selectedGroupForDetail = createdGroup
            },
            onBack = { showCreateScreen = false },
            modifier = modifier
        )
        return
    }

    if (selectedGroupForDetail != null) {
        GroupDetailView(
            group = selectedGroupForDetail!!,
            userProfile = userProfile,
            groupPageRepository = groupPageRepository,
            postRepository = postRepository,
            mediaUploadService = effectiveMediaUploadService,
            onBack = { selectedGroupForDetail = null },
            modifier = modifier
        )
        return
    }

    val currentUid = userProfile?.uid ?: ""
    val userGroups = groups.filter { it.creatorId == currentUid || joinedGroups[it.id] == true || it.creatorId.isBlank() }
    val displayedGroups = if (selectedTab == 1) userGroups else groups

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("groups_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Groups",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                }

                Button(
                    onClick = { showCreateScreen = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Create", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF1877F2)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

            if (displayedGroups.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (selectedTab == 1) "No Groups Joined Yet" else "No Groups Available",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (selectedTab == 1)
                            "You haven't created or joined any groups yet. Switch to Discover or create one!"
                        else
                            "Connect with people who share your interests by creating the first community group.",
                        fontSize = 14.sp,
                        color = Color(0xFF65676B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showCreateScreen = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Create a Group", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displayedGroups, key = { it.id }) { group ->
                        val isPrivate = group.privacy.equals("Private", ignoreCase = true)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGroupForDetail = group },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (group.coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = group.coverUrl,
                                        contentDescription = group.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(90.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF1877F2), Color(0xFF0D54BA))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = group.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF050505)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Public,
                                            contentDescription = null,
                                            tint = Color(0xFF65676B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${group.privacy} Group • ${group.membersCount} member${if (group.membersCount > 1) "s" else ""}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF65676B)
                                        )
                                    }

                                    if (group.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = group.description,
                                            fontSize = 13.sp,
                                            color = Color(0xFF65676B),
                                            maxLines = 2
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = { selectedGroupForDetail = group },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEBF5FF),
                                            contentColor = Color(0xFF1877F2)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "View Group", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
