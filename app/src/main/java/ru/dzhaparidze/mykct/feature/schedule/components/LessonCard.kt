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

    // propagateMinConstraints: минимальную высоту карточке задаёт таймлайн (она
    // пропорциональна длительности пары), а без этого она доходила только до внешнего
    // Box — Surface внутри мерился по своему содержимому, и карточка не дотягивалась
    // до отметки конца пары.
    Box(modifier = modifier, propagateMinConstraints = true) {
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
            // жмётся любая пара: в листе не только подгруппы, но и детали с портала
            .clickable(onClick = onClick),
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
                painter = painterResource(subjectIcon(lesson.title)),
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
 * так что это подбор по ключевому слову, а не справочник: неизвестный предмет получает
 * `ic_school`. Порядок правил значим — частные слова стоят раньше общих, иначе
 * «Физическая культура» уходит в физику, «Русский язык» — в иностранный,
 * а «Языки программирования» — в перевод.
 */
@DrawableRes
internal fun subjectIcon(title: String): Int {
    val n = title.lowercase().replace('ё', 'е')
    return when {
        // Физкультура — до физики: «физическая» есть в обоих названиях.
        "физкультур" in n || "физическ" in n || "спорт" in n -> R.drawable.ic_fitness

        // Профильный цикл
        ("баз" in n && "данн" in n) || "субд" in n || "sql" in n -> R.drawable.ic_database
        "сет" in n || "маршрутизац" in n || "телекоммуникац" in n -> R.drawable.ic_network
        "операционн" in n || "linux" in n || "windows" in n -> R.drawable.ic_terminal
        "алгоритм" in n || "структур данных" in n || "дискретн" in n -> R.drawable.ic_algorithm
        "тестирован" in n || "отладк" in n || "качеств" in n -> R.drawable.ic_bug
        "мобильн" in n || "android" in n || "ios" in n -> R.drawable.ic_mobile
        "веб" in n || "web" in n || "сайт" in n || "html" in n || "фронтенд" in n -> R.drawable.ic_web
        "криптограф" in n || ("безопасн" in n && ("информ" in n || "данн" in n)) ||
            ("защит" in n && "информ" in n) -> R.drawable.ic_security
        "разработ" in n || "программ" in n || "модул" in n || "информатик" in n ->
            R.drawable.ic_code
        "аппаратн" in n || "эвм" in n || "архитектур" in n || "схемотехник" in n ->
            R.drawable.ic_memory

        // Языки — русский до иностранного, иностранный после «программирования» выше.
        "русск" in n || "литератур" in n || "родн" in n -> R.drawable.ic_book
        "английск" in n || "иностран" in n || "язык" in n -> R.drawable.ic_translate

        // Математика и естественные науки
        "статистик" in n || "вероятност" in n -> R.drawable.ic_statistics
        "математик" in n || "матем" in n || "численн метод" in n -> R.drawable.ic_math
        "астроном" in n -> R.drawable.ic_astronomy
        "физик" in n || "хими" in n -> R.drawable.ic_science
        "биолог" in n || "естествознан" in n || "эколог" in n -> R.drawable.ic_biology
        "географ" in n -> R.drawable.ic_public

        // Гуманитарный цикл
        "истори" in n -> R.drawable.ic_history
        "обществ" in n || "правов" in n || "юрид" in n || "законодат" in n -> R.drawable.ic_law
        "психолог" in n || "общени" in n || "этик" in n -> R.drawable.ic_psychology
        "эконом" in n || "финанс" in n || "предпринимат" in n || "бухгалт" in n ||
            "менеджмент" in n || "маркетинг" in n -> R.drawable.ic_economics

        // Организационное
        "жизнедеятельн" in n || "обж" in n || "охран труда" in n || "медицин" in n ->
            R.drawable.ic_safety
        "черчени" in n || "график" in n || "дизайн" in n || "инженерн" in n ->
            R.drawable.ic_design
        "практик" in n || "производствен" in n || "стажировк" in n -> R.drawable.ic_practice
        "проект" in n || "курсов" in n || "диплом" in n || "вкр" in n -> R.drawable.ic_assignment
        "экзамен" in n || "зачет" in n || "консультац" in n || "аттестац" in n ->
            R.drawable.ic_exam
        "классн час" in n || "куратор" in n || "собрани" in n -> R.drawable.ic_groups

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
