package com.example.ui.menu

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.components.VerificationBadge
import com.example.data.repository.UserRepository
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MarketplaceItem
import com.example.data.model.UserProfile
import com.example.data.repository.MarketplaceRepository
import com.example.data.service.MediaUploadService
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceView(
    userProfile: UserProfile?,
    marketplaceRepository: MarketplaceRepository,
    mediaUploadService: MediaUploadService?,
    onBack: () -> Unit,
    onOpenChat: (peer: UserProfile, initialMessage: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items by marketplaceRepository.getItemsFlow().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showOnlyMyListings by remember { mutableStateOf(false) }

    var showSellModal by remember { mutableStateOf(false) }
    var selectedProductForDetail by remember { mutableStateOf<MarketplaceItem?>(null) }

    val currentUid = userProfile?.uid ?: ""
    val currentUserName = userProfile?.fullName.orEmpty().ifBlank { "${userProfile?.firstName} ${userProfile?.lastName}".trim() }.ifBlank { "User" }
    val currentUserAvatar = userProfile?.profilePictureUrl ?: ""

    val categories = listOf("All", "Mobiles", "Vehicles", "Electronics", "Fashion", "Home Goods", "Hobbies", "Property")

    val filteredItems = items.filter { item ->
        val matchesCategory = (selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true))
        val matchesSearch = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true) || item.location.contains(searchQuery, ignoreCase = true)
        val matchesMyListings = !showOnlyMyListings || item.sellerId == currentUid
        matchesCategory && matchesSearch && matchesMyListings
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("marketplace_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack, modifier = Modifier.testTag("marketplace_back_button")) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF050505)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Marketplace",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF050505)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isSearchActive = !isSearchActive },
                                modifier = Modifier.testTag("marketplace_search_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF050505)
                                )
                            }

                            // Sell Button (+)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF1877F2),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showSellModal = true }
                                    .testTag("marketplace_sell_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Sell",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sell",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    // Search Input Bar (Toggleable)
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search Marketplace in Bangladesh...", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF65676B)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF0F2F5),
                                unfocusedContainerColor = Color(0xFFF0F2F5),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("marketplace_search_input")
                        )
                    }

                    // Top Action Pills (Sell, Categories, My Listings)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (showOnlyMyListings) Color(0xFFE8F1FD) else Color(0xFFF0F2F5),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showOnlyMyListings = !showOnlyMyListings }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (showOnlyMyListings) Color(0xFF1877F2) else Color(0xFF050505),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (showOnlyMyListings) "All Items" else "My Listings",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (showOnlyMyListings) Color(0xFF1877F2) else Color(0xFF050505)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFF0F2F5),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showSellModal = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sell an Item",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1877F2)
                                )
                            }
                        }
                    }

                    // Category Chips Bar
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF1877F2) else Color(0xFFE4E6EB),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF050505),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Products Grid
            if (filteredItems.isEmpty()) {
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
                                    contentDescription = null,
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (showOnlyMyListings) "You have no listings yet" else "No marketplace items found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Sell' to list your items in BDT (৳) for other users to buy!",
                            fontSize = 13.sp,
                            color = Color(0xFF65676B),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showSellModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Post an Item for Sale (৳)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val isItemSaved = marketplaceRepository.isProductSaved(currentUid, item.id)
                        MarketplaceProductCard(
                            item = item,
                            isSaved = isItemSaved,
                            onToggleSave = {
                                val nowSaved = marketplaceRepository.toggleSaveProduct(currentUid, item.id)
                                Toast.makeText(context, if (nowSaved) "Saved to Products in Saved Items" else "Removed from saved", Toast.LENGTH_SHORT).show()
                            },
                            onClick = { selectedProductForDetail = item }
                        )
                    }
                }
            }
        }

        // Sell New Item BottomSheet / Modal
        if (showSellModal) {
            SellListingBottomSheet(
                currentUserId = currentUid,
                currentUserName = currentUserName,
                currentUserAvatar = currentUserAvatar,
                mediaUploadService = mediaUploadService,
                onDismiss = { showSellModal = false },
                onPublish = { newItem ->
                    marketplaceRepository.createListing(newItem)
                    showSellModal = false
                    Toast.makeText(context, "Item listed on Marketplace successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Full Screen Book-like Bottom Modal for Product Detail
        selectedProductForDetail?.let { item ->
            val isItemSaved = marketplaceRepository.isProductSaved(currentUid, item.id)
            ProductDetailBottomSheet(
                item = item,
                currentUserId = currentUid,
                isSaved = isItemSaved,
                onToggleSave = {
                    val nowSaved = marketplaceRepository.toggleSaveProduct(currentUid, item.id)
                    Toast.makeText(context, if (nowSaved) "Saved to Products in Saved Items" else "Removed from saved", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { selectedProductForDetail = null },
                onMessageSeller = { message ->
                    val sellerProfile = UserProfile(
                        uid = item.sellerId,
                        fullName = item.sellerName,
                        firstName = item.sellerName,
                        profilePictureUrl = item.sellerAvatarUrl
                    )
                    selectedProductForDetail = null
                    onOpenChat(sellerProfile, message)
                },
                onMarkAsSold = { isSold ->
                    marketplaceRepository.markAsSold(item.id, isSold)
                    selectedProductForDetail = item.copy(isSold = isSold)
                },
                onDeleteListing = {
                    marketplaceRepository.deleteListing(item.id)
                    selectedProductForDetail = null
                    Toast.makeText(context, "Listing deleted", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun MarketplaceProductCard(
    item: MarketplaceItem,
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    onClick: () -> Unit
) {
    val formattedPrice = "৳ " + NumberFormat.getNumberInstance(Locale.US).format(item.price.toLong())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .testTag("marketplace_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFFE4E6EB))
            ) {
                if (item.coverImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.coverImageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFFB0B3B8),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                if (item.isSold) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "SOLD",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }

                // Bookmark / Save Quick Button on Top End
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .clickable(onClick = onToggleSave)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) Color(0xFF1877F2) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Condition Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = item.condition,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                // Price in BDT
                Text(
                    text = formattedPrice,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (item.isSold) Color(0xFF65676B) else Color(0xFF050505),
                    textDecoration = if (item.isSold) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Title
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF050505),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF8A8D91),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = item.location,
                        fontSize = 11.sp,
                        color = Color(0xFF65676B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// FULL-SCREEN BOTTOM SHEET PRODUCT DETAIL (BOOK-LIKE SLIDE UP)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailBottomSheet(
    item: MarketplaceItem,
    currentUserId: String,
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    onDismiss: () -> Unit,
    onMessageSeller: (String) -> Unit,
    onMarkAsSold: (Boolean) -> Unit,
    onDeleteListing: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val userRepo = remember { UserRepository(context) }
    val currentUser = remember { userRepo.getCurrentUser() }
    val sellerProfile = remember(item.sellerId) { userRepo.getLocalUserProfile(item.sellerId) }
    val persistentVerification = remember(item.sellerId) { userRepo.getPersistentVerification(item.sellerId) }
    val isOwner = currentUserId == item.sellerId
    val isSellerVerified = (sellerProfile?.isVerificationActive() == true) ||
                           (persistentVerification != null) ||
                           (isOwner && (currentUser?.isVerificationActive() == true || userRepo.getPersistentVerification(currentUserId) != null))

    var prefilledMessage by remember { mutableStateOf("Hi ${item.sellerName}, is this item still available?") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val formattedPrice = "৳ " + NumberFormat.getNumberInstance(Locale.US).format(item.price.toLong())
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(item.createdAt))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }

                Row {
                    IconButton(onClick = onToggleSave) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) Color(0xFF1877F2) else Color(0xFF050505)
                        )
                    }

                    IconButton(onClick = {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Check out ${item.title} on Frndom Marketplace for $formattedPrice!\nLocation: ${item.location}")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFF050505)
                        )
                    }
                }
            }

            // Image Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color(0xFFE4E6EB))
            ) {
                if (item.coverImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.coverImageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFF8A8D91),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                if (item.isSold) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "MARKED AS SOLD",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Product Information
            Column(modifier = Modifier.padding(16.dp)) {
                // Title
                Text(
                    text = item.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Price in BDT (৳)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedPrice,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0866FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F1FD)
                    ) {
                        Text(
                            text = "৳ BDT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1877F2),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Listed in ${item.location} • $formattedDate",
                    fontSize = 13.sp,
                    color = Color(0xFF65676B)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFFE4E6EB), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Details Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0F2F5),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Condition", fontSize = 11.sp, color = Color(0xFF65676B))
                            Text(text = item.condition, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF050505))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0F2F5),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Category", fontSize = 11.sp, color = Color(0xFF65676B))
                            Text(text = item.category, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF050505))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = "Description",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.description.ifBlank { "No detailed description provided by seller." },
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFE4E6EB), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Seller Information Card
                Text(
                    text = "Seller Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = CircleShape,
                            color = Color(0xFFE8F1FD)
                        ) {
                            if (item.sellerAvatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.sellerAvatarUrl,
                                    contentDescription = item.sellerName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = item.sellerName.firstOrNull()?.uppercase() ?: "S",
                                        fontSize = 20.sp,
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
                                    text = item.sellerName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050505)
                                )
                                if (isSellerVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    VerificationBadge(size = 16.dp, show = true)
                                }
                            }
                            Text(
                                text = if (isSellerVerified) "Frndom Verified Marketplace Seller" else "Marketplace Seller",
                                fontSize = 12.sp,
                                color = if (isSellerVerified) Color(0xFF31A24C) else Color(0xFF65676B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Owner Actions vs Buyer Actions
                if (isOwner) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onMarkAsSold(!item.isSold) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (item.isSold) Color(0xFF00A86B) else Color(0xFF65676B)
                            )
                        ) {
                            Text(
                                text = if (item.isSold) "Mark as Available" else "Mark as Sold",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Delete Listing", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Direct Messenger Chat Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F1FD)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1877F2).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = null,
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send seller a message",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0866FF)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = prefilledMessage,
                                onValueChange = { prefilledMessage = it },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF1877F2),
                                    unfocusedBorderColor = Color(0xFFCED0D4)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { onMessageSeller(prefilledMessage) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send Message",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Listing?") },
            text = { Text("Are you sure you want to permanently remove this listing from Marketplace?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteListing()
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// SELL ITEM BOTTOM SHEET / MODAL
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SellListingBottomSheet(
    currentUserId: String,
    currentUserName: String,
    currentUserAvatar: String,
    mediaUploadService: MediaUploadService?,
    onDismiss: () -> Unit,
    onPublish: (MarketplaceItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Electronics") }
    var condition by remember { mutableStateOf("Brand New") }
    var location by remember { mutableStateOf("Dhaka, Bangladesh") }
    var description by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Electronics", "Mobiles", "Vehicles", "Fashion", "Home Goods", "Hobbies", "Property", "Other")
    val conditions = listOf("Brand New", "Used - Like New", "Used - Good", "Used - Fair")

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && mediaUploadService != null) {
            isUploadingPhoto = true
            scope.launch {
                val res = mediaUploadService.uploadImageUri(uri, folder = "marketplace")
                photoUrl = res.getOrDefault("")
                isUploadingPhoto = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create Marketplace Listing",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Photo Upload Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Uploaded Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isUploadingPhoto) {
                        CircularProgressIndicator(color = Color(0xFF1877F2))
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Photo",
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add Product Photos",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1877F2)
                            )
                            Text(
                                text = "Photos help buyers see your item clearly",
                                fontSize = 11.sp,
                                color = Color(0xFF65676B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("Item Title") },
                placeholder = { Text("e.g. iPhone 14 Pro Max 256GB Deep Purple") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1877F2),
                    unfocusedBorderColor = Color(0xFFCED0D4)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Price in ৳ BDT
            OutlinedTextField(
                value = priceText,
                onValueChange = {
                    priceText = it.filter { ch -> ch.isDigit() || ch == '.' }
                    errorMessage = null
                },
                label = { Text("Price in Taka (৳ BDT)") },
                prefix = { Text("৳ ", fontWeight = FontWeight.Bold, color = Color(0xFF0866FF)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1877F2),
                    unfocusedBorderColor = Color(0xFFCED0D4)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Selection
            Text(text = "Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF65676B))
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = category == cat
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF1877F2) else Color(0xFFF0F2F5),
                        modifier = Modifier.clickable { category = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF050505),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Condition Selection
            Text(text = "Condition", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF65676B))
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                conditions.forEach { cond ->
                    val isSelected = condition == cond
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFE8F1FD) else Color(0xFFF0F2F5),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1877F2)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { condition = cond }
                    ) {
                        Text(
                            text = cond,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF1877F2) else Color(0xFF050505),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location / City") },
                placeholder = { Text("e.g. Dhanmondi, Dhaka") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1877F2),
                    unfocusedBorderColor = Color(0xFFCED0D4)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Describe condition, warranty, features, and reason for selling...") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1877F2),
                    unfocusedBorderColor = Color(0xFFCED0D4)
                )
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    fontSize = 12.sp,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    when {
                        title.isBlank() -> errorMessage = "Please enter an item title"
                        price <= 0.0 -> errorMessage = "Please enter a valid price in Taka (৳)"
                        location.isBlank() -> errorMessage = "Please enter a location"
                        else -> {
                            val newItem = MarketplaceItem(
                                id = UUID.randomUUID().toString(),
                                title = title.trim(),
                                price = price,
                                category = category,
                                condition = condition,
                                location = location.trim(),
                                description = description.trim(),
                                coverImageUrl = photoUrl.ifBlank { "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600" },
                                imageUrls = if (photoUrl.isNotBlank()) listOf(photoUrl) else emptyList(),
                                sellerId = currentUserId,
                                sellerName = currentUserName,
                                sellerAvatarUrl = currentUserAvatar,
                                createdAt = System.currentTimeMillis()
                            )
                            onPublish(newItem)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
            ) {
                Text(
                    text = "Publish Listing (৳ BDT)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
