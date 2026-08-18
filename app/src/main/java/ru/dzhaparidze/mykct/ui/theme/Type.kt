package ru.dzhaparidze.mykct.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.dzhaparidze.mykct.R

/**
 * Inter — ближайший свободный аналог гротеска из референсов (SF Pro), с кириллицей.
 * Лежит статикой в трёх начертаниях: вариативный файл требует API 26, а у нас minSdk 24 —
 * на Android 7 вариации игнорируются и весь текст стал бы Regular.
 * Файлы получены из вариативного Inter (OFL) инстансированием на wght 400/600/700
 * и подрезаны до латиницы с кириллицей — 98 КБ вместо 856 КБ на начертание.
 */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

// Межбуквенное сжатие на крупном тексте — как в референсах: чем крупнее, тем плотнее.
val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)
