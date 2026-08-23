package com.example.ui.menu

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GroupItem
import com.example.data.model.PostItem
import com.example.data.model.UserProfile
import com.example.data.repository.GroupPageRepository
import com.example.data.repository.PostRepository
import com.example.data.service.MediaUploadService
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.home.PostCardItem
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailView(
    group: GroupItem,
    userProfile: UserProfile?,
    groupPageRepository: GroupPageRepository,
    postRepository: PostRepository,
    mediaUploadService: MediaUploadService? = null,
    onBack: () -> Unit,
    onUserClick: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val allPosts by postRepository.postsFlow.collectAsState()
    val allGroups by groupPageRepository.groupsFlow.collectAsState()

    val currentGroup = allGroups.find { it.id == group.id } ?: group
    val groupPosts = allPosts.filter { it.groupId == currentGroup.id }

    val currentUid = userProfile?.uid ?: ""
    val isCreator = currentGroup.creatorId == currentUid || currentGroup.creatorId.isBlank()

    var isJoined by remember { mutableStateOf(isCreator || currentGroup.membersCount > 0) }
    var memberCount by remember { mutableIntStateOf(currentGroup.membersCount.coerceAtLeast(1)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Posts", "About", "Members")

    var showMenuDropdown by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditScreen by remember { mutableStateOf(false) }

    var showCreatePostSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf<PostItem?>(null) }
    var showGroupOptionsSheet by remember { mutableStateOf(false) }

    var postText by remember { mutableStateOf("") }
    var mediaUrlInput by remember { mutableStateOf("") }
    var showMediaUrlDialog by remember { mutableStateOf(false) }

    val isPrivate = currentGroup.privacy.equals("Private", ignoreCase = true)
    val canViewFeed = !isPrivate || isJoined || isCreator

    val displayName = userProfile?.fullName.orEmpty().ifBlank { "${userProfile?.firstName} ${userProfile?.lastName}".trim() }.ifBlank { "User" }
    val userAvatar = userProfile?.profilePictureUrl.orEmpty()
    val initial = displayName.firstOrNull()?.uppercase() ?: "U"

    if (showEditScreen) {
        val uploadService = mediaUploadService ?: remember { MediaUploadService(context, com.example.data.repository.StorageRepository(context)) }
        CreateGroupScreen(
            userProfile = userProfile,
            groupPageRepository = groupPageRepository,
            mediaUploadService = uploadService,
            groupToEdit = currentGroup,
            onGroupCreated = {
                showEditScreen = false
            },
            onBack = { showEditScreen = false },
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("group_detail_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
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
                            text = currentGroup.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val link = "https://frndom.app/group/${currentGroup.id}"
                            clipboardManager.setText(AnnotatedString(link))
                            Toast.makeText(context, "Group link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color(0xFF050505)
                            )
                        }

                        if (isCreator) {
                            Box {
                                IconButton(onClick = { showMenuDropdown = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color(0xFF050505)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenuDropdown,
                                    onDismissRequest = { showMenuDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Edit Group")
                                            }
                                        },
                                        onClick = {
                                            showMenuDropdown = false
                                            showEditScreen = true
                                        }
                                    )

                                    Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Delete Group", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        onClick = {
                                            showMenuDropdown = false
                                            showDeleteConfirmDialog = true
                                        }
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = { showGroupOptionsSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "More Options",
                                    tint = Color(0xFF050505)
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // 1. Group Cover Photo & Header Banner (Facebook groups only feature cover banners, no avatar)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                        ) {
                            if (currentGroup.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentGroup.coverUrl,
                                    contentDescription = currentGroup.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF1877F2), Color(0xFF0C59CF))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                        }

                        // Group Title, Privacy, Stats
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = currentGroup.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Public,
                                    contentDescription = currentGroup.privacy,
                                    tint = Color(0xFF65676B),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${if (isPrivate) "Private" else "Public"} group • $memberCount member${if (memberCount > 1) "s" else ""}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF65676B)
                                )
                            }

                            if (currentGroup.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentGroup.description,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1C1E21),
                                    lineHeight = 19.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isCreator) {
                                    Button(
                                        onClick = { showCreatePostSheet = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1877F2),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Post in Group",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = { showEditScreen = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFE4E6EB),
                                            contentColor = Color(0xFF050505)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Edit",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    // Join / Joined Button
                                    Button(
                                        onClick = {
                                            isJoined = !isJoined
                                            if (isJoined) {
                                                memberCount++
                                                Toast.makeText(context, "You joined ${currentGroup.name}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                memberCount = (memberCount - 1).coerceAtLeast(1)
                                                Toast.makeText(context, "You left ${currentGroup.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isJoined) Color(0xFFE4E6EB) else Color(0xFF1877F2),
                                            contentColor = if (isJoined) Color(0xFF050505) else Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isJoined) Icons.Default.Check else Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isJoined) "Joined" else "Join Group",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Invite / Share Button
                                    Button(
                                        onClick = {
                                            val link = "https://frndom.app/group/${currentGroup.id}"
                                            clipboardManager.setText(AnnotatedString(link))
                                            Toast.makeText(context, "Group invite link copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFE4E6EB),
                                            contentColor = Color(0xFF050505)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Invite",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Tab navigation
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
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // POSTS TAB
                        if (canViewFeed) {
                            // Composer bar
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    color = Color.White,
                                    shadowElevation = 0.5.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape),
                                            shape = CircleShape,
                                            color = Color(0xFF1877F2).copy(alpha = 0.15f)
                                        ) {
                                            if (userAvatar.isNotBlank()) {
                                                AsyncImage(
                                                    model = userAvatar,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = initial,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1877F2),
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFFF0F2F5))
                                                .clickable {
                                                    if (isJoined || isCreator) {
                                                        showCreatePostSheet = true
                                                    } else {
                                                        Toast.makeText(context, "Join this group to participate and post!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "Write something in ${currentGroup.name}...",
                                                color = Color(0xFF65676B),
                                                fontSize = 14.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(onClick = {
                                            if (isJoined || isCreator) {
                                                showMediaUrlDialog = true
                                            } else {
                                                Toast.makeText(context, "Join this group to share photos!", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = "Photo/Video",
                                                tint = Color(0xFF45BD62),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (groupPosts.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFEBF5FF),
                                                modifier = Modifier.size(64.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Group,
                                                        contentDescription = null,
                                                        tint = Color(0xFF1877F2),
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "No Posts Yet",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF050505)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Be the first member to start a discussion in this group!",
                                                fontSize = 13.sp,
                                                color = Color(0xFF65676B),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = {
                                                    if (isJoined || isCreator) {
                                                        showCreatePostSheet = true
                                                    } else {
                                                        Toast.makeText(context, "Join the group to create the first post!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "Create Post", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(groupPosts, key = { it.id }) { post ->
                                    PostCardItem(
                                        post = post,
                                        currentUserId = currentUid,
                                        onUserClick = {},
                                        onLikeClick = { postRepository.toggleLike(post.id, currentUid) },
                                        onReactionClick = { reaction -> postRepository.setReaction(post.id, currentUid, reaction) },
                                        onCommentClick = { showCommentsSheet = post },
                                        onShareClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Check out this post in ${currentGroup.name} on Frndom: https://frndom.app/post/${post.id}")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Group Post"))
                                        }
                                    )
                                }
                            }
                        } else {
                            // Private group lock message
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFFFEBEE),
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = Color(0xFFD32F2F),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "This Group is Private",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF050505)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Join this group to view posts, participate in discussions, and connect with members.",
                                            fontSize = 13.sp,
                                            color = Color(0xFF65676B),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(18.dp))
                                        Button(
                                            onClick = {
                                                isJoined = true
                                                memberCount++
                                                Toast.makeText(context, "You joined ${currentGroup.name}!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                                        ) {
                                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "Join Group", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // ABOUT TAB
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "About this group",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF050505)
                                    )

                                    if (currentGroup.description.isNotBlank()) {
                                        Text(
                                            text = currentGroup.description,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1C1E21),
                                            lineHeight = 20.sp
                                        )
                                        Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Public,
                                            contentDescription = null,
                                            tint = Color(0xFF1877F2),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = "${currentGroup.privacy} Group", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF050505))
                                            Text(
                                                text = if (isPrivate) "Only members can see who's in the group and what they post." else "Anyone can see who's in the group and what they post.",
                                                fontSize = 12.sp,
                                                color = Color(0xFF65676B)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = "$memberCount Members", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF050505))
                                            Text(text = "Active community discussions", fontSize = 12.sp, color = Color(0xFF65676B))
                                        }
                                    }

                                    if (isCreator) {
                                        Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { showEditScreen = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Edit Group")
                                            }

                                            Button(
                                                onClick = { showDeleteConfirmDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFD32F2F)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Delete")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // MEMBERS TAB
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Group Admin & Members ($memberCount)",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF050505)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(42.dp),
                                            shape = CircleShape,
                                            color = Color(0xFF1877F2).copy(alpha = 0.15f)
                                        ) {
                                            if (userAvatar.isNotBlank()) {
                                                AsyncImage(
                                                    model = userAvatar,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = initial,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1877F2),
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF050505)
                                            )
                                            Text(
                                                text = if (isCreator) "Admin & Creator" else "Member",
                                                fontSize = 12.sp,
                                                color = if (isCreator) Color(0xFF1877F2) else Color(0xFF65676B),
                                                fontWeight = if (isCreator) FontWeight.SemiBold else FontWeight.Normal
                                            )
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

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Group", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${currentGroup.name}'? This action cannot be undone and will permanently remove this group.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        groupPageRepository.deleteGroup(currentGroup.id)
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "Group '${currentGroup.name}' deleted.", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete Group", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create Post in Group Bottom Sheet
    if (showCreatePostSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreatePostSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Post in Group",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )

                    Button(
                        onClick = {
                            if (postText.isNotBlank() || mediaUrlInput.isNotBlank()) {
                                val newPost = PostItem(
                                    id = "post_" + UUID.randomUUID().toString().take(8),
                                    authorId = currentUid,
                                    authorName = displayName,
                                    authorAvatarUrl = userAvatar,
                                    content = postText.trim(),
                                    mediaUrl = mediaUrlInput.trim(),
                                    mediaUrls = if (mediaUrlInput.isNotBlank()) listOf(mediaUrlInput.trim()) else emptyList(),
                                    mediaType = if (mediaUrlInput.isNotBlank()) "photo" else "text",
                                    groupId = currentGroup.id,
                                    audience = "Group",
                                    createdAt = System.currentTimeMillis()
                                )
                                postRepository.createPost(newPost)
                                postText = ""
                                mediaUrlInput = ""
                                showCreatePostSheet = false
                                Toast.makeText(context, "Posted in ${currentGroup.name}!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = postText.isNotBlank() || mediaUrlInput.isNotBlank(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                    ) {
                        Text("Post", fontWeight = FontWeight.Bold)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFF1877F2).copy(alpha = 0.15f)
                    ) {
                        if (userAvatar.isNotBlank()) {
                            AsyncImage(
                                model = userAvatar,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = initial, fontWeight = FontWeight.Bold, color = Color(0xFF1877F2), fontSize = 16.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF050505)
                        )
                        Text(
                            text = "Posting to: ${currentGroup.name}",
                            fontSize = 12.sp,
                            color = Color(0xFF65676B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFFF0F2F5),
                        unfocusedContainerColor = Color(0xFFF0F2F5)
                    )
                )

                if (mediaUrlInput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = mediaUrlInput,
                            contentDescription = "Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showMediaUrlDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF0F2F5),
                        contentColor = Color(0xFF050505)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0xFF45BD62),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (mediaUrlInput.isNotBlank()) "Change Photo URL" else "Add Photo URL",
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showMediaUrlDialog) {
        var tempUrl by remember { mutableStateOf(mediaUrlInput) }
        AlertDialog(
            onDismissRequest = { showMediaUrlDialog = false },
            title = { Text("Attach Photo URL", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = { tempUrl = it },
                    label = { Text("Image URL") },
                    placeholder = { Text("https://example.com/photo.jpg") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mediaUrlInput = tempUrl.trim()
                        showMediaUrlDialog = false
                        showCreatePostSheet = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                ) {
                    Text("Attach")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMediaUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Group Options Bottom Sheet
    if (showGroupOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGroupOptionsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = currentGroup.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )
                Text(
                    text = "${currentGroup.privacy} Group • $memberCount members",
                    fontSize = 13.sp,
                    color = Color(0xFF65676B)
                )

                Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB), modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val link = "https://frndom.app/group/${currentGroup.id}"
                            clipboardManager.setText(AnnotatedString(link))
                            Toast.makeText(context, "Group link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showGroupOptionsSheet = false
                        }
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Link",
                        tint = Color(0xFF050505),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Copy Link to Group",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF050505)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Comments Bottom Sheet
    showCommentsSheet?.let { post ->
        CommentsBottomSheet(
            postRepository = postRepository,
            postId = post.id,
            userProfile = userProfile,
            onDismiss = { showCommentsSheet = null },
            onCommentAdded = {}
        )
    }
}
