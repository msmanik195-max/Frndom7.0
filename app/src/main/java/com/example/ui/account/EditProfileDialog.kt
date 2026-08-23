package com.example.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserProfile

@Composable
fun EditProfileDialog(
    userProfile: UserProfile?,
    onSave: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var fullName by remember { mutableStateOf(userProfile?.fullName ?: "") }
    var bio by remember { mutableStateOf(userProfile?.bio ?: "") }
    var work by remember { mutableStateOf(userProfile?.work ?: "") }
    var education by remember { mutableStateOf(userProfile?.education ?: "") }
    var currentCity by remember { mutableStateOf(userProfile?.currentCity ?: "") }
    var hometown by remember { mutableStateOf(userProfile?.hometown ?: "") }
    var relationshipStatus by remember { mutableStateOf(userProfile?.relationshipStatus ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF050505)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF050505))
                    }
                }

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Bio
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio (Describe yourself)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )

                // Work
                OutlinedTextField(
                    value = work,
                    onValueChange = { work = it },
                    label = { Text("Work / Workplace") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Education
                OutlinedTextField(
                    value = education,
                    onValueChange = { education = it },
                    label = { Text("Education / School / University") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Current City
                OutlinedTextField(
                    value = currentCity,
                    onValueChange = { currentCity = it },
                    label = { Text("Current City") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Hometown
                OutlinedTextField(
                    value = hometown,
                    onValueChange = { hometown = it },
                    label = { Text("Hometown") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Relationship Status
                OutlinedTextField(
                    value = relationshipStatus,
                    onValueChange = { relationshipStatus = it },
                    label = { Text("Relationship Status (e.g. Single, In a relationship)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Save Button
                Button(
                    onClick = {
                        val updated = (userProfile ?: UserProfile()).copy(
                            fullName = fullName.trim(),
                            bio = bio.trim(),
                            work = work.trim(),
                            education = education.trim(),
                            currentCity = currentCity.trim(),
                            hometown = hometown.trim(),
                            relationshipStatus = relationshipStatus.trim()
                        )
                        onSave(updated)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0866FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(text = "Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
