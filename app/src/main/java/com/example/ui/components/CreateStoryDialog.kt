package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.StoryItem
import com.example.data.model.UserProfile
import com.example.data.service.MediaUploadService
import com.example.ui.create.PostBackgroundStyle
import kotlinx.coroutines.launch

@Composable
fun CreateStoryDialog(
    userProfile: UserProfile?,
    mediaUploadService: MediaUploadService? = null,
    onStoryCreated: (StoryItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var storyText by remember { mutableStateOf("") }
    var selectedBackground by remember { mutableStateOf(PostBackgroundStyle.BG_SUNSET) }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf("text") } // "text", "image", "video"
    var isUploading by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            selectedMediaType = "image"
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            selectedMediaType = "video"
        }
    }

    val displayName = when {
        !userProfile?.fullName.isNullOrBlank() -> userProfile?.fullName ?: "User"
        !userProfile?.firstName.isNullOrBlank() -> "${userProfile?.firstName} ${userProfile?.lastName}".trim()
        else -> "User"
    }

    val canShare = (storyText.isNotBlank() || selectedMediaUri != null) && !isUploading

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .testTag("create_story_dialog")
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = if (selectedMediaType == "video") "Create Video Story" else "Create Story",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = {
                        if (canShare) {
                            isUploading = true
                            scope.launch {
                                var finalMediaUrl = ""
                                val uri = selectedMediaUri
                                if (uri != null && mediaUploadService != null) {
                                    finalMediaUrl = if (selectedMediaType == "video") {
                                        val res = mediaUploadService.uploadVideoUri(uri, folder = "stories")
                                        res.getOrDefault(uri.toString())
                                    } else {
                                        val res = mediaUploadService.uploadImageUri(uri, folder = "stories")
                                        res.getOrDefault(uri.toString())
                                    }
                                } else if (uri != null) {
                                    finalMediaUrl = uri.toString()
                                }

                                val finalType = if (selectedMediaUri != null) selectedMediaType else "text"

                                val newStory = StoryItem(
                                    userId = userProfile?.uid ?: "user_id",
                                    userName = displayName,
                                    userAvatar = userProfile?.profilePictureUrl ?: "",
                                    mediaUrl = finalMediaUrl,
                                    caption = storyText.trim(),
                                    backgroundStyle = if (selectedMediaUri != null) "none" else selectedBackground.id,
                                    mediaType = finalType
                                )

                                isUploading = false
                                onStoryCreated(newStory)
                            }
                        }
                    },
                    enabled = canShare,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1877F2),
                        disabledContainerColor = Color(0xFF1877F2).copy(alpha = 0.4f)
                    )
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "Share", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Central Story Canvas Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(440.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (selectedMediaUri != null) {
                                Modifier.background(Color(0xFF0F172A))
                            } else if (selectedBackground.isGradient) {
                                Modifier.background(Brush.linearGradient(selectedBackground.gradientColors))
                            } else {
                                Modifier.background(selectedBackground.singleColor)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedMediaUri != null) {
                        if (selectedMediaType == "image") {
                            AsyncImage(
                                model = selectedMediaUri,
                                contentDescription = "Selected story photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Video Story Preview box
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleOutline,
                                    contentDescription = "Video Story",
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Video Story Ready",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // Close media button
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(32.dp)
                                .clickable {
                                    selectedMediaUri = null
                                    selectedMediaType = "text"
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove media",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Story Caption Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = if (selectedMediaUri != null) Alignment.BottomCenter else Alignment.Center
                    ) {
                        OutlinedTextField(
                            value = storyText,
                            onValueChange = { storyText = it },
                            placeholder = {
                                Text(
                                    text = if (selectedMediaUri != null) "Add caption..." else "Start typing your story...",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = if (selectedMediaUri != null) 16.sp else 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            textStyle = TextStyle(
                                fontSize = if (selectedMediaUri != null) 16.sp else 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // Bottom Actions: Photo/Video Gallery Buttons + Gradient Styles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Style, Photo or Video",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Photo Picker Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Photo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Video Picker Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFC62828).copy(alpha = 0.4f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Video",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(
                        listOf(
                            PostBackgroundStyle.BG_SUNSET,
                            PostBackgroundStyle.BG_NIGHT,
                            PostBackgroundStyle.BG_OCEAN,
                            PostBackgroundStyle.BG_HILLS,
                            PostBackgroundStyle.BLUE,
                            PostBackgroundStyle.RED,
                            PostBackgroundStyle.GREEN
                        )
                    ) { bg ->
                        val isSelected = selectedBackground == bg && selectedMediaUri == null
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .then(
                                    if (bg.isGradient) {
                                        Modifier.background(Brush.linearGradient(bg.gradientColors))
                                    } else {
                                        Modifier.background(bg.singleColor)
                                    }
                                )
                                .clickable {
                                    selectedBackground = bg
                                    selectedMediaUri = null
                                    selectedMediaType = "text"
                                }
                        )
                    }
                }
            }
        }
    }
}
