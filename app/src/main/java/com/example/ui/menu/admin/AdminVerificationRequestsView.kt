package com.example.ui.menu.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.VerificationRequestItem
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationRequestsView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    val userRepo = remember { UserRepository(context) }
    val walletRepo = remember { WalletRepository.getInstance(context) }

    val verificationRequests by adminRepo.verificationRequestsFlow.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var selectedItemForAction by remember { mutableStateOf<VerificationRequestItem?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "APPROVE" or "REJECT"
    var rejectionReason by remember { mutableStateOf("") }

    val filteredList = remember(verificationRequests, selectedFilter, searchQuery) {
        verificationRequests.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> item.status == "PENDING"
                "APPROVED" -> item.status == "APPROVED"
                "REJECTED" -> item.status == "REJECTED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    item.userName.contains(searchQuery, ignoreCase = true) ||
                    item.userEmail.contains(searchQuery, ignoreCase = true) ||
                    item.userPhone.contains(searchQuery, ignoreCase = true) ||
                    item.planTitle.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val pendingCount = verificationRequests.count { it.status == "PENDING" }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("admin_verification_requests_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("verification_requests_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Verification Requests",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505)
                            )
                            if (pendingCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF00C853)
                                ) {
                                    Text(
                                        text = "$pendingCount Pending",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Grant Green Verification Badges & process refunds",
                            fontSize = 12.sp,
                            color = Color(0xFF65676B)
                        )
                    }
                }
            }

            // Search and Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by User, Email, Phone or Plan", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF65676B))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All (${verificationRequests.size})", "PENDING" to "Pending ($pendingCount)", "APPROVED" to "Approved", "REJECTED" to "Rejected").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00A86B),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_verified_badge_green),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No verification requests",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                        Text(
                            text = "When users subscribe to Green Verification Badges, requests appear here.",
                            fontSize = 13.sp,
                            color = Color(0xFF65676B),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        VerificationRequestCard(
                            item = item,
                            onApprove = {
                                selectedItemForAction = item
                                actionType = "APPROVE"
                            },
                            onReject = {
                                selectedItemForAction = item
                                actionType = "REJECT"
                                rejectionReason = ""
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Action confirmation dialogs
        if (selectedItemForAction != null && actionType == "APPROVE") {
            val item = selectedItemForAction!!
            AlertDialog(
                onDismissRequest = {
                    selectedItemForAction = null
                    actionType = null
                },
                icon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_verified_badge_green),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text("Approve Green Badge for ${item.userName}?", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            text = "Confirm approval for ${item.planTitle} (${item.durationDays} days). The Green Verification Badge will be activated immediately on their account.",
                            fontSize = 14.sp,
                            color = Color(0xFF050505)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            adminRepo.approveVerificationRequest(item.id, userRepo)
                            selectedItemForAction = null
                            actionType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))
                    ) {
                        Text("Grant Green Badge", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedItemForAction = null
                        actionType = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (selectedItemForAction != null && actionType == "REJECT") {
            val item = selectedItemForAction!!
            AlertDialog(
                onDismissRequest = {
                    selectedItemForAction = null
                    actionType = null
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text("Reject & Refund ৳${item.price.toInt()}?", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            text = "Rejecting this verification request will refund ৳${item.price.toInt()} back to ${item.userName}'s wallet balance. No badge will be granted.",
                            fontSize = 14.sp,
                            color = Color(0xFF050505)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = rejectionReason,
                            onValueChange = { rejectionReason = it },
                            label = { Text("Reason (Optional)") },
                            placeholder = { Text("e.g. Account name mismatch with official ID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            adminRepo.rejectVerificationRequest(item.id, walletRepo, userRepo, rejectionReason)
                            selectedItemForAction = null
                            actionType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Reject & Refund Money", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedItemForAction = null
                        actionType = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun VerificationRequestCard(
    item: VerificationRequestItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val statusBgColor = when (item.status) {
        "APPROVED" -> Color(0xFFE8F8F0)
        "REJECTED" -> Color(0xFFFFEBEE)
        else -> Color(0xFFFFF8E1)
    }
    val statusTextColor = when (item.status) {
        "APPROVED" -> Color(0xFF00A86B)
        "REJECTED" -> Color(0xFFD32F2F)
        else -> Color(0xFFE65100)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("verification_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: User name & Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.userName.ifBlank { "User ${item.userId.take(6)}" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Image(
                            painter = painterResource(id = R.drawable.ic_verified_badge_green),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (item.userEmail.isNotBlank()) {
                        Text(
                            text = item.userEmail,
                            fontSize = 11.sp,
                            color = Color(0xFF65676B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBgColor
                ) {
                    Text(
                        text = item.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(thickness = 0.5.dp, color = Color(0xFFF0F2F5))
            Spacer(modifier = Modifier.height(10.dp))

            // Package Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Selected Package",
                        fontSize = 11.sp,
                        color = Color(0xFF65676B)
                    )
                    Text(
                        text = item.planTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF008937)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Package Fee",
                        fontSize = 11.sp,
                        color = Color(0xFF65676B)
                    )
                    Text(
                        text = "৳ ${item.price.toInt()} BDT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF050505)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF7F8FA)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Duration:", fontSize = 11.sp, color = Color(0xFF65676B))
                        Text(text = "${item.durationDays} Days", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF050505))
                    }

                    if (item.userPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Phone:", fontSize = 11.sp, color = Color(0xFF65676B))
                            Text(text = item.userPhone, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF050505))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Requested At:", fontSize = 11.sp, color = Color(0xFF65676B))
                        Text(
                            text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.createdAt)),
                            fontSize = 11.sp,
                            color = Color(0xFF65676B)
                        )
                    }

                    if (item.adminNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Admin Note: ${item.adminNote}",
                            fontSize = 11.sp,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Action Buttons for PENDING
            if (item.status == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("verification_reject_${item.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject & Refund", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("verification_approve_${item.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Grant Badge", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
