package io.legado.app.ui.compose.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val md_light_blue_600 = Color(0xFF039BE5)
val md_light_blue_700 = Color(0xFF0288D1)
val md_pink_800 = Color(0xFFC2185B)
val md_grey_50 = Color(0xFFFAFAFA)
val md_grey_100 = Color(0xFFF5F5F5)
val md_grey_200 = Color(0xFFEEEEEE)
val md_light_disabled = Color(0xFFBDBDBD)

val LightPrimary = md_light_blue_600
val LightPrimaryDark = md_light_blue_700
val LightAccent = md_pink_800
val LightBackground = md_grey_50
val LightSurface = Color.White
val LightOnPrimary = Color.White
val LightOnSecondary = Color.White
val LightOnBackground = Color(0xFF212121)
val LightOnSurface = Color(0xFF212121)

val DarkPrimary = Color(0xFF4FC3F7)
val DarkPrimaryDark = Color(0xFF0288D1)
val DarkAccent = Color(0xFFFF80AB)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnPrimary = Color.Black
val DarkOnSecondary = Color.Black
val DarkOnBackground = Color(0xFFE0E0E0)
val DarkOnSurface = Color(0xFFE0E0E0)

val Error = Color(0xFFEB4333)
val Success = Color(0xFF439B53)
val Disabled = md_light_disabled
val Divider = Color(0x66666666)
val PrimaryText = Color(0xDE000000)
val SecondaryText = Color(0x8A000000)
val BackgroundMenu = md_grey_200
val BackgroundCard = md_grey_100

val LocalContentColor = staticCompositionLocalOf { Color.Unspecified }
