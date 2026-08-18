package ru.dzhaparidze.mykct.feature.auth

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.ui.theme.DarkBackground
import ru.dzhaparidze.mykct.ui.theme.Violet
import ru.dzhaparidze.mykct.ui.theme.VioletLight

/**
 * Экран запуска: near-black фон, световая дуга сверху, лого, заголовок и кнопка.
 * Формы входа тут нет намеренно — в референсе её нет, да и слать логин пока некуда:
 * токен выдаёт внешний auth-сервис колледжа, его контракта у нас ещё нет.
 * Рабочий путь — «Продолжить без входа».
 *
 * Экран всегда тёмный, независимо от темы приложения.
 */
@Composable
fun AuthScreen(onEnter: () -> Unit) {
    var notice by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .drawBehind { drawGlowArc() }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Свет занимает верхнюю половину, контент начинается под ним — как в референсе.
        Spacer(Modifier.weight(1f))

        // Логотип приложения — тот же, что в лаунчере. Он на белой подложке:
        // у панды чёрный контур, на near-black фоне без плашки он бы пропал.
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.mipmap.app_icon_foreground),
                contentDescription = null,
                // foreground адаптивной иконки нарисован с полями безопасной зоны,
                // поэтому его увеличиваем и обрезаем плашкой
                modifier = Modifier.size(112.dp),
            )
        }

        Text(
            text = "Добро пожаловать в МойКЦТ",
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 28.dp),
        )

        Text(
            text = "Расписание, посещаемость и баллы —\nвсё в одном приложении.",
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )

        PrimaryButton(
            text = "Войти",
            onClick = { notice = true },
            modifier = Modifier.padding(top = 32.dp),
        )

        Text(
            text = buildAnnotatedString {
                append("Нет аккаунта? ")
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                    append("Продолжить без входа")
                }
            },
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(CircleShape)
                .clickable(onClick = onEnter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )

        Text(
            text = if (notice) {
                "Вход появится вместе с сервисом авторизации колледжа. " +
                    "Расписание работает и без него."
            } else {
                "Расписание доступно без входа. Вход нужен для посещаемости и баллов."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = if (notice) 0.7f else 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
        )
    }
}

/** Белая пилюля с мягким свечением — главная кнопка из референса. */
@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (CAN_BLUR) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(46.dp)
                    // Unbounded, иначе размытие обрезается по краям слоя и вместо
                    // свечения выходит светлая плашка с резкими кромками.
                    .blur(26.dp, BlurredEdgeTreatment.Unbounded)
                    .background(Color.White.copy(alpha = 0.30f), CircleShape),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF121213),
            )
        }
    }
}

/** Размытие свечения требует Android 12; ниже кнопка остаётся просто белой. */
private val CAN_BLUR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Световая дуга из референса: свет идёт из точки под экраном, наружу пробивается
 * только верхний край гало. Поэтому это не пятно, а кольцо — радиальный градиент,
 * прозрачный в середине и яркий у своей кромки. Тёплое кольцо снаружи,
 * фиолетовое чуть внутри, как холодное ядро в референсе.
 */
private fun DrawScope.drawGlowArc() {
    val w = size.width
    val h = size.height
    val center = Offset(w / 2f, h * 0.58f)
    val warm = Color(0xFFEDE3D0)

    drawRect(
        Brush.radialGradient(
            0.70f to Color.Transparent,
            0.87f to warm.copy(alpha = 0.60f),
            0.95f to warm.copy(alpha = 0.14f),
            1f to Color.Transparent,
            center = center,
            radius = h * 0.47f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            0.50f to Color.Transparent,
            0.78f to VioletLight.copy(alpha = 0.80f),
            0.90f to Violet.copy(alpha = 0.30f),
            1f to Color.Transparent,
            center = center,
            radius = h * 0.44f,
        ),
    )

    // Кольцо целиком — это круг; в референсе видна только его верхняя дуга.
    // Гасим низ вертикальной шторкой цвета фона, ножки дуги уходят в чёрное.
    drawRect(
        Brush.verticalGradient(
            0.34f to Color.Transparent,
            0.48f to DarkBackground.copy(alpha = 0.85f),
            0.58f to DarkBackground,
            1f to DarkBackground,
        ),
    )
}
