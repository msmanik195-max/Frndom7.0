package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun FrndomTopHeader(
    onSearchClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("frndom_top_header"),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Frndom Logo + Brand Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("brand_header_left")
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_frndom_logo),
                        contentDescription = "Frndom Logo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Frndom",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
            }

            // Right Action Icons with exact user PNG/vector icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("header_action_buttons")
            ) {
                HeaderIconButton(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_header_search),
                    contentDescription = "Search",
                    testTag = "header_search_button",
                    onClick = onSearchClick
                )

                HeaderIconButton(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_header_friends),
                    contentDescription = "Friends",
                    testTag = "header_friends_button",
                    onClick = onFriendsClick
                )

                HeaderIconButton(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_header_notifications),
                    contentDescription = "Notifications",
                    testTag = "header_notifications_button",
                    onClick = onNotificationsClick
                )
                
                HeaderIconButton(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_header_menu),
                    contentDescription = "Menu",
                    testTag = "header_menu_button",
                    onClick = onMenuClick
                )
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    painter: Painter,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFF0F2F5),
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = Color(0xFF1C1E21),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
