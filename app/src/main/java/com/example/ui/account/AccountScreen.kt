package com.example.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.data.model.UserProfile
import com.example.data.repository.PostRepository
import com.example.data.repository.UserRepository
import com.example.data.service.MediaUploadService

@Composable
fun AccountScreen(
    userProfile: UserProfile?,
    onLogoutClick: () -> Unit,
    postRepository: PostRepository,
    userRepository: UserRepository,
    mediaUploadService: MediaUploadService? = null,
    onAddStoryClick: () -> Unit = {},
    onMessageClick: (UserProfile) -> Unit = {},
    onUserClick: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val current = userProfile ?: UserProfile()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("account_screen")
    ) {
        FacebookProfileView(
            targetUser = current,
            currentUserId = current.uid,
            postRepository = postRepository,
            userRepository = userRepository,
            mediaUploadService = mediaUploadService,
            onAddStoryClick = onAddStoryClick,
            onMessageClick = onMessageClick,
            onUserClick = onUserClick
        )
    }
}
