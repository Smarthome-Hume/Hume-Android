package com.smarthome.hume.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/*
 * DIEM 3: bong den va pin ve lai cho giong ban HTML.
 *
 * Ban cu (LIGHTBULB va BATTERY_PATH trong HumePhosphorIcons.kt) ve bong den
 * bang mot cung tron ban kinh 60 tren mot chord chi 72 -> sagitta gan 118 nen
 * bau den bi keo dai nhu vien thuoc, day lai phang. Pin thi chi la mot hinh
 * chu nhat goc vuong, thieu bo goc va cuc duong dung kich thuoc.
 *
 * Phosphor 2.1.1 (ph-lightbulb, ph-battery-charging, ph-battery-full) ma ban
 * HTML nap co dang:
 *  - bong den: bau TRON o tren (ban kinh 60, tam 128,100), than thu nhe xuong
 *    y=168, roi hai vach ren o de.
 *  - pin: than hinh chu nhat BO GOC 16, cuc duong la vach dung ngan ben phai;
 *    ban charging co tia set zigzag, ban full co ba vach doc.
 *
 * Ten mang path phai KHAC ten property trong object (loi "recursive problem"
 * da gap truoc day), nen dat hau to _HTML.
 */
private const val HTML_STROKE = 16f

private fun htmlIcon(name: String, vararg pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 256f,
        viewportHeight = 256f,
    ).apply {
        pathData.forEach { data ->
            addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = HTML_STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** ph-lightbulb: bau tron + than thu nhe + hai vach ren o de. */
private val LIGHTBULB_HTML = arrayOf(
    "M128,40 A60,60 0 0 1 188,100 C188,132 168,152 160,168 L96,168 C88,152 68,132 68,100 A60,60 0 0 1 128,40 Z",
    "M100,196 L156,196",
    "M108,220 L148,220",
)

/** Than pin bo goc 16, dung chung cho ban charging va full. */
private const val BATTERY_BODY_HTML =
    "M40,80 L152,80 A16,16 0 0 1 168,96 L168,160 A16,16 0 0 1 152,176 L40,176 " +
        "A16,16 0 0 1 24,160 L24,96 A16,16 0 0 1 40,80 Z"

/** Cuc duong ben phai. */
private const val BATTERY_TERMINAL_HTML = "M192,108 L192,148"

/** ph-battery-charging: them tia set trong than pin. */
private val BATTERY_CHARGING_HTML = arrayOf(
    BATTERY_BODY_HTML,
    BATTERY_TERMINAL_HTML,
    "M108,100 L80,132 L112,132 L84,156",
)

/** ph-battery-full: ba vach doc trong than pin. */
private val BATTERY_FULL_HTML = arrayOf(
    BATTERY_BODY_HTML,
    BATTERY_TERMINAL_HTML,
    "M60,108 L60,148",
    "M96,108 L96,148",
    "M132,108 L132,148",
)

/** Cac icon da ve lai dung theo ban HTML. */
object PhHtml {
    val Lightbulb: ImageVector = htmlIcon("ph-lightbulb", *LIGHTBULB_HTML)
    val BatteryCharging: ImageVector = htmlIcon("ph-battery-charging", *BATTERY_CHARGING_HTML)
    val BatteryFull: ImageVector = htmlIcon("ph-battery-full", *BATTERY_FULL_HTML)
}
