package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReactionType
import kotlinx.coroutines.delay

@Composable
fun FacebookReactionsPopup(
    onReactionSelected: (ReactionType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reactions = listOf(
        ReactionType.LIKE,
        ReactionType.LOVE,
        ReactionType.CARE,
        ReactionType.HAHA,
        ReactionType.WOW,
        ReactionType.SAD,
        ReactionType.ANGRY
    )

    Surface(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .border(0.5.dp, Color(0xFFE4E6EB), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            reactions.forEachIndexed { index, reaction ->
                ReactionItem(
                    reaction = reaction,
                    index = index,
                    onClick = {
                        onReactionSelected(reaction)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ReactionItem(
    reaction: ReactionType,
    index: Int,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    var isHovered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 35L)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .scale(if (isHovered) scale.value * 1.35f else scale.value)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = reaction.emoji,
            fontSize = 32.sp
        )
    }
}
