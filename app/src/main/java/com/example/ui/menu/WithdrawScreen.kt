package com.example.ui.menu

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethodItem
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun WithdrawScreen(
    onBack: () -> Unit,
    onWithdrawSuccess: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val walletRepo = remember { WalletRepository.getInstance(context) }
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    val userRepo = remember { UserRepository(context) }

    val balance by walletRepo.balanceFlow.collectAsState()
    val paymentMethods by adminRepo.paymentMethodsFlow.collectAsState()
    val activeMethods = remember(paymentMethods) {
        val active = paymentMethods.filter { it.isActive }
        if (active.isEmpty()) paymentMethods else active
    }

    val scope = rememberCoroutineScope()

    var withdrawAmount by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var selectedMethodId by remember { mutableStateOf("") }
    val selectedMethodItem = remember(activeMethods, selectedMethodId) {
        activeMethods.firstOrNull { it.id == selectedMethodId }
            ?: activeMethods.firstOrNull()
            ?: PaymentMethodItem()
    }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessCard by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastWithdrawnAmount by remember { mutableStateOf(0.0) }

    val presetAmounts = listOf("100", "300", "500", "1000", "2000", "5000")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("withdraw_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("withdraw_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF050505)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Withdraw Funds",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Balance Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF6C5CE7), Color(0xFF4834D4))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Available Wallet Balance",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "৳ ${String.format(Locale.US, "%.2f", balance)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSuccessCard) {
                    // Success View
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F0FF)),
                        border = BorderStroke(1.5.dp, Color(0xFF6C5CE7))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF6C5CE7),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Withdrawal Request Submitted!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4834D4)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "৳ ${String.format(Locale.US, "%.2f", lastWithdrawnAmount)} request via ${selectedMethodItem.name} ($accountNumber) is under admin review.",
                                fontSize = 13.sp,
                                color = Color(0xFF050505),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Funds are reserved from your wallet and will be transferred to your account shortly upon approval. (If rejected, full refund is guaranteed).",
                                fontSize = 12.sp,
                                color = Color(0xFF65676B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    showSuccessCard = false
                                    onWithdrawSuccess?.invoke() ?: onBack()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Amount Selection Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "1. Select Withdrawal Amount",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Grid of presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presetAmounts.take(3).forEach { amt ->
                                    val isSelected = withdrawAmount == amt
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFF6C5CE7) else Color(0xFFF0F2F5),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { withdrawAmount = amt }
                                    ) {
                                        Text(
                                            text = "৳$amt",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF050505),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presetAmounts.drop(3).forEach { amt ->
                                    val isSelected = withdrawAmount == amt
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFF6C5CE7) else Color(0xFFF0F2F5),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { withdrawAmount = amt }
                                    ) {
                                        Text(
                                            text = "৳$amt",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF050505),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = withdrawAmount,
                                onValueChange = {
                                    withdrawAmount = it.filter { c -> c.isDigit() || c == '.' }
                                    errorMessage = null
                                },
                                label = { Text("Custom Amount") },
                                prefix = { Text("৳ ", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6C5CE7),
                                    unfocusedBorderColor = Color(0xFFCED0D4)
                                )
                            )
                        }
                    }

                    // Payment Method Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "2. Select Receiving Payment Method",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050505)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            activeMethods.forEach { method ->
                                val isSelected = selectedMethodItem.id == method.id
                                val methodColor = try {
                                    Color(android.graphics.Color.parseColor(method.colorHex))
                                } catch (_: Exception) {
                                    Color(0xFF6C5CE7)
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { selectedMethodId = method.id },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFF3F0FF) else Color(0xFFF8F9FA)
                                    ),
                                    border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF6C5CE7)) else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = methodColor.copy(alpha = 0.15f),
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Payment,
                                                        contentDescription = null,
                                                        tint = methodColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = method.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color(0xFF6C5CE7) else Color(0xFF050505)
                                                )
                                                Text(
                                                    text = method.accountType,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF65676B)
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF6C5CE7),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = {
                                    accountNumber = it
                                    errorMessage = null
                                },
                                label = { Text("${selectedMethodItem.name} Account / Mobile Number") },
                                placeholder = { Text("01XXXXXXXXX") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6C5CE7),
                                    unfocusedBorderColor = Color(0xFFCED0D4)
                                )
                            )
                        }
                    }

                    // Error Message if any
                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            border = BorderStroke(1.dp, Color(0xFFD32F2F))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFD32F2F),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Withdraw Submit Button
                    val amt = withdrawAmount.toDoubleOrNull() ?: 0.0
                    Button(
                        onClick = {
                            if (amt <= 0) {
                                errorMessage = "Please enter a valid amount."
                                return@Button
                            }
                            if (amt > balance) {
                                errorMessage = "Insufficient balance! Your current balance is ৳${String.format(Locale.US, "%.2f", balance)}"
                                return@Button
                            }
                            if (accountNumber.trim().length < 5) {
                                errorMessage = "Please enter a valid account / mobile number."
                                return@Button
                            }

                            if (!isProcessing) {
                                isProcessing = true
                                scope.launch {
                                    delay(600)
                                    val currentUser = userRepo.getCurrentUser()
                                    val userId = currentUser?.uid ?: "user_me"
                                    val userName = if (!currentUser?.fullName.isNullOrBlank()) {
                                        currentUser?.fullName ?: "Frndom User"
                                    } else if (!currentUser?.firstName.isNullOrBlank()) {
                                        "${currentUser.firstName} ${currentUser.lastName}".trim()
                                    } else {
                                        "Frndom User"
                                    }
                                    val userEmail = currentUser?.email ?: ""

                                    // Deduct from wallet and submit to admin
                                    val deducted = walletRepo.withdraw(amt, selectedMethodItem.name, accountNumber.trim())
                                    if (deducted) {
                                        adminRepo.submitWithdrawRequest(
                                            userId = userId,
                                            userName = userName,
                                            userEmail = userEmail,
                                            amount = amt,
                                            methodName = selectedMethodItem.name,
                                            accountNumber = accountNumber.trim()
                                        )
                                        lastWithdrawnAmount = amt
                                        isProcessing = false
                                        showSuccessCard = true
                                    } else {
                                        isProcessing = false
                                        errorMessage = "Failed to process withdrawal from wallet."
                                    }
                                }
                            }
                        },
                        enabled = amt > 0 && accountNumber.isNotBlank() && !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_withdrawal_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Submitting Withdrawal...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                text = "Confirm Withdrawal ৳$withdrawAmount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
