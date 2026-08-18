package ru.dzhaparidze.mykct.feature.settings

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.ThemeMode
import ru.dzhaparidze.mykct.ui.dotGrid
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
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Row(
                    // selectable, а не clickable: иначе строка и радиокнопка — два
                    // отдельных таргета, и TalkBack читает их дважды
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = themeMode == mode,
                            role = Role.RadioButton,
                            onClick = { onThemeChange(mode) },
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = themeMode == mode, onClick = null)
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp),
                    )
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
            LinkRow(R.drawable.ic_report, "Сообщить о проблеме", "https://t.me/airsss993")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            LinkRow(R.drawable.ic_code, "Исходный код", "https://github.com/airsss993/mykct-android-app")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            LinkRow(R.drawable.ic_public, "Веб-сайт", "https://it-college.ru/")
        }

        SectionTitle("Разработчики")
        Card {
            Person("Артём Джапаридзе", "Android разработчик", "https://github.com/airsss993", "https://t.me/airsss993")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            Person("Иван Коломацкий", "iOS разработчик", "https://github.com/anton1ks96", "https://t.me/IKolomatskii")
        }

        SectionTitle("Маркетинг")
        Card {
            Person("Илья Некрасов", "Маркетолог", "https://github.com/necrasov-ilya", "https://t.me/NKSV_ILYA")
        }

        Spacer(Modifier.height(NAV_BAR_INSET))
    }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Person(name: String, role: String, github: String, telegram: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SocialButton(R.drawable.github_icon, "GitHub $name") { uriHandler.openUri(github) }
        Spacer(Modifier.width(8.dp))
        // telegram_icon нарисован «вверх ногами» относительно остальных — отражаем по вертикали
        SocialButton(R.drawable.telegram_icon, "Telegram $name", flip = true) { uriHandler.openUri(telegram) }
    }
}

@Composable
private fun SocialButton(iconRes: Int, description: String, flip: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(18.dp)
                .scale(scaleX = 1f, scaleY = if (flip) -1f else 1f),
        )
    }
}
