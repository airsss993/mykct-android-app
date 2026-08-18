package ru.dzhaparidze.mykct.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.ui.theme.AccentGradient
import ru.dzhaparidze.mykct.ui.theme.DarkBackground
import ru.dzhaparidze.mykct.ui.theme.Violet
import ru.dzhaparidze.mykct.ui.theme.VioletDeep
import ru.dzhaparidze.mykct.ui.theme.VioletLight

/**
 * Экран входа при первом запуске: герой на градиенте и карточка с формой снизу.
 * Экран всегда тёмный независимо от темы приложения — как заставка в референсе.
 *
 * Самого входа пока нет: бэкенд валидирует чужой токен (LDAP-сервис колледжа),
 * эндпоинта выдачи токена мы ещё не знаем. Поэтому «Войти» честно говорит об этом,
 * а рабочий путь — «Продолжить без входа».
 */
@Composable
fun AuthScreen(onEnter: () -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    // Свет сверху и уход в near-black к низу — как в референсе.
                    0f to VioletLight,
                    0.16f to Violet,
                    0.34f to VioletDeep,
                    0.58f to DarkBackground,
                    1f to DarkBackground,
                ),
            )
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ImageSlot(
                label = "лого",
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(8.dp),
            )
            Text(
                text = "МойКЦТ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Badge(text = "Расписание всегда под рукой")

        Spacer(Modifier.height(20.dp))

        Text(
            text = "ВСЯ УЧЁБА\nВ ОДНОМ ПРИЛОЖЕНИИ",
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Text(
            text = "Расписание, посещаемость и баллы — в одном месте",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, start = 32.dp, end = 32.dp),
        )

        ImageSlot(
            label = "иллюстрация героя",
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(28.dp),
        )

        LoginCard(
            login = login,
            password = password,
            notice = notice,
            onLoginChange = { login = it },
            onPasswordChange = { password = it },
            onSubmit = { notice = true },
            onSkip = onEnter,
        )
    }
}

/** Пилюля-бейдж над заголовком. */
@Composable
private fun Badge(text: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Нижняя карточка с формой — в референсе она отделена от героя и прижата к низу. */
@Composable
private fun LoginCard(
    login: String,
    password: String,
    notice: Boolean,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(32.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Уже учишься в КЦТ?",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )

        Field(
            value = login,
            onValueChange = onLoginChange,
            placeholder = "Логин колледжа",
            modifier = Modifier.padding(top = 16.dp),
        )

        Field(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Пароль",
            isPassword = true,
            modifier = Modifier.padding(top = 10.dp),
        )

        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(54.dp)
                .clip(CircleShape)
                .background(AccentGradient)
                .clickable(onClick = onSubmit),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Войти",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }

        if (notice) {
            Text(
                text = "Вход появится вместе с сервисом авторизации колледжа. " +
                    "Пока расписание работает и без него.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Text(
            text = "Продолжить без входа",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(CircleShape)
                .clickable(onClick = onSkip)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.45f)) },
        singleLine = true,
        shape = CircleShape,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.10f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
            focusedBorderColor = Color.White.copy(alpha = 0.35f),
            unfocusedBorderColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Заглушка под картинку: рамка с подписью, чтобы было видно место и размер.
 * Меняется на `Image(painterResource(...))`, когда появятся сами картинки.
 */
@Composable
private fun ImageSlot(
    label: String,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
    }
}
