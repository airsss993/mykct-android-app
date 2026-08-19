package ru.dzhaparidze.mykct.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.ui.ShinyPill
import ru.dzhaparidze.mykct.ui.theme.DarkBackground
import ru.dzhaparidze.mykct.ui.theme.Violet
import ru.dzhaparidze.mykct.ui.theme.VioletLight

/**
 * Форма входа. Отправлять пока некуда: токен выдаёт внешний auth-сервис колледжа,
 * его контракта у нас нет — кнопка проверяет заполненность и показывает, чего ждём.
 * Когда контракт появится, сюда придёт вызов репозитория, а вёрстка останется.
 *
 * Экран всегда тёмный, как и [AuthScreen], независимо от темы приложения.
 */
@Composable
fun LoginScreen(onBack: () -> Unit, onSkip: () -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordShown by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .drawBehind { drawGlowArc() }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_chevron_left),
                contentDescription = "Назад",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "Вход",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
        )
        Text(
            text = "Логин и пароль — те же, что в личном кабинете колледжа.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        Field(
            value = login,
            onValueChange = { login = it; notice = false },
            label = "Логин",
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
        )

        Spacer(Modifier.height(12.dp))

        Field(
            value = password,
            onValueChange = { password = it; notice = false },
            label = "Пароль",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            visualTransformation = if (passwordShown) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                // Текстом, а не иконкой-глазом: своего глаза в наборе нет, а тащить
                // ещё один svg ради одного места незачем.
                Text(
                    text = if (passwordShown) "Скрыть" else "Показать",
                    style = MaterialTheme.typography.labelLarge,
                    color = VioletLight,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { passwordShown = !passwordShown }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            },
        )

        ShinyPill(
            text = "Войти",
            onClick = { notice = true },
            enabled = login.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.padding(top = 24.dp),
        )

        Text(
            text = if (notice) {
                "Вход появится вместе с сервисом авторизации колледжа — его API нам ещё не отдали."
            } else {
                "Вход нужен для посещаемости и баллов. Расписание работает и без него."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = if (notice) 0.7f else 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        Text(
            text = "Продолжить без входа",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 24.dp)
                .fillMaxWidth()
                .clip(CircleShape)
                .clickable(onClick = onSkip)
                .padding(vertical = 12.dp),
        )
    }
}

/** Поле в тёмном стекле: заливка светлой плёнкой, кромка — акцент при фокусе. */
@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.06f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
            cursorColor = VioletLight,
            focusedBorderColor = Violet,
            unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
            focusedLabelColor = VioletLight,
            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
