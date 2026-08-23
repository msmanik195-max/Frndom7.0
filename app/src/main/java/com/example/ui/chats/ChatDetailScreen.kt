package com.example.ui.chats

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AudioCallSession
import com.example.data.model.ChatMessage
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.data.service.MediaUploadService
import com.example.ui.components.FrndomVideoPlayer
import com.example.ui.components.VerificationBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    currentUserId: String,
    currentUserName: String,
    currentUserAvatar: String,
    peerProfile: UserProfile,
    chatRepository: ChatRepository,
    mediaUploadService: MediaUploadService?,
    onBackClick: () -> Unit,
    initialMessage: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by chatRepository.getMessagesFlow(currentUserId, peerProfile.uid).collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf(initialMessage) }
    var isUploadingMedia by remember { mutableStateOf(false) }

    // Audio Calling State
    var activeCallSession by remember { mutableStateOf<AudioCallSession?>(null) }

    // Audio Voice Recording State
    val audioRecordingManager = remember { AudioRecordingManager(context) }
    val voicePlayerManager = remember { VoicePlayerManager(context) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Microphone permission enabled. Press & hold the mic to send a voice note.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice notes & audio calls.", Toast.LENGTH_SHORT).show()
        }
    }

    fun hasAudioPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Selected Message for Long-press Action Sheet (Delete for Me / Delete for Everyone / Download)
    var selectedMessageForOptions by remember { mutableStateOf<ChatMessage?>(null) }
    var showDeleteForEveryoneDialog by remember { mutableStateOf(false) }

    val peerDisplayName = when {
        peerProfile.fullName.isNotBlank() -> peerProfile.fullName
        peerProfile.firstName.isNotBlank() -> "${peerProfile.firstName} ${peerProfile.lastName}".trim()
        else -> "User"
    }

    // Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && mediaUploadService != null) {
            isUploadingMedia = true
            scope.launch {
                val res = mediaUploadService.uploadImageUri(uri, folder = "chats")
                val url = res.getOrDefault("")
                isUploadingMedia = false
                if (url.isNotBlank()) {
                    chatRepository.sendMessage(
                        senderId = currentUserId,
                        senderName = currentUserName,
                        senderAvatar = currentUserAvatar,
                        receiverId = peerProfile.uid,
                        receiverName = peerDisplayName,
                        receiverAvatar = peerProfile.profilePictureUrl,
                        text = "",
                        mediaType = "image",
                        mediaUrl = url
                    )
                }
            }
        }
    }

    // Video Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && mediaUploadService != null) {
            isUploadingMedia = true
            scope.launch {
                val res = mediaUploadService.uploadVideoUri(uri, folder = "chats")
                val url = res.getOrDefault("")
                isUploadingMedia = false
                if (url.isNotBlank()) {
                    chatRepository.sendMessage(
                        senderId = currentUserId,
                        senderName = currentUserName,
                        senderAvatar = currentUserAvatar,
                        receiverId = peerProfile.uid,
                        receiverName = peerDisplayName,
                        receiverAvatar = peerProfile.profilePictureUrl,
                        text = "",
                        mediaType = "video",
                        mediaUrl = url
                    )
                }
            }
        }
    }

    // Recording timer loop
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingDurationSeconds = 0
            while (isActive && isRecordingAudio) {
                delay(1000)
                recordingDurationSeconds++
            }
        }
    }

    // Cleanup audio on leave
    DisposableEffect(Unit) {
        onDispose {
            voicePlayerManager.stop()
        }
    }

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Check for incoming call directly in chat
    val incomingCall by chatRepository.listenForIncomingCalls(currentUserId).collectAsState(initial = null)
    LaunchedEffect(incomingCall) {
        if (incomingCall != null && activeCallSession == null) {
            activeCallSession = incomingCall
        }
    }

    // If active call session is ongoing, render AudioCallScreen
    if (activeCallSession != null) {
        AudioCallScreen(
            currentUserId = currentUserId,
            callSession = activeCallSession!!,
            chatRepository = chatRepository,
            onCallClosed = { activeCallSession = null }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
            .testTag("chat_detail_screen")
    ) {
        // Top Header
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF050505)
                    )
                }

                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (peerProfile.profilePictureUrl.isNotBlank()) {
                        AsyncImage(
                            model = peerProfile.profilePictureUrl,
                            contentDescription = peerDisplayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = peerDisplayName.firstOrNull()?.uppercase() ?: "U",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = peerDisplayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                        if (peerProfile.isVerificationActive()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerificationBadge(size = 15.dp, show = true)
                        }
                    }
                    Text(
                        text = "Active in Frndom",
                        fontSize = 12.sp,
                        color = Color(0xFF31A24C)
                    )
                }

                // Audio Call Action (Audio only, no video call as requested!)
                IconButton(
                    onClick = {
                        if (!hasAudioPermission()) {
                            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            return@IconButton
                        }
                        val callId = chatRepository.initiateAudioCall(
                            callerId = currentUserId,
                            callerName = currentUserName,
                            callerAvatar = currentUserAvatar,
                            receiverId = peerProfile.uid,
                            receiverName = peerDisplayName,
                            receiverAvatar = peerProfile.profilePictureUrl
                        )
                        activeCallSession = AudioCallSession(
                            callId = callId,
                            callerId = currentUserId,
                            callerName = currentUserName,
                            callerAvatarUrl = currentUserAvatar,
                            receiverId = peerProfile.uid,
                            receiverName = peerDisplayName,
                            receiverAvatarUrl = peerProfile.profilePictureUrl,
                            status = "RINGING"
                        )
                    },
                    modifier = Modifier.testTag("chat_audio_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Audio Call",
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF0F2F5))
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (peerProfile.profilePictureUrl.isNotBlank()) {
                            AsyncImage(
                                model = peerProfile.profilePictureUrl,
                                contentDescription = peerDisplayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = peerDisplayName.firstOrNull()?.uppercase() ?: "U",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = peerDisplayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                    Text(
                        text = "Say hello or send a message to start chatting!",
                        fontSize = 14.sp,
                        color = Color(0xFF65676B)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == currentUserId
                        ChatMessageBubble(
                            message = msg,
                            isMe = isMe,
                            voicePlayerManager = voicePlayerManager,
                            onLongPress = {
                                selectedMessageForOptions = msg
                            },
                            onDownloadMedia = { url, isVideo ->
                                ChatMediaHelper.downloadMediaFile(context, url, isVideo = isVideo)
                            }
                        )
                    }
                }
            }
        }

        // Uploading indicator
        if (isUploadingMedia) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE4E6EB))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Uploading media to cloud...", fontSize = 12.sp, color = Color(0xFF050505))
            }
        }

        // Audio Recording Banner while pressing & holding mic
        AnimatedVisibility(
            visible = isRecordingAudio,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = Color(0xFFEF4444),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(12.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recording Voice Note (${String.format("%02d:%02d", recordingDurationSeconds / 60, recordingDurationSeconds % 60)})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Release to Send",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom Input Bar
        Surface(
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo picker
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Send Photo",
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Video picker
                IconButton(
                    onClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Send Video",
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Press and Hold Voice / Audio Recording Mic Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isRecordingAudio) Color(0xFFEF4444) else Color.Transparent)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    if (!hasAudioPermission()) {
                                        audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        return@detectTapGestures
                                    }
                                    isRecordingAudio = true
                                    val started = audioRecordingManager.startRecording()
                                    if (!started) {
                                        isRecordingAudio = false
                                        return@detectTapGestures
                                    }
                                    val released = tryAwaitRelease()
                                    isRecordingAudio = false
                                    val recordedFile = audioRecordingManager.stopRecording()
                                    if (released && recordedFile != null && recordedFile.exists()) {
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val bytes = recordedFile.readBytes()
                                                if (bytes.isNotEmpty()) {
                                                    val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                                    val audioDataUrl = "data:audio/mp4;base64,$base64Data"
                                                    chatRepository.sendMessage(
                                                        senderId = currentUserId,
                                                        senderName = currentUserName,
                                                        senderAvatar = currentUserAvatar,
                                                        receiverId = peerProfile.uid,
                                                        receiverName = peerDisplayName,
                                                        receiverAvatar = peerProfile.profilePictureUrl,
                                                        text = "",
                                                        mediaType = "audio",
                                                        mediaUrl = audioDataUrl
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("ChatDetail", "Failed to send voice note: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record Voice (Hold)",
                        tint = if (isRecordingAudio) Color.White else Color(0xFF1877F2),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Text Input
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Aa", color = Color(0xFF8A8D91), fontSize = 15.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F2F5),
                        unfocusedContainerColor = Color(0xFFF0F2F5),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    maxLines = 4
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            chatRepository.sendMessage(
                                senderId = currentUserId,
                                senderName = currentUserName,
                                senderAvatar = currentUserAvatar,
                                receiverId = peerProfile.uid,
                                receiverName = peerDisplayName,
                                receiverAvatar = peerProfile.profilePictureUrl,
                                text = messageText.trim(),
                                mediaType = "text"
                            )
                            messageText = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank()) Color(0xFF1877F2) else Color(0xFFB0B3B8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Long-press Message Actions Bottom Sheet (Delete for Me, Delete for Everyone, Download Media, Copy)
    selectedMessageForOptions?.let { msg ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isSender = msg.senderId == currentUserId
        val hasMedia = msg.mediaUrl.isNotBlank()

        ModalBottomSheet(
            onDismissRequest = { selectedMessageForOptions = null },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Message Options",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. Download Media (if photo/video/audio)
                if (hasMedia) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMessageForOptions = null
                                ChatMediaHelper.downloadMediaFile(
                                    context = context,
                                    url = msg.mediaUrl,
                                    isVideo = msg.mediaType == "video"
                                )
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color(0xFF1877F2))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (msg.mediaType == "video") "Download Video to Gallery" else "Download Photo to Gallery",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF050505)
                        )
                    }
                }

                // 2. Copy Text
                if (msg.text.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("chat", msg.text))
                                selectedMessageForOptions = null
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF65676B))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = "Copy Text", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF050505))
                    }
                }

                // 3. Delete for Me
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val msgId = msg.id
                            selectedMessageForOptions = null
                            chatRepository.deleteMessageForMe(currentUserId, peerProfile.uid, msgId)
                            Toast.makeText(context, "Deleted for you", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFF65676B))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "Delete for You", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF050505))
                }

                // 4. Delete for Everyone
                if (isSender) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDeleteForEveryoneDialog = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Unsend for Everyone",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showDeleteForEveryoneDialog && selectedMessageForOptions != null) {
        val targetMsg = selectedMessageForOptions!!
        AlertDialog(
            onDismissRequest = {
                showDeleteForEveryoneDialog = false
                selectedMessageForOptions = null
            },
            title = { Text("Unsend for Everyone?") },
            text = { Text("This will permanently remove the message for everyone and erase it from the database.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatRepository.deleteMessageForEveryone(currentUserId, peerProfile.uid, targetMsg.id)
                        showDeleteForEveryoneDialog = false
                        selectedMessageForOptions = null
                        Toast.makeText(context, "Message unsent for everyone", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Unsend", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteForEveryoneDialog = false
                        selectedMessageForOptions = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    voicePlayerManager: VoicePlayerManager,
    onLongPress: () -> Unit,
    onDownloadMedia: (url: String, isVideo: Boolean) -> Unit
) {
    val bubbleColor = if (isMe) Color(0xFF0866FF) else Color.White
    val textColor = if (isMe) Color.White else Color(0xFF050505)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    val isPlaying by voicePlayerManager.isPlaying.collectAsState()
    val playProgress by voicePlayerManager.currentProgress.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() }
                )
            },
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = bubbleColor,
            shadowElevation = if (isMe) 0.dp else 1.dp,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Column(modifier = Modifier.padding(if (message.mediaType == "text") 12.dp else 4.dp)) {
                // Call notification log bubble
                if (message.mediaType == "call") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = if (isMe) Color.White else Color(0xFF1877F2),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                // Image Message with direct download overlay
                else if (message.mediaType == "image" && message.mediaUrl.isNotBlank()) {
                    Box {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "Image message",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 220.dp, height = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        // Download Button Overlay
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(34.dp)
                                .clickable { onDownloadMedia(message.mediaUrl, false) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                // Video Message with direct download overlay
                else if (message.mediaType == "video" && message.mediaUrl.isNotBlank()) {
                    Box {
                        FrndomVideoPlayer(
                            videoUrl = message.mediaUrl,
                            modifier = Modifier
                                .size(width = 220.dp, height = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        // Download Button Overlay
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(34.dp)
                                .clickable { onDownloadMedia(message.mediaUrl, true) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                // Audio Voice Note Bubble
                else if (message.mediaType == "audio") {
                    Row(
                        modifier = Modifier
                            .width(200.dp)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isMe) Color.White else Color(0xFF1877F2),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    if (message.mediaUrl.isNotBlank()) {
                                        voicePlayerManager.playOrPause(message.mediaUrl, scope)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play voice",
                                    tint = if (isMe) Color(0xFF0866FF) else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { playProgress },
                                color = if (isMe) Color.White else Color(0xFF1877F2),
                                trackColor = if (isMe) Color.White.copy(alpha = 0.3f) else Color(0xFFE4E6EB),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Voice note",
                                fontSize = 11.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.85f) else Color(0xFF65676B)
                            )
                        }
                    }
                }

                if (message.text.isNotBlank() && message.mediaType != "call") {
                    if (message.mediaType != "text") {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Text(
            text = formattedTime,
            fontSize = 10.sp,
            color = Color(0xFF65676B),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
