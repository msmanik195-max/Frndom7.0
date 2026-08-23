package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.navigation.AppDestination

@Composable
fun FrndomBottomNavigation(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Navigation Surface Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Divider(thickness = 0.5.dp, color = Color(0xFFE4E6EB))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        modifier = Modifier.weight(1f),
                        painter = painterResource(id = R.drawable.ic_nav_home),
                        label = "Home",
                        isSelected = currentDestination == AppDestination.HOME,
                        testTag = "nav_tab_home",
                        onClick = { onNavigate(AppDestination.HOME) }
                    )
                    BottomNavItem(
                        modifier = Modifier.weight(1f),
                        painter = painterResource(id = R.drawable.ic_nav_reels),
                        label = "Reels",
                        isSelected = currentDestination == AppDestination.REELS,
                        testTag = "nav_tab_reels",
                        onClick = { onNavigate(AppDestination.REELS) }
                    )

                    // Center Placeholder Gap for the Elevated Floating Plus Button
                    Spacer(modifier = Modifier.weight(1f))

                    BottomNavItem(
                        modifier = Modifier.weight(1f),
                        painter = painterResource(id = R.drawable.ic_nav_chats),
                        label = "Chats",
                        isSelected = currentDestination == AppDestination.CHATS,
                        testTag = "nav_tab_chats",
                        onClick = { onNavigate(AppDestination.CHATS) }
                    )
                    BottomNavItem(
                        modifier = Modifier.weight(1f),
                        painter = painterResource(id = R.drawable.ic_nav_account),
                        label = "Account",
                        isSelected = currentDestination == AppDestination.ACCOUNT,
                        testTag = "nav_tab_account",
                        onClick = { onNavigate(AppDestination.ACCOUNT) }
                    )
                }
            }
        }

        // Center Floating Plus (+) Button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(46.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                .border(
                    width = 2.5.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onNavigate(AppDestination.CREATE_POST) }
                )
                .testTag("nav_tab_create_post"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Post",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    painter: Painter,
    label: String,
    isSelected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = Color(0xFF65676B)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 2.dp)
            .testTag(testTag)
    ) {
        Icon(
            painter = painter,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}

