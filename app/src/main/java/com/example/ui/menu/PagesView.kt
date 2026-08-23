package com.example.ui.menu

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PageItem
import com.example.data.model.UserProfile
import com.example.data.repository.GroupPageRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.StorageRepository
import com.example.data.service.MediaUploadService

@Composable
fun PagesView(
    userProfile: UserProfile?,
    groupPageRepository: GroupPageRepository,
    postRepository: PostRepository,
    mediaUploadService: MediaUploadService? = null,
    onBack: () -> Unit,
    onSwitchToPage: ((PageItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveMediaUploadService = mediaUploadService ?: remember { MediaUploadService(context, StorageRepository(context)) }

    val pages by groupPageRepository.pagesFlow.collectAsState()
    val likedPages = remember { mutableStateMapOf<String, Boolean>() }
    var selectedPageForDetail by remember { mutableStateOf<PageItem?>(null) }
    var showCreateScreen by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Discover", "Your Pages")

    if (showCreateScreen) {
        CreatePageScreen(
            userProfile = userProfile,
            groupPageRepository = groupPageRepository,
            mediaUploadService = effectiveMediaUploadService,
            onPageCreated = { createdPage ->
                showCreateScreen = false
                selectedPageForDetail = createdPage
            },
            onBack = { showCreateScreen = false },
            modifier = modifier
        )
        return
    }

    if (selectedPageForDetail != null) {
        PageDetailView(
            page = selectedPageForDetail!!,
            userProfile = userProfile,
            groupPageRepository = groupPageRepository,
            postRepository = postRepository,
            mediaUploadService = effectiveMediaUploadService,
            onBack = { selectedPageForDetail = null },
            onSwitchToPage = onSwitchToPage,
            modifier = modifier
        )
        return
    }

    val currentUid = userProfile?.uid ?: ""
    val userPages = pages.filter { it.creatorId == currentUid || likedPages[it.id] == true || it.creatorId.isBlank() }
    val displayedPages = if (selectedTab == 1) userPages else pages

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("pages_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pages",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                }

                Button(
                    onClick = { showCreateScreen = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Create", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF1877F2)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

            if (displayedPages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEBF5FF),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (selectedTab == 1) "No Pages Created or Liked" else "No Pages Available",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (selectedTab == 1)
                            "Create your page with custom profile picture, banner, and category to post updates and grow an audience!"
                        else
                            "Create a page to share your passion, business, or brand with followers on Frndom.",
                        fontSize = 14.sp,
                        color = Color(0xFF65676B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showCreateScreen = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Create a Page", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displayedPages, key = { it.id }) { page ->
                        val isCreator = page.creatorId == currentUid || page.creatorId.isBlank()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPageForDetail = page },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (page.coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = page.coverUrl,
                                        contentDescription = page.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF1877F2), Color(0xFF0F57C4))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape),
                                            shape = CircleShape,
                                            color = Color(0xFFEBF5FF)
                                        ) {
                                            if (page.avatarUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = page.avatarUrl,
                                                    contentDescription = page.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Flag,
                                                        contentDescription = null,
                                                        tint = Color(0xFF1877F2),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = page.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF050505)
                                            )
                                            Text(
                                                text = "${page.category} • ${page.likesCount} likes",
                                                fontSize = 12.sp,
                                                color = Color(0xFF65676B)
                                            )
                                        }
                                    }

                                    if (page.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = page.description,
                                            fontSize = 13.sp,
                                            color = Color(0xFF65676B),
                                            maxLines = 2
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { selectedPageForDetail = page },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEBF5FF), contentColor = Color(0xFF1877F2)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("View Page", fontWeight = FontWeight.Bold)
                                        }

                                        if (isCreator && onSwitchToPage != null) {
                                            Button(
                                                onClick = { onSwitchToPage(page) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2), contentColor = Color.White),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Switch", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
