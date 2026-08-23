package com.example.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.WalletRepository
import com.example.data.repository.WalletTxItem
import java.util.Locale

enum class WalletSubPage {
    OVERVIEW,
    DEPOSIT,
    WITHDRAW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletView(
    userProfile: UserProfile?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val walletRepo = remember { WalletRepository.getInstance(context) }
    val balance by walletRepo.balanceFlow.collectAsState()
    val totalIn by walletRepo.totalInFlow.collectAsState()
    val totalOut by walletRepo.totalOutFlow.collectAsState()
    val transactions by walletRepo.transactionsFlow.collectAsState()

    var currentSubPage by remember { mutableStateOf(WalletSubPage.OVERVIEW) }
    var showHistorySheet by remember { mutableStateOf(false) }

    BackHandler(enabled = currentSubPage != WalletSubPage.OVERVIEW) {
        currentSubPage = WalletSubPage.OVERVIEW
    }

    when (currentSubPage) {
        WalletSubPage.DEPOSIT -> {
            DepositScreen(
                onBack = { currentSubPage = WalletSubPage.OVERVIEW },
                onDepositSuccess = { currentSubPage = WalletSubPage.OVERVIEW },
                modifier = modifier
            )
            return
        }
        WalletSubPage.WITHDRAW -> {
            WithdrawScreen(
                onBack = { currentSubPage = WalletSubPage.OVERVIEW },
                onWithdrawSuccess = { currentSubPage = WalletSubPage.OVERVIEW },
                modifier = modifier
            )
            return
        }
        WalletSubPage.OVERVIEW -> { /* render main wallet view below */ }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .testTag("wallet_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("wallet_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF050505)
                        )
                    }

                    Text(
                        text = "Wallet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )

                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("wallet_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = Color(0xFF050505)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Dark Rich Royal Sapphire Blue Wallet Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = Color(0xFF0B5ED7).copy(alpha = 0.4f)
                            )
                            .testTag("wallet_balance_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF0B5ED7),
                                            Color(0xFF084298)
                                        )
                                    )
                                )
                                .padding(22.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Top row: "Available Balance" + "৳ BDT" Pill
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Available Balance",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = "৳ BDT",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Big Balance: ৳ 0.00
                                Text(
                                    text = "৳ ${String.format(Locale.US, "%.2f", balance)}",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Divider Line
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.25f))
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Bottom Row: Total In & Total Out
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Total In",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "৳ ${String.format(Locale.US, "%.2f", totalIn)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(36.dp))

                                    Column {
                                        Text(
                                            text = "Total Out",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "৳ ${String.format(Locale.US, "%.2f", totalOut)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3 Action Cards (Deposit/Recharge, Withdraw, Transaction History)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Action 1: Deposit / Recharge
                        WalletActionCard(
                            title = "Deposit",
                            icon = Icons.Default.Add,
                            iconColor = Color(0xFF1877F2),
                            iconBgColor = Color(0xFFE8F1FD),
                            modifier = Modifier.weight(1f),
                            testTag = "wallet_action_recharge",
                            onClick = { currentSubPage = WalletSubPage.DEPOSIT }
                        )

                        // Action 2: Withdraw
                        WalletActionCard(
                            title = "Withdraw",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconColor = Color(0xFF00A86B),
                            iconBgColor = Color(0xFFE8F8F0),
                            modifier = Modifier.weight(1f),
                            testTag = "wallet_action_withdraw",
                            onClick = { currentSubPage = WalletSubPage.WITHDRAW }
                        )

                        // Action 3: Transaction History
                        WalletActionCard(
                            title = "Transaction\nHistory",
                            icon = Icons.Default.History,
                            iconColor = Color(0xFFFF9800),
                            iconBgColor = Color(0xFFFFF3E0),
                            modifier = Modifier.weight(1f),
                            testTag = "wallet_action_history",
                            onClick = { showHistorySheet = true }
                        )
                    }
                }

                // Transactions Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transactions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )

                        Text(
                            text = "See All",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1877F2),
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showHistorySheet = true }
                                )
                                .padding(4.dp)
                        )
                    }
                }

                // Transaction Items
                if (transactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(0.5.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No transactions yet",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF65676B)
                                )
                                Text(
                                    text = "Recharge your wallet or earn money from content",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8A8D91),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(transactions.take(8), key = { it.id }) { tx ->
                        WalletTransactionCard(tx = tx)
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }

        // History Bottom Sheet
        if (showHistorySheet) {
            TransactionHistoryBottomSheet(
                transactions = transactions,
                onDismiss = { showHistorySheet = false }
            )
        }
    }
}

@Composable
private fun WalletActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = iconBgColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF050505),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun WalletTransactionCard(
    tx: WalletTxItem,
    modifier: Modifier = Modifier
) {
    val isPending = tx.status == "PENDING"
    val isRejected = tx.status == "REJECTED"

    val statusContainerColor = when {
        isPending -> Color(0xFFFFF8E1)
        isRejected -> Color(0xFFFFEBEE)
        tx.isPositive -> Color(0xFFE8F8F0)
        else -> Color(0xFFFFEBEE)
    }

    val statusIconColor = when {
        isPending -> Color(0xFFF57C00)
        isRejected -> Color(0xFFD32F2F)
        tx.isPositive -> Color(0xFF00A86B)
        else -> Color(0xFFD32F2F)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusContainerColor,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                isPending -> Icons.Default.Schedule
                                isRejected -> Icons.Default.Close
                                tx.isPositive -> Icons.Default.ArrowDownward
                                else -> Icons.Default.ArrowUpward
                            },
                            contentDescription = null,
                            tint = statusIconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tx.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        when {
                            isPending -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFF3E0)
                                ) {
                                    Text(
                                        text = "Pending",
                                        color = Color(0xFFE65100),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            isRejected -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFEBEE)
                                ) {
                                    Text(
                                        text = "Rejected",
                                        color = Color(0xFFD32F2F),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "Approved",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (tx.subtitle.isNotBlank()) {
                        Text(
                            text = tx.subtitle,
                            fontSize = 12.sp,
                            color = Color(0xFF65676B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = tx.date,
                        fontSize = 11.sp,
                        color = Color(0xFF8A8D91)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val amountColor = when {
                    isPending -> Color(0xFFE65100)
                    isRejected -> Color(0xFF9E9E9E)
                    tx.isPositive -> Color(0xFF00A86B)
                    else -> Color(0xFFD32F2F)
                }
                Text(
                    text = "${if (tx.isPositive) "+" else "-"}৳${String.format(Locale.US, "%.2f", tx.amount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = if (isPending) "Processing" else "Bal: ৳${String.format(Locale.US, "%.2f", tx.balanceAfter)}",
                    fontSize = 11.sp,
                    color = if (isPending) Color(0xFFE65100) else Color(0xFF8A8D91)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RechargeBottomSheet(
    onDismiss: () -> Unit,
    onConfirmRecharge: (Double, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedAmount by remember { mutableStateOf("50") }
    var selectedMethod by remember { mutableStateOf("bKash") }
    val presetAmounts = listOf("50", "100", "200", "500", "1000")
    val paymentMethods = listOf("bKash", "Nagad", "Rocket", "EPS", "Bank Transfer")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recharge Wallet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Select Amount (৳ BDT)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF65676B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Preset Amount Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetAmounts.forEach { amt ->
                    val isSelected = selectedAmount == amt
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF1877F2) else Color(0xFFF0F2F5),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedAmount = amt }
                    ) {
                        Text(
                            text = "৳$amt",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF050505),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = selectedAmount,
                onValueChange = { selectedAmount = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Custom Amount (৳)") },
                prefix = { Text("৳ ", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1877F2),
                    unfocusedBorderColor = Color(0xFFCED0D4)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Payment Method",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF65676B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            paymentMethods.forEach { method ->
                val isSelected = selectedMethod == method
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedMethod = method },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFE8F1FD) else Color(0xFFF8F9FA)
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1877F2)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF1877F2) else Color(0xFF65676B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = method,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1877F2) else Color(0xFF050505)
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val amt = selectedAmount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirmRecharge(amt, selectedMethod)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B5ED7))
            ) {
                Text(
                    text = "Proceed to Recharge ৳$selectedAmount",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithdrawBottomSheet(
    currentBalance: Double,
    onDismiss: () -> Unit,
    onConfirmWithdraw: (Double, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("bKash") }
    var accountNo by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val paymentMethods = listOf("bKash", "Nagad", "Rocket", "Bank Account")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Withdraw Funds",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Available Balance: ৳${String.format(Locale.US, "%.2f", currentBalance)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF00A86B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it.filter { char -> char.isDigit() || char == '.' }
                    errorMessage = null
                },
                label = { Text("Withdraw Amount (৳)") },
                prefix = { Text("৳ ", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1877F2),
                    unfocusedBorderColor = Color(0xFFCED0D4)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Withdraw To",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF65676B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentMethods.forEach { method ->
                    val isSelected = selectedMethod == method
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF1877F2) else Color(0xFFF0F2F5),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMethod = method }
                    ) {
                        Text(
                            text = method,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF050505),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = accountNo,
                onValueChange = {
                    accountNo = it
                    errorMessage = null
                },
                label = { Text("$selectedMethod Account / Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
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
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    when {
                        amt <= 0 -> errorMessage = "Please enter a valid amount"
                        amt > currentBalance -> errorMessage = "Insufficient wallet balance"
                        accountNo.isBlank() -> errorMessage = "Please enter account number"
                        else -> onConfirmWithdraw(amt, selectedMethod, accountNo)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))
            ) {
                Text(
                    text = "Confirm Withdrawal",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionHistoryBottomSheet(
    transactions: List<WalletTxItem>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filterIndex by remember { mutableStateOf(0) } // 0: All, 1: Approved, 2: Pending, 3: Cash Out, 4: Rejected

    val filteredList = when (filterIndex) {
        1 -> transactions.filter { it.status == "COMPLETED" && it.isPositive }
        2 -> transactions.filter { it.status == "PENDING" }
        3 -> transactions.filter { !it.isPositive }
        4 -> transactions.filter { it.status == "REJECTED" }
        else -> transactions
    }

    val pendingCount = transactions.count { it.status == "PENDING" }

    val filterTabs = listOf(
        "All (${transactions.size})",
        "Approved",
        if (pendingCount > 0) "Pending ($pendingCount)" else "Pending",
        "Cash Out",
        "Rejected"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050505)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Scrollable / Adaptive Filter Tabs
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filterTabs) { idx, label ->
                    val isSelected = filterIndex == idx
                    val isPendingTab = idx == 2 && pendingCount > 0
                    val tabColor = when {
                        isSelected -> Color(0xFF1877F2)
                        isPendingTab -> Color(0xFFFFF3E0)
                        else -> Color(0xFFF0F2F5)
                    }
                    val textColor = when {
                        isSelected -> Color.White
                        isPendingTab -> Color(0xFFE65100)
                        else -> Color(0xFF050505)
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = tabColor,
                        modifier = Modifier.clickable { filterIndex = idx }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions found in this category",
                        fontSize = 14.sp,
                        color = Color(0xFF65676B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.id }) { tx ->
                        WalletTransactionCard(tx = tx)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
