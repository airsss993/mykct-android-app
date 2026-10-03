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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.Groups
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.ui.theme.AccentGradient
import ru.dzhaparidze.mykct.ui.theme.VioletLight
import ru.dzhaparidze.mykct.ui.theme.VioletTint
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/** Карточка пары из референса: пилюля со временем, крупный заголовок, тема, чипы. */
@Composable
fun LessonCard(
    lesson: Lesson,
    isPast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Часы экрана, если пара идёт сейчас; null - не идёт. */
    now: LocalTime? = null,
) {
    val isNow = now != null
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
                    // склеенные названия подгрупп, если пара делится, иначе своё название
                    text = lesson.displayTitle,
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

                // Остаток от тех же минутных часов, что и линия "сейчас": свои посекундные
                // часы у карточки расходились с линией, а полосу прогресса iOS тоже убрал.
                Chips(lesson = lesson, secondsLeft = now?.let { secondsLeft(lesson, it) })
            }
        }
    }
    }
}

/** Размытие свечения требует Android 12; ниже идущая пара опознаётся только кромкой. */
private val CAN_BLUR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * FlowRow, а не Row: на узком экране «Идёт · осталось N мин» рядом с «Подгруппы: N»
 * не влезает в строку и второй чип обрезается.
 */
@Composable
private fun Chips(lesson: Lesson, secondsLeft: Int?) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (secondsLeft != null) RemainingChip(remainingText(secondsLeft))
        if (lesson.room.isNotBlank()) {
            Chip(text = lesson.room, icon = R.drawable.ic_place)
        }
        if (lesson.subgroups.isNotEmpty()) {
            Chip(text = "Подгруппы: ${lesson.subgroups.size}", icon = R.drawable.ic_list)
        }
    }
}

/**
 * Чип остатка сжимается под ширину карточки, как `ViewThatFits` в iOS: две пары рядом
 * не вмещают "Идёт · осталось 40 мин", и тогда показываем "Осталось 40 мин" или "40 мин".
 */
@Composable
private fun RemainingChip(left: String) {
    Layout(
        contents = listOf(
            { Chip(text = "Идёт · осталось $left", icon = R.drawable.ic_clock) },
            { Chip(text = "Осталось $left", icon = R.drawable.ic_clock) },
            { Chip(text = left, icon = R.drawable.ic_clock) },
        ),
    ) { variants, constraints ->
        val loose = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
        val placeables = variants.map { it.first().measure(loose) }
        val chosen = placeables.firstOrNull { it.width <= constraints.maxWidth }
            ?: variants.last().first().measure(constraints.copy(minWidth = 0))
        layout(chosen.width, chosen.height) { chosen.place(0, 0) }
    }
}

/** До конца пары, с округлением вверх: на последней секунде честнее «1 с», чем «0 с». */
internal fun secondsLeft(lesson: Lesson, at: LocalTime): Int {
    val millis = Duration.between(at, lesson.end).toMillis()
    return if (millis <= 0) 0 else ((millis + 999) / 1000).toInt()
}

/** Последнюю минуту отсчёт идёт секундами — иначе «осталось 0 мин» висит целую минуту. */
internal fun remainingText(seconds: Int): String =
    if (seconds < 60) "$seconds с" else "${seconds / 60} мин"


/**
 * Водяной знак по названию предмета, алгоритм как в iOS (`SubjectIcon`). Название
 * сводится к ключу справочника [PORTAL_ICONS]: точное совпадение, другое написание
 * ([ALIASES]), то же без профиля ("ТестUI-FE" -> "ТестUI" -> "ТестИнтерф"), и только
 * потом ключевое слово - полное название вроде "Базы данных" получает иконку короткого.
 *
 * Порядок ключевых правил значим - частные слова стоят раньше общих, иначе
 * "Физическая культура" уходит в физику, а "Языки программирования" - в иностранный.
 */
@DrawableRes
internal fun subjectIcon(title: String): Int {
    val name = title.trim()
    val subject = known(name) ?: withoutProfile(name)?.let(::known) ?: keywordSubject(name)
    return subject?.let(PORTAL_ICONS::get) ?: R.drawable.ic_school
}

private fun known(title: String): String? =
    (ALIASES[title] ?: title).takeIf { it in PORTAL_ICONS }

/** Профиль приклеен к названию: "-FE", ".PM", "BE". UI бывает только через разделитель. */
private val PROFILE_SUFFIXES: List<String> = Groups.PROFILES.map { it.id }.let { ids ->
    (ids + "UI").flatMap { listOf("-$it", ".$it") } + ids
}

private fun withoutProfile(title: String): String? =
    PROFILE_SUFFIXES.firstOrNull { title.endsWith(it) && title.length > it.length }
        ?.let { title.dropLast(it.length) }

private fun keywordSubject(title: String): String? {
    val n = title.lowercase().replace('ё', 'е')
    fun has(vararg parts: String) = parts.any { it in n }

    return when {
        has("физкультур", "спорт") || (has("физическ") && has("культур")) -> "Физкульт"
        has("мышлен") && has("инженерн") -> "ИнжМыш"
        has("мышлен") && has("критическ") -> "КритМыш"

        (has("баз") && has("данн")) || has("субд", "sql") -> "СУБД"
        has("сети", "сетев", "маршрутизац", "телекоммуникац") -> "КомпСети"
        has("операционн", "linux", "windows") -> "ОперСистемы"
        has("дискретн") -> "ДискрМат"
        has("алгоритм") || (has("структур") && has("данн")) -> "АиСД"
        has("тестирован", "отладк", "качеств") -> "Тестирование"
        has("мобильн", "android", "ios") -> "Мобильная разработка"
        has("веб", "web", "сайт", "html", "фронтенд") -> "Веб-Дизайн"
        has("криптограф") || (has("безопасн") && has("информ", "данн")) ||
            (has("защит") && has("информ")) -> "ИнфоБез"
        has("паттерн") -> "АрхПаттерны"
        has("разработ", "программ", "модул", "информатик") -> "РазработкаПО"
        has("аппаратн", "эвм", "архитектур", "схемотехник") -> "Hardware"

        has("русск", "родн") -> "РусЯз"
        has("литератур") -> "Литер"
        has("английск", "иностран") && has("профессиональн") -> "АнглЯзПро"
        has("английск", "иностран", "язык") -> "АнглЯз"

        has("статистик", "вероятност") -> "ТеорВер"
        has("численн") -> "ЧислМетоды"
        has("высш") && has("матем") -> "ЭлВышМат"
        has("математик", "матем") -> "Математика"
        has("астроном") -> "Астрономия"
        has("хими") -> "Химия"
        has("физик") -> "Физика"
        has("биолог", "естествознан", "эколог") -> "Биология"
        has("географ") -> "География"

        has("истори") && has("технолог") -> "ИстТехно"
        has("истори") -> "История"
        has("обществ") -> "Обществознание"
        has("правов", "юрид", "законодат") -> "ПОПД"
        has("философ") -> "Философия"
        has("психолог", "общени", "этик") -> "ПсихОбщен"
        has("финанс") -> "ФинГрамота"
        has("эконом") -> "Экономика"
        has("маркетинг") -> "Маркетинг"
        has("предпринимат", "бухгалт", "менеджмент") -> "Предпринимат"

        has("жизнедеятельн", "обж", "охран труда") -> "ОБиЗР"
        has("медицин") -> "Медицина"
        has("график", "графическ") -> "ГрафДизайн"
        has("черчени", "дизайн", "инженерн") -> "Дизайн"
        has("практик", "производствен", "стажировк") -> "ПроизвПракт.01"
        has("проект", "курсов", "диплом", "вкр") -> "Проект"
        has("экзамен", "зачет", "консультац", "аттестац") -> "Демоэкзамен"
        has("собрани") -> "ОргСобрание"
        has("классн час", "куратор") -> "Классный час"
        else -> null
    }
}

/**
 * Предметы портала - те же ключи, что в iOS (`SubjectIcon.icons`), но символов там
 * ~150, а иконок здесь 25: близкие предметы делят одну. Пополняется вместе с порталом,
 * обычно каждый сентябрь; профильные варианты и другие написания сюда не пишутся.
 *
 * ponytail: палитра сознательно сужена. Нужны свои символы под каждое название -
 * это ~125 новых vector drawable, таблица тогда меняется значениями, а не строением.
 */
private val PORTAL_ICONS: Map<String, Int> = mapOf(
    // Общеобразовательный цикл
    "Математика" to R.drawable.ic_math,
    "ЭлВышМат" to R.drawable.ic_math,
    "ДискрМат" to R.drawable.ic_algorithm,
    "ТеорВер" to R.drawable.ic_statistics,
    "ЧислМетоды" to R.drawable.ic_math,
    "Физика" to R.drawable.ic_science,
    "Химия" to R.drawable.ic_science,
    "Биология" to R.drawable.ic_biology,
    "Астрономия" to R.drawable.ic_astronomy,
    "История" to R.drawable.ic_history,
    "ИстТехно" to R.drawable.ic_history,
    "ИсторияТ" to R.drawable.ic_history,
    "Обществознание" to R.drawable.ic_law,
    "Философия" to R.drawable.ic_psychology,
    "РусЯз" to R.drawable.ic_book,
    "Литер" to R.drawable.ic_book,
    "АнглЯз" to R.drawable.ic_translate,
    "АнглЯзПро" to R.drawable.ic_translate,
    "Физкульт" to R.drawable.ic_fitness,
    "ОБиЗР" to R.drawable.ic_safety,
    "ЭлДок" to R.drawable.ic_assignment,
    "ТехДокиРус" to R.drawable.ic_assignment,
    "ФинГрамота" to R.drawable.ic_economics,
    "Экономика" to R.drawable.ic_economics,
    "ПОПД" to R.drawable.ic_law,
    "Предпринимат" to R.drawable.ic_economics,
    "ПредпрКлас" to R.drawable.ic_economics,
    "ПсихОбщен" to R.drawable.ic_psychology,
    "КритМыш" to R.drawable.ic_psychology,
    "ИнжМыш" to R.drawable.ic_design,
    "ТРИЗ" to R.drawable.ic_psychology,
    "АктМаст" to R.drawable.ic_groups,
    "ЛичБренд" to R.drawable.ic_person,
    "КреативМ" to R.drawable.ic_design,
    "ОКРиУП" to R.drawable.ic_assignment,
    "ВведСпец" to R.drawable.ic_school,
    "ВВСпец-П" to R.drawable.ic_school,

    // Профильный цикл
    "РазработкаПО" to R.drawable.ic_code,
    "РазработкаПО-1" to R.drawable.ic_code,
    "РазработкаПО-2" to R.drawable.ic_code,
    "РазрПО-П" to R.drawable.ic_code,
    "ВведениеООП" to R.drawable.ic_code,
    "ПрогрC#" to R.drawable.ic_code,
    "UnityC#" to R.drawable.ic_code,
    "Frameworks" to R.drawable.ic_code,
    "React" to R.drawable.ic_web,
    "АиСД" to R.drawable.ic_algorithm,
    "АиСД-2" to R.drawable.ic_algorithm,
    "Hardware" to R.drawable.ic_memory,
    "ОперСистемы" to R.drawable.ic_terminal,
    "КомпСети" to R.drawable.ic_network,
    "СУБД" to R.drawable.ic_database,
    "СУБД-1" to R.drawable.ic_database,
    "СУБД-2" to R.drawable.ic_database,
    "СУБД-проектирование" to R.drawable.ic_database,
    "ИнфоБез" to R.drawable.ic_security,
    "ОсновыML" to R.drawable.ic_memory,
    "АрхПаттерны" to R.drawable.ic_algorithm,
    "ПарадигмыПроект" to R.drawable.ic_algorithm,
    "UML" to R.drawable.ic_algorithm,
    "УчПроект" to R.drawable.ic_code,
    "УчПроект02" to R.drawable.ic_code,
    "Микросервисы" to R.drawable.ic_network,
    "ИнтегрПО" to R.drawable.ic_network,
    "BE-Production" to R.drawable.ic_terminal,
    "Тестирование" to R.drawable.ic_bug,
    "ТестИнтерф" to R.drawable.ic_bug,
    "РазрИнтерф" to R.drawable.ic_design,
    "ИнстРазрИнтерф" to R.drawable.ic_design,
    "РазрИгрИнтерф" to R.drawable.ic_design,
    "ИТ-инфр-проект" to R.drawable.ic_network,
    "ИТ-инфр-разв" to R.drawable.ic_network,
    "ИТ-инфр-экспл" to R.drawable.ic_terminal,

    // Дизайн
    "Веб-Дизайн" to R.drawable.ic_web,
    "ГрафДизайн" to R.drawable.ic_design,
    "ДизИнтерфейсов" to R.drawable.ic_design,
    "ДизДиджитал" to R.drawable.ic_mobile,
    "ДизМедиа" to R.drawable.ic_design,
    "СтилиДизайн" to R.drawable.ic_design,
    "Композиция" to R.drawable.ic_design,
    "Колористика" to R.drawable.ic_design,
    "2D-КомпГраф" to R.drawable.ic_design,
    "3D-КомпГраф" to R.drawable.ic_design,
    "3D-Интерфейсы" to R.drawable.ic_design,
    "UX/UI дизайн" to R.drawable.ic_design,
    "АналитикаUX" to R.drawable.ic_statistics,

    // Разработка игр
    "GameDev-2(1)" to R.drawable.ic_code,
    "GameDev-2(2)" to R.drawable.ic_code,
    "GameDev-3(3)" to R.drawable.ic_code,
    "GameDev-практ" to R.drawable.ic_practice,
    "ИгроДев" to R.drawable.ic_code,
    "ИгроМех" to R.drawable.ic_code,
    "РазработкаИгрП" to R.drawable.ic_code,
    "СопрИгрПрод" to R.drawable.ic_practice,
    "РевьюКода" to R.drawable.ic_bug,
    "РевьюИгроКейс2" to R.drawable.ic_bug,
    "РевьюИгроКейс3" to R.drawable.ic_bug,
    "Маркетинг" to R.drawable.ic_economics,

    // Управление проектами
    "УпрИТ-проект" to R.drawable.ic_assignment,
    "ВидыПроект" to R.drawable.ic_assignment,
    "ФормПроекта" to R.drawable.ic_assignment,
    "ГруппаПроекта" to R.drawable.ic_groups,
    "ЭтапыПроекта" to R.drawable.ic_list,
    "КейсыПроектов" to R.drawable.ic_assignment,
    "ПроектированиеБП" to R.drawable.ic_assignment,
    "ПроектыБП" to R.drawable.ic_assignment,
    "ПсихологияБП" to R.drawable.ic_psychology,
    "ПсихоПроекта" to R.drawable.ic_psychology,
    "МаркетингПМ" to R.drawable.ic_economics,
    "ПродРазр" to R.drawable.ic_mobile,
    "ОтрасПР" to R.drawable.ic_practice,
    "Предприятия" to R.drawable.ic_economics,

    // Проекты, практики и мероприятия
    "Проект" to R.drawable.ic_assignment,
    "Проект-3" to R.drawable.ic_assignment,
    "ПрофПредмет" to R.drawable.ic_school,
    "ПроизвПракт.01" to R.drawable.ic_practice,
    "УчПракт03" to R.drawable.ic_practice,
    "УчПракт04" to R.drawable.ic_practice,
    "УчПракт06" to R.drawable.ic_practice,
    "УчПракт.БП" to R.drawable.ic_practice,
    "УчПракт" to R.drawable.ic_practice,
    "АлгоТруд-3" to R.drawable.ic_person,
    "Демоэкзамен" to R.drawable.ic_exam,
    "Предзащита" to R.drawable.ic_exam,
    "Нормоконтроль" to R.drawable.ic_assignment,
    "В.Сборы" to R.drawable.ic_safety,
    "Буткемп" to R.drawable.ic_practice,
    "Выставка" to R.drawable.ic_groups,
    "ФорумБудущего" to R.drawable.ic_groups,
    "ОргСобрание" to R.drawable.ic_groups,
    "Подгруппы" to R.drawable.ic_list,
    "Подгруппы-1к" to R.drawable.ic_list,
    "Подгруппы-2к" to R.drawable.ic_list,
    "Подгруппы-3к" to R.drawable.ic_list,

    // Ключи для полных названий, у портала таких нет
    "Мобильная разработка" to R.drawable.ic_mobile,
    "География" to R.drawable.ic_public,
    "Медицина" to R.drawable.ic_safety,
    "Дизайн" to R.drawable.ic_design,
    "Классный час" to R.drawable.ic_groups,
)

/** Другие написания того же предмета на портале. */
private val ALIASES: Map<String, String> = mapOf(
    "Литература" to "Литер",
    "АнлгЯзПро" to "АнглЯзПро",
    "Физкультура" to "Физкульт",
    "ВВСпец" to "ВведСпец",
    "ОперСистем" to "ОперСистемы",
    "АрхПаттерны3" to "АрхПаттерны",
    "ПарадПроекта" to "ПарадигмыПроект",
    "ИнтегрПО2" to "ИнтегрПО",
    "ТестИнтерфейс" to "ТестИнтерф",
    "ТестИнтерфейсов" to "ТестИнтерф",
    "ТестUI" to "ТестИнтерф",
    "РазрUI" to "РазрИнтерф",
    "ИПИР" to "ИнстРазрИнтерф",
    "Проект-ИТ-ИНФ" to "ИТ-инфр-проект",
    "Развер-ИТ-ИНФ" to "ИТ-инфр-разв",
    "Экспл-ИТ-ИНФ" to "ИТ-инфр-экспл",
    "ВебДизайн" to "Веб-Дизайн",
    "ИнтерфДиз" to "ДизИнтерфейсов",
    "ДигиДиз" to "ДизДиджитал",
    "СтилиДиз" to "СтилиДизайн",
    "2D-Граф" to "2D-КомпГраф",
    "3D-Граф" to "3D-КомпГраф",
    "3D-Интерф" to "3D-Интерфейсы",
    "ИгроМаркетинг" to "Маркетинг",
    "КейсыПроекта" to "КейсыПроектов",
)

@Composable
internal fun TimePill(text: String, showCheck: Boolean, accent: Color) {
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
internal fun Chip(text: String, @DrawableRes icon: Int? = null) {
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
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}
