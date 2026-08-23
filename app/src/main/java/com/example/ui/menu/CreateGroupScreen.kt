package com.example.ui.menu

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GroupItem
import com.example.data.model.UserProfile
import com.example.data.repository.GroupPageRepository
import com.example.data.service.MediaUploadService
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CreateGroupScreen(
    userProfile: UserProfile?,
    groupPageRepository: GroupPageRepository,
    mediaUploadService: MediaUploadService,
    groupToEdit: GroupItem? = null,
    onGroupCreated: (GroupItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditMode = groupToEdit != null
    val scope = rememberCoroutineScope()

    var groupName by remember { mutableStateOf(groupToEdit?.name.orEmpty()) }
    var groupDescription by remember { mutableStateOf(groupToEdit?.description.orEmpty()) }
    var groupPrivacy by remember { mutableStateOf(groupToEdit?.privacy ?: "Public") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var currentCoverUrl by remember { mutableStateOf(groupToEdit?.coverUrl.orEmpty()) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Cover Banner Only Picker Launcher (Facebook-style: Groups have only a cover banner, no profile picture)
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coverUri = uri
        }
    }

    val isValid = groupName.trim().length >= 2 && !isSubmitting

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("create_group_screen")
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
                            text = if (isEditMode) "Edit Group" else "Create a Group",
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
                                    var finalCover = currentCoverUrl

                                    coverUri?.let { uri ->
                                        val res = mediaUploadService.uploadImageUri(uri, folder = "groups/covers")
                                        finalCover = res.getOrDefault(uri.toString())
                                    }

                                    val savedGroup = if (isEditMode) {
                                        val updated = groupToEdit!!.copy(
                                            name = groupName.trim(),
                                            description = groupDescription.trim(),
                                            privacy = groupPrivacy,
                                            coverUrl = finalCover
                                        )
                                        groupPageRepository.updateGroup(updated)
                                        updated
                                    } else {
                                        val newGroup = GroupItem(
                                            id = "grp_" + UUID.randomUUID().toString().take(8),
                                            name = groupName.trim(),
                                            privacy = groupPrivacy,
                                            description = groupDescription.trim(),
                                            coverUrl = finalCover,
                                            creatorId = userProfile?.uid ?: "user",
                                            membersCount = 1,
                                            createdAt = System.currentTimeMillis()
                                        )
                                        groupPageRepository.createGroup(newGroup)
                                        newGroup
                                    }

                                    isSubmitting = false
                                    onGroupCreated(savedGroup)
                                }
                            }
                        },
                        enabled = isValid,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            disabledContainerColor = Color(0xFF1877F2).copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("submit_group_button")
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
                // Section 1: Group Cover Banner (Facebook standard: Groups only have cover banners)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Group Cover Photo",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                                Text(
                                    text = "Groups feature a wide cover banner header",
                                    fontSize = 12.sp,
                                    color = Color(0xFF65676B)
                                )
                            }
                        }

                        Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
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
                                    contentDescription = "Group Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (currentCoverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentCoverUrl,
                                    contentDescription = "Group Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF1877F2), Color(0xFF0D54BA))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap to choose a Group Banner",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Camera button
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
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
                    }
                }

                // Section 2: Group Info
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
                            text = "Group Information",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )

                        // Name
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("Group Name *") },
                            placeholder = { Text("e.g. Kotlin & Android Developers") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1877F2),
                                focusedLabelColor = Color(0xFF1877F2)
                            )
                        )

                        // Description
                        OutlinedTextField(
                            value = groupDescription,
                            onValueChange = { groupDescription = it },
                            label = { Text("About This Group") },
                            placeholder = { Text("Describe what members can discuss, share, and learn...") },
                            shape = RoundedCornerShape(8.dp),
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_desc_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1877F2),
                                focusedLabelColor = Color(0xFF1877F2)
                            )
                        )
                    }
                }

                // Section 3: Privacy
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
                            text = "Choose Privacy",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Public Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { groupPrivacy = "Public" }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = groupPrivacy == "Public",
                                onClick = { groupPrivacy = "Public" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1877F2))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Public Group", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF050505))
                                Text("Anyone can see who's in the group and what they post", fontSize = 12.sp, color = Color(0xFF65676B))
                            }
                        }

                        // Private Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { groupPrivacy = "Private" }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = groupPrivacy == "Private",
                                onClick = { groupPrivacy = "Private" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1877F2))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF65676B), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Private Group", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF050505))
                                Text("Only members can see who's in the group and what they post", fontSize = 12.sp, color = Color(0xFF65676B))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
