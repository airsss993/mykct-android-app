package ru.dzhaparidze.collegeapp.features.shared.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*

@Composable
fun CustomNavBar(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50.dp),
            border = BorderStroke(1.6.dp, MaterialTheme.colorScheme.scrim),
            shadowElevation = 5.dp,
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NavBarItem(
                    isSelected = currentScreen == Screen.SCHEDULE,
                    icon = Icons.Outlined.DateRange,
                    label = "Расписание",
                    onClick = { onScreenChange(Screen.SCHEDULE) })

                NavBarItem(
                    isSelected = currentScreen == Screen.SETTINGS,
                    icon = Icons.Outlined.Settings,
                    label = "Настройки",
                    onClick = { onScreenChange(Screen.SETTINGS) })
            }
        }
    }
}

@Composable
private fun NavBarItem(
    isSelected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f),
        animationSpec = tween(
            durationMillis = 250
        ),
    )

    val iconColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val iconOffsetY by animateDpAsState(
        targetValue = if (isSelected) (-8).dp else 0.dp,
        animationSpec = tween(durationMillis = 250)
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 250)
    )

    Box(
        modifier = Modifier
            .width(100.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .size(21.dp)
                .offset(y = iconOffsetY),
            imageVector = icon,
            tint = iconColor,
            contentDescription = null
        )

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha),
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )
    }
}