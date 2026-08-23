package com.example.ui.menu

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PageItem
import com.example.data.model.UserProfile
import com.example.data.repository.GroupPageRepository
import com.example.data.service.MediaUploadService
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CreatePageScreen(
    userProfile: UserProfile?,
    groupPageRepository: GroupPageRepository,
    mediaUploadService: MediaUploadService,
    pageToEdit: PageItem? = null,
    onPageCreated: (PageItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditMode = pageToEdit != null
    val scope = rememberCoroutineScope()

    var pageName by remember { mutableStateOf(pageToEdit?.name.orEmpty()) }
    var pageCategory by remember { mutableStateOf(pageToEdit?.category ?: "Digital Creator") }
    var pageDescription by remember { mutableStateOf(pageToEdit?.description.orEmpty()) }
    var pagePrivacy by remember { mutableStateOf(if (pageToEdit?.id?.isNotBlank() == true) "Public" else "Public") }

    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }

    var currentAvatarUrl by remember { mutableStateOf(pageToEdit?.avatarUrl.orEmpty()) }
    var currentCoverUrl by remember { mutableStateOf(pageToEdit?.coverUrl.orEmpty()) }

    var isSubmitting by remember { mutableStateOf(false) }

    val categories = listOf(
        "Digital Creator",
        "Community & Social",
        "Business & Brand",
        "Entertainment",
        "News & Media",
        "Tech & Gaming",
        "Education & Science",
        "Art & Design",
        "Health & Fitness"
    )

    // Avatar Picker Launcher
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarUri = uri
        }
    }

    // Cover Banner Picker Launcher
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coverUri = uri
        }
    }

    val isValid = pageName.trim().length >= 2 && !isSubmitting

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("create_page_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF050505)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEditMode) "Edit Page" else "Create a Page",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                    }

                    Button(
                        onClick = {
                            if (isValid) {
                                isSubmitting = true
                                scope.launch {
                                    var finalAvatar = currentAvatarUrl
                                    var finalCover = currentCoverUrl

                                    avatarUri?.let { uri ->
                                        val res = mediaUploadService.uploadImageUri(uri, folder = "pages/avatars")
                                        finalAvatar = res.getOrDefault(uri.toString())
                                    }

                                    coverUri?.let { uri ->
                                        val res = mediaUploadService.uploadImageUri(uri, folder = "pages/covers")
                                        finalCover = res.getOrDefault(uri.toString())
                                    }

                                    val savedPage = if (isEditMode) {
                                        val updated = pageToEdit!!.copy(
                                            name = pageName.trim(),
                                            category = pageCategory,
                                            description = pageDescription.trim(),
                                            avatarUrl = finalAvatar,
                                            coverUrl = finalCover
                                        )
                                        groupPageRepository.updatePage(updated)
                                        updated
                                    } else {
                                        val newPage = PageItem(
                                            id = "page_" + UUID.randomUUID().toString().take(8),
                                            name = pageName.trim(),
                                            category = pageCategory,
                                            description = pageDescription.trim(),
                                            coverUrl = finalCover,
                                            avatarUrl = finalAvatar,
                                            creatorId = userProfile?.uid ?: "user",
                                            followersCount = 1,
                                            likesCount = 1,
                                            createdAt = System.currentTimeMillis()
                                        )
                                        groupPageRepository.createPage(newPage)
                                        newPage
                                    }

                                    isSubmitting = false
                                    onPageCreated(savedPage)
                                }
                            }
                        },
                        enabled = isValid,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            disabledContainerColor = Color(0xFF1877F2).copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("submit_page_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isEditMode) "Save" else "Create",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Page Visual Branding (Banner & Profile Picture)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Page Branding",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505),
                            modifier = Modifier.padding(14.dp)
                        )

                        Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                        // Cover Photo Picker Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(Color(0xFFE4E6EB))
                                .clickable {
                                    coverPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverUri != null) {
                                AsyncImage(
                                    model = coverUri,
                                    contentDescription = "Cover Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (currentCoverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentCoverUrl,
                                    contentDescription = "Cover Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = Color(0xFF65676B),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add Page Cover Banner",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF65676B)
                                    )
                                }
                            }

                            // Camera button overlay on top right of banner
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(36.dp),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.9f),
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change Cover",
                                        tint = Color(0xFF050505),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Avatar Section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFF1877F2), CircleShape)
                                    .clickable {
                                        avatarPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarUri != null) {
                                    AsyncImage(
                                        model = avatarUri,
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (currentAvatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = currentAvatarUrl,
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = Color(0xFFEBF5FF)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = Color(0xFF1877F2),
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF1877F2)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Upload Avatar",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Page Profile Picture",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                                Text(
                                    text = "Tap circle to choose an icon or photo",
                                    fontSize = 12.sp,
                                    color = Color(0xFF65676B)
                                )
                            }
                        }
                    }
                }

                // Section 2: Page Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Page Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )

                        // Page Name
                        OutlinedTextField(
                            value = pageName,
                            onValueChange = { pageName = it },
                            label = { Text("Page Name *") },
                            placeholder = { Text("e.g. Awesome Tech Creators") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("page_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1877F2),
                                focusedLabelColor = Color(0xFF1877F2)
                            )
                        )

                        // Description / Bio
                        OutlinedTextField(
                            value = pageDescription,
                            onValueChange = { pageDescription = it },
                            label = { Text("Description & Bio") },
                            placeholder = { Text("Tell people what your Page is about...") },
                            shape = RoundedCornerShape(8.dp),
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("page_desc_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1877F2),
                                focusedLabelColor = Color(0xFF1877F2)
                            )
                        )
                    }
                }

                // Section 3: Category Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Select Category",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A category helps people discover your page easily",
                            fontSize = 12.sp,
                            color = Color(0xFF65676B)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            categories.chunked(2).forEach { rowCategories ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowCategories.forEach { cat ->
                                        val isSelected = pageCategory == cat
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { pageCategory = cat },
                                            label = {
                                                Text(
                                                    text = cat,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            leadingIcon = if (isSelected) {
                                                { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFEBF5FF),
                                                selectedLabelColor = Color(0xFF1877F2)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowCategories.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Public / Private Visibility Option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Page Visibility",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Public Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pagePrivacy = "Public" }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = pagePrivacy == "Public",
                                onClick = { pagePrivacy = "Public" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1877F2))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Public Page", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF050505))
                                Text("Anyone on or off Frndom can see your page and posts", fontSize = 12.sp, color = Color(0xFF65676B))
                            }
                        }

                        // Private Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pagePrivacy = "Private" }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = pagePrivacy == "Private",
                                onClick = { pagePrivacy = "Private" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1877F2))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF65676B), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Private Page", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF050505))
                                Text("Only approved followers can see what you post", fontSize = 12.sp, color = Color(0xFF65676B))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
