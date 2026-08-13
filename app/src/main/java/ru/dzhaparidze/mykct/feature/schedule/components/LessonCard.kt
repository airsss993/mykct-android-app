package ru.dzhaparidze.mykct.feature.schedule.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.data.Lesson
import java.time.format.DateTimeFormatter

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/** Карточка пары из референса: пилюля со временем, крупный заголовок, тема, чипы. */
@Composable
fun LessonCard(
    lesson: Lesson,
    isPast: Boolean,
    modifier: Modifier = Modifier,
) {
    val container = lesson.colorHex.toColorOrNull() ?: MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (isPast) container.copy(alpha = 0.55f) else container,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            TimePill(
                text = "${lesson.start.format(TIME)} - ${lesson.end.format(TIME)}",
                showCheck = isPast,
                accent = container,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (lesson.topic.isNotBlank()) {
                Text(
                    text = lesson.topic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (lesson.room.isNotBlank()) {
                    Chip(text = lesson.room, icon = Icons.Default.Place)
                }
                lesson.subgroup?.let { Chip(text = it) }
            }
        }
    }
}

@Composable
private fun TimePill(text: String, showCheck: Boolean, accent: Color) {
    Row(
        modifier = Modifier
            .background(Color.White, CircleShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheck) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun Chip(text: String, icon: ImageVector? = null) {
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

/** Портал отдаёт цвет пары строкой; кривой формат не должен ронять экран. */
private fun String?.toColorOrNull(): Color? {
    val hex = this?.trim()?.removePrefix("#")?.takeIf { it.length == 6 } ?: return null
    val value = hex.toLongOrNull(16) ?: return null
    return Color(value or 0xFF000000L)
}
