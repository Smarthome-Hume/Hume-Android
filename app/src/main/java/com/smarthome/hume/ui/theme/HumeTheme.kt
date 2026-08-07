package com.smarthome.hume.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

object HumeColors {
    var isDark by mutableStateOf(false)
        internal set

    private fun dyn(light: Color, dark: Color): Color = if (isDark) dark else light

    val Gray000: Color get() = dyn(Color(0xFFFFFFFF), Color(0xFF161616))
    val Gray00: Color get() = dyn(Color(0xFFF6F6F6), Color(0xFF313131))
    val Gray01: Color get() = dyn(Color(0xFFF6F6F6), Color(0xFF1C1C1C))
    val Gray100: Color get() = dyn(Color(0xFFCECECE), Color(0xFF2F2F2F))
    val Gray200: Color get() = dyn(Color(0xFFB5B5B5), Color(0xFF3A3A3A))
    val Gray300: Color get() = dyn(Color(0xFF9A9A9A), Color(0xFF545454))
    val Gray400: Color get() = dyn(Color(0xFF828282), Color(0xFF696969))
    val Gray500: Color get() = dyn(Color(0xFF6D6D6D), Color(0xFF7F7F7F))
    val Gray600: Color get() = dyn(Color(0xFF595959), Color(0xFF979797))
    val Gray700: Color get() = dyn(Color(0xFF484848), Color(0xFFAFAFAF))
    val Gray800: Color get() = dyn(Color(0xFF373737), Color(0xFFC7C7C7))
    val Gray900: Color get() = dyn(Color(0xFF262626), Color(0xFFEDEDED))
    val Gray1000: Color get() = dyn(Color(0xFF101010), Color(0xFFFAFAFA))

    val PageBG: Color get() = dyn(Color(0xFFFFFFFF), Color(0xFF000000))
    val Label: Color get() = dyn(Color(0xFF000000), Color(0xFFFFFFFF))
    val LabelSecondary: Color get() = dyn(Color(0x993C3C43), Color(0x99EBEBF5))
    val LabelTertiary: Color get() = dyn(Color(0x4D3C3C43), Color(0x4DEBEBF5))
    val FillTertiary: Color get() = dyn(Color(0x1F767680), Color(0x3D767680))
    val FillSecondary: Color get() = dyn(Color(0x29787880), Color(0x52787880))
    val Separator: Color get() = dyn(Color(0x4A3C3C43), Color(0xA6545458))

    val Orange = Color(0xFFF9784C)
    val OrangeDeep = Color(0xFFE8653A)
    val Salmon = Color(0xFFFAC0B6)
    val OrangePure = Color(0xFFFF7700)
    val Green = Color(0xFF4AC84F)
    val Purple = Color(0xFFAD99E6)
    val Yellow = Color(0xFFF2D26F)
    val Red = Color(0xFFF28073)
    val Blue = Color(0xFF73B9F2)
    val Pink = Color(0xFFF285C9)
    val Lime = Color(0xFFB8E674)
    val AlarmGreen = Color(0xFF4CAF50)
    val AlarmOrange = Color(0xFFFF9800)
    val AlarmBlue = Color(0xFF5C6BC0)
    val AlarmRed = Color(0xFFF28073)
    val NotifRed = Color(0xFFFF5252)
    val SolarYellow = Color(0xFFF2D26F)
    val BattGreen = Color(0xFF3EFD51)
    val BattOrange = Color(0xFFF9784C)

    /** Android has no live WallpaperBackground from Swift, so use the Swift grey ramp to keep cards visible. */
    val Background: Color get() = dyn(Color(0xFFF1F1F3), Color(0xFF000000))
    val Card: Color get() = dyn(Color(0xFFFFFFFF), Color(0xFF161616))
    val CardSunken: Color get() = Gray01
    val TextPrimary: Color get() = Label
    val TextSecondary: Color get() = LabelSecondary
    val Divider: Color get() = dyn(Color(0x1F000000), Color(0xFF2F2F2F))
    val Ink: Color get() = Gray1000
    val BarGrey: Color get() = Gray100

    /*
     * DIEM 4: nen cua VONG ICON va cac o nho trong the.
     *
     * Truoc day cac vong icon dung Background (sang: #F1F1F3, toi: #000000).
     * Tren mat the (Card: #FFFFFF / #161616) thi:
     *  - o che do toi, #000000 gan nhu bang mat the #161616 -> vong icon TAN
     *    vao nen, chinh la loi "icon bg bi chim".
     *  - o che do sang, #F1F1F3 chi cach mat the trang 6% do sang nen cung mo.
     * IconBg duoc chon lech han mot bac so voi mat the (toi: sang hon, sang:
     * dam hon) nen vong icon luon co ranh gioi ro rang.
     */
    val IconBg: Color get() = dyn(Color(0xFFEAEAEF), Color(0xFF2C2C30))
    val IconBgStrong: Color get() = dyn(Color(0xFFDEDEE5), Color(0xFF3A3A40))

    val OrangeSoft: Color get() = Orange.copy(alpha = if (isDark) 0.22f else 0.14f)
    val OrangeSofter: Color get() = Orange.copy(alpha = if (isDark) 0.14f else 0.08f)
    val ChipPink: Color get() = Orange.copy(alpha = if (isDark) 0.24f else 0.12f)
    val ChipYellow: Color get() = Yellow.copy(alpha = if (isDark) 0.24f else 0.18f)
    val ChipYellowIcon: Color get() = Yellow
    val SceneGreenBg: Color get() = AlarmGreen.copy(alpha = if (isDark) 0.22f else 0.14f)
    val SceneGreen: Color get() = AlarmGreen
    val SalmonSoft: Color get() = Salmon.copy(alpha = if (isDark) 0.28f else 0.55f)
    val Amber: Color get() = AlarmOrange
    val AmberBar: Color get() = Color(0xFFFFB74D)

    val RoomOnStart: Color get() = Orange.copy(alpha = if (isDark) 0.34f else 0.22f)
    val RoomOnEnd: Color get() = Salmon.copy(alpha = if (isDark) 0.26f else 0.55f)
}

object HumeGlass {
    val card: Color get() = if (HumeColors.isDark) Color(0xB3161616) else Color(0xF7FFFFFF)
    val edge: Color get() = if (HumeColors.isDark) Color(0x24FFFFFF) else Color(0x1F000000)
    val element: Color get() = HumeColors.FillTertiary
}

private fun schemeFor(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = Color(0xFFF9784C),
        onPrimary = Color(0xFF3A1206),
        primaryContainer = Color(0xFF7A2E14),
        onPrimaryContainer = Color(0xFFFFDBCF),
        secondary = Color(0xFFFAC0B6),
        onSecondary = Color(0xFF3A1206),
        tertiary = Color(0xFFF2D26F),
        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF161616),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF1C1C1C),
        onSurfaceVariant = Color(0xFFAFAFAF),
        outline = Color(0xFF545454),
        outlineVariant = Color(0xFF2F2F2F),
        error = Color(0xFFF28073),
    )
} else {
    lightColorScheme(
        primary = Color(0xFFF9784C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFAC0B6),
        onPrimaryContainer = Color(0xFF101010),
        secondary = Color(0xFFFAC0B6),
        onSecondary = Color(0xFF101010),
        tertiary = Color(0xFFF2D26F),
        background = Color(0xFFF1F1F3),
        onBackground = Color(0xFF000000),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFF6F6F6),
        onSurfaceVariant = Color(0xFF6D6D6D),
        outline = Color(0xFF9A9A9A),
        outlineVariant = Color(0xFFCECECE),
        error = Color(0xFFF28073),
    )
}

private val HumeMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private fun typographyFor(family: FontFamily): Typography {
    fun TextStyle.withHumeFont() = copy(fontFamily = family)
    val t = Typography()
    return Typography(
        displayLarge = t.displayLarge.withHumeFont(),
        displayMedium = t.displayMedium.withHumeFont(),
        displaySmall = t.displaySmall.withHumeFont(),
        headlineLarge = t.headlineLarge.withHumeFont(),
        headlineMedium = t.headlineMedium.withHumeFont(),
        headlineSmall = t.headlineSmall.withHumeFont(),
        titleLarge = t.titleLarge.withHumeFont(),
        titleMedium = t.titleMedium.withHumeFont(),
        titleSmall = t.titleSmall.withHumeFont(),
        bodyLarge = t.bodyLarge.withHumeFont(),
        bodyMedium = t.bodyMedium.withHumeFont(),
        bodySmall = t.bodySmall.withHumeFont(),
        labelLarge = t.labelLarge.withHumeFont(),
        labelMedium = t.labelMedium.withHumeFont(),
        labelSmall = t.labelSmall.withHumeFont(),
    )
}

/*
 * VI SAO MAT MONTSERRAT:
 * MaterialTheme chi dat Typography cho cac component cua Material. Text() goi
 * truc tiep (gan nhu toan bo man hinh nay) lai doc LocalTextStyle, ma
 * MaterialTheme KHONG he cung cap LocalTextStyle -> moi chu do roi ve font he
 * thong (Roboto). Nen o day phai tu cung cap LocalTextStyle mang fontFamily
 * Montserrat thi ca app moi dung font.
 */
@Composable
fun HumeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    SideEffect { HumeColors.isDark = darkTheme }
    val context = LocalContext.current
    val fontFamily = remember(context) { humeFontFamily(context) }
    val typography = remember(fontFamily) { typographyFor(fontFamily) }
    MaterialTheme(
        colorScheme = schemeFor(darkTheme),
        typography = typography,
        shapes = HumeMaterialShapes,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides typography.bodyLarge.copy(fontFamily = fontFamily),
            content = content,
        )
    }
}
