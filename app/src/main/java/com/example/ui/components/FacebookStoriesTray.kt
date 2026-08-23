package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.repository.UserRepository
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.StoryItem
import com.example.data.model.StoryViewerInfo
import com.example.data.model.UserProfile
import com.example.data.repository.StoryRepository
import com.example.ui.create.PostBackgroundStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class UserStoryGroup(
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val isVerified: Boolean = false,
    val stories: List<StoryItem>
) {
    val count: Int get() = stories.size
    val latestStory: StoryItem get() = stories.maxByOrNull { it.createdAt } ?: stories.first()
}

@Composable
fun FacebookStoriesTray(
    userProfile: UserProfile?,
    stories: List<StoryItem>,
    onCreateStoryClick: () -> Unit,
    onStoryGroupClick: (UserStoryGroup, List<UserStoryGroup>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userRepo = remember { UserRepository(context) }
    val currentUserId = userProfile?.uid ?: ""
    val currentUserAvatar = userProfile?.profilePictureUrl ?: ""
    val initial = userProfile?.firstName?.firstOrNull()?.uppercase()
        ?: userProfile?.fullName?.firstOrNull()?.uppercase()
        ?: "U"

    // Group stories by userId and sort stories inside each group chronologically
    val storyGroups = remember(stories, currentUserId, currentUserAvatar, userProfile) {
        val groups = stories.groupBy { it.userId }
            .map { (uid, userStories) ->
                val sorted = userStories.sortedBy { it.createdAt }
                val first = sorted.first()
                val cachedUser = userRepo.getLocalUserProfile(uid)

                val avatar = if (uid == currentUserId && currentUserAvatar.isNotBlank()) {
                    currentUserAvatar
                } else if (!cachedUser?.profilePictureUrl.isNullOrBlank()) {
                    cachedUser?.profilePictureUrl ?: ""
                } else {
                    first.userAvatar.ifBlank { if (uid == currentUserId) currentUserAvatar else "" }
                }

                val name = if (uid == currentUserId && !userProfile?.fullName.isNullOrBlank()) {
                    userProfile?.fullName ?: first.userName
                } else if (!cachedUser?.fullName.isNullOrBlank()) {
                    cachedUser?.fullName ?: first.userName
                } else {
                    first.userName.ifBlank { "User" }
                }

                val isVerifiedUser = if (uid == currentUserId) {
                    userProfile?.isVerificationActive() == true || userRepo.getPersistentVerification(currentUserId) != null
                } else {
                    cachedUser?.isVerificationActive() == true || userRepo.getPersistentVerification(uid) != null
                }

                UserStoryGroup(
                    userId = uid,
                    userName = name,
                    userAvatar = avatar,
                    isVerified = isVerifiedUser,
                    stories = sorted
                )
            }

        // Put current user's stories first if they exist, followed by others sorted by latest post
        val (ownGroup, otherGroups) = groups.partition { it.userId == currentUserId }
        val sortedOthers = otherGroups.sortedByDescending { it.latestStory.createdAt }
        ownGroup + sortedOthers
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("facebook_stories_tray"),
        color = Color.White,
        shadowElevation = 0.5.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. "Create Story" Card (Facebook-Style)
            item {
                Card(
                    modifier = Modifier
                        .width(108.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onCreateStoryClick)
                        .testTag("create_story_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top User Avatar Image Canvas (68%)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.68f)
                                    .background(Color(0xFFE4E6EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentUserAvatar.isNotBlank()) {
                                    AsyncImage(
                                        model = currentUserAvatar,
                                        contentDescription = "User Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.size(54.dp),
                                        shape = CircleShape,
                                        color = Color(0xFFD8DADF)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = initial,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // Bottom White Area with Label (32%)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.32f)
                                    .background(Color.White),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    text = "Create story",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        // Overlapping Blue '+' FAB Circle
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1877F2),
                            border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 40.dp)
                                .size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Story",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Active User Story Groups (Grouped by User)
            items(storyGroups, key = { it.userId }) { group ->
                StoryGroupCardItem(
                    group = group,
                    isCurrentUser = group.userId == currentUserId,
                    currentUserAvatar = currentUserAvatar,
                    onClick = { onStoryGroupClick(group, storyGroups) }
                )
            }
        }
    }
}

@Composable
fun StoryGroupCardItem(
    group: UserStoryGroup,
    isCurrentUser: Boolean,
    currentUserAvatar: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestStory = group.latestStory
    val authorInitial = group.userName.firstOrNull()?.uppercase() ?: "U"
    val bgStyle = PostBackgroundStyle.entries.firstOrNull { it.id == latestStory.backgroundStyle }
        ?: PostBackgroundStyle.BG_SUNSET

    val avatarToDisplay = if (isCurrentUser && currentUserAvatar.isNotBlank()) {
        currentUserAvatar
    } else {
        group.userAvatar.ifBlank { if (isCurrentUser) currentUserAvatar else "" }
    }

    Card(
        modifier = modifier
            .width(108.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("story_card_${group.userId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (latestStory.mediaUrl.isNotBlank()) {
                        Modifier
                    } else if (bgStyle.isGradient) {
                        Modifier.background(Brush.linearGradient(bgStyle.gradientColors))
                    } else if (bgStyle != PostBackgroundStyle.NONE) {
                        Modifier.background(bgStyle.singleColor)
                    } else {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF1E3A8A))
                            )
                        )
                    }
                )
        ) {
            // Media Image / Thumbnail if available
            if (latestStory.mediaUrl.isNotBlank()) {
                AsyncImage(
                    model = latestStory.mediaUrl,
                    contentDescription = latestStory.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Dark gradient overlay for text legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // Story Text in Center if no photo
            if (latestStory.mediaUrl.isBlank() && latestStory.caption.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = latestStory.caption,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Top-Left User Avatar with Blue/White Border Ring (Facebook style)
            Surface(
                shape = CircleShape,
                color = Color(0xFF1877F2),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1877F2)),
                modifier = Modifier
                    .padding(8.dp)
                    .size(36.dp)
                    .align(Alignment.TopStart)
            ) {
                if (avatarToDisplay.isNotBlank()) {
                    AsyncImage(
                        model = avatarToDisplay,
                        contentDescription = group.userName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = authorInitial,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Top-Right Multi-Story Count Badge (if user has > 1 story)
            if (group.count > 1) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "${group.count}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Bottom Author Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isCurrentUser) "Your story" else group.userName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (group.isVerified) {
                    VerificationBadge(size = 12.dp, show = true)
                }
            }
        }
    }
}

private val STORY_REACTIONS = listOf(
    "LIKE" to "👍",
    "LOVE" to "❤️",
    "HAHA" to "😂",
    "WOW" to "😮",
    "SAD" to "😢",
    "ANGRY" to "😡"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenStoryViewer(
    storyGroups: List<UserStoryGroup>,
    initialGroupIndex: Int = 0,
    currentUserProfile: UserProfile?,
    storyRepository: StoryRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (storyGroups.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val context = LocalContext.current
    val userRepo = remember { UserRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = currentUserProfile?.uid ?: ""
    val currentUserName = currentUserProfile?.fullName.orEmpty().ifBlank {
        "${currentUserProfile?.firstName} ${currentUserProfile?.lastName}".trim()
    }.ifBlank { "User" }
    val currentUserAvatar = currentUserProfile?.profilePictureUrl ?: ""

    var groupIndex by remember {
        mutableIntStateOf(initialGroupIndex.coerceIn(0, storyGroups.size - 1))
    }
    var storyIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    // Floating reaction animation state
    var activeReactionEmoji by remember { mutableStateOf<String?>(null) }
    val reactionAnimY = remember { Animatable(0f) }
    val reactionAnimAlpha = remember { Animatable(1f) }

    val currentGroup = storyGroups.getOrNull(groupIndex) ?: storyGroups.first()
    val activeStory = currentGroup.stories.getOrNull(storyIndex) ?: currentGroup.stories.first()
    val isOwner = activeStory.userId == currentUserId

    // Relative formatted time
    val timeAgo = remember(activeStory.createdAt) {
        formatStoryTimeAgo(activeStory.createdAt)
    }

    // Auto mark viewed if viewing someone else's story
    LaunchedEffect(activeStory.id, groupIndex, storyIndex) {
        if (!isOwner && currentUserId.isNotBlank() && activeStory.id.isNotBlank()) {
            storyRepository.markStoryViewed(
                storyId = activeStory.id,
                viewer = StoryViewerInfo(
                    userId = currentUserId,
                    userName = currentUserName,
                    userAvatar = currentUserAvatar,
                    viewedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // Story progress animation timer
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(groupIndex, storyIndex, isPaused, showViewersSheet) {
        if (!isPaused && !showViewersSheet) {
            val totalDurationMs = 5000L
            val intervalMs = 50L
            val step = intervalMs.toFloat() / totalDurationMs.toFloat()

            while (progress < 1f) {
                delay(intervalMs)
                if (!isPaused && !showViewersSheet) {
                    progress += step
                }
            }

            // Auto Advance to next story / group
            if (storyIndex < currentGroup.stories.size - 1) {
                storyIndex++
                progress = 0f
            } else if (groupIndex < storyGroups.size - 1) {
                groupIndex++
                storyIndex = 0
                progress = 0f
            } else {
                onDismiss()
            }
        }
    }

    fun goToNextStory() {
        if (storyIndex < currentGroup.stories.size - 1) {
            storyIndex++
            progress = 0f
        } else if (groupIndex < storyGroups.size - 1) {
            groupIndex++
            storyIndex = 0
            progress = 0f
        } else {
            onDismiss()
        }
    }

    fun goToPreviousStory() {
        if (storyIndex > 0) {
            storyIndex--
            progress = 0f
        } else if (groupIndex > 0) {
            groupIndex--
            val prevGroup = storyGroups[groupIndex]
            storyIndex = (prevGroup.stories.size - 1).coerceAtLeast(0)
            progress = 0f
        }
    }

    val storyAuthorProfile = remember(activeStory.userId) { userRepo.getLocalUserProfile(activeStory.userId) }
    val authorAvatar = if (isOwner && currentUserAvatar.isNotBlank()) {
        currentUserAvatar
    } else if (!storyAuthorProfile?.profilePictureUrl.isNullOrBlank()) {
        storyAuthorProfile?.profilePictureUrl ?: ""
    } else {
        activeStory.userAvatar.ifBlank { currentGroup.userAvatar }
    }
    val authorName = if (isOwner && !currentUserProfile?.fullName.isNullOrBlank()) {
        currentUserProfile?.fullName ?: activeStory.userName
    } else if (!storyAuthorProfile?.fullName.isNullOrBlank()) {
        storyAuthorProfile?.fullName ?: activeStory.userName
    } else {
        activeStory.userName.ifBlank { currentGroup.userName }
    }
    val authorInitial = authorName.firstOrNull()?.uppercase() ?: "U"
    val bgStyle = PostBackgroundStyle.entries.firstOrNull { it.id == activeStory.backgroundStyle }
        ?: PostBackgroundStyle.BG_SUNSET

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("story_viewer_dialog")
                .pointerInput(groupIndex, storyIndex) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val width = size.width
                            if (offset.x < width * 0.35f) {
                                goToPreviousStory()
                            } else if (offset.x > width * 0.65f) {
                                goToNextStory()
                            }
                        }
                    )
                }
                .pointerInput(groupIndex) {
                    var totalDragX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onDragEnd = {
                            if (totalDragX < -100f) {
                                // Swipe Left -> Next User
                                if (groupIndex < storyGroups.size - 1) {
                                    groupIndex++
                                    storyIndex = 0
                                    progress = 0f
                                } else {
                                    onDismiss()
                                }
                            } else if (totalDragX > 100f) {
                                // Swipe Right -> Previous User
                                if (groupIndex > 0) {
                                    groupIndex--
                                    storyIndex = 0
                                    progress = 0f
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDragX += dragAmount
                        }
                    )
                }
        ) {
            // 1. Story Visual Content (Media / Video / Gradient + Text)
            if (activeStory.mediaUrl.isNotBlank()) {
                if (activeStory.mediaType == "video") {
                    FrndomVideoPlayer(
                        videoUrl = activeStory.mediaUrl,
                        modifier = Modifier.fillMaxSize(),
                        autoPlay = !isPaused && !showViewersSheet,
                        isLooping = true
                    )
                } else {
                    AsyncImage(
                        model = activeStory.mediaUrl,
                        contentDescription = activeStory.caption,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (bgStyle.isGradient) {
                                Modifier.background(Brush.linearGradient(bgStyle.gradientColors))
                            } else {
                                Modifier.background(bgStyle.singleColor)
                            }
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeStory.caption,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Top Gradient Scrim for Header Legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom Gradient Scrim for Bottom Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // 2. Top Header (Segmented Progress Bars + User Info + Close Button)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp)
            ) {
                // Segmented Progress Bars (one for each story of the current user)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentGroup.stories.forEachIndexed { idx, _ ->
                        val segmentProgress = when {
                            idx < storyIndex -> 1f
                            idx == storyIndex -> progress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Author Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1877F2),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                            modifier = Modifier.size(38.dp)
                        ) {
                            if (authorAvatar.isNotBlank()) {
                                AsyncImage(
                                    model = authorAvatar,
                                    contentDescription = authorName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = authorInitial,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isOwner) "Your story" else authorName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (currentGroup.isVerified || (isOwner && (currentUserProfile?.isVerificationActive() == true || userRepo.getPersistentVerification(currentUserId) != null)) || (storyAuthorProfile?.isVerificationActive() == true || userRepo.getPersistentVerification(activeStory.userId) != null)) {
                                    VerificationBadge(size = 14.dp, show = true)
                                }
                                if (currentGroup.count > 1) {
                                    Text(
                                        text = " (${storyIndex + 1}/${currentGroup.count})",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Text(
                                text = timeAgo,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOwner) {
                            IconButton(onClick = {
                                isPaused = true
                                showDeleteConfirmDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Story",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Story",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 3. Floating Reaction Animation Bubble
            activeReactionEmoji?.let { emoji ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset { IntOffset(0, reactionAnimY.value.roundToInt()) }
                        .padding(bottom = 90.dp)
                ) {
                    Text(
                        text = emoji,
                        fontSize = 52.sp,
                        modifier = Modifier.alpha(reactionAnimAlpha.value)
                    )
                }
            }

            // 4. Bottom Bar: Unified Reaction & Reply Controls (With Owner Viewers Bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isOwner) {
                        // Owner View: "👁️ X Viewers" Pill Button
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    isPaused = true
                                    showViewersSheet = true
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveRedEye,
                                        contentDescription = "Viewers",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (activeStory.viewersCount > 0) {
                                            "${activeStory.viewersCount} Viewers"
                                        } else {
                                            "No viewers yet"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                val topReactions = activeStory.viewers.values
                                    .filter { it.reaction.isNotBlank() }
                                    .mapNotNull { v -> STORY_REACTIONS.firstOrNull { it.first == v.reaction }?.second }
                                    .take(3)

                                if (topReactions.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        topReactions.forEach { emoji ->
                                            Text(text = emoji, fontSize = 15.sp, modifier = Modifier.padding(start = 2.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Activity",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Tap to see viewers",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // Floating Quick Reaction Bar (Always visible for design fidelity)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        STORY_REACTIONS.forEach { (reactionKey, emoji) ->
                            val hasReacted = !isOwner && activeStory.viewers[currentUserId]?.reaction == reactionKey
                            Surface(
                                shape = CircleShape,
                                color = if (hasReacted) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.3f),
                                border = if (hasReacted) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1877F2)) else null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        coroutineScope.launch {
                                            activeReactionEmoji = emoji
                                            reactionAnimY.snapTo(0f)
                                            reactionAnimAlpha.snapTo(1f)

                                            // Only record reaction in repository if viewer is NOT the owner
                                            if (!isOwner && currentUserId.isNotBlank()) {
                                                storyRepository.reactToStory(
                                                    storyId = activeStory.id,
                                                    viewer = StoryViewerInfo(
                                                        userId = currentUserId,
                                                        userName = currentUserName,
                                                        userAvatar = currentUserAvatar,
                                                        reaction = reactionKey,
                                                        viewedAt = System.currentTimeMillis()
                                                    ),
                                                    reaction = reactionKey
                                                )
                                            }

                                            launch {
                                                reactionAnimY.animateTo(
                                                    targetValue = -350f,
                                                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                                                )
                                            }
                                            launch {
                                                delay(300)
                                                reactionAnimAlpha.animateTo(0f, tween(400))
                                                activeReactionEmoji = null
                                            }
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = emoji,
                                        fontSize = 22.sp
                                    )
                                }
                            }
                        }
                    }

                    // Reply Input Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (replyText.isEmpty()) {
                                    Text(
                                        text = if (isOwner) "Send a reply to your story..." else "Send a reply to ${activeStory.userName}...",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                                BasicTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    coroutineScope.launch {
                                        if (!isOwner && currentUserId.isNotBlank()) {
                                            storyRepository.reactToStory(
                                                storyId = activeStory.id,
                                                viewer = StoryViewerInfo(
                                                    userId = currentUserId,
                                                    userName = currentUserName,
                                                    userAvatar = currentUserAvatar,
                                                    reaction = "LIKE",
                                                    viewedAt = System.currentTimeMillis()
                                                ),
                                                reaction = "LIKE"
                                            )
                                        }
                                        replyText = ""
                                    }
                                }
                            },
                            enabled = replyText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (replyText.isNotBlank()) Color(0xFF1877F2) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 5. Delete Story Confirmation Dialog
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteConfirmDialog = false
                        isPaused = false
                    },
                    title = { Text("Delete this story?") },
                    text = { Text("This will permanently remove this story from Frndom.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                storyRepository.deleteStory(activeStory.id)
                                showDeleteConfirmDialog = false
                                isPaused = false
                                if (currentGroup.stories.size <= 1) {
                                    onDismiss()
                                } else {
                                    storyIndex = (storyIndex - 1).coerceAtLeast(0)
                                    progress = 0f
                                }
                            }
                        ) {
                            Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDeleteConfirmDialog = false
                            isPaused = false
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // 6. Viewers & Reactions Bottom Sheet
            if (showViewersSheet) {
                StoryViewersBottomSheet(
                    story = activeStory,
                    onDismiss = {
                        showViewersSheet = false
                        isPaused = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewersBottomSheet(
    story: StoryItem,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewersList = story.viewersList
    val totalViews = viewersList.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Story Activity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE7F3FF),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "$totalViews ${if (totalViews == 1) "view" else "views"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1877F2),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Reactions Summary Badges
            val reactionsCountMap = remember(viewersList) {
                viewersList
                    .filter { it.reaction.isNotBlank() }
                    .groupBy { it.reaction }
                    .mapValues { it.value.size }
            }

            if (reactionsCountMap.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reactionsCountMap.toList()) { (reactionKey, count) ->
                        val emoji = STORY_REACTIONS.firstOrNull { it.first == reactionKey }?.second ?: "👍"
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF0F2F5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E6EB))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = emoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$count",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE4E6EB), thickness = 0.8.dp)

            // Viewers List
            if (viewersList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = "No Views",
                            tint = Color(0xFF8A8D91),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No viewers yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF65676B)
                        )
                        Text(
                            text = "When friends view your story, they'll show up here.",
                            fontSize = 12.sp,
                            color = Color(0xFF8A8D91),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewersList, key = { it.userId }) { viewer ->
                        ViewerRowItem(viewer = viewer)
                    }
                }
            }
        }
    }
}

@Composable
fun ViewerRowItem(
    viewer: StoryViewerInfo,
    modifier: Modifier = Modifier
) {
    val initial = viewer.userName.firstOrNull()?.uppercase() ?: "U"
    val reactionEmoji = STORY_REACTIONS.firstOrNull { it.first == viewer.reaction }?.second
    val timeAgo = remember(viewer.viewedAt) { formatStoryTimeAgo(viewer.viewedAt) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Viewer Avatar with mini reaction badge overlay
            Box(modifier = Modifier.size(44.dp)) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1877F2),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (viewer.userAvatar.isNotBlank()) {
                        AsyncImage(
                            model = viewer.userAvatar,
                            contentDescription = viewer.userName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initial,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // If viewer reacted, show emoji badge at bottom-right of avatar
                if (reactionEmoji != null) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = reactionEmoji, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = viewer.userName.ifBlank { "Frndom User" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )
                Text(
                    text = timeAgo,
                    fontSize = 11.sp,
                    color = Color(0xFF65676B)
                )
            }
        }

        if (reactionEmoji != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0F2F5),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = reactionEmoji, fontSize = 16.sp)
                }
            }
        }
    }
}

private fun formatStoryTimeAgo(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
