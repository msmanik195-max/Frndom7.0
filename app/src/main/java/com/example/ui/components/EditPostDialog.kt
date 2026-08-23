package com.example.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostItem
import com.example.data.repository.PostRepository
import com.example.data.service.MediaUploadService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostDialog(
    post: PostItem,
    postRepository: PostRepository,
    mediaUploadService: MediaUploadService,
    onDismiss: () -> Unit,
    onPostUpdated: (PostItem) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isVideoOrReel = post.mediaType == "reel" || post.mediaType == "video"

    var textContent by remember { mutableStateOf(post.content) }
    var selectedAudience by remember { mutableStateOf(post.audience) }
    var isSaving by remember { mutableStateOf(false) }

    // Existing images (URLs)
    val existingImageUrls = remember { mutableStateListOf<String>().apply { addAll(post.getAllMediaUrls()) } }
    // Newly picked image URIs
    val newSelectedImageUris = remember { mutableStateListOf<Uri>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            newSelectedImageUris.addAll(uris)
        }
    }

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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF050505)
                    )
                }

                Text(
                    text = if (isVideoOrReel) "Edit Video" else "Edit Post",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )

                Button(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            scope.launch {
                                val uploadedNewUrls = mutableListOf<String>()
                                if (newSelectedImageUris.isNotEmpty()) {
                                    val tasks = newSelectedImageUris.map { uri ->
                                        async {
                                            val res = mediaUploadService.uploadImageUri(uri, folder = "posts")
                                            res.getOrDefault(uri.toString())
                                        }
                                    }
                                    uploadedNewUrls.addAll(tasks.awaitAll())
                                }

                                val finalMediaUrls = mutableListOf<String>()
                                finalMediaUrls.addAll(existingImageUrls)
                                finalMediaUrls.addAll(uploadedNewUrls)

                                val updatedPost = post.copy(
                                    content = textContent.trim(),
                                    audience = selectedAudience,
                                    mediaUrl = if (isVideoOrReel) post.mediaUrl else (finalMediaUrls.firstOrNull() ?: ""),
                                    mediaUrls = if (isVideoOrReel) post.mediaUrls else finalMediaUrls,
                                    mediaType = if (isVideoOrReel) post.mediaType else if (finalMediaUrls.isNotEmpty()) "photo" else "text"
                                )

                                postRepository.updatePost(updatedPost)
                                onPostUpdated(updatedPost)
                                isSaving = false
                                Toast.makeText(context, "Post updated successfully!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB), modifier = Modifier.padding(vertical = 8.dp))

            // Audience selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE4E6EB),
                    modifier = Modifier.clickable {
                        selectedAudience = when (selectedAudience) {
                            "Public" -> "Friends"
                            "Friends" -> "Only Me"
                            else -> "Public"
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (selectedAudience) {
                                "Friends" -> Icons.Default.Public
                                "Only Me" -> Icons.Default.Lock
                                else -> Icons.Default.Public
                            },
                            contentDescription = selectedAudience,
                            tint = Color(0xFF050505),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedAudience,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF050505)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caption / Content Input
            TextField(
                value = textContent,
                onValueChange = { textContent = it },
                placeholder = { Text("What's on your mind? Use #hashtags", color = Color(0xFF65676B)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("edit_post_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF050505))
            )

            // Hashtag Helper suggestions bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("#frndom", "#photo", "#viral", "#reels", "#bangladesh", "#lifestyle").forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEBF5FF),
                        modifier = Modifier.clickable {
                            textContent = if (textContent.isBlank()) "$tag " else "$textContent $tag "
                        }
                    ) {
                        Text(
                            text = tag,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1877F2),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isVideoOrReel) {
                // Video Attached Info Notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Original video attached. Video files cannot be replaced during editing, but you can update the caption, hashtags, and audience.",
                            fontSize = 12.sp,
                            color = Color(0xFFE65100),
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                // Photo Management (Change / Remove / Add Photos)
                Text(
                    text = "Post Photos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF050505),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (existingImageUrls.isNotEmpty() || newSelectedImageUris.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Existing images
                        itemsIndexed(existingImageUrls) { index, url ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE4E6EB), RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clickable { existingImageUrls.removeAt(index) },
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.65f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Newly picked images
                        itemsIndexed(newSelectedImageUris) { index, uri ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, Color(0xFF1877F2), RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "New Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clickable { newSelectedImageUris.removeAt(index) },
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.65f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Button to Add / Change Photos
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE4E6EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Add/Change Photos",
                        tint = Color(0xFF050505),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (existingImageUrls.isEmpty() && newSelectedImageUris.isEmpty()) "Add Photos" else "Add / Change More Photos",
                        color = Color(0xFF050505),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
