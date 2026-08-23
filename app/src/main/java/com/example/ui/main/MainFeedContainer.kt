package com.example.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.model.AudioCallSession
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.StorageRepository
import com.example.data.repository.StoryRepository
import com.example.data.repository.UserRepository
import com.example.data.service.MediaUploadService
import com.example.ui.account.AccountScreen
import com.example.ui.account.FacebookProfileView
import com.example.ui.chats.ChatDetailScreen
import com.example.ui.chats.ChatsScreen
import com.example.ui.components.FrndomBottomNavigation
import com.example.ui.components.FrndomTopHeader
import com.example.ui.create.CreatePostScreen
import com.example.ui.friends.FriendsScreen
import com.example.ui.home.HomeScreen
import com.example.ui.menu.MenuScreen
import com.example.ui.navigation.AppDestination
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.reels.ReelsScreen
import com.example.ui.search.SearchScreen
import com.example.ui.storage.StorageManagementScreen
import kotlinx.coroutines.launch

@Composable
fun MainFeedContainer(
    userProfile: UserProfile?,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val postRepository = remember { PostRepository(context) }
    val storageRepository = remember { StorageRepository(context) }
    val storyRepository = remember { StoryRepository(context) }
    val userRepository = remember { UserRepository(context) }
    val chatRepository = remember { ChatRepository(context) }
    val notificationRepository = remember { com.example.data.repository.NotificationRepository(context) }
    val mediaUploadService = remember { MediaUploadService(context, storageRepository) }

    var currentActiveProfile by remember { mutableStateOf(userProfile) }
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    var visitedUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var activeChatPeer by remember { mutableStateOf<UserProfile?>(null) }
    var isMenuSubScreenActive by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentUid = currentActiveProfile?.uid ?: ""
    val liveUserProfile by userRepository.getUserProfileFlow(currentUid).collectAsState(initial = currentActiveProfile)
    val effectiveUserProfile = liveUserProfile ?: currentActiveProfile

    val currentUserName = effectiveUserProfile?.fullName.orEmpty().ifBlank { "${effectiveUserProfile?.firstName} ${effectiveUserProfile?.lastName}".trim() }.ifBlank { "User" }
    val currentUserAvatar = effectiveUserProfile?.profilePictureUrl ?: ""

    // Global Incoming Call Listener
    var activeGlobalCallSession by remember { mutableStateOf<AudioCallSession?>(null) }
    val incomingCall by chatRepository.listenForIncomingCalls(currentUid).collectAsState(initial = null)
    LaunchedEffect(incomingCall) {
        if (incomingCall != null && activeGlobalCallSession == null) {
            activeGlobalCallSession = incomingCall
        }
    }

    // Global Audio Call Screen overlay
    val currentCall = activeGlobalCallSession
    if (currentCall != null) {
        com.example.ui.chats.AudioCallScreen(
            currentUserId = currentUid,
            callSession = currentCall,
            chatRepository = chatRepository,
            onCallClosed = { activeGlobalCallSession = null }
        )
        return
    }

    // Mobile system/device back button handler
    BackHandler(enabled = activeChatPeer != null || visitedUserProfile != null || isMenuSubScreenActive || currentDestination != AppDestination.HOME) {
        if (activeChatPeer != null) {
            activeChatPeer = null
        } else if (visitedUserProfile != null) {
            visitedUserProfile = null
        } else if (currentDestination == AppDestination.SERVER_SETTINGS) {
            currentDestination = AppDestination.MENU
        } else if (isMenuSubScreenActive) {
            isMenuSubScreenActive = false
        } else {
            currentDestination = AppDestination.HOME
        }
    }

    val isFullScreenDestination = activeChatPeer != null ||
            visitedUserProfile != null ||
            currentDestination == AppDestination.CREATE_POST ||
            currentDestination == AppDestination.SERVER_SETTINGS ||
            currentDestination == AppDestination.SEARCH ||
            (currentDestination == AppDestination.MENU && isMenuSubScreenActive)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_feed_container"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isFullScreenDestination) {
                FrndomTopHeader(
                    onSearchClick = { currentDestination = AppDestination.SEARCH },
                    onFriendsClick = { currentDestination = AppDestination.FRIENDS },
                    onNotificationsClick = { currentDestination = AppDestination.NOTIFICATIONS },
                    onMenuClick = { currentDestination = AppDestination.MENU }
                )
            }
        },
        bottomBar = {
            if (!isFullScreenDestination) {
                FrndomBottomNavigation(
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        visitedUserProfile = null
                        activeChatPeer = null
                        isMenuSubScreenActive = false
                        currentDestination = destination
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeChatPeer != null) {
                // 1-on-1 Messenger Chat Screen
                ChatDetailScreen(
                    currentUserId = currentUid,
                    currentUserName = currentUserName,
                    currentUserAvatar = currentUserAvatar,
                    peerProfile = activeChatPeer!!,
                    chatRepository = chatRepository,
                    mediaUploadService = mediaUploadService,
                    onBackClick = { activeChatPeer = null }
                )
            } else if (visitedUserProfile != null) {
                // Visiting another user's Facebook profile
                FacebookProfileView(
                    targetUser = visitedUserProfile!!,
                    currentUserId = currentUid,
                    postRepository = postRepository,
                    userRepository = userRepository,
                    mediaUploadService = mediaUploadService,
                    onAddStoryClick = { currentDestination = AppDestination.HOME },
                    onMessageClick = { peer ->
                        activeChatPeer = peer
                    },
                    onUserClick = { clickedPeer ->
                        visitedUserProfile = clickedPeer
                    }
                )
            } else {
                when (currentDestination) {
                    AppDestination.HOME -> {
                        HomeScreen(
                            userProfile = effectiveUserProfile,
                            postRepository = postRepository,
                            storageRepository = storageRepository,
                            storyRepository = storyRepository,
                            mediaUploadService = mediaUploadService,
                            onCreatePostClick = { currentDestination = AppDestination.CREATE_POST },
                            onProfileClick = { currentDestination = AppDestination.ACCOUNT },
                            onUserClick = { author ->
                                if (author.uid == currentUid) {
                                    currentDestination = AppDestination.ACCOUNT
                                } else {
                                    visitedUserProfile = author
                                }
                            },
                            onShareClick = { post ->
                                val sendIntent: android.content.Intent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Check out this post on Frndom: https://frndom.app/post/${post.id}")
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            }
                        )
                    }

                    AppDestination.REELS -> {
                        ReelsScreen(
                            userProfile = effectiveUserProfile,
                            postRepository = postRepository,
                            onCreateReelClick = { currentDestination = AppDestination.CREATE_POST },
                            onCommentClick = { reel ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Comment added to ${reel.authorName}'s reel")
                                }
                            },
                            onShareClick = { reel ->
                                val sendIntent: android.content.Intent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Check out this reel on Frndom: https://frndom.app/post/${reel.id}")
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            }
                        )
                    }

                    AppDestination.CREATE_POST -> {
                        CreatePostScreen(
                            userProfile = effectiveUserProfile,
                            mediaUploadService = mediaUploadService,
                            onPostCreated = { newPost ->
                                postRepository.createPost(
                                    newPost.copy(
                                        authorAvatarUrl = effectiveUserProfile?.profilePictureUrl ?: ""
                                    )
                                )
                                currentDestination = if (newPost.mediaType == "reel") AppDestination.REELS else AppDestination.HOME
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (newPost.mediaType == "reel") "Reel published successfully!" else "Post published successfully!"
                                    )
                                }
                            },
                            onBackClick = { currentDestination = AppDestination.HOME }
                        )
                    }

                    AppDestination.CHATS -> {
                        ChatsScreen(
                            currentUserId = currentUid,
                            chatRepository = chatRepository,
                            onOpenChat = { peer ->
                                activeChatPeer = peer
                            }
                        )
                    }

                    AppDestination.ACCOUNT -> {
                        AccountScreen(
                            userProfile = effectiveUserProfile,
                            onLogoutClick = onLogoutClick,
                            postRepository = postRepository,
                            userRepository = userRepository,
                            mediaUploadService = mediaUploadService,
                            onAddStoryClick = { currentDestination = AppDestination.HOME },
                            onMessageClick = { peer -> activeChatPeer = peer },
                            onUserClick = { clickedPeer ->
                                if (clickedPeer.uid == currentUid) {
                                    currentDestination = AppDestination.ACCOUNT
                                } else {
                                    visitedUserProfile = clickedPeer
                                }
                            }
                        )
                    }

                    AppDestination.SEARCH -> {
                        SearchScreen(
                            currentUserId = currentUid,
                            userRepository = userRepository,
                            postRepository = postRepository,
                            onUserClick = { searchedUser ->
                                if (searchedUser.uid == currentUid) {
                                    currentDestination = AppDestination.ACCOUNT
                                } else {
                                    visitedUserProfile = searchedUser
                                }
                            },
                            onReelClick = {
                                currentDestination = AppDestination.REELS
                            },
                            onBackClick = {
                                currentDestination = AppDestination.HOME
                            }
                        )
                    }

                    AppDestination.FRIENDS -> {
                        FriendsScreen(
                            currentUserId = currentUid,
                            userRepository = userRepository,
                            onUserClick = { friendUser ->
                                if (friendUser.uid == currentUid) {
                                    currentDestination = AppDestination.ACCOUNT
                                } else {
                                    visitedUserProfile = friendUser
                                }
                            }
                        )
                    }

                    AppDestination.NOTIFICATIONS -> {
                        NotificationsScreen(
                            currentUserId = currentUid,
                            notificationRepository = notificationRepository,
                            userRepository = userRepository,
                            onUserClick = { notifUser ->
                                if (notifUser.uid == currentUid) {
                                    currentDestination = AppDestination.ACCOUNT
                                } else {
                                    visitedUserProfile = notifUser
                                }
                            }
                        )
                    }

                    AppDestination.MENU -> {
                        MenuScreen(
                            userProfile = effectiveUserProfile,
                            onProfileClick = { currentDestination = AppDestination.ACCOUNT },
                            onServerSettingsClick = { currentDestination = AppDestination.SERVER_SETTINGS },
                            onLogoutClick = onLogoutClick,
                            onSearchClick = { currentDestination = AppDestination.SEARCH },
                            onPostClick = { post ->
                                currentDestination = if (post.mediaType == "reel") AppDestination.REELS else AppDestination.HOME
                            },
                            onAccountSwitched = { newProfile ->
                                currentActiveProfile = newProfile
                                scope.launch {
                                    snackbarHostState.showSnackbar("Switched to ${newProfile.fullName.ifBlank { newProfile.firstName }}")
                                }
                            },
                            onAddNewAccount = onLogoutClick,
                            onSubScreenChanged = { isMenuSubScreenActive = it },
                            mediaUploadService = mediaUploadService,
                            onOpenChat = { peer, initialMsg ->
                                activeChatPeer = peer
                                if (initialMsg.isNotBlank()) {
                                    chatRepository.sendMessage(
                                        senderId = currentUid,
                                        senderName = currentUserName,
                                        senderAvatar = currentUserAvatar,
                                        receiverId = peer.uid,
                                        receiverName = peer.fullName.ifBlank { peer.firstName },
                                        receiverAvatar = peer.profilePictureUrl,
                                        text = initialMsg
                                    )
                                }
                            }
                        )
                    }

                    AppDestination.SERVER_SETTINGS -> {
                        StorageManagementScreen(
                            storageRepository = storageRepository,
                            userId = effectiveUserProfile?.uid ?: "global",
                            onBackClick = { currentDestination = AppDestination.MENU }
                        )
                    }
                }
            }
        }
    }
}
