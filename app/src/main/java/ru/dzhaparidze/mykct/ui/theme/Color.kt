package ru.dzhaparidze.mykct.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Токены сняты с крипто-референса: near-black фон, сине-фиолетовый акцент,
// шапка — градиент из трёх остановок (светлая сирень сверху к глубокому индиго снизу).
val Violet = Color(0xFF6C5CE7)        // акцент: активный пункт, кольцо, ссылки
val VioletTint = Color(0xFFC3B6FF)    // самый светлый — блик в градиенте
// Соседи акцента по кругу: пурпур в тёплую сторону, индиго в холодную.
// Нужны, чтобы свечение фона не было одноцветным пятном.
val VioletMagenta = Color(0xFFA347E0)
val VioletIndigo = Color(0xFF4A54C9)
val VioletLight = Color(0xFF9B8CFF)   // верхняя остановка градиента
val VioletDeep = Color(0xFF4B3FBF)    // нижняя остановка градиента
val VioletSoft = Color(0xFFE8E5FF)    // светлая заливка под акцент в светлой теме

/**
 * Фирменный градиент: шапка, активный пункт навбара, выбранный день, карточки пар.
 * Четыре остановки, а не две: блик — светлая сирень — акцент — индиго. На двух
 * переход читается как плоская заливка, на четырёх появляется объём, как в референсах.
 */
val AccentGradient = Brush.linearGradient(listOf(VioletTint, VioletLight, Violet, VioletDeep))

val Green = Color(0xFF22C55E)         // кольцо прогресса, «в плюсе»

// Светлая тема
val LightBackground = Color(0xFFF4F4F8)
val GreyFill = Color(0xFFE7E7EE)      // невыбранные круги дней
// Оттенок с референса, но затемнён: исходный #9A9AA5 давал на белом 2.8:1 при норме
// WCAG AA 4.5:1 — подписи дней и время в таймлайне читались плохо. Этот даёт 5.0:1.
val GreyText = Color(0xFF6E6E7A)
val Ink = Color(0xFF16161D)

// Тёмная тема (основная в референсе)
val DarkBackground = Color(0xFF0C0C11)
val DarkSurface = Color(0xFF16161D)   // карточки, навбар
val DarkGreyFill = Color(0xFF23232D)  // круглые кнопки, невыбранные круги
val DarkGreyText = Color(0xFF8A8A99)
