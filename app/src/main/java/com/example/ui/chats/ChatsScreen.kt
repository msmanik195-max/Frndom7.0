package com.example.ui.chats

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatConversation
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatsScreen(
    currentUserId: String,
    chatRepository: ChatRepository,
    onOpenChat: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userRepository = androidx.compose.runtime.remember { com.example.data.repository.UserRepository(context) }
    val conversations by chatRepository.getConversationsFlow(currentUserId).collectAsState(initial = emptyList())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("chats_screen")
    ) {
        if (conversations.isEmpty()) {
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
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "No Chats",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No Messages Yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Search for friends or visit profiles to start messaging!",
                    fontSize = 14.sp,
                    color = Color(0xFF65676B),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                items(conversations, key = { it.peerId }) { conv ->
                    val peerFlow = androidx.compose.runtime.remember(conv.peerId) {
                        userRepository.getUserProfileFlow(conv.peerId)
                    }
                    val peerProfile by peerFlow.collectAsState(initial = userRepository.getLocalUserProfile(conv.peerId))
                    val isVerified = peerProfile?.isVerificationActive() == true

                    ConversationItemRow(
                        conversation = conv,
                        isVerified = isVerified,
                        onClick = {
                            val resolvedProfile = peerProfile ?: userRepository.getLocalUserProfile(conv.peerId) ?: UserProfile(
                                uid = conv.peerId,
                                fullName = conv.peerName,
                                profilePictureUrl = conv.peerAvatarUrl
                            )
                            onOpenChat(resolvedProfile)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItemRow(
    conversation: ChatConversation,
    isVerified: Boolean = false,
    onClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = if (conversation.lastMessageTime > 0) timeFormat.format(Date(conversation.lastMessageTime)) else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (conversation.peerAvatarUrl.isNotBlank()) {
                AsyncImage(
                    model = conversation.peerAvatarUrl,
                    contentDescription = conversation.peerName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = conversation.peerName.firstOrNull()?.uppercase() ?: "U",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.peerName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )
                if (isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    com.example.ui.components.VerificationBadge(size = 15.dp, show = true)
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = conversation.lastMessage,
                fontSize = 14.sp,
                color = Color(0xFF65676B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (formattedTime.isNotBlank()) {
            Text(
                text = formattedTime,
                fontSize = 12.sp,
                color = Color(0xFF8A8D91),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
