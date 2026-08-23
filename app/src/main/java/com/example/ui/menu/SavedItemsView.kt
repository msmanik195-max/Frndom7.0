package com.example.ui.menu

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.MarketplaceItem
import com.example.data.model.PostItem
import com.example.data.model.UserProfile
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.PostRepository
import com.example.ui.components.HashtagText
import com.example.ui.components.PostOptionsBottomSheet
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedItemsView(
    currentUserId: String,
    postRepository: PostRepository,
    marketplaceRepository: MarketplaceRepository,
    onPostClick: (PostItem) -> Unit,
    onOpenChat: (peer: UserProfile, initialMsg: String) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Products", "Saved Posts", "Saved Videos")

    // Marketplace Products
    val allMarketplaceItems by marketplaceRepository.getItemsFlow().collectAsState(initial = emptyList())
    var savedProductRefreshTrigger by remember { mutableIntStateOf(0) }
    val savedProductsList = remember(allMarketplaceItems, currentUserId, savedProductRefreshTrigger) {
        marketplaceRepository.getSavedProducts(currentUserId)
    }

    // Feed Posts & Videos
    val allPosts by postRepository.postsFlow.collectAsState()
    val savedPostsList = remember(allPosts, currentUserId) {
        postRepository.getSavedPosts(currentUserId)
    }

    val regularSavedPosts = savedPostsList.filter { it.mediaType != "reel" && it.mediaType != "video" }
    val savedVideosList = savedPostsList.filter { it.mediaType == "reel" || it.mediaType == "video" }

    var selectedPostForOptions by remember { mutableStateOf<PostItem?>(null) }
    var selectedProductForDetail by remember { mutableStateOf<MarketplaceItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("saved_items_view")
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("saved_items_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF050505)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Saved Items",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                }

                // 3 Tabs (Products | Saved Posts | Saved Videos)
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = Color(0xFF1877F2)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val count = when (index) {
                            0 -> savedProductsList.size
                            1 -> regularSavedPosts.size
                            else -> savedVideosList.size
                        }
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier.testTag("saved_tab_$index"),
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = if (selectedTabIndex == index) Color(0xFF1877F2) else Color(0xFFE4E6EB)
                                        ) {
                                            Text(
                                                text = "$count",
                                                color = if (selectedTabIndex == index) Color.White else Color(0xFF050505),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> {
                // Products Tab (Saved from Marketplace)
                if (savedProductsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = Color(0xFFE8F1FD)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = "Empty Products",
                                        tint = Color(0xFF1877F2),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Saved Products",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Items you bookmark and save from Marketplace will appear here.",
                                fontSize = 14.sp,
                                color = Color(0xFF65676B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(savedProductsList, key = { it.id }) { item ->
                            SavedProductCard(
                                item = item,
                                onClick = { selectedProductForDetail = item },
                                onUnsave = {
                                    marketplaceRepository.toggleSaveProduct(currentUserId, item.id)
                                    savedProductRefreshTrigger++
                                    Toast.makeText(context, "Removed from saved products", Toast.LENGTH_SHORT).show()
                                },
                                onMessageSeller = {
                                    val sellerProfile = UserProfile(
                                        uid = item.sellerId,
                                        fullName = item.sellerName,
                                        firstName = item.sellerName,
                                        profilePictureUrl = item.sellerAvatarUrl
                                    )
                                    onOpenChat(sellerProfile, "Hi ${item.sellerName}, is this ${item.title} still available?")
                                }
                            )
                        }
                    }
                }
            }
            1 -> {
                // Saved Posts Tab
                if (regularSavedPosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = Color(0xFFE4E6EB)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkBorder,
                                        contentDescription = "Empty Posts",
                                        tint = Color(0xFF65676B),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Saved Posts",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Feed posts you save will show up here.",
                                fontSize = 14.sp,
                                color = Color(0xFF65676B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(regularSavedPosts, key = { it.id }) { post ->
                            SavedPostCard(
                                post = post,
                                onClick = { onPostClick(post) },
                                onOptionsClick = { selectedPostForOptions = post },
                                onUnsave = {
                                    postRepository.toggleSavePost(currentUserId, post.id)
                                    Toast.makeText(context, "Removed from saved posts", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
            2 -> {
                // Saved Videos Tab
                if (savedVideosList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = Color(0xFFE4E6EB)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = "Empty Videos",
                                        tint = Color(0xFF65676B),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Saved Videos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Reels and videos you save will show up here.",
                                fontSize = 14.sp,
                                color = Color(0xFF65676B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(savedVideosList, key = { it.id }) { post ->
                            SavedPostCard(
                                post = post,
                                onClick = { onPostClick(post) },
                                onOptionsClick = { selectedPostForOptions = post },
                                onUnsave = {
                                    postRepository.toggleSavePost(currentUserId, post.id)
                                    Toast.makeText(context, "Removed from saved videos", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full Screen Product Detail Modal (for Saved Products)
    selectedProductForDetail?.let { item ->
        val isSaved = marketplaceRepository.isProductSaved(currentUserId, item.id)
        ProductDetailBottomSheet(
            item = item,
            currentUserId = currentUserId,
            isSaved = isSaved,
            onToggleSave = {
                val nowSaved = marketplaceRepository.toggleSaveProduct(currentUserId, item.id)
                savedProductRefreshTrigger++
                Toast.makeText(context, if (nowSaved) "Saved to Products in Saved Items" else "Removed from saved", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { selectedProductForDetail = null },
            onMessageSeller = { msg ->
                val sellerProfile = UserProfile(
                    uid = item.sellerId,
                    fullName = item.sellerName,
                    firstName = item.sellerName,
                    profilePictureUrl = item.sellerAvatarUrl
                )
                selectedProductForDetail = null
                onOpenChat(sellerProfile, msg)
            },
            onMarkAsSold = { isSold ->
                marketplaceRepository.markAsSold(item.id, isSold)
                selectedProductForDetail = item.copy(isSold = isSold)
            },
            onDeleteListing = {
                marketplaceRepository.deleteListing(item.id)
                selectedProductForDetail = null
                savedProductRefreshTrigger++
                Toast.makeText(context, "Listing deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Post Options BottomSheet
    selectedPostForOptions?.let { post ->
        PostOptionsBottomSheet(
            post = post,
            currentUserId = currentUserId,
            postRepository = postRepository,
            onEditClick = {},
            onDeletePost = {
                selectedPostForOptions = null
            },
            onDismiss = { selectedPostForOptions = null }
        )
    }
}

@Composable
fun SavedProductCard(
    item: MarketplaceItem,
    onClick: () -> Unit,
    onUnsave: () -> Unit,
    onMessageSeller: () -> Unit
) {
    val formattedPrice = "৳ " + NumberFormat.getNumberInstance(Locale.US).format(item.price.toLong())
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("saved_product_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Cover Thumbnail
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE4E6EB)),
                contentAlignment = Alignment.Center
            ) {
                if (item.coverImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.coverImageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = Color(0xFF8A8D91),
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (item.isSold) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "SOLD",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                // Price in BDT ৳
                Text(
                    text = formattedPrice,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0866FF)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Title
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF050505),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Location & Seller
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF8A8D91),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${item.location} • ${item.sellerName}",
                        fontSize = 11.sp,
                        color = Color(0xFF65676B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Saved Product • Tap to view",
                    fontSize = 11.sp,
                    color = Color(0xFF1877F2),
                    fontWeight = FontWeight.Medium
                )
            }

            // Options 3-dots
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = Color(0xFF65676B)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Message Seller") },
                        leadingIcon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onMessageSeller()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from Saved", color = Color(0xFFD32F2F)) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFD32F2F)) },
                        onClick = {
                            showMenu = false
                            onUnsave()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedPostCard(
    post: PostItem,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onUnsave: () -> Unit
) {
    val isVideo = post.mediaType == "reel" || post.mediaType == "video"
    val mediaUrls = post.getAllMediaUrls()
    val authorInitial = post.authorName.firstOrNull()?.uppercase() ?: "U"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Media Preview
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE4E6EB)),
                contentAlignment = Alignment.Center
            ) {
                if (mediaUrls.isNotEmpty()) {
                    AsyncImage(
                        model = mediaUrls.first(),
                        contentDescription = "Saved media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isVideo) {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Text preview box
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF1877F2), Color(0xFF00C6FF)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.content.take(20),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                if (post.content.isNotBlank()) {
                    HashtagText(
                        text = post.content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF050505),
                        onTextClick = onClick
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(20.dp),
                        shape = CircleShape,
                        color = Color(0xFFD8DADF)
                    ) {
                        if (post.authorAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = post.authorAvatarUrl,
                                contentDescription = post.authorName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = authorInitial,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.authorName,
                        fontSize = 12.sp,
                        color = Color(0xFF65676B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isVideo) "Saved Video • Tap to play" else "Saved Post • Tap to view",
                    fontSize = 11.sp,
                    color = Color(0xFF1877F2),
                    fontWeight = FontWeight.Medium
                )
            }

            // Options 3-Dot Button
            IconButton(onClick = onOptionsClick) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Options",
                    tint = Color(0xFF65676B)
                )
            }
        }
    }
}
