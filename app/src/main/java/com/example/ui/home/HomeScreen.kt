package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.components.HashtagText
import com.example.ui.components.PostOptionsBottomSheet
import com.example.ui.components.EditPostDialog
import com.example.data.repository.AppSettingsRepository
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.ui.components.VerificationBadge
import com.example.data.model.PostItem
import com.example.data.model.ReactionType
import com.example.data.model.StoryItem
import com.example.data.model.UserProfile
import com.example.data.repository.PostRepository
import com.example.data.repository.StorageRepository
import com.example.data.repository.StoryRepository
import com.example.data.service.MediaUploadService
import com.example.ui.components.CreateStoryDialog
import com.example.ui.components.FacebookReactionsPopup
import com.example.ui.components.FacebookStoriesTray
import com.example.ui.components.FrndomVideoPlayer
import com.example.ui.components.FullScreenImageViewer
import com.example.ui.components.FullScreenStoryViewer
import com.example.ui.create.PostBackgroundStyle

data class ActiveImageViewerState(
    val urls: List<String>,
    val initialIndex: Int = 0,
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val caption: String = "",
    val isAuthorVerified: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    postRepository: PostRepository,
    storyRepository: StoryRepository,
    onCreatePostClick: () -> Unit,
    storageRepository: StorageRepository? = null,
    mediaUploadService: MediaUploadService? = null,
    onProfileClick: () -> Unit = {},
    onUserClick: (UserProfile) -> Unit = {},
    onCommentClick: (PostItem) -> Unit = {},
    onShareClick: (PostItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val posts by postRepository.postsFlow.collectAsState()
    val isRefreshing by postRepository.isRefreshing.collectAsState()
    val stories by storyRepository.storiesFlow.collectAsState()
    val feedPosts = posts.filter { it.mediaType != "reel" }

    val context = androidx.compose.ui.platform.LocalContext.current
    val appSettingsRepository = remember { AppSettingsRepository.getInstance(context) }
    val autoPlayVideos by appSettingsRepository.autoPlayVideos.collectAsState()

    val effectiveMediaUploadService = remember(mediaUploadService, storageRepository) {
        mediaUploadService ?: MediaUploadService(context, storageRepository ?: StorageRepository(context))
    }

    var selectedStoryGroupsToView by remember { mutableStateOf<Pair<Int, List<com.example.ui.components.UserStoryGroup>>?>(null) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    
    // Comments Bottom Sheet State
    var showCommentsSheet by remember { mutableStateOf<PostItem?>(null) }

    // Post Options Bottom Sheet & Edit Dialog State
    var selectedPostForOptions by remember { mutableStateOf<PostItem?>(null) }
    var editingPost by remember { mutableStateOf<PostItem?>(null) }

    // Full Screen Image Viewer State
    var activeImageViewerData by remember { mutableStateOf<ActiveImageViewerState?>(null) }

    val initial = userProfile?.firstName?.firstOrNull()?.uppercase()
        ?: userProfile?.fullName?.firstOrNull()?.uppercase()
        ?: "U"
    val userId = userProfile?.uid ?: "user_id"

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                postRepository.refreshPosts()
                storyRepository.refreshStories()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF0F2F5))
                    .testTag("home_screen_feed")
            ) {
                // 1. "What's on your mind?" Top Bar
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compose_post_bar"),
                        color = Color.White,
                        shadowElevation = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User Avatar
                            Surface(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onProfileClick)
                                    .testTag("home_user_avatar"),
                                shape = CircleShape,
                                color = Color(0xFFE4E6EB)
                            ) {
                                if (!userProfile?.profilePictureUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = userProfile?.profilePictureUrl,
                                        contentDescription = "User Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = initial,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Input Box Pill
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(onClick = onCreatePostClick)
                                    .testTag("whats_on_your_mind_button"),
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF0F2F5)
                            ) {
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = "What's on your mind?",
                                        color = Color(0xFF65676B),
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Green Gallery/Photo Icon Button
                            IconButton(
                                onClick = onCreatePostClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("home_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Photos",
                                    tint = Color(0xFF45BD62),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 2. Facebook-Style Stories Tray
                item {
                    FacebookStoriesTray(
                        userProfile = userProfile,
                        stories = stories,
                        onCreateStoryClick = { showCreateStoryDialog = true },
                        onStoryGroupClick = { clickedGroup, allGroups ->
                            val idx = allGroups.indexOfFirst { it.userId == clickedGroup.userId }.coerceAtLeast(0)
                            selectedStoryGroupsToView = Pair(idx, allGroups)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. Posts Feed
                val isLoadingInitial = feedPosts.isEmpty() && posts.isEmpty() && !isRefreshing
                if (isRefreshing || isLoadingInitial) {
                    items(3) {
                        ShimmerPostCard()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else if (feedPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No posts yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF65676B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap the '+' button or 'What's on your mind?' to share a post!",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8A8D91),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(feedPosts, key = { it.id }) { post ->
                        PostCardItem(
                            post = post,
                            currentUserId = userId,
                            currentUserProfile = userProfile,
                            autoPlayVideos = autoPlayVideos,
                            onUserClick = {
                                val peer = UserProfile(
                                    uid = post.authorId,
                                    fullName = post.authorName,
                                    profilePictureUrl = post.authorAvatarUrl
                                )
                                onUserClick(peer)
                            },
                            onOptionsClick = {
                                selectedPostForOptions = post
                            },
                            onLikeClick = { postRepository.toggleLike(post.id, userId) },
                            onReactionClick = { r -> postRepository.setReaction(post.id, userId, r) },
                            onCommentClick = {
                                showCommentsSheet = post
                            },
                            onShareClick = {
                                postRepository.incrementShare(post.id)
                                onShareClick(post)
                            },
                            onImageClick = { urls, index ->
                                val isAuthorVerified = post.isAuthorVerified || (userProfile != null && post.authorId == userProfile.uid && userProfile.isVerificationActive())
                                activeImageViewerData = ActiveImageViewerState(
                                    urls = urls,
                                    initialIndex = index,
                                    authorName = post.authorName,
                                    authorAvatarUrl = post.authorAvatarUrl,
                                    caption = post.content,
                                    isAuthorVerified = isAuthorVerified
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Full Screen Story Viewer
        selectedStoryGroupsToView?.let { (initialIndex, groups) ->
            FullScreenStoryViewer(
                storyGroups = groups,
                initialGroupIndex = initialIndex,
                currentUserProfile = userProfile,
                storyRepository = storyRepository,
                onDismiss = { selectedStoryGroupsToView = null }
            )
        }

        // Full Screen Multi-Image Viewer
        activeImageViewerData?.let { data ->
            FullScreenImageViewer(
                images = data.urls,
                initialIndex = data.initialIndex,
                title = "Photo",
                authorName = data.authorName,
                authorAvatarUrl = data.authorAvatarUrl,
                caption = data.caption,
                isAuthorVerified = data.isAuthorVerified,
                onDismiss = { activeImageViewerData = null }
            )
        }

        // Create Story Dialog
        if (showCreateStoryDialog) {
            CreateStoryDialog(
                userProfile = userProfile,
                mediaUploadService = effectiveMediaUploadService,
                onStoryCreated = { newStory ->
                    storyRepository.createStory(newStory)
                    showCreateStoryDialog = false
                },
                onDismiss = { showCreateStoryDialog = false }
            )
        }

        // Comments Bottom Sheet
        showCommentsSheet?.let { post ->
            com.example.ui.components.CommentsBottomSheet(
                postRepository = postRepository,
                postId = post.id,
                userProfile = userProfile,
                onDismiss = { showCommentsSheet = null },
                onCommentAdded = { }
            )
        }

        // Post Options Bottom Sheet (Edit, Delete, Save, Copy Link, Report)
        selectedPostForOptions?.let { post ->
            PostOptionsBottomSheet(
                post = post,
                currentUserId = userId,
                postRepository = postRepository,
                onEditClick = { editingPost = it },
                onDeletePost = {
                    selectedPostForOptions = null
                },
                onDismiss = { selectedPostForOptions = null }
            )
        }

        // Edit Post Dialog (Edit text, replace photos, locked video for video posts)
        editingPost?.let { post ->
            EditPostDialog(
                post = post,
                postRepository = postRepository,
                mediaUploadService = effectiveMediaUploadService,
                onDismiss = { editingPost = null },
                onPostUpdated = {
                    editingPost = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCardItem(
    post: PostItem,
    currentUserId: String,
    currentUserProfile: UserProfile? = null,
    autoPlayVideos: Boolean = true,
    onUserClick: () -> Unit = {},
    onOptionsClick: () -> Unit = {},
    onLikeClick: () -> Unit,
    onReactionClick: (ReactionType) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onImageClick: ((urls: List<String>, index: Int) -> Unit)? = null
) {
    val userReaction = post.getUserReaction(currentUserId)
    val isVerifiedAuthor = post.isAuthorVerified || (currentUserProfile != null && post.authorId == currentUserProfile.uid && currentUserProfile.isVerificationActive())
    val authorInitial = post.authorName.firstOrNull()?.uppercase() ?: "U"

    var showReactionsTray by remember { mutableStateOf(false) }

    val bgStyle = PostBackgroundStyle.entries.firstOrNull { it.id == post.backgroundStyle } ?: PostBackgroundStyle.NONE
    val hasBackground = bgStyle != PostBackgroundStyle.NONE && bgStyle != PostBackgroundStyle.WHITE

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        color = Color.White,
        shadowElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onUserClick)
                        .padding(2.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFFD8DADF)
                    ) {
                        if (post.authorAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = post.authorAvatarUrl,
                                contentDescription = post.authorName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = authorInitial,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        if (post.groupName.isNotBlank()) {
                            Text(
                                text = post.groupName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = post.authorName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1877F2)
                                )
                                if (isVerifiedAuthor) {
                                    VerificationBadge(size = 14.dp, show = true)
                                }
                                Text(
                                    text = " • Just now • ",
                                    fontSize = 12.sp,
                                    color = Color(0xFF65676B)
                                )
                                Icon(
                                    imageVector = when (post.audience) {
                                        "Private" -> Icons.Default.Lock
                                        else -> Icons.Default.Public
                                    },
                                    contentDescription = post.audience,
                                    tint = Color(0xFF65676B),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = post.authorName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                                if (isVerifiedAuthor) {
                                    VerificationBadge(size = 16.dp, show = true)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Just now",
                                    fontSize = 12.sp,
                                    color = Color(0xFF65676B)
                                )
                                Text(
                                    text = " • ",
                                    fontSize = 12.sp,
                                    color = Color(0xFF65676B)
                                )
                                Icon(
                                    imageVector = when (post.audience) {
                                        "Friends" -> Icons.Default.Group
                                        "Only Me" -> Icons.Default.Lock
                                        else -> Icons.Default.Public
                                    },
                                    contentDescription = post.audience,
                                    tint = Color(0xFF65676B),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onOptionsClick) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = Color(0xFF65676B)
                    )
                }
            }

            // Post Content
            if (hasBackground) {
                // Colored / Gradient Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .then(
                            if (bgStyle.isGradient) {
                                Modifier.background(Brush.linearGradient(bgStyle.gradientColors))
                            } else {
                                Modifier.background(bgStyle.singleColor)
                            }
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HashtagText(
                        text = post.content,
                        fontSize = post.fontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = bgStyle.textColor,
                        hashtagColor = if (bgStyle.textColor == Color.White) Color(0xFF80D8FF) else Color(0xFF1877F2),
                        textAlign = when (post.textAlign) {
                            "left" -> TextAlign.Left
                            "right" -> TextAlign.Right
                            else -> TextAlign.Center
                        },
                        lineHeight = (post.fontSize + 6).sp
                    )
                }
            } else {
                // Normal Text Post
                if (post.content.isNotEmpty()) {
                    HashtagText(
                        text = post.content,
                        fontSize = 15.sp,
                        color = Color(0xFF050505),
                        hashtagColor = Color(0xFF1877F2),
                        lineHeight = 21.sp,
                        textAlign = when (post.textAlign) {
                            "left" -> TextAlign.Left
                            "right" -> TextAlign.Right
                            else -> TextAlign.Start
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Media Video or Images
                if (post.mediaType == "video" || post.mediaType == "reel") {
                    if (post.mediaUrl.isNotBlank()) {
                        FrndomVideoPlayer(
                            videoUrl = post.mediaUrl,
                            autoPlay = autoPlayVideos,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    }
                } else {
                    val allUrls = post.getAllMediaUrls()
                    if (allUrls.isNotEmpty()) {
                        PostMediaGrid(
                            urls = allUrls,
                            onImageClick = { urls, index ->
                                onImageClick?.invoke(urls, index)
                            }
                        )
                    }
                }
            }

            // Reactions Counts Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (post.likesCount > 0) {
                        val topReactions = post.getTopReactions()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            topReactions.forEachIndexed { index, reaction ->
                                Box(
                                    modifier = Modifier
                                        .offset(x = if (index > 0) (-4 * index).dp else 0.dp)
                                        .zIndex((topReactions.size - index).toFloat())
                                ) {
                                    Text(
                                        text = reaction.emoji,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${post.likesCount}",
                            fontSize = 13.sp,
                            color = Color(0xFF65676B)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (post.commentsCount > 0) {
                        Text(
                            text = "${post.commentsCount} comments",
                            fontSize = 13.sp,
                            color = Color(0xFF65676B)
                        )
                    }
                    if (post.sharesCount > 0) {
                        Text(
                            text = "${post.sharesCount} shares",
                            fontSize = 13.sp,
                            color = Color(0xFF65676B)
                        )
                    }
                }
            }

            Divider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 0.5.dp,
                color = Color(0xFFE4E6EB)
            )

            // Action Buttons: Like, Comment, Share with Floating Reaction popup
            Box(modifier = Modifier.fillMaxWidth()) {
                // Floating Reactions Tray
                if (showReactionsTray) {
                    Box(
                        modifier = Modifier
                            .offset(x = 12.dp, y = (-46).dp)
                            .zIndex(10f)
                    ) {
                        FacebookReactionsPopup(
                            onReactionSelected = { reaction ->
                                onReactionClick(reaction)
                                showReactionsTray = false
                            },
                            onDismiss = { showReactionsTray = false }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Like Button (Click = Like toggle, Long Click = Reactions popup)
                    Row(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = onLikeClick,
                                onLongClick = { showReactionsTray = true }
                            )
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .testTag("post_like_button_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (userReaction != null) {
                            Text(
                                text = userReaction.emoji,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = userReaction.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(userReaction.colorHex)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = Color(0xFF65676B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Like",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF65676B)
                            )
                        }
                    }

                    // Comment Button
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onCommentClick)
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .testTag("post_comment_button_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = Color(0xFF65676B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Comment",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF65676B)
                        )
                    }

                    // Share Button
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onShareClick)
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .testTag("post_share_button_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFF65676B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF65676B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostMediaGrid(
    urls: List<String>,
    modifier: Modifier = Modifier,
    onImageClick: (urls: List<String>, index: Int) -> Unit = { _, _ -> }
) {
    if (urls.isEmpty()) return

    when (urls.size) {
        1 -> {
            // Adaptive Single Image: Fits wide (16:9), square (1:1), and vertical (4:5 / 9:16) without cutting off!
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(Color(0xFF0F0F0F))
                    .clip(RoundedCornerShape(0.dp))
                    .clickable { onImageClick(urls, 0) },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = urls[0],
                    contentDescription = "Post Image",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(min = 180.dp, max = 560.dp)
                )
            }
        }
        2 -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onImageClick(urls, 0) }
                ) {
                    AsyncImage(
                        model = urls[0],
                        contentDescription = "Photo 1",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onImageClick(urls, 1) }
                ) {
                    AsyncImage(
                        model = urls[1],
                        contentDescription = "Photo 2",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        3 -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxSize()
                        .clickable { onImageClick(urls, 0) }
                ) {
                    AsyncImage(
                        model = urls[0],
                        contentDescription = "Photo 1",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable { onImageClick(urls, 1) }
                    ) {
                        AsyncImage(
                            model = urls[1],
                            contentDescription = "Photo 2",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable { onImageClick(urls, 2) }
                    ) {
                        AsyncImage(
                            model = urls[2],
                            contentDescription = "Photo 3",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        else -> {
            // 4 or more photos (up to 10) with 2x2 grid & +N overlay
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Top row
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onImageClick(urls, 0) }
                    ) {
                        AsyncImage(
                            model = urls[0],
                            contentDescription = "Photo 1",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onImageClick(urls, 1) }
                    ) {
                        AsyncImage(
                            model = urls[1],
                            contentDescription = "Photo 2",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Bottom row
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onImageClick(urls, 2) }
                    ) {
                        AsyncImage(
                            model = urls[2],
                            contentDescription = "Photo 3",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onImageClick(urls, 3) }
                    ) {
                        AsyncImage(
                            model = urls[3],
                            contentDescription = "Photo 4",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (urls.size > 4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${urls.size - 3}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerPostCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    val shimmerColor = Color(0xFFE4E6EB).copy(alpha = alpha)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(shimmerColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Box(modifier = Modifier.height(14.dp).width(120.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.height(10.dp).width(80.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.9f).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.7f).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Media Image Placeholder
            Box(modifier = Modifier.height(200.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(shimmerColor))
        }
    }
}
