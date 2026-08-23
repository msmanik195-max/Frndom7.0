package com.example.ui.menu

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ThumbUp
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
import com.example.data.model.PageItem
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
fun PageDetailView(
    page: PageItem,
    userProfile: UserProfile?,
    groupPageRepository: GroupPageRepository,
    postRepository: PostRepository,
    mediaUploadService: MediaUploadService? = null,
    onBack: () -> Unit,
    onSwitchToPage: ((PageItem) -> Unit)? = null,
    onUserClick: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val allPosts by postRepository.postsFlow.collectAsState()
    val allPages by groupPageRepository.pagesFlow.collectAsState()

    // Find the latest state of the page
    val currentPage = allPages.find { it.id == page.id } ?: page
    val pagePosts = allPosts.filter { it.pageId == currentPage.id || it.authorName == currentPage.name }

    val currentUid = userProfile?.uid ?: ""
    val isCreator = currentPage.creatorId == currentUid || currentPage.creatorId.isBlank()
    val isSwitchedToThisPage = currentUid == "page_profile_${currentPage.id}"

    var isLiked by remember { mutableStateOf(isCreator) }
    var likesCount by remember { mutableIntStateOf(currentPage.likesCount.coerceAtLeast(1)) }
    var followersCount by remember { mutableIntStateOf(currentPage.followersCount.coerceAtLeast(1)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Posts", "About")

    var showMenuDropdown by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditScreen by remember { mutableStateOf(false) }

    var showCreatePostSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf<PostItem?>(null) }
    var postText by remember { mutableStateOf("") }
    var mediaUrlInput by remember { mutableStateOf("") }
    var showMediaDialog by remember { mutableStateOf(false) }

    if (showEditScreen) {
        val uploadService = mediaUploadService ?: remember { MediaUploadService(context, com.example.data.repository.StorageRepository(context)) }
        CreatePageScreen(
            userProfile = userProfile,
            groupPageRepository = groupPageRepository,
            mediaUploadService = uploadService,
            pageToEdit = currentPage,
            onPageCreated = {
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
            .testTag("page_detail_view")
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
                            text = currentPage.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val link = "https://frndom.app/page/${currentPage.id}"
                            clipboardManager.setText(AnnotatedString(link))
                            Toast.makeText(context, "Page link copied!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color(0xFF050505)
                            )
                        }

                        // 3-dot More Menu for Page Creator (Edit & Delete options)
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
                                                Text("Edit Page")
                                            }
                                        },
                                        onClick = {
                                            showMenuDropdown = false
                                            showEditScreen = true
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Switch to this Page")
                                            }
                                        },
                                        onClick = {
                                            showMenuDropdown = false
                                            if (onSwitchToPage != null) {
                                                onSwitchToPage(currentPage)
                                            } else {
                                                Toast.makeText(context, "Switched into ${currentPage.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )

                                    Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Delete Page", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        onClick = {
                                            showMenuDropdown = false
                                            showDeleteConfirmDialog = true
                                        }
                                    )
                                }
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
                // Cover & Profile Area
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        // Cover photo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            if (currentPage.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentPage.coverUrl,
                                    contentDescription = currentPage.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF1877F2), Color(0xFF0A4FBA))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(52.dp)
                                    )
                                }
                            }
                        }

                        // Avatar & Title row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            // Overlapping avatar
                            Surface(
                                modifier = Modifier
                                    .offset(y = (-36).dp)
                                    .size(76.dp)
                                    .clip(CircleShape),
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 3.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEBF5FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentPage.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = currentPage.avatarUrl,
                                            contentDescription = currentPage.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = Color(0xFF1877F2),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            // Page Details
                            Column(modifier = Modifier.offset(y = (-20).dp)) {
                                Text(
                                    text = currentPage.name,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEBF5FF)
                                ) {
                                    Text(
                                        text = currentPage.category,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1877F2),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "$likesCount likes • $followersCount followers",
                                    fontSize = 13.sp,
                                    color = Color(0xFF65676B),
                                    fontWeight = FontWeight.Medium
                                )

                                if (currentPage.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentPage.description,
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
                                    // If user is the creator, provide Switch to Page action
                                    if (isCreator) {
                                        Button(
                                            onClick = {
                                                if (onSwitchToPage != null) {
                                                    onSwitchToPage(currentPage)
                                                } else {
                                                    Toast.makeText(context, "Switched into ${currentPage.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
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
                                                imageVector = Icons.Default.SwapHoriz,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isSwitchedToThisPage) "Active Page" else "Switch to Page",
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
                                        // Regular visitor actions
                                        Button(
                                            onClick = {
                                                isLiked = !isLiked
                                                if (isLiked) {
                                                    likesCount++
                                                    followersCount++
                                                    Toast.makeText(context, "You liked and followed ${currentPage.name}", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    likesCount = (likesCount - 1).coerceAtLeast(1)
                                                    followersCount = (followersCount - 1).coerceAtLeast(1)
                                                    Toast.makeText(context, "Unliked ${currentPage.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isLiked) Color(0xFFE4E6EB) else Color(0xFF1877F2),
                                                contentColor = if (isLiked) Color(0xFF050505) else Color.White
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isLiked) Icons.Default.Check else Icons.Default.ThumbUp,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isLiked) "Liked" else "Like Page",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "Messaging ${currentPage.name}", Toast.LENGTH_SHORT).show()
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
                                                imageVector = Icons.Default.Message,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Message",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
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
                        // Constraint rule: Users cannot post as a Page while in their personal account; they must switch to the page first, or if switched, they can post!
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
                                        color = Color(0xFFE4E6EB)
                                    ) {
                                        if (currentPage.avatarUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = currentPage.avatarUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(imageVector = Icons.Default.Flag, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(20.dp))
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
                                                if (isSwitchedToThisPage) {
                                                    showCreatePostSheet = true
                                                } else if (isCreator) {
                                                    Toast.makeText(
                                                        context,
                                                        "Switch to '${currentPage.name}' to post as this Page.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Only the Page admin can create posts.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = if (isSwitchedToThisPage)
                                                "Write a post as ${currentPage.name}..."
                                            else if (isCreator)
                                                "Switch to ${currentPage.name} to post..."
                                            else
                                                "Only ${currentPage.name} can post here",
                                            color = Color(0xFF65676B),
                                            fontSize = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(onClick = {
                                        if (isSwitchedToThisPage) {
                                            showMediaDialog = true
                                        } else if (isCreator) {
                                            Toast.makeText(
                                                context,
                                                "Switch to '${currentPage.name}' to post photos/videos as this Page.",
                                                Toast.LENGTH_LONG
                                            ).show()
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

                        if (pagePosts.isEmpty()) {
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
                                                    imageVector = Icons.Default.Flag,
                                                    contentDescription = null,
                                                    tint = Color(0xFF1877F2),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No Posts on this Page Yet",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF050505)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Switch into this page from Account Switcher to post updates and media for your audience!",
                                            fontSize = 13.sp,
                                            color = Color(0xFF65676B),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        if (isCreator) {
                                            Button(
                                                onClick = {
                                                    if (isSwitchedToThisPage) {
                                                        showCreatePostSheet = true
                                                    } else if (onSwitchToPage != null) {
                                                        onSwitchToPage(currentPage)
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                                            ) {
                                                Icon(
                                                    imageVector = if (isSwitchedToThisPage) Icons.Default.Add else Icons.Default.SwapHoriz,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isSwitchedToThisPage) "Create First Post" else "Switch to Page to Post",
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            items(pagePosts, key = { it.id }) { post ->
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
                                            putExtra(Intent.EXTRA_TEXT, "Check out this update from ${currentPage.name} on Frndom: https://frndom.app/post/${post.id}")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Page Post"))
                                    }
                                )
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
                                        text = "About ${currentPage.name}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF050505)
                                    )

                                    if (currentPage.description.isNotBlank()) {
                                        Text(
                                            text = currentPage.description,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1C1E21),
                                            lineHeight = 20.sp
                                        )
                                        Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = "Category", fontSize = 12.sp, color = Color(0xFF65676B))
                                            Text(text = currentPage.category, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF050505))
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = "Engagement", fontSize = 12.sp, color = Color(0xFF65676B))
                                            Text(text = "$likesCount Likes • $followersCount Followers", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF050505))
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
                                                Text("Edit Page")
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
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Page", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${currentPage.name}'? This action cannot be undone and will permanently remove this page.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        groupPageRepository.deletePage(currentPage.id)
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "Page '${currentPage.name}' deleted.", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete Page", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create Post Sheet for Page (When active as page)
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
                        text = "Create Post as Page",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )

                    Button(
                        onClick = {
                            if (postText.isNotBlank() || mediaUrlInput.isNotBlank()) {
                                val newPost = PostItem(
                                    id = "post_" + UUID.randomUUID().toString().take(8),
                                    authorId = "page_profile_${currentPage.id}",
                                    authorName = currentPage.name,
                                    authorAvatarUrl = currentPage.avatarUrl,
                                    content = postText.trim(),
                                    mediaUrl = mediaUrlInput.trim(),
                                    mediaUrls = if (mediaUrlInput.isNotBlank()) listOf(mediaUrlInput.trim()) else emptyList(),
                                    mediaType = if (mediaUrlInput.isNotBlank()) "photo" else "text",
                                    pageId = currentPage.id,
                                    audience = "Public",
                                    createdAt = System.currentTimeMillis()
                                )
                                postRepository.createPost(newPost)
                                postText = ""
                                mediaUrlInput = ""
                                showCreatePostSheet = false
                                Toast.makeText(context, "Published to ${currentPage.name}!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = postText.isNotBlank() || mediaUrlInput.isNotBlank(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                    ) {
                        Text("Publish", fontWeight = FontWeight.Bold)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFFEBF5FF)
                    ) {
                        if (currentPage.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = currentPage.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Flag, contentDescription = null, tint = Color(0xFF1877F2))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentPage.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF050505)
                        )
                        Text(
                            text = "Publishing as Page Admin",
                            fontSize = 12.sp,
                            color = Color(0xFF1877F2),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    placeholder = { Text("What's new with ${currentPage.name}?") },
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
                    onClick = { showMediaDialog = true },
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

    if (showMediaDialog) {
        var tempUrl by remember { mutableStateOf(mediaUrlInput) }
        AlertDialog(
            onDismissRequest = { showMediaDialog = false },
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
                        showMediaDialog = false
                        showCreatePostSheet = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                ) {
                    Text("Attach")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMediaDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
