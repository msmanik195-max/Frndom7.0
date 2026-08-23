package com.example.ui.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.R2StorageConfig
import com.example.data.repository.StorageRepository
import kotlinx.coroutines.launch

@Composable
fun StorageManagementScreen(
    storageRepository: StorageRepository,
    userId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var configs by remember { mutableStateOf(storageRepository.getLocalConfigs()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<R2StorageConfig?>(null) }
    var configToDelete by remember { mutableStateOf<R2StorageConfig?>(null) }

    var testResultDialog by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var testingConfigId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val activeConfig = configs.firstOrNull { it.isActive } ?: configs.firstOrNull()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("storage_management_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("storage_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF050505)
                            )
                        }

                        Text(
                            text = "Storage Management",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                    }

                    IconButton(
                        onClick = {
                            // Fresh blank configuration for adding new R2 account
                            editingConfig = R2StorageConfig(
                                id = "",
                                label = "Cloudflare R2 Storage ${configs.size + 1}",
                                bucketName = "",
                                accountId = "",
                                accessKeyId = "",
                                secretAccessKey = "",
                                publicEndpoint = "",
                                isActive = configs.isEmpty()
                            )
                            showEditDialog = true
                        },
                        modifier = Modifier.testTag("storage_add_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Account",
                            tint = Color(0xFF1877F2),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingConfig = R2StorageConfig(
                        id = "",
                        label = "Cloudflare R2 Storage ${configs.size + 1}",
                        bucketName = "",
                        accountId = "",
                        accessKeyId = "",
                        secretAccessKey = "",
                        publicEndpoint = "",
                        isActive = configs.isEmpty()
                    )
                    showEditDialog = true
                },
                containerColor = Color(0xFF1877F2),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 8.dp)
                    .size(56.dp)
                    .testTag("storage_fab_add")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add R2 Server",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F8FA))
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info Card (Matching Screenshot 5)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF3FE)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1877F2),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = "Cloudflare R2",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Cloudflare R2 Object Storage",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1877F2)
                                )
                                Text(
                                    text = "S3-Compatible Zero-Egress Media Hosting",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5E6D82)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Connect your Cloudflare R2 bucket to store compressed images (100–600 KB) and optimized video posts. You can configure multiple accounts and toggle the Active bucket with the Radio button.",
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Green Compression Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD1F2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = Color(0xFF0F9D58),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Client Compression: Photos (100–600 KB) • Videos Optimized",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0A6838)
                                )
                            }
                        }
                    }
                }
            }

            // Section Header: "Configured R2 Accounts (N)" & Active Badge
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configured R2 Accounts (${configs.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )

                    if (activeConfig != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFD1F2E2),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(
                                    text = "Active:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0A6838)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF0F9D58), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeConfig.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0A6838)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // List of Configured Account Cards (Matching Screenshot 5)
            items(configs, key = { it.id }) { cfg ->
                val isActive = cfg.isActive
                val isTesting = testingConfigId == cfg.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) Color(0xFF1877F2) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("storage_account_card_${cfg.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Title row with Radio button & ACTIVE badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isActive,
                                onClick = {
                                    storageRepository.setActiveConfig(cfg.id, userId)
                                    configs = storageRepository.getLocalConfigs()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("${cfg.label} is now Active!")
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF1877F2),
                                    unselectedColor = Color(0xFF94A3B8)
                                )
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = cfg.label,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )

                                    if (isActive) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF1877F2)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Bucket: ${cfg.bucketName}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Divider(
                            thickness = 0.5.dp,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // Credentials Detail Rows
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Account ID
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Account ID",
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Account ID: ${if (cfg.accountId.length > 14) cfg.accountId.take(14) + "..." else cfg.accountId}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Access Key ID
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Access Key ID",
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Access Key ID: ${if (cfg.accessKeyId.length > 8) cfg.accessKeyId.take(8) + "••••••••" else cfg.accessKeyId}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Public Endpoint
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Public Endpoint",
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Public Endpoint: ${if (cfg.publicEndpoint.length > 24) cfg.publicEndpoint.take(24) + "..." else cfg.publicEndpoint}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Test, Edit, Delete (Matching Screenshot 5)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Test Button with Real Network Check
                                OutlinedButton(
                                    onClick = {
                                        testingConfigId = cfg.id
                                        scope.launch {
                                            val result = storageRepository.testConnection(cfg)
                                            testingConfigId = null
                                            testResultDialog = result
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1877F2)),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    if (isTesting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF1877F2)
                                        )
                                    } else {
                                        Icon(imageVector = Icons.Default.Wifi, contentDescription = "Test", modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = if (isTesting) "Testing..." else "Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Edit Button
                                OutlinedButton(
                                    onClick = {
                                        editingConfig = cfg
                                        showEditDialog = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1877F2)),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Delete Button
                            IconButton(
                                onClick = {
                                    configToDelete = cfg
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFFA383E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add / Edit Config Dialog
    if (showEditDialog && editingConfig != null) {
        val isNew = editingConfig?.id.isNullOrBlank()
        var label by remember { mutableStateOf(editingConfig?.label ?: "Cloudflare R2 Storage") }
        var bucketName by remember { mutableStateOf(editingConfig?.bucketName ?: "") }
        var accountId by remember { mutableStateOf(editingConfig?.accountId ?: "") }
        var accessKeyId by remember { mutableStateOf(editingConfig?.accessKeyId ?: "") }
        var secretAccessKey by remember { mutableStateOf(editingConfig?.secretAccessKey ?: "") }
        var publicEndpoint by remember { mutableStateOf(editingConfig?.publicEndpoint ?: "") }
        var isActive by remember { mutableStateOf(editingConfig?.isActive ?: (configs.isEmpty())) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = if (isNew) "Add R2 Server" else "Edit R2 Server",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Account Label") },
                        placeholder = { Text("e.g. Cloudflare R2 Primary") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bucketName,
                        onValueChange = { bucketName = it },
                        label = { Text("Bucket Name") },
                        placeholder = { Text("e.g. social-image-video") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accountId,
                        onValueChange = { accountId = it },
                        label = { Text("Cloudflare Account ID") },
                        placeholder = { Text("32-character Account ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accessKeyId,
                        onValueChange = { accessKeyId = it },
                        label = { Text("Access Key ID") },
                        placeholder = { Text("R2 Access Key ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = secretAccessKey,
                        onValueChange = { secretAccessKey = it },
                        label = { Text("Secret Access Key") },
                        placeholder = { Text("R2 Secret Access Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = publicEndpoint,
                        onValueChange = { publicEndpoint = it },
                        label = { Text("Public URL Endpoint (CDN/R2)") },
                        placeholder = { Text("https://pub-xxx.r2.dev") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Set as Active Bucket", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1877F2))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = (editingConfig ?: R2StorageConfig()).copy(
                            label = label.ifBlank { "Cloudflare R2 Storage" },
                            bucketName = bucketName.ifBlank { "social-image-video" },
                            accountId = accountId,
                            accessKeyId = accessKeyId,
                            secretAccessKey = secretAccessKey,
                            publicEndpoint = publicEndpoint,
                            isActive = isActive
                        )
                        storageRepository.saveOrUpdateConfig(updated, userId)
                        configs = storageRepository.getLocalConfigs()
                        showEditDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Server configuration saved successfully!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = Color(0xFF65676B))
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (configToDelete != null) {
        val target = configToDelete!!
        AlertDialog(
            onDismissRequest = { configToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Server",
                    tint = Color(0xFFFA383E),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(text = "Remove Server?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to remove '${target.label}'? You can re-add it at any time.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        storageRepository.deleteConfig(target.id, userId)
                        configs = storageRepository.getLocalConfigs()
                        configToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Server removed.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFA383E))
                ) {
                    Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { configToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Test Connection Result Dialog (Matching User Request)
    if (testResultDialog != null) {
        val (isSuccess, message) = testResultDialog!!
        AlertDialog(
            onDismissRequest = { testResultDialog = null },
            icon = {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (isSuccess) "Success" else "Error",
                    tint = if (isSuccess) Color(0xFF0F9D58) else Color(0xFFFA383E),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = if (isSuccess) "Connection Test Passed" else "Connection Test Failed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    if (isSuccess) {
                        Text(
                            text = "Bucket and endpoint are active and reachable.",
                            fontSize = 12.sp,
                            color = Color(0xFF65676B)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { testResultDialog = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSuccess) Color(0xFF0F9D58) else Color(0xFF1877F2)
                    )
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
