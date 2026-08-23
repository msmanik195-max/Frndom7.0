package com.example.ui.search

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GroupItem
import com.example.data.model.PageItem
import com.example.data.model.PostItem
import com.example.data.model.UserProfile
import com.example.data.repository.GroupPageRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.SearchHistoryRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.VerificationBadge
import com.example.ui.home.PostCardItem

@Composable
fun SearchScreen(
    currentUserId: String,
    userRepository: UserRepository,
    postRepository: PostRepository,
    onUserClick: (UserProfile) -> Unit,
    onReelClick: (PostItem) -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val groupPageRepository = remember { GroupPageRepository(context) }
    val searchHistoryRepository = remember { SearchHistoryRepository(context) }

    BackHandler {
        onBackClick()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Posts", "Reels", "People", "Pages", "Groups")

    val allUsers by userRepository.getAllUsersFlow().collectAsState(initial = emptyList())
    val currentUserProfile = remember(allUsers, currentUserId) { allUsers.find { it.uid == currentUserId } }
    val allPosts by postRepository.postsFlow.collectAsState()
    val allPages by groupPageRepository.pagesFlow.collectAsState()
    val allGroups by groupPageRepository.groupsFlow.collectAsState()
    val recentSearches by searchHistoryRepository.recentSearchesFlow.collectAsState()

    // Joined groups and followed pages local states
    val followedPages = remember { mutableStateMapOf<String, Boolean>() }
    val joinedGroups = remember { mutableStateMapOf<String, Boolean>() }

    // Filtered items when query is present
    val filteredUsers = remember(searchQuery, allUsers) {
        if (searchQuery.isBlank()) emptyList()
        else allUsers.filter { u ->
            val name = u.fullName.ifBlank { "${u.firstName} ${u.lastName}" }
            name.contains(searchQuery, ignoreCase = true) ||
                    u.email.contains(searchQuery, ignoreCase = true) ||
                    u.currentCity.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredPosts = remember(searchQuery, allPosts) {
        val nonReels = allPosts.filter { it.mediaType != "reel" }
        if (searchQuery.isBlank()) emptyList()
        else nonReels.filter {
            it.content.contains(searchQuery, ignoreCase = true) ||
                    it.authorName.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredReels = remember(searchQuery, allPosts) {
        val reels = allPosts.filter { it.mediaType == "reel" || it.mediaType == "video" }
        if (searchQuery.isBlank()) emptyList()
        else reels.filter {
            it.content.contains(searchQuery, ignoreCase = true) ||
                    it.authorName.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredPages = remember(searchQuery, allPages) {
        if (searchQuery.isBlank()) emptyList()
        else allPages.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredGroups = remember(searchQuery, allGroups) {
        if (searchQuery.isBlank()) emptyList()
        else allGroups.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.privacy.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .testTag("search_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF050505)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isNotBlank()) {
                            searchHistoryRepository.addSearchQuery(it)
                        }
                    },
                    placeholder = { Text("Search Posts, Reels, People, Pages, Groups...", color = Color(0xFF65676B), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF65676B)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF65676B)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFFF0F2F5),
                        unfocusedContainerColor = Color(0xFFF0F2F5)
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("search_query_input")
                )
            }

            // If searchQuery is empty -> Show Recent searches & Ranked / Trending
            if (searchQuery.isBlank()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Recent Searches
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                                TextButton(onClick = { searchHistoryRepository.clearAll() }) {
                                    Text(
                                        text = "Clear all",
                                        fontSize = 13.sp,
                                        color = Color(0xFF1877F2),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        items(recentSearches, key = { "recent_$it" }) { query ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { searchQuery = query }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF0F2F5),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = Color(0xFF65676B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = query,
                                        fontSize = 15.sp,
                                        color = Color(0xFF050505),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                IconButton(
                                    onClick = { searchHistoryRepository.removeSearchQuery(query) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color(0xFF65676B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Divider(
                                thickness = 4.dp,
                                color = Color(0xFFF0F2F5),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Ranked Pages (Top Trending Pages)
                    if (allPages.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Top Ranked Pages",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                            }
                        }

                        items(allPages.take(4), key = { "ranked_page_${it.id}" }) { page ->
                            PageSearchRow(
                                page = page,
                                isFollowed = followedPages[page.id] == true,
                                onToggleFollow = { followedPages[page.id] = !(followedPages[page.id] ?: false) },
                                onClick = { searchQuery = page.name }
                            )
                        }

                        item {
                            Divider(
                                thickness = 4.dp,
                                color = Color(0xFFF0F2F5),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Ranked Groups (Top Ranked Communities)
                    if (allGroups.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ranked Groups & Communities",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                            }
                        }

                        items(allGroups.take(4), key = { "ranked_group_${it.id}" }) { group ->
                            GroupSearchRow(
                                group = group,
                                isJoined = joinedGroups[group.id] == true,
                                onToggleJoin = { joinedGroups[group.id] = !(joinedGroups[group.id] ?: false) },
                                onClick = { searchQuery = group.name }
                            )
                        }
                    }

                    if (recentSearches.isEmpty() && allPages.isEmpty() && allGroups.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF0F2F5),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color(0xFF65676B),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Search Frndom",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Find friends, reels, posts, photos, and communities",
                                    fontSize = 13.sp,
                                    color = Color(0xFF65676B)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            } else {
                // Active Search State: Display Tab filters & results matching searchQuery
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                    containerColor = Color.White,
                    contentColor = Color(0xFF0866FF),
                    divider = { Divider(thickness = 0.5.dp, color = Color(0xFFCED0D4)) },
                    modifier = Modifier.testTag("search_tabs")
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 -> { // ALL
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // People
                            if (filteredUsers.isNotEmpty()) {
                                item {
                                    SearchSectionHeader(title = "People", onSeeAll = { selectedTabIndex = 3 })
                                }
                                items(filteredUsers.take(3), key = { "user_${it.uid}" }) { user ->
                                    UserSearchRow(
                                        user = user,
                                        currentUserId = currentUserId,
                                        onUserClick = { onUserClick(user) },
                                        onToggleFollow = { userRepository.toggleFollow(currentUserId, user.uid) }
                                    )
                                }
                            }

                            // Pages
                            if (filteredPages.isNotEmpty()) {
                                item {
                                    SearchSectionHeader(title = "Pages", onSeeAll = { selectedTabIndex = 4 })
                                }
                                items(filteredPages.take(3), key = { "page_${it.id}" }) { page ->
                                    PageSearchRow(
                                        page = page,
                                        isFollowed = followedPages[page.id] == true,
                                        onToggleFollow = { followedPages[page.id] = !(followedPages[page.id] ?: false) },
                                        onClick = {}
                                    )
                                }
                            }

                            // Groups
                            if (filteredGroups.isNotEmpty()) {
                                item {
                                    SearchSectionHeader(title = "Groups", onSeeAll = { selectedTabIndex = 5 })
                                }
                                items(filteredGroups.take(3), key = { "group_${it.id}" }) { group ->
                                    GroupSearchRow(
                                        group = group,
                                        isJoined = joinedGroups[group.id] == true,
                                        onToggleJoin = { joinedGroups[group.id] = !(joinedGroups[group.id] ?: false) },
                                        onClick = {}
                                    )
                                }
                            }

                            // Reels
                            if (filteredReels.isNotEmpty()) {
                                item {
                                    SearchSectionHeader(title = "Reels", onSeeAll = { selectedTabIndex = 2 })
                                }
                                item {
                                    LazyRow(
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(filteredReels.take(6), key = { "reel_row_${it.id}" }) { reel ->
                                            ReelSearchCard(reel = reel, onReelClick = onReelClick)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }

                            // Posts
                            if (filteredPosts.isNotEmpty()) {
                                item {
                                    SearchSectionHeader(title = "Posts", onSeeAll = { selectedTabIndex = 1 })
                                }
                                items(filteredPosts.take(4), key = { "post_${it.id}" }) { post ->
                                    PostCardItem(
                                        post = post,
                                        currentUserId = currentUserId,
                                        currentUserProfile = currentUserProfile,
                                        onUserClick = {
                                            val profile = UserProfile(
                                                uid = post.authorId,
                                                fullName = post.authorName,
                                                profilePictureUrl = post.authorAvatarUrl
                                            )
                                            onUserClick(profile)
                                        },
                                        onLikeClick = { postRepository.toggleLike(post.id, currentUserId) },
                                        onReactionClick = { reaction -> postRepository.setReaction(post.id, currentUserId, reaction) },
                                        onCommentClick = { },
                                        onShareClick = { }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            if (filteredUsers.isEmpty() && filteredPages.isEmpty() && filteredGroups.isEmpty() && filteredPosts.isEmpty() && filteredReels.isEmpty()) {
                                item {
                                    EmptySearchResults(query = searchQuery)
                                }
                            }
                        }
                    }

                    1 -> { // POSTS
                        if (filteredPosts.isEmpty()) {
                            EmptySearchResults(query = searchQuery)
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredPosts, key = { "tab_post_${it.id}" }) { post ->
                                    PostCardItem(
                                        post = post,
                                        currentUserId = currentUserId,
                                        currentUserProfile = currentUserProfile,
                                        onUserClick = {
                                            val profile = UserProfile(
                                                uid = post.authorId,
                                                fullName = post.authorName,
                                                profilePictureUrl = post.authorAvatarUrl
                                            )
                                            onUserClick(profile)
                                        },
                                        onLikeClick = { postRepository.toggleLike(post.id, currentUserId) },
                                        onReactionClick = { reaction -> postRepository.setReaction(post.id, currentUserId, reaction) },
                                        onCommentClick = { },
                                        onShareClick = { }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    2 -> { // REELS
                        if (filteredReels.isEmpty()) {
                            EmptySearchResults(query = searchQuery)
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredReels, key = { "tab_reel_${it.id}" }) { reel ->
                                    ReelSearchCard(reel = reel, onReelClick = onReelClick, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }

                    3 -> { // PEOPLE
                        if (filteredUsers.isEmpty()) {
                            EmptySearchResults(query = searchQuery)
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredUsers, key = { "tab_user_${it.uid}" }) { user ->
                                    UserSearchRow(
                                        user = user,
                                        currentUserId = currentUserId,
                                        onUserClick = { onUserClick(user) },
                                        onToggleFollow = { userRepository.toggleFollow(currentUserId, user.uid) }
                                    )
                                }
                            }
                        }
                    }

                    4 -> { // PAGES
                        if (filteredPages.isEmpty()) {
                            EmptySearchResults(query = searchQuery)
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredPages, key = { "tab_page_${it.id}" }) { page ->
                                    PageSearchRow(
                                        page = page,
                                        isFollowed = followedPages[page.id] == true,
                                        onToggleFollow = { followedPages[page.id] = !(followedPages[page.id] ?: false) },
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }

                    5 -> { // GROUPS
                        if (filteredGroups.isEmpty()) {
                            EmptySearchResults(query = searchQuery)
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredGroups, key = { "tab_group_${it.id}" }) { group ->
                                    GroupSearchRow(
                                        group = group,
                                        isJoined = joinedGroups[group.id] == true,
                                        onToggleJoin = { joinedGroups[group.id] = !(joinedGroups[group.id] ?: false) },
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF050505)
        )
        Text(
            text = "See all",
            fontSize = 14.sp,
            color = Color(0xFF1877F2),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onSeeAll)
        )
    }
}

@Composable
private fun UserSearchRow(
    user: UserProfile,
    currentUserId: String,
    onUserClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val displayName = user.fullName.ifBlank { "${user.firstName} ${user.lastName}".trim().ifBlank { "User" } }
    val isFollowing = user.followersMap.containsKey(currentUserId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (user.profilePictureUrl.isNotBlank()) {
            AsyncImage(
                model = user.profilePictureUrl,
                contentDescription = displayName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(0xFFE4E6EB)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1877F2)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )
                if (user.isVerificationActive()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerificationBadge(size = 15.dp, show = true)
                }
            }
            val subtext = when {
                user.currentCity.isNotBlank() -> "Lives in ${user.currentCity}"
                user.email.isNotBlank() -> user.email
                else -> "${user.followersCount} followers"
            }
            Text(
                text = subtext,
                fontSize = 13.sp,
                color = Color(0xFF65676B)
            )
        }

        if (user.uid != currentUserId) {
            Button(
                onClick = onToggleFollow,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) Color(0xFFE4E6EB) else Color(0xFF1877F2),
                    contentColor = if (isFollowing) Color(0xFF050505) else Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PageSearchRow(
    page: PageItem,
    isFollowed: Boolean,
    onToggleFollow: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (page.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = page.avatarUrl,
                contentDescription = page.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(0xFFFCE4EC)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = page.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF050505)
            )
            Text(
                text = "${page.category} • ${page.followersCount} followers",
                fontSize = 13.sp,
                color = Color(0xFF65676B)
            )
        }

        Button(
            onClick = onToggleFollow,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFollowed) Color(0xFFE4E6EB) else Color(0xFF1877F2),
                contentColor = if (isFollowed) Color(0xFF050505) else Color.White
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = if (isFollowed) "Following" else "Follow",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GroupSearchRow(
    group: GroupItem,
    isJoined: Boolean,
    onToggleJoin: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (group.coverUrl.isNotBlank()) {
            AsyncImage(
                model = group.coverUrl,
                contentDescription = group.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE0F2F1)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = Color(0xFF009688),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF050505),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${group.privacy} • ${group.membersCount} members",
                fontSize = 13.sp,
                color = Color(0xFF65676B)
            )
        }

        Button(
            onClick = onToggleJoin,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isJoined) Color(0xFFE4E6EB) else Color(0xFF1877F2),
                contentColor = if (isJoined) Color(0xFF050505) else Color.White
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = if (isJoined) "Joined" else "Join",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReelSearchCard(
    reel: PostItem,
    onReelClick: (PostItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(130.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onReelClick(reel) },
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (reel.mediaUrl.isNotBlank()) {
                AsyncImage(
                    model = reel.mediaUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                            startY = 100f
                        )
                    )
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = reel.authorName.ifBlank { "Creator" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${reel.likesCount} likes",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun EmptySearchResults(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF0F2F5),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF65676B),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No results found for \"$query\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF050505)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Try searching with different keywords, check the spelling, or discover top ranked pages and groups.",
                fontSize = 13.sp,
                color = Color(0xFF65676B),
                lineHeight = 18.sp
            )
        }
    }
}
