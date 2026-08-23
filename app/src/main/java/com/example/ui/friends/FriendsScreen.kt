package com.example.ui.friends

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.ui.components.VerificationBadge

@Composable
fun FriendsScreen(
    currentUserId: String,
    userRepository: UserRepository,
    onUserClick: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val allUsers by userRepository.getAllUsersFlow().collectAsState(initial = emptyList())
    val otherUsers = allUsers.filter { it.uid.isNotBlank() && it.uid != currentUserId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("friends_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "People & Friends",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF050505),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

            if (otherUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No other users registered yet",
                        fontSize = 15.sp,
                        color = Color(0xFF65676B)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(otherUsers, key = { it.uid }) { user ->
                        val isFollowing = user.followersMap[currentUserId] == true
                        val displayName = user.fullName.ifBlank { "${user.firstName} ${user.lastName}".trim() }.ifBlank { "User" }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUserClick(user) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(54.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                if (user.profilePictureUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = user.profilePictureUrl,
                                        contentDescription = displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = displayName.firstOrNull()?.uppercase() ?: "U",
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
                                        text = displayName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF050505)
                                    )
                                    if (user.isVerificationActive()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        VerificationBadge(size = 15.dp, show = true)
                                    }
                                }

                                if (user.bio.isNotBlank()) {
                                    Text(
                                        text = user.bio,
                                        fontSize = 13.sp,
                                        color = Color(0xFF65676B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { userRepository.toggleFollow(currentUserId, user.uid) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color(0xFFE4E6EB) else Color(0xFF0866FF),
                                    contentColor = if (isFollowing) Color(0xFF050505) else Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isFollowing) "Following" else "Follow", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
