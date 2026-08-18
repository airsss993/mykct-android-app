package ru.dzhaparidze.mykct.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.ThemeMode
import ru.dzhaparidze.mykct.ui.dotGrid
import ru.dzhaparidze.mykct.ui.theme.AccentGradient
import ru.dzhaparidze.mykct.ui.hairline
import ru.dzhaparidze.mykct.feature.NAV_BAR_INSET

/**
 * Настройки версии 1.1.1 одним экраном: там тема и «О приложении» были отдельными
 * экранами с кнопкой «назад», но содержимого на них по горсти — вложенность не нужна.
 */
@Composable
fun SettingsScreen(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .dotGrid()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp),
        )

        SectionTitle("Тема")
        Card {
            ThemeMode.entries.forEachIndexed { index, mode ->
                if (index > 0) Divider()
                // selectable, а не clickable: иначе строка и отметка — два отдельных
                // таргета, и TalkBack читает их дважды
                SettingsRow(
                    icon = mode.icon(),
                    text = mode.title,
                    modifier = Modifier.selectable(
                        selected = themeMode == mode,
                        role = Role.RadioButton,
                        onClick = { onThemeChange(mode) },
                    ),
                ) {
                    // Галочка вместо радиокнопки — как в референсе: выбранное состояние
                    // отмечено акцентом, а не отдельным элементом управления.
                    if (themeMode == mode) {
                        Icon(
                            painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        SectionTitle("О приложении")
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "МойКЦТ для Android",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Версия ${appVersion()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = "© 2021–2025 АНПОО «Колледж Цифровых Технологий»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        SectionTitle("Действия")
        Card {
            LinkRow(R.drawable.ic_code, "Исходный код", "https://github.com/airsss993/mykct-android-app")
            Divider()
            LinkRow(R.drawable.ic_public, "Веб-сайт", "https://it-college.ru/")
        }

        SectionTitle("Команда")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PersonCard(
                name = "Артём Джапаридзе",
                role = "Android-разработчик",
                github = "https://github.com/airsss993",
                telegram = "https://t.me/airsss993",
                avatar = R.drawable.avatar_artem,
            )
            PersonCard(
                name = "Иван Коломацкий",
                role = "iOS-разработчик",
                github = "https://github.com/anton1ks96",
                telegram = "https://t.me/IKolomatskii",
                avatar = R.drawable.avatar_ivan,
                wideShot = true,
            )
        }

        Spacer(Modifier.height(28.dp))

        PillButton(
            icon = R.drawable.ic_send,
            text = "Написать разработчику",
            url = "https://t.me/airsss993",
        )

        Spacer(Modifier.height(NAV_BAR_INSET))
    }
}

/**
 * Белая пилюля во всю ширину — главное действие экрана, как «Log Out» в референсе.
 * Цвет заливки не из палитры темы: кнопка одинаково белая в обеих темах, это её роль.
 */
@Composable
private fun PillButton(@DrawableRes icon: Int, text: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable { uriHandler.openUri(url) }
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = INK,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = INK,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

private val INK = Color(0xFF121213)

/**
 * Разделитель внутри карточки — с отступом под иконку, как в референсе.
 * Цвет свой: штатный outlineVariant в тёмной теме совпадает с фоном карточки
 * (оба DarkGreyFill), и линии не видно вовсе.
 */
@Composable
private fun Divider() = HorizontalDivider(
    modifier = Modifier.padding(start = 54.dp),
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
)

/** Строка настроек: иконка, подпись и что-нибудь справа — отметка или стрелка. */
@Composable
private fun SettingsRow(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
        trailing()
    }
}

@DrawableRes
private fun ThemeMode.icon(): Int = when (this) {
    ThemeMode.SYSTEM -> R.drawable.ic_theme_system
    ThemeMode.LIGHT -> R.drawable.ic_theme_light
    ThemeMode.DARK -> R.drawable.ic_theme_dark
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .hairline(RoundedCornerShape(20.dp)),
        content = content,
    )
}

@Composable
private fun LinkRow(@DrawableRes icon: Int, text: String, url: String) {
    val uriHandler = LocalUriHandler.current
    SettingsRow(
        icon = icon,
        text = text,
        modifier = Modifier.clickable { uriHandler.openUri(url) },
    ) {
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Карточка человека: слева подпись и соцсети, справа портрет, за ним — фигуры
 * фирменного градиента. Портрет вырезан по контуру (PNG с альфой), поэтому голова
 * заходит за круг, как в референсе; с прямоугольным фото так не выйдет.
 */
@Composable
private fun PersonCard(
    name: String,
    role: String,
    github: String,
    telegram: String,
    modifier: Modifier = Modifier,
    @DrawableRes avatar: Int? = null,
    // Кадр «человек у машины» шире портрета: если масштабировать его по высоте
    // карточки, машина съёживается в оранжевое пятно. Такие кадры тянем по ширине.
    wideShot: Boolean = false,
) {
    val uriHandler = LocalUriHandler.current

    // Портрет — сосед карточки, а не её содержимое: внутри его обрезала бы скруглённая
    // кромка, а он должен выступать над ней, как в референсе.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height((if (wideShot) WIDE_CARD_HEIGHT else CARD_HEIGHT) + PORTRAIT_RISE),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (wideShot) WIDE_CARD_HEIGHT else CARD_HEIGHT)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .hairline(RoundedCornerShape(24.dp)),
        ) {
            Blobs()

            if (avatar == null) {
                Text(
                    text = name.initials(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 52.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 18.dp, end = if (wideShot) 186.dp else 140.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = role.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    SocialButton(R.drawable.github_icon, "GitHub $name") { uriHandler.openUri(github) }
                    // telegram_icon нарисован «вверх ногами» относительно остальных — отражаем
                    SocialButton(R.drawable.telegram_icon, "Telegram $name", flip = true) { uriHandler.openUri(telegram) }
                }
            }
        }

        if (avatar != null) {
            Image(
                painter = painterResource(avatar),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = if (wideShot) 4.dp else 20.dp)
                    .then(if (wideShot) Modifier.fillMaxWidth(0.56f) else Modifier.fillMaxHeight()),
            )
        }
    }
}

private val CARD_HEIGHT = 158.dp

/** Кадр с машиной требует высоты: иначе от неё остаётся полоска у нижней кромки. */
private val WIDE_CARD_HEIGHT = 190.dp

/** На сколько портрет выступает над верхней кромкой карточки. */
private val PORTRAIT_RISE = 26.dp

/** Фигуры за портретом: два круга фирменного градиента, обрезанные кромкой карточки. */
@Composable
private fun BoxScope.Blobs() {
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 34.dp)
            .size(118.dp)
            .clip(CircleShape)
            .background(AccentGradient),
    )
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 26.dp, y = (-22).dp)
            .size(88.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    )
}

/** Круглая кнопка соцсети в фирменной обводке. */
@Composable
private fun SocialButton(@DrawableRes iconRes: Int, description: String, flip: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(AccentGradient)
            .clickable(onClick = onClick)
            .padding(1.5.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(16.dp)
                .scale(scaleX = 1f, scaleY = if (flip) -1f else 1f),
        )
    }
}


/** Первые буквы имени и фамилии: «Артём Джапаридзе» → «АД». */
private fun String.initials(): String =
    trim().split(" ").filter { it.isNotBlank() }.take(2).map { it.first().uppercase() }.joinToString("")


