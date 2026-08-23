package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun VerificationBadge(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    show: Boolean = true
) {
    if (!show) return
    Image(
        painter = painterResource(id = R.drawable.ic_verified_badge_green),
        contentDescription = "Verified Badge",
        modifier = modifier
            .size(size)
            .padding(start = 2.dp)
            .testTag("verified_badge_green")
    )
}
