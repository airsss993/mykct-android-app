package ru.dzhaparidze.mykct.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.Groups
import ru.dzhaparidze.mykct.data.auth.User
import ru.dzhaparidze.mykct.ui.hairline
import ru.dzhaparidze.mykct.ui.theme.AccentGradient
import ru.dzhaparidze.mykct.ui.theme.statusDanger

/**
 * Кнопка аккаунта в шапке всех трёх экранов, как в iOS: у вошедшего открывает профиль,
 * без входа - форму входа. Что именно открыть, решает `AppShell`.
 */
@Composable
fun AccountButton(signedIn: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(
                if (signedIn) {
                    Modifier.background(AccentGradient, CircleShape)
                } else {
                    Modifier.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), CircleShape)
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = if (signedIn) "Профиль" else "Войти",
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_person),
            contentDescription = if (signedIn) "Профиль" else "Войти",
            tint = if (signedIn) Color.White else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Профиль: что знает о студенте auth и выход. Заменил блок "Аккаунт" в настройках. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(user: User, onSignOut: () -> Unit, onDismiss: () -> Unit) {
    var confirm by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Профиль",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Caption("Информация об аккаунте")
            Card {
                val rows = profileRows(user)
                rows.forEachIndexed { index, (label, value) ->
                    if (index > 0) Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                        )
                    }
                }
            }

            Caption("Действия")
            Card {
                Text(
                    text = "Выйти из аккаунта",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = statusDanger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { confirm = true }
                        .padding(16.dp),
                )
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Вы уверены, что хотите выйти из аккаунта?") },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Отмена") } },
            confirmButton = {
                TextButton(onClick = { confirm = false; onSignOut() }) {
                    Text("Выйти", color = statusDanger)
                }
            },
        )
    }
}

/** Строки профиля: пустые поля (у преподавателя групп нет) не показываем вовсе. */
private fun profileRows(user: User): List<Pair<String, String>> {
    val group = user.academicGroup.orEmpty()
    fun titleOf(id: String?) = id?.let { value -> Groups.subgroups(group).firstOrNull { it.id == value }?.title ?: value }
    val subgroupTitle = user.subgroup?.let { id -> if (id.startsWith("Подгр")) "Подгруппа ${id.removePrefix("Подгр")}" else id }
    return listOf(
        "Логин" to user.id,
        "ФИО" to user.username,
        "Группа" to user.academicGroup,
        "Профиль" to titleOf(user.profile),
        "Группа английского" to user.englishGroup,
        "Подгруппа" to subgroupTitle,
    ).mapNotNull { (label, value) -> value?.takeIf { it.isNotBlank() }?.let { label to it } }
}

@Composable
private fun Caption(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp, start = 4.dp),
)

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surface)
        .hairline(RoundedCornerShape(20.dp)),
    content = content,
)

@Composable
private fun Divider() = HorizontalDivider(
    modifier = Modifier.padding(start = 16.dp),
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
)
