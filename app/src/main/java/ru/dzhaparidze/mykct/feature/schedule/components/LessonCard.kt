package ru.dzhaparidze.mykct.feature.schedule.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.ui.theme.AccentGradient
import java.time.format.DateTimeFormatter

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/** Карточка пары из референса: пилюля со временем, крупный заголовок, тема, чипы. */
@Composable
fun LessonCard(
    lesson: Lesson,
    isPast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Монохром по референсу: все карточки в фирменном градиенте, предметы различает
    // водяной знак, а не цвет. `colorHex` с портала намеренно игнорируется.
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            // прошедшая пара просто гасится целиком — так же, как строки истории в референсе
            .alpha(if (isPast) 0.55f else 1f)
            // жать не на что, если пара общая для всей группы
            .clickable(enabled = lesson.subgroups.isNotEmpty(), onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = accent,
        shadowElevation = 6.dp,
    ) {
        // matchParentSize, а не fillMaxSize: фон и водяной знак не должны участвовать
        // в измерении карточки, иначе она растянется на всю высоту таймлайна.
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(AccentGradient),
            )

            Icon(
                imageVector = lesson.backgroundIcon(),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.13f),
                modifier = Modifier
                    .matchParentSize()
                    .wrapContentSize(Alignment.BottomEnd)
                    .offset(x = 16.dp, y = 16.dp)
                    .size(92.dp),
            )

            Column(modifier = Modifier.padding(11.dp)) {
                TimePill(
                    text = "${lesson.start.format(TIME)} - ${lesson.end.format(TIME)}",
                    showCheck = isPast,
                    accent = accent,
                )

                Spacer(Modifier.height(8.dp))

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

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lesson.room.isNotBlank()) {
                        Chip(text = lesson.room, icon = Icons.Default.Place)
                    }
                    if (lesson.subgroups.isNotEmpty()) {
                        Chip(text = "Подгруппы: ${lesson.subgroups.size}", icon = Icons.Default.List)
                    }
                }
            }
        }
    }
}

/**
 * Водяной знак по названию предмета. Названия приходят с портала свободным текстом,
 * так что это подбор по ключевому слову с запасным вариантом, а не справочник.
 */
private fun Lesson.backgroundIcon(): ImageVector {
    val name = title.lowercase()
    return when {
        "физич" in name || "физкультур" in name -> Icons.Default.FavoriteBorder
        "английск" in name || "язык" in name -> Icons.Default.Face
        "баз" in name && "данных" in name -> Icons.Default.List
        "операционн" in name || "сет" in name -> Icons.Default.Settings
        "разработ" in name || "модул" in name || "программ" in name -> Icons.Default.Build
        else -> Icons.Default.Create
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
            // в референсе галочка — белая на цветном круге внутри белой пилюли
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
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
