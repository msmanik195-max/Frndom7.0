package com.example.ui.reels

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostItem
import com.example.data.model.ReactionType
import com.example.data.model.UserProfile
import com.example.data.repository.PostRepository
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.FrndomVideoPlayer
import com.example.ui.components.HashtagText
import com.example.ui.components.PostOptionsBottomSheet
import com.example.ui.components.EditPostDialog
import com.example.ui.components.VerificationBadge
import com.example.data.service.MediaUploadService
import com.example.data.repository.StorageRepository
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReelsScreen(
    userProfile: UserProfile?,
    postRepository: PostRepository,
    onCreateReelClick: () -> Unit = {},
    onCommentClick: (PostItem) -> Unit = {},
    onShareClick: (PostItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageRepository = remember { StorageRepository(context) }
    val mediaUploadService = remember { MediaUploadService(context, storageRepository) }

    val posts by postRepository.postsFlow.collectAsState()
    val reels = remember(posts) { posts.filter { it.mediaType == "reel" || it.mediaType == "video" } }
    val userId = userProfile?.uid ?: "user_id"

    var showCommentsSheet by remember { mutableStateOf<PostItem?>(null) }
    var selectedReelForOptions by remember { mutableStateOf<PostItem?>(null) }
    var editingReel by remember { mutableStateOf<PostItem?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_screen")
    ) {
        if (reels.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = "No Reels",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No Reels Yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Be the first to share a video reel!",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onCreateReelClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Create Reel", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { reels.size })

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val reel = reels[page]
                ReelVideoItem(
                    reel = reel,
                    currentUserId = userId,
                    currentUserProfile = userProfile,
                    onDoubleTapLike = {
                        postRepository.setReaction(reel.id, userId, ReactionType.LOVE)
                    },
                    onLikeClick = { postRepository.toggleLike(reel.id, userId) },
                    onCommentClick = {
                        showCommentsSheet = reel
                    },
                    onOptionsClick = {
                        selectedReelForOptions = reel
                    },
                    onShareClick = {
                        postRepository.incrementShare(reel.id)
                        onShareClick(reel)
                    }
                )
            }
        }

        // Top Bar (Reels header title + Camera/Create Reel Floating Action Icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reels",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onCreateReelClick)
                    .testTag("reels_create_fab")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Create Reel",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Comments Bottom Sheet
        showCommentsSheet?.let { reel ->
            CommentsBottomSheet(
                postRepository = postRepository,
                postId = reel.id,
                userProfile = userProfile,
                onDismiss = { showCommentsSheet = null },
                onCommentAdded = { }
            )
        }

        // Reel Options Bottom Sheet (Edit, Delete, Save, Copy Link, Report)
        selectedReelForOptions?.let { reel ->
            PostOptionsBottomSheet(
                post = reel,
                currentUserId = userId,
                postRepository = postRepository,
                onEditClick = { editingReel = it },
                onDeletePost = {
                    selectedReelForOptions = null
                },
                onDismiss = { selectedReelForOptions = null }
            )
        }

        // Edit Reel Dialog (Edit caption & audience, original video locked)
        editingReel?.let { reel ->
            EditPostDialog(
                post = reel,
                postRepository = postRepository,
                mediaUploadService = mediaUploadService,
                onDismiss = { editingReel = null },
                onPostUpdated = {
                    editingReel = null
                }
            )
        }
    }
}

@Composable
private fun ReelVideoItem(
    reel: PostItem,
    currentUserId: String,
    currentUserProfile: UserProfile? = null,
    onDoubleTapLike: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val isLiked = reel.likedByMap[currentUserId] == true
    val isVerifiedAuthor = reel.isAuthorVerified || (currentUserProfile != null && reel.authorId == currentUserProfile.uid && currentUserProfile.isVerificationActive())
    val authorInitial = reel.authorName.firstOrNull()?.uppercase() ?: "U"

    val coroutineScope = rememberCoroutineScope()
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }

    val triggerHeartAnimation: () -> Unit = {
        onDoubleTapLike()
        coroutineScope.launch {
            heartScale.snapTo(0.2f)
            heartAlpha.snapTo(1f)
            // Spring pop animation
            launch {
                heartScale.animateTo(
                    targetValue = 1.35f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                heartScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 150)
                )
            }
            delay(600)
            heartAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(reel.id) {
                detectTapGestures(
                    onDoubleTap = { triggerHeartAnimation() }
                )
            }
            .testTag("reel_item_${reel.id}")
    ) {
        // Video Player centered with black background (Handles any aspect ratio letterbox/pillarbox)
        if (reel.mediaUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                FrndomVideoPlayer(
                    videoUrl = reel.mediaUrl,
                    modifier = Modifier.fillMaxSize(),
                    autoPlay = true,
                    isLooping = true,
                    onDoubleTap = { triggerHeartAnimation() }
                )
            }
        } else {
            // Text or Audio reel fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = "Video",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(64.dp)
                    )
                    if (reel.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = reel.content,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Animated Big Emoji Heart Pop on Double Tap
        if (heartAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .scale(heartScale.value)
                        .alpha(heartAlpha.value),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFA383E).copy(alpha = 0.2f),
                        modifier = Modifier.size(110.dp)
                    ) {}

                    Text(
                        text = "❤️",
                        fontSize = 72.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom gradient scrim for high readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Right Action Bar (Like, Comment, Share)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp)
        ) {
            // Like
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onLikeClick)
                    .testTag("reel_like_${reel.id}")
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFFA383E) else Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "${reel.likesCount}",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Comment
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onCommentClick)
                    .testTag("reel_comment_${reel.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = "Comments",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = "${reel.commentsCount}",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Share
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onShareClick)
                    .testTag("reel_share_${reel.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = if (reel.sharesCount > 0) "${reel.sharesCount}" else "Share",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onOptionsClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom Left Creator Info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.75f)
                .padding(start = 16.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (reel.authorAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = reel.authorAvatarUrl,
                            contentDescription = reel.authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = authorInitial,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reel.authorName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isVerifiedAuthor) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerificationBadge(size = 15.dp, show = true)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1877F2),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {}
                ) {
                    Text(
                        text = "Follow",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (reel.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HashtagText(
                    text = reel.content,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    hashtagColor = Color(0xFF80D8FF),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Audio",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Original Audio • ${reel.authorName}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}
