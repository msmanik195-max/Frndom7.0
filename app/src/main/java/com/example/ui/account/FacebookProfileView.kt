package com.example.ui.account

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.components.VerificationBadge
import com.example.ui.verification.VerificationBadgeScreen
import com.example.data.repository.GroupPageRepository
import com.example.ui.menu.DashboardView
import com.example.ui.menu.GroupsView
import com.example.ui.menu.PagesView
import com.example.ui.menu.SettingsView
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostItem
import com.example.data.model.UserProfile
import com.example.data.repository.PostRepository
import com.example.data.repository.UserRepository
import com.example.data.service.MediaUploadService
import com.example.ui.components.FullScreenImageViewer
import com.example.ui.home.PostCardItem
import kotlinx.coroutines.launch

enum class ProfileSubScreen {
    DASHBOARD,
    PAGES,
    GROUPS,
    SETTINGS,
    VERIFICATION
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FacebookProfileView(
    targetUser: UserProfile,
    currentUserId: String,
    postRepository: PostRepository,
    userRepository: UserRepository,
    mediaUploadService: MediaUploadService?,
    onAddStoryClick: () -> Unit = {},
    onMessageClick: (UserProfile) -> Unit = {},
    onUserClick: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val groupPageRepository = remember { GroupPageRepository(context) }
    val scope = rememberCoroutineScope()
    val isMyProfile = targetUser.uid == currentUserId

    var currentSubScreen by remember { mutableStateOf<ProfileSubScreen?>(null) }

    // Live profile data
    val liveProfile by userRepository.getUserProfileFlow(targetUser.uid).collectAsState(initial = targetUser)
    val user = liveProfile ?: targetUser

    // If a sub screen is open (e.g. Dashboard, Pages, Groups, Settings), display it directly
    when (currentSubScreen) {
        ProfileSubScreen.DASHBOARD -> {
            DashboardView(
                userProfile = user,
                onBack = { currentSubScreen = null },
                modifier = modifier
            )
            return
        }
        ProfileSubScreen.PAGES -> {
            PagesView(
                userProfile = user,
                groupPageRepository = groupPageRepository,
                postRepository = postRepository,
                onBack = { currentSubScreen = null },
                modifier = modifier
            )
            return
        }
        ProfileSubScreen.GROUPS -> {
            GroupsView(
                userProfile = user,
                groupPageRepository = groupPageRepository,
                postRepository = postRepository,
                onBack = { currentSubScreen = null },
                modifier = modifier
            )
            return
        }
        ProfileSubScreen.SETTINGS -> {
            SettingsView(
                userProfile = user,
                onServerSettingsClick = {},
                onVerificationBadgeClick = { currentSubScreen = ProfileSubScreen.VERIFICATION },
                onLogoutClick = {},
                onBack = { currentSubScreen = null },
                modifier = modifier
            )
            return
        }
        ProfileSubScreen.VERIFICATION -> {
            VerificationBadgeScreen(
                userProfile = user,
                userRepository = userRepository,
                onBack = { currentSubScreen = null },
                onNavigateToProfile = { currentSubScreen = null },
                modifier = modifier
            )
            return
        }
        null -> { /* Render profile screen */ }
    }

    // Live posts data
    val allPosts by postRepository.postsFlow.collectAsState()
    val userPosts = allPosts.filter { it.authorId == user.uid }
    val userReels = userPosts.filter { it.mediaType == "reel" || it.mediaType == "video" }
    val userPhotos = userPosts.flatMap { it.getAllMediaUrls() }

    // Live users for following/friends
    val allUsers by userRepository.getAllUsersFlow().collectAsState(initial = emptyList())
    val followingUserList = allUsers.filter { user.followingMap[it.uid] == true }.take(20)

    val isFollowing = user.followersMap[currentUserId] == true

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Posts", "Reels", "Photos", "About", "Friends")

    var showProfileOptionsSheet by remember { mutableStateOf(false) }
    var showCoverOptionsSheet by remember { mutableStateOf(false) }
    var showProfileSettingsSheet by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var fullScreenViewUrl by remember { mutableStateOf<String?>(null) }
    var fullScreenViewTitle by remember { mutableStateOf("") }
    var isUploadingProfile by remember { mutableStateOf(false) }
    var isUploadingCover by remember { mutableStateOf(false) }

    val profilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && mediaUploadService != null) {
            isUploadingProfile = true
            scope.launch {
                val res = mediaUploadService.uploadImageUri(uri, folder = "profiles")
                val remoteUrl = res.getOrDefault(uri.toString())
                isUploadingProfile = false
                val updated = user.copy(profilePictureUrl = remoteUrl)
                userRepository.updateUserProfile(updated)
                Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && mediaUploadService != null) {
            isUploadingCover = true
            scope.launch {
                val res = mediaUploadService.uploadImageUri(uri, folder = "covers")
                val remoteUrl = res.getOrDefault(uri.toString())
                isUploadingCover = false
                val updated = user.copy(coverPictureUrl = remoteUrl)
                userRepository.updateUserProfile(updated)
                Toast.makeText(context, "Cover photo updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val displayName = when {
        user.fullName.isNotBlank() -> user.fullName
        user.firstName.isNotBlank() -> "${user.firstName} ${user.lastName}".trim()
        else -> "User"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("facebook_profile_view")
    ) {
        // 1. Cover & Centered Profile Photo Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                // Cover Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clickable {
                            if (user.coverPictureUrl.isNotBlank()) {
                                showCoverOptionsSheet = true
                            } else if (isMyProfile) {
                                coverPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        }
                ) {
                    if (user.coverPictureUrl.isNotBlank()) {
                        AsyncImage(
                            model = user.coverPictureUrl,
                            contentDescription = "Cover Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE4E6EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isMyProfile) {
                                Text(
                                    text = "Tap to add cover photo",
                                    fontSize = 13.sp,
                                    color = Color(0xFF65676B)
                                )
                            }
                        }
                    }

                    // Cover Camera Button (Only if my profile)
                    if (isMyProfile) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 12.dp)
                                .size(36.dp)
                                .shadow(2.dp, CircleShape)
                        ) {
                            IconButton(onClick = { showCoverOptionsSheet = true }) {
                                if (isUploadingCover) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Cover Options",
                                        tint = Color(0xFF1C1E21),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Centered Profile Avatar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 0.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .shadow(6.dp, CircleShape)
                            .clickable {
                                if (user.profilePictureUrl.isNotBlank()) {
                                    showProfileOptionsSheet = true
                                } else if (isMyProfile) {
                                    profilePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                        shape = CircleShape,
                        color = Color(0xFFD8DADF)
                    ) {
                        if (user.profilePictureUrl.isNotBlank()) {
                            AsyncImage(
                                model = user.profilePictureUrl,
                                contentDescription = displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = displayName.firstOrNull()?.uppercase() ?: "U",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Avatar Edit Button (Centered alignment)
                    if (isMyProfile) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF0F2F5),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(34.dp)
                                .border(2.dp, Color.White, CircleShape)
                        ) {
                            IconButton(onClick = { showProfileOptionsSheet = true }) {
                                if (isUploadingProfile) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Profile Picture Options",
                                        tint = Color(0xFF1C1E21),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Name & Stats
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505),
                        textAlign = TextAlign.Center
                    )
                    if (user.isVerificationActive()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerificationBadge(size = 22.dp, show = true)
                    }
                }

                if (user.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.bio,
                        fontSize = 14.sp,
                        color = Color(0xFF65676B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real Statistics Counters (No Demo Data)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatCounter(title = "Followers", count = user.followersCount)
                    StatCounter(title = "Posts", count = userPosts.size)
                    StatCounter(title = "Following", count = user.followingCount)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                if (isMyProfile) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onAddStoryClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0866FF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Share Story", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = { showEditProfileDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE4E6EB),
                                contentColor = Color(0xFF050505)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Edit Profile", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE4E6EB),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showProfileSettingsSheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Profile Settings",
                                    tint = Color(0xFF050505),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { userRepository.toggleFollow(currentUserId, user.uid) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) Color(0xFFE4E6EB) else Color(0xFF0866FF),
                                contentColor = if (isFollowing) Color(0xFF050505) else Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (isFollowing) {
                            Button(
                                onClick = { onMessageClick(user) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0866FF),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Message", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE4E6EB),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showProfileSettingsSheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Profile Options",
                                    tint = Color(0xFF050505),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Tab Bar (Posts, Reels, Photos, About, Friends)
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = Color.White,
                contentColor = Color(0xFF0866FF),
                divider = { Divider(thickness = 0.5.dp, color = Color(0xFFCED0D4)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    )
                }
            }
        }

        // 4. Tab Content
        when (selectedTabIndex) {
            0 -> { // Posts
                if (userPosts.isEmpty()) {
                    item {
                        EmptyTabContent(message = "No posts yet")
                    }
                } else {
                    items(userPosts, key = { it.id }) { post ->
                        PostCardItem(
                            post = post,
                            currentUserId = currentUserId,
                            currentUserProfile = user,
                            onLikeClick = { postRepository.toggleLike(post.id, currentUserId) },
                            onReactionClick = { r -> postRepository.setReaction(post.id, currentUserId, r) },
                            onCommentClick = { postRepository.addComment(post.id) },
                            onShareClick = { postRepository.incrementShare(post.id) }
                        )
                    }
                }
            }

            1 -> { // Reels
                if (userReels.isEmpty()) {
                    item {
                        EmptyTabContent(message = "No reels published yet")
                    }
                } else {
                    items(userReels, key = { it.id }) { reel ->
                        PostCardItem(
                            post = reel,
                            currentUserId = currentUserId,
                            currentUserProfile = user,
                            onLikeClick = { postRepository.toggleLike(reel.id, currentUserId) },
                            onReactionClick = { r -> postRepository.setReaction(reel.id, currentUserId, r) },
                            onCommentClick = { postRepository.addComment(reel.id) },
                            onShareClick = { postRepository.incrementShare(reel.id) }
                        )
                    }
                }
            }

            2 -> { // Photos
                if (userPhotos.isEmpty()) {
                    item {
                        EmptyTabContent(message = "No photos uploaded yet")
                    }
                } else {
                    item {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            maxItemsInEachRow = 3,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            userPhotos.forEach { photoUrl ->
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(116.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            fullScreenViewUrl = photoUrl
                                            fullScreenViewTitle = "Photo"
                                        }
                                )
                            }
                        }
                    }
                }
            }

            3 -> { // About Details
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(text = "About Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF050505))

                        if (user.work.isNotBlank()) {
                            AboutInfoRow(icon = Icons.Default.Work, text = "Works at ${user.work}")
                        }
                        if (user.education.isNotBlank()) {
                            AboutInfoRow(icon = Icons.Default.School, text = "Studied at ${user.education}")
                        }
                        if (user.currentCity.isNotBlank()) {
                            AboutInfoRow(icon = Icons.Default.LocationOn, text = "Lives in ${user.currentCity}")
                        }
                        if (user.hometown.isNotBlank()) {
                            AboutInfoRow(icon = Icons.Default.Home, text = "From ${user.hometown}")
                        }
                        if (user.relationshipStatus.isNotBlank()) {
                            AboutInfoRow(icon = Icons.Default.Favorite, text = user.relationshipStatus)
                        }
                        if (user.formattedBirthDate.isNotBlank()) {
                            AboutInfoRow(icon = Icons.Default.Cake, text = "Born on ${user.formattedBirthDate}")
                        }
                    }
                }
            }

            4 -> { // Friends (First 20 real users)
                if (followingUserList.isEmpty()) {
                    item {
                        EmptyTabContent(message = "No friends or followed users yet")
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Friends (${followingUserList.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                maxItemsInEachRow = 3,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                followingUserList.forEach { fUser ->
                                    val fName = if (fUser.fullName.isNotBlank()) fUser.fullName else "${fUser.firstName} ${fUser.lastName}".trim()
                                    Column(
                                        modifier = Modifier
                                            .width(105.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onUserClick(fUser) }
                                            .padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(90.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            if (fUser.profilePictureUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = fUser.profilePictureUrl,
                                                    contentDescription = fName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = fName.firstOrNull()?.uppercase() ?: "U",
                                                        fontSize = 28.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = fName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF050505),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
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

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            userProfile = user,
            onSave = { updated ->
                userRepository.updateUserProfile(updated) { success ->
                    if (success) {
                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }

    // Profile Picture Options Bottom Sheet
    if (showProfileOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileOptionsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "Profile Picture", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF050505))

                if (user.profilePictureUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showProfileOptionsSheet = false
                                fullScreenViewUrl = user.profilePictureUrl
                                fullScreenViewTitle = "Profile Picture"
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = "View", tint = Color(0xFF1877F2), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = "View Profile Picture", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF050505))
                    }
                }

                if (isMyProfile) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showProfileOptionsSheet = false
                                profilePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Select", tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = "Choose New Profile Picture", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF050505))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Cover Photo Options Bottom Sheet
    if (showCoverOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCoverOptionsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "Cover Photo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF050505))

                if (user.coverPictureUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showCoverOptionsSheet = false
                                fullScreenViewUrl = user.coverPictureUrl
                                fullScreenViewTitle = "Cover Photo"
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = "View", tint = Color(0xFF1877F2), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = "View Cover Photo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF050505))
                    }
                }

                if (isMyProfile) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showCoverOptionsSheet = false
                                coverPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Select", tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = "Choose New Cover Photo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF050505))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Profile Settings & Actions Bottom Sheet (3-Dots Menu)
    if (showProfileSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isMyProfile) "Profile Settings" else "${displayName}'s Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (isMyProfile) {
                    // 1. Professional Dashboard / Monetization
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showProfileSettingsSheet = false
                                currentSubScreen = ProfileSubScreen.DASHBOARD
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEBF5FF),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Monetization",
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Monetization & Dashboard",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF050505)
                            )
                            Text(
                                text = "View analytics, stars, gifts & creator payouts",
                                fontSize = 12.sp,
                                color = Color(0xFF65676B)
                            )
                        }
                    }

                    // 2. Pages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showProfileSettingsSheet = false
                                currentSubScreen = ProfileSubScreen.PAGES
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFF3E0),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = "Pages",
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Pages",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF050505)
                            )
                            Text(
                                text = "Discover, create and manage your pages",
                                fontSize = 12.sp,
                                color = Color(0xFF65676B)
                            )
                        }
                    }

                    // 3. Groups
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showProfileSettingsSheet = false
                                currentSubScreen = ProfileSubScreen.GROUPS
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "Groups",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Groups",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF050505)
                            )
                            Text(
                                text = "Join, build and participate in communities",
                                fontSize = 12.sp,
                                color = Color(0xFF65676B)
                            )
                        }
                    }

                    // 4. Verification Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showProfileSettingsSheet = false
                                currentSubScreen = ProfileSubScreen.VERIFICATION
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_verified_badge_green),
                                    contentDescription = "Verification Badge",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Verification Badge",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF050505)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (user.isVerificationActive()) {
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
                                text = "Get your Green Verification Badge & build credibility",
                                fontSize = 12.sp,
                                color = Color(0xFF65676B)
                            )
                        }
                    }

                    // 5. Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showProfileSettingsSheet = false
                                currentSubScreen = ProfileSubScreen.SETTINGS
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF0F2F5),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color(0xFF1C1E21),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Settings",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF050505)
                            )
                            Text(
                                text = "Account preferences, privacy and security",
                                fontSize = 12.sp,
                                color = Color(0xFF65676B)
                            )
                        }
                    }

                    Divider(thickness = 0.5.dp, color = Color(0xFFCED0D4), modifier = Modifier.padding(vertical = 6.dp))
                }

                // 5. Copy Profile Link Section
                val profileLink = "https://frndom.app/user/${user.uid}"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F2F5))
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (isMyProfile) "Your Profile Link" else "${displayName}'s Profile Link",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profileLink,
                        fontSize = 13.sp,
                        color = Color(0xFF65676B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(profileLink))
                            Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showProfileSettingsSheet = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Copy Profile Link", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Full Screen Image Viewer Modal Dialog
    fullScreenViewUrl?.let { url ->
        FullScreenImageViewer(
            imageUrl = url,
            title = fullScreenViewTitle,
            onDismiss = { fullScreenViewUrl = null }
        )
    }
}

@Composable
private fun StatCounter(title: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF050505)
        )
        Text(
            text = title,
            fontSize = 13.sp,
            color = Color(0xFF65676B)
        )
    }
}

@Composable
private fun AboutInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF65676B), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 15.sp, color = Color(0xFF050505))
    }
}

@Composable
private fun EmptyTabContent(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, fontSize = 15.sp, color = Color(0xFF65676B))
    }
}
