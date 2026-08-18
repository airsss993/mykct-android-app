package ru.dzhaparidze.mykct.feature.schedule.components

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.ui.theme.AccentGradient
import ru.dzhaparidze.mykct.ui.theme.VioletLight
import ru.dzhaparidze.mykct.ui.theme.VioletTint
import java.time.format.DateTimeFormatter

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/** Карточка пары из референса: пилюля со временем, крупный заголовок, тема, чипы. */
@Composable
fun LessonCard(
    lesson: Lesson,
    isPast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNow: Boolean = false,
    remaining: Int? = null,
) {
    // Монохром по референсу: все карточки в фирменном градиенте, предметы различает
    // водяной знак, а не цвет. `colorHex` с портала намеренно игнорируется.
    val accent = MaterialTheme.colorScheme.primary

    Box(modifier = modifier) {
        // Свечение из-под идущей пары: узкая полоса у нижней кромки, а не заливка
        // во всю карточку — так свет читается как отблеск, а не как вторая карточка.
        // Градиент гаснет к краям, иначе после размытия видны торцы полосы.
        if (isNow && CAN_BLUR) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .wrapContentSize(Alignment.BottomCenter)
                    .fillMaxWidth(0.9f)
                    .height(10.dp)
                    // уезжает под карточку: если полоса стоит вровень с кромкой,
                    // свет отрывается от неё и висит отдельной подсветкой
                    .offset(y = (-8).dp)
                    .blur(24.dp, BlurredEdgeTreatment.Unbounded)
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.25f to VioletLight.copy(alpha = 0.45f),
                            0.5f to VioletTint.copy(alpha = 0.65f),
                            0.75f to VioletLight.copy(alpha = 0.45f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // прошедшая пара просто гасится целиком — так же, как строки истории в референсе
            .alpha(if (isPast) 0.55f else 1f)
            // жать не на что, если пара общая для всей группы
            .clickable(enabled = lesson.subgroups.isNotEmpty(), onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = accent,
        // Идущая пара обведена светлой кромкой — её видно, не читая время.
        border = if (isNow) BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)) else null,
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
                painter = painterResource(lesson.backgroundIcon()),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.13f),
                modifier = Modifier
                    .matchParentSize()
                    .wrapContentSize(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(84.dp),
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

                // FlowRow, а не Row: на узком экране «Идёт · осталось N мин» рядом с
                // «Подгруппы: N» не влезает в строку и второй чип обрезается.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (remaining != null) {
                        Chip(text = "Идёт · осталось $remaining мин", icon = R.drawable.ic_clock)
                    }
                    if (lesson.room.isNotBlank()) {
                        Chip(text = lesson.room, icon = R.drawable.ic_place)
                    }
                    if (lesson.subgroups.isNotEmpty()) {
                        Chip(text = "Подгруппы: ${lesson.subgroups.size}", icon = R.drawable.ic_list)
                    }
                }
            }
        }
    }
    }
}

/** Размытие свечения требует Android 12; ниже идущая пара опознаётся только кромкой. */
private val CAN_BLUR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Водяной знак по названию предмета. Названия приходят с портала свободным текстом,
 * так что это подбор по ключевому слову с запасным вариантом, а не справочник.
 */
@DrawableRes
private fun Lesson.backgroundIcon(): Int {
    val name = title.lowercase()
    return when {
        "физич" in name || "физкультур" in name -> R.drawable.ic_fitness
        "английск" in name || "язык" in name -> R.drawable.ic_translate
        "баз" in name && "данных" in name -> R.drawable.ic_list
        "операционн" in name || "сет" in name -> R.drawable.ic_memory
        "разработ" in name || "модул" in name || "программ" in name -> R.drawable.ic_code
        else -> R.drawable.ic_school
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
                    painter = painterResource(R.drawable.ic_check),
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
private fun Chip(text: String, @DrawableRes icon: Int? = null) {
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
                    painter = painterResource(it),
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
