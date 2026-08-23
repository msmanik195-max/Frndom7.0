package com.example.ui.menu.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.ui.components.VerificationBadge
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class UserFilterTab(val title: String) {
    ALL("All Users"),
    ACTIVE("Active / Online"),
    BLOCKED("Blocked"),
    VERIFIED("Verified Badge"),
    MONETIZED("Monetized")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    val usersList by userRepository.getAllUsersFlow().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(UserFilterTab.ALL) }

    // Dialog States
    var userToEdit by remember { mutableStateOf<UserProfile?>(null) }
    var userToDelete by remember { mutableStateOf<UserProfile?>(null) }
    var userToManageVerification by remember { mutableStateOf<UserProfile?>(null) }
    var isProcessingAction by remember { mutableStateOf(false) }

    // Filter Logic
    val filteredUsers = remember(usersList, searchQuery, selectedTab) {
        val query = searchQuery.trim().lowercase()
        usersList.filter { user ->
            // Search filter by Name, Phone, Email, UID
            val matchesSearch = query.isBlank() ||
                    user.fullName.lowercase().contains(query) ||
                    user.firstName.lowercase().contains(query) ||
                    user.lastName.lowercase().contains(query) ||
                    user.email.lowercase().contains(query) ||
                    user.phoneNumber.replace(" ", "").replace("-", "").contains(query) ||
                    user.uid.lowercase().contains(query)

            if (!matchesSearch) return@filter false

            when (selectedTab) {
                UserFilterTab.ALL -> true
                UserFilterTab.ACTIVE -> user.isUserOnline()
                UserFilterTab.BLOCKED -> user.isBlocked
                UserFilterTab.VERIFIED -> user.isVerificationActive()
                UserFilterTab.MONETIZED -> user.isMonetized
            }
        }.sortedWith(
            compareByDescending<UserProfile> { it.isUserOnline() }
                .thenByDescending { it.lastActiveAt }
                .thenByDescending { it.createdAt }
                .thenBy { it.uid.ifBlank { it.email } }
        )
    }

    // Counts for chips
    val allCount = usersList.size
    val activeCount = usersList.count { it.isUserOnline() }
    val blockedCount = usersList.count { it.isBlocked }
    val verifiedCount = usersList.count { it.isVerificationActive() }
    val monetizedCount = usersList.count { it.isMonetized }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "User Management",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color(0xFF050505)
                        )
                        Text(
                            text = "Total ${usersList.size} registered users",
                            fontSize = 12.sp,
                            color = Color(0xFF65676B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_user_mgmt_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF050505)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF0F2F5),
        modifier = modifier.testTag("admin_user_management_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar & Filter Chips Header
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    // Search Text Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_search_input"),
                        placeholder = { Text("Search by name, email, or phone number...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF1877F2)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF65676B)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF0F2F5),
                            unfocusedContainerColor = Color(0xFFF0F2F5),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF1877F2)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(UserFilterTab.entries) { tab ->
                            val count = when (tab) {
                                UserFilterTab.ALL -> allCount
                                UserFilterTab.ACTIVE -> activeCount
                                UserFilterTab.BLOCKED -> blockedCount
                                UserFilterTab.VERIFIED -> verifiedCount
                                UserFilterTab.MONETIZED -> monetizedCount
                            }
                            val isSelected = selectedTab == tab

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tab.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color(0xFF1877F2) else Color(0xFFE4E6EB),
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$count",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else Color(0xFF050505)
                                                )
                                            }
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE7F3FF),
                                    selectedLabelColor = Color(0xFF1877F2),
                                    containerColor = Color(0xFFF0F2F5),
                                    labelColor = Color(0xFF050505)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = null,
                                modifier = Modifier.testTag("user_filter_chip_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // User List / Table
            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE4E6EB),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF65676B),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No users match '$searchQuery'" else "No users in this category",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                        Text(
                            text = "Try clearing filters or search term.",
                            fontSize = 13.sp,
                            color = Color(0xFF65676B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredUsers,
                        key = { user ->
                            val pk = user.uid.ifBlank { user.email }.ifBlank { user.phoneNumber }
                            if (pk.isNotBlank()) pk else user.hashCode().toString()
                        }
                    ) { user ->
                        AdminUserCard(
                            user = user,
                            onToggleBlock = { isBlocked ->
                                coroutineScope.launch {
                                    userRepository.setUserBlocked(user.uid, isBlocked) { success ->
                                        Toast.makeText(
                                            context,
                                            if (isBlocked) "User ${user.fullName} Blocked" else "User ${user.fullName} Unblocked",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onToggleMonetization = { isMonetized ->
                                coroutineScope.launch {
                                    userRepository.setUserMonetized(user.uid, isMonetized) { success ->
                                        Toast.makeText(
                                            context,
                                            if (isMonetized) "Monetization enabled for ${user.fullName}" else "Monetization disabled",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onManageVerification = {
                                userToManageVerification = user
                            },
                            onEditUser = {
                                userToEdit = user
                            },
                            onDeleteUser = {
                                userToDelete = user
                            }
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // 1. EDIT USER DIALOG
    // ==========================================
    if (userToEdit != null) {
        val target = userToEdit!!
        var editName by remember { mutableStateOf(target.fullName) }
        var editEmail by remember { mutableStateOf(target.email) }
        var editPhone by remember { mutableStateOf(target.phoneNumber) }
        var editBio by remember { mutableStateOf(target.bio) }
        var editBalance by remember { mutableStateOf(target.walletBalance.toString()) }

        AlertDialog(
            onDismissRequest = { if (!isProcessingAction) userToEdit = null },
            title = {
                Text(
                    text = "Edit User: ${target.fullName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editBalance,
                        onValueChange = { editBalance = it },
                        label = { Text("Wallet Balance (৳ BDT)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingAction = true
                        val balanceVal = editBalance.toDoubleOrNull() ?: 0.0
                        userRepository.editUserDetails(
                            uid = target.uid,
                            fullName = editName,
                            email = editEmail,
                            phone = editPhone,
                            bio = editBio,
                            balance = balanceVal
                        ) {
                            isProcessingAction = false
                            userToEdit = null
                            Toast.makeText(context, "User details updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // 2. VERIFICATION MANAGEMENT DIALOG
    // ==========================================
    if (userToManageVerification != null) {
        val target = userToManageVerification!!
        val isVerified = target.isVerificationActive()
        val remainingDays = target.getRemainingDays()

        AlertDialog(
            onDismissRequest = { if (!isProcessingAction) userToManageVerification = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verification Badge Control",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "User: ${target.fullName}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color(0xFF050505)
                    )
                    Text(
                        text = "UID: ${target.uid.take(12)}... | ${target.email.ifBlank { target.phoneNumber }}",
                        fontSize = 12.sp,
                        color = Color(0xFF65676B)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = if (isVerified) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isVerified) "Status: Active Green Badge" else "Status: Inactive / Not Verified",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isVerified) Color(0xFF008937) else Color(0xFFE65100)
                                )
                                if (isVerified) {
                                    Text(
                                        text = "$remainingDays days remaining (${formatTimestamp(target.verifiedUntil)})",
                                        fontSize = 11.sp,
                                        color = Color(0xFF008937)
                                    )
                                }
                            }
                            Switch(
                                checked = isVerified,
                                onCheckedChange = { activate ->
                                    isProcessingAction = true
                                    userRepository.setUserVerification(
                                        uid = target.uid,
                                        isVerified = activate,
                                        verifiedUntil = if (activate) System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000) else 0L,
                                        planTitle = if (activate) "Admin 1-Year Badge" else ""
                                    ) {
                                        isProcessingAction = false
                                        userToManageVerification = null
                                        Toast.makeText(
                                            context,
                                            if (activate) "Green Badge Activated for ${target.fullName}" else "Badge Deactivated",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00C853)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Extend Validity Duration:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF050505)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isProcessingAction = true
                                userRepository.extendUserVerification(target.uid, 30) {
                                    isProcessingAction = false
                                    userToManageVerification = null
                                    Toast.makeText(context, "+30 Days Added to Badge!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+30 D", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                isProcessingAction = true
                                userRepository.extendUserVerification(target.uid, 90) {
                                    isProcessingAction = false
                                    userToManageVerification = null
                                    Toast.makeText(context, "+90 Days Added to Badge!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+90 D", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                isProcessingAction = true
                                userRepository.extendUserVerification(target.uid, 365) {
                                    isProcessingAction = false
                                    userToManageVerification = null
                                    Toast.makeText(context, "+1 Year Added to Badge!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("+1 Year", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { userToManageVerification = null }) {
                    Text("Close")
                }
            }
        )
    }

    // ==========================================
    // 3. DELETE USER PERMANENTLY DIALOG
    // ==========================================
    if (userToDelete != null) {
        val target = userToDelete!!
        AlertDialog(
            onDismissRequest = { if (!isProcessingAction) userToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Delete User Account?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to permanently delete '${target.fullName}' (${target.email.ifBlank { target.phoneNumber }})?",
                        fontSize = 14.sp,
                        color = Color(0xFF050505)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Warning: This action is permanent. The user profile, verifications, and associated account data will be completely removed from Firebase and local database.",
                        fontSize = 12.sp,
                        color = Color(0xFFE53935)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingAction = true
                        userRepository.deleteUserCompletely(target.uid) { success ->
                            isProcessingAction = false
                            userToDelete = null
                            Toast.makeText(
                                context,
                                if (success) "User ${target.fullName} deleted permanently" else "Error deleting user",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    if (isProcessingAction) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Delete Permanently")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminUserCard(
    user: UserProfile,
    onToggleBlock: (Boolean) -> Unit,
    onToggleMonetization: (Boolean) -> Unit,
    onManageVerification: () -> Unit,
    onEditUser: () -> Unit,
    onDeleteUser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline = user.isUserOnline()
    val isVerified = user.isVerificationActive()
    val isBlocked = user.isBlocked
    val isMonetized = user.isMonetized

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_user_card_${user.uid}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked) Color(0xFFFFF8F8) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar, Name, Badges, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with online green ring/dot
                Box {
                    if (user.profilePictureUrl.isNotBlank()) {
                        AsyncImage(
                            model = user.profilePictureUrl,
                            contentDescription = user.fullName,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = Color(0xFF1877F2).copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.fullName.firstOrNull()?.uppercase() ?: "U",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1877F2)
                                )
                            }
                        }
                    }

                    // Online indicator badge dot
                    if (isOnline) {
                        Surface(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .border(2.dp, Color.White, CircleShape),
                            shape = CircleShape,
                            color = Color(0xFF00C853)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name, Handles, Contact
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.fullName.ifBlank { "User ${user.uid.take(6)}" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerificationBadge(size = 15.dp, show = true)
                        }
                    }

                    // Email or Phone
                    val contactText = when {
                        user.email.isNotBlank() && user.phoneNumber.isNotBlank() -> "${user.email} • ${user.phoneNumber}"
                        user.email.isNotBlank() -> user.email
                        user.phoneNumber.isNotBlank() -> user.phoneNumber
                        else -> "UID: ${user.uid.take(14)}..."
                    }
                    Text(
                        text = contactText,
                        fontSize = 12.sp,
                        color = Color(0xFF65676B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Account Status Pill (Active / Blocked)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isBlocked) Color(0xFFFFEBEE) else if (isOnline) Color(0xFFE8F5E9) else Color(0xFFF0F2F5)
                ) {
                    Text(
                        text = if (isBlocked) "Blocked" else if (isOnline) "Online" else "Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBlocked) Color(0xFFD32F2F) else if (isOnline) Color(0xFF008937) else Color(0xFF65676B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(thickness = 0.5.dp, color = Color(0xFFF0F2F5))
            Spacer(modifier = Modifier.height(8.dp))

            // Badges & Features Status Row (Verification, Monetization, Wallet Balance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Verification badge status & click to manage
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isVerified) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                    modifier = Modifier
                        .clickable(onClick = onManageVerification)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Badge",
                            tint = if (isVerified) Color(0xFF00C853) else Color(0xFF9E9E9E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isVerified) "${user.getRemainingDays()}d Left" else "No Badge",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVerified) Color(0xFF008937) else Color(0xFF757575)
                        )
                    }
                }

                // 2. Monetization status switch
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isMonetized) Color(0xFFFFF8E1) else Color(0xFFF5F5F5)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Monetization",
                            tint = if (isMonetized) Color(0xFFFFA000) else Color(0xFF9E9E9E),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Monetized",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMonetized) Color(0xFFE65100) else Color(0xFF757575)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = isMonetized,
                            onCheckedChange = onToggleMonetization,
                            modifier = Modifier.size(24.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFFA000)
                            )
                        )
                    }
                }

                // 3. Wallet Balance
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE3F2FD)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "৳ ${String.format(Locale.US, "%.0f", user.walletBalance)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Block/Unblock, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Block/Unblock Button
                OutlinedButton(
                    onClick = { onToggleBlock(!isBlocked) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isBlocked) Color(0xFF008937) else Color(0xFFD32F2F)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBlocked) "Unblock" else "Block",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Edit Button
                OutlinedButton(
                    onClick = onEditUser,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1877F2)
                    ),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Edit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Delete Button
                OutlinedButton(
                    onClick = onDeleteUser,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE53935)
                    ),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Delete",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timeMs: Long): String {
    if (timeMs <= 0L) return "Lifetime"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timeMs))
}
