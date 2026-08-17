package ru.dzhaparidze.mykct.ui.theme

import androidx.compose.ui.graphics.Color

// Токены сняты с референса (college-app-core-рефы/a1.webp).
val Purple = Color(0xFF6C4CC4)      // карточки пар, акцент
val Lavender = Color(0xFFE7EAFA)    // фон шапки
val Orange = Color(0xFFF5A623)      // выбранный день
val Green = Color(0xFF00C08B)       // кольцо прогресса

val GreyFill = Color(0xFFECECEC)    // невыбранные круги дней
// Оттенок с референса, но затемнён: исходный #9A9AA5 давал на белом 2.8:1 при норме
// WCAG AA 4.5:1 — подписи дней и время в таймлайне читались плохо. Этот даёт 5.0:1.
val GreyText = Color(0xFF6E6E7A)    // подписи, время в таймлайне
val Ink = Color(0xFF16161D)         // основной текст

val DarkBackground = Color(0xFF121217)
val DarkSurface = Color(0xFF1C1C24)
val DarkLavender = Color(0xFF262636)
val DarkGreyFill = Color(0xFF2A2A34)
val DarkGreyText = Color(0xFF8E8E9A)
