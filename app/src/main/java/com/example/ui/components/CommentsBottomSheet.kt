package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.CommentItem
import com.example.data.model.UserProfile
import com.example.ui.components.HashtagText
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    postRepository: com.example.data.repository.PostRepository,
    postId: String,
    userProfile: UserProfile?,
    onDismiss: () -> Unit,
    onCommentAdded: () -> Unit
) {
    // Open immediately full height (leaving slight top margin)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val comments by postRepository.getCommentsFlow(postId).collectAsState(initial = emptyList<CommentItem>())
    var textInput by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<CommentItem?>(null) }

    val quickEmojis = listOf("❤️", "🙌", "🔥", "👏", "😍", "😂", "😮", "👍", "💯")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 28.dp) // Slight top gap as requested by user
            .testTag("comments_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (comments.isNotEmpty()) "Comments (${comments.size})" else "Comments",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF050505)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF65676B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

            // Scrollable Comments List
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No comments yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF65676B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Be the first to comment on this post!",
                            fontSize = 13.sp,
                            color = Color(0xFF8A8D91)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        CommentRow(
                            comment = comment,
                            currentUserProfile = userProfile,
                            onReplyClick = { replyingTo = comment },
                            onLikeClick = {
                                val newLikes = if (comment.likesCount > 0) 0 else 1
                                postRepository.addDetailedComment(comment.copy(likesCount = newLikes))
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }

            // Replying Banner
            if (replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0F2F5))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Replying to ${replyingTo!!.authorName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF65676B)
                    )
                    IconButton(
                        onClick = { replyingTo = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel reply",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF65676B)
                        )
                    }
                }
            }

            // Quick Emoji Reaction Bar (One tap comment)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F8FA))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickEmojis) { emoji ->
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                val newComment = CommentItem(
                                    id = UUID.randomUUID().toString(),
                                    postId = postId,
                                    authorId = userProfile?.uid ?: "",
                                    authorName = userProfile?.fullName ?: "User",
                                    authorAvatarUrl = userProfile?.profilePictureUrl ?: "",
                                    emojiSticker = emoji,
                                    replyToAuthorName = replyingTo?.authorName ?: "",
                                    isAuthorVerified = userProfile?.isVerificationActive() == true
                                )
                                postRepository.addDetailedComment(newComment)
                                replyingTo = null
                                onCommentAdded()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

            // Bottom Input Box: ALWAYS visible without needing to drag!
            Surface(
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val userAvatar = userProfile?.profilePictureUrl.orEmpty()
                    val userInitial = userProfile?.firstName?.firstOrNull()?.uppercase()
                        ?: userProfile?.fullName?.firstOrNull()?.uppercase()
                        ?: "U"

                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = Color(0xFF1877F2)
                    ) {
                        if (userAvatar.isNotBlank()) {
                            AsyncImage(
                                model = userAvatar,
                                contentDescription = "My Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userInitial,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = if (replyingTo != null) "Reply to ${replyingTo!!.authorName}..." else "Write a comment...",
                                fontSize = 14.sp,
                                color = Color(0xFF8A8D91)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comment_input_field"),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF0F2F5),
                            unfocusedContainerColor = Color(0xFFF0F2F5),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = false,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                val newComment = CommentItem(
                                    id = UUID.randomUUID().toString(),
                                    postId = postId,
                                    authorId = userProfile?.uid ?: "",
                                    authorName = userProfile?.fullName ?: "User",
                                    authorAvatarUrl = userProfile?.profilePictureUrl ?: "",
                                    text = textInput.trim(),
                                    replyToAuthorName = replyingTo?.authorName ?: "",
                                    isAuthorVerified = userProfile?.isVerificationActive() == true
                                )
                                postRepository.addDetailedComment(newComment)
                                textInput = ""
                                replyingTo = null
                                onCommentAdded()
                            }
                        },
                        enabled = textInput.isNotBlank(),
                        modifier = Modifier.testTag("comment_send_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (textInput.isNotBlank()) Color(0xFF1877F2) else Color(0xFFBCC0C4),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentRow(
    comment: CommentItem,
    currentUserProfile: UserProfile? = null,
    onReplyClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    val isVerifiedAuthor = comment.isAuthorVerified || (currentUserProfile != null && comment.authorId == currentUserProfile.uid && currentUserProfile.isVerificationActive())
    val authorInitial = comment.authorName.firstOrNull()?.uppercase() ?: "U"

    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = Color(0xFFD8DADF)
        ) {
            if (comment.authorAvatarUrl.isNotBlank()) {
                AsyncImage(
                    model = comment.authorAvatarUrl,
                    contentDescription = comment.authorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = authorInitial,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1877F2)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (comment.text.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF0F2F5)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = comment.authorName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF050505)
                            )
                            if (isVerifiedAuthor) {
                                VerificationBadge(size = 14.dp, show = true)
                            }
                        }
                        if (comment.replyToAuthorName.isNotBlank()) {
                            Text(
                                text = "Replying to ${comment.replyToAuthorName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1877F2)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        HashtagText(
                            text = comment.text,
                            fontSize = 14.sp,
                            color = Color(0xFF050505),
                            hashtagColor = Color(0xFF1877F2),
                            lineHeight = 19.sp
                        )
                    }
                }
            } else if (comment.emojiSticker.isNotBlank()) {
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = comment.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF050505)
                        )
                        if (isVerifiedAuthor) {
                            VerificationBadge(size = 14.dp, show = true)
                        }
                    }
                    if (comment.replyToAuthorName.isNotBlank()) {
                        Text(
                            text = "Replying to ${comment.replyToAuthorName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1877F2)
                        )
                    }
                    Text(
                        text = comment.emojiSticker,
                        fontSize = 38.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Just now", fontSize = 12.sp, color = Color(0xFF65676B))
                Text(
                    text = "Like",
                    fontSize = 12.sp,
                    fontWeight = if (comment.likesCount > 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (comment.likesCount > 0) Color(0xFF1877F2) else Color(0xFF65676B),
                    modifier = Modifier.clickable(onClick = onLikeClick)
                )
                Text(
                    text = "Reply",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF65676B),
                    modifier = Modifier.clickable(onClick = onReplyClick)
                )
                if (comment.likesCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1877F2),
                            modifier = Modifier.size(14.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ThumbUp,
                                    contentDescription = "Like",
                                    tint = Color.White,
                                    modifier = Modifier.size(8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${comment.likesCount}", fontSize = 12.sp, color = Color(0xFF65676B))
                    }
                }
            }
        }
    }
}
