package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostItem
import com.example.data.repository.PostRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    post: PostItem,
    currentUserId: String,
    postRepository: PostRepository,
    onEditClick: (PostItem) -> Unit,
    onDeletePost: (PostItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isOwner = post.authorId.isNotBlank() && post.authorId == currentUserId
    val isVideoOrReel = post.mediaType == "reel" || post.mediaType == "video"

    var isSaved by remember { mutableStateOf(postRepository.isPostSaved(currentUserId, post.id)) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFBCC0C4))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isOwner) {
                // 1. Edit Post / Reel
                PostOptionRow(
                    icon = Icons.Default.Edit,
                    title = if (isVideoOrReel) "Edit Video Caption & Settings" else "Edit Post",
                    subtitle = if (isVideoOrReel) "Edit caption and audience (video cannot be replaced)" else "Modify text, change photos, and settings",
                    iconTint = Color(0xFF050505),
                    iconBg = Color(0xFFE4E6EB),
                    onClick = {
                        onDismiss()
                        onEditClick(post)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Copy Link
                PostOptionRow(
                    icon = Icons.Default.Link,
                    title = "Copy Link",
                    subtitle = "Copy link to this ${if (isVideoOrReel) "video" else "post"} to clipboard",
                    iconTint = Color(0xFF050505),
                    iconBg = Color(0xFFE4E6EB),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Post Link", "https://frndom.app/post/${post.id}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Delete Post
                PostOptionRow(
                    icon = Icons.Default.Delete,
                    title = if (isVideoOrReel) "Delete Video" else "Delete Post",
                    subtitle = "Move to trash / delete permanently",
                    iconTint = Color(0xFFE53935),
                    iconBg = Color(0xFFFFEBEE),
                    onClick = {
                        showDeleteConfirmDialog = true
                    }
                )
            } else {
                // 1. Save Post / Save Video
                val itemTypeLabel = if (isVideoOrReel) "Video" else "Post"
                PostOptionRow(
                    icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    title = if (isSaved) "Unsave $itemTypeLabel" else "Save $itemTypeLabel",
                    subtitle = if (isSaved) "Remove this from your Saved Items" else "Add this to your saved items",
                    iconTint = if (isSaved) Color(0xFF1877F2) else Color(0xFF050505),
                    iconBg = if (isSaved) Color(0xFFEBF5FF) else Color(0xFFE4E6EB),
                    onClick = {
                        val nowSaved = postRepository.toggleSavePost(currentUserId, post.id)
                        isSaved = nowSaved
                        Toast.makeText(
                            context,
                            if (nowSaved) "Saved to your Saved ${if (isVideoOrReel) "Videos" else "Posts"}!" else "Removed from saved items.",
                            Toast.LENGTH_SHORT
                        ).show()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Copy Link
                PostOptionRow(
                    icon = Icons.Default.Link,
                    title = "Copy Link",
                    subtitle = "Copy link to this ${if (isVideoOrReel) "video" else "post"} to clipboard",
                    iconTint = Color(0xFF050505),
                    iconBg = Color(0xFFE4E6EB),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Post Link", "https://frndom.app/post/${post.id}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Report Post
                PostOptionRow(
                    icon = Icons.Default.Flag,
                    title = "Report $itemTypeLabel",
                    subtitle = "We won't let ${post.authorName.ifBlank { "the author" }} know who reported this",
                    iconTint = Color(0xFFE65100),
                    iconBg = Color(0xFFFFF3E0),
                    onClick = {
                        showReportDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete ${if (isVideoOrReel) "Video" else "Post"}?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF050505)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this ${if (isVideoOrReel) "video" else "post"}? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = Color(0xFF65676B)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        postRepository.deletePost(post.id)
                        onDeletePost(post)
                        Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF050505))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Report Post Dialog
    if (showReportDialog) {
        ReportPostDialog(
            post = post,
            onDismiss = { showReportDialog = false },
            onSubmitReport = { reason ->
                postRepository.reportPost(post.id, currentUserId, reason)
                showReportDialog = false
                Toast.makeText(context, "Thank you for reporting. We will review this post.", Toast.LENGTH_LONG).show()
                onDismiss()
            }
        )
    }
}

@Composable
fun PostOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = iconBg
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (iconTint == Color(0xFFE53935)) Color(0xFFE53935) else Color(0xFF050505)
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF65676B)
                )
            }
        }
    }
}

@Composable
fun ReportPostDialog(
    post: PostItem,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String) -> Unit
) {
    val reasons = listOf(
        "Harassment or bullying",
        "Spam or misleading content",
        "Violence or dangerous content",
        "Hate speech or discrimination",
        "Nudity or sexually explicit content",
        "False information or scam",
        "Something else"
    )
    var selectedReason by remember { mutableStateOf(reasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Report",
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Report Post",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF050505)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Why are you reporting this post?",
                    fontSize = 14.sp,
                    color = Color(0xFF65676B),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedReason = reason }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1877F2))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = reason,
                            fontSize = 14.sp,
                            color = Color(0xFF050505)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitReport(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
            ) {
                Text("Submit Report", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF050505))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
