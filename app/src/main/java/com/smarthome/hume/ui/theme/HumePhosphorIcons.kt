package com.smarthome.hume.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/*
 * BO ICON PORT TU BAN HTML (cocopi) SANG COMPOSE.
 *
 * Ban HTML dung Phosphor Icons: trong bundle moi icon la <i class="ph ph-...">,
 * ve tren khung viewBox 0 0 256 256, net deu, dau bo tron, KHONG DAC.
 * Material Rounded ben Android lai la ban FILLED bo goc - do la ly do man hinh
 * day icon dac. Nen o day khung 256 cua Phosphor duoc dung lai nguyen ven va
 * moi icon noi dung ve bang NET (stroke) thay vi to dac.
 *
 *  - Regular: net 16 (giong Phosphor regular) - dung cho moi icon noi dung.
 *  - Fill:    to DAC (Phosphor fill)          - CHI dung cho tab dang chon.
 *
 * Icon.tint van doi mau binh thuong vi Compose to mau ca stroke lan fill.
 */
private const val REGULAR = 16f

private fun phosphor(
    name: String,
    strokeWidth: Float = REGULAR,
    vararg pathData: String,
): ImageVector = ImageVector.Builder(
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
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()

/** Ban TO DAC (Phosphor fill) - dung EvenOdd de khoet lo ben trong (vd cua nha). */
private fun phosphorFill(
    name: String,
    vararg pathData: String,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 256f,
    viewportHeight = 256f,
).apply {
    pathData.forEach { data ->
        addPath(
            pathData = PathParser().parsePathString(data).toNodes(),
            pathFillType = PathFillType.EvenOdd,
            fill = SolidColor(Color.Black),
            stroke = null,
        )
    }
}.build()

/** ph-lightbulb */
private val LIGHTBULB = arrayOf(
    "M92,168 L92,148 A60,60 0 1 1 164,148 L164,168 Z",
    "M100,196 L156,196",
    "M108,220 L148,220",
)

/** ph-drop */
private val DROP = arrayOf(
    "M128,40 C92,84 72,112 72,144 A56,56 0 0 0 184,144 C184,112 164,84 128,40 Z",
)

/** ph-snowflake */
private val SNOWFLAKE = arrayOf(
    "M128,24 L128,232",
    "M38,76 L218,180",
    "M218,76 L38,180",
    "M104,48 L128,72 L152,48",
    "M104,208 L128,184 L152,208",
)

/** ph-thermometer */
private val THERMOMETER = arrayOf(
    "M112,152 L112,56 A16,16 0 0 1 144,56 L144,152 A28,28 0 1 1 112,152 Z",
)

/** ph-door-open */
private val DOOR_OPEN = arrayOf(
    "M64,224 L64,48 L168,24 L168,224",
    "M32,224 L224,224",
    "M144,132 L144,132",
)

/** ph-door */
private val DOOR = arrayOf(
    "M56,224 L56,32 L200,32 L200,224",
    "M32,224 L224,224",
    "M168,132 L168,132",
)

/** ph-shield */
private val SHIELD = arrayOf(
    "M48,56 L128,32 L208,56 L208,120 C208,176 168,206 128,224 C88,206 48,176 48,120 Z",
)

/** ph-shield-check */
private val SHIELD_CHECK = arrayOf(
    "M48,56 L128,32 L208,56 L208,120 C208,176 168,206 128,224 C88,206 48,176 48,120 Z",
    "M96,124 L120,148 L164,104",
)

/** ph-bell */
private val BELL = arrayOf(
    "M56,184 C70,166 72,150 72,112 A56,56 0 0 1 184,112 C184,150 186,166 200,184 Z",
    "M100,208 A28,28 0 0 0 156,208",
)

/** ph-moon-stars */
private val MOON = arrayOf(
    "M216,152 A96,96 0 1 1 104,40 A80,80 0 0 0 216,152 Z",
)

/** ph-sun */
private val SUN = arrayOf(
    "M128,72 A56,56 0 1 0 128,184 A56,56 0 1 0 128,72",
    "M128,20 L128,44",
    "M128,212 L128,236",
    "M20,128 L44,128",
    "M212,128 L236,128",
    "M52,52 L68,68",
    "M188,188 L204,204",
    "M204,52 L188,68",
    "M68,188 L52,204",
)

/** ph-sun-horizon */
private val SUN_HORIZON = arrayOf(
    "M64,168 A64,64 0 0 1 192,168",
    "M24,200 L232,200",
    "M128,40 L128,64",
    "M52,72 L68,88",
    "M204,72 L188,88",
)

/** ph-house */
private val HOUSE = arrayOf(
    "M40,216 L40,112 L128,40 L216,112 L216,216 Z",
    "M100,216 L100,160 L156,160 L156,216",
)

/** ph-lightning */
private val LIGHTNING = arrayOf(
    "M96,240 L112,144 L48,120 L160,16 L144,112 L208,136 Z",
)

/** ph-battery-charging */
private val BATTERY = arrayOf(
    "M24,80 L160,80 L160,176 L24,176 Z",
    "M184,108 L184,148",
    "M96,96 L72,132 L112,132 L88,160",
)

/** ph-plug */
private val PLUG = arrayOf(
    "M96,24 L96,88",
    "M160,24 L160,88",
    "M72,88 L184,88 L184,120 A56,56 0 0 1 72,120 Z",
    "M128,176 L128,232",
)

/** ph-bed */
private val BED = arrayOf(
    "M32,88 L32,200",
    "M32,152 L224,152 L224,200",
    "M64,120 L184,120 A32,32 0 0 1 216,152",
)

/** ph-baby */
private val BABY = arrayOf(
    "M128,40 A88,88 0 1 0 128,216 A88,88 0 1 0 128,40",
    "M100,112 L100,112",
    "M156,112 L156,112",
    "M96,152 A44,44 0 0 0 160,152",
)

/** ph-couch */
private val COUCH = arrayOf(
    "M40,120 A16,16 0 0 1 72,120 L72,152 L184,152 L184,120 A16,16 0 0 1 216,120 L216,192 L40,192 Z",
    "M64,96 A16,16 0 0 1 80,80 L176,80 A16,16 0 0 1 192,96",
    "M64,192 L64,216",
    "M192,192 L192,216",
)

/** ph-bathtub */
private val BATHTUB = arrayOf(
    "M32,120 L224,120 L224,152 A48,48 0 0 1 176,200 L80,200 A48,48 0 0 1 32,152 Z",
    "M64,200 L56,224",
    "M192,200 L200,224",
    "M80,120 L80,64 A24,24 0 0 1 128,64",
)

/** ph-cooking-pot */
private val COOKING_POT = arrayOf(
    "M56,96 L200,96 L200,152 A40,40 0 0 1 160,192 L96,192 A40,40 0 0 1 56,152 Z",
    "M32,120 L56,120",
    "M200,120 L224,120",
    "M96,64 L96,40",
    "M128,56 L128,32",
    "M160,64 L160,40",
)

/** ph-washing-machine */
private val WASHER = arrayOf(
    "M48,32 L208,32 L208,224 L48,224 Z",
    "M48,80 L208,80",
    "M128,112 A48,48 0 1 0 128,208 A48,48 0 1 0 128,112",
    "M84,56 L84,56",
    "M120,56 L120,56",
)

/** ph-stairs */
private val STAIRS = arrayOf(
    "M32,208 L32,160 L96,160 L96,112 L160,112 L160,64 L224,64",
)

/** ph-user */
private val USER = arrayOf(
    "M128,40 A44,44 0 1 0 128,128 A44,44 0 1 0 128,40",
    "M40,212 A100,100 0 0 1 216,212",
)

/** ph-sparkle */
private val SPARKLE = arrayOf(
    "M128,28 L148,108 L228,128 L148,148 L128,228 L108,148 L28,128 L108,108 Z",
)

/** ph-fire-simple */
private val FIRE = arrayOf(
    "M128,232 A72,72 0 0 0 200,160 C200,100 156,72 128,24 C100,72 56,100 56,160 A72,72 0 0 0 128,232 Z",
)

/** ph-sign-out */
private val SIGN_OUT = arrayOf(
    "M112,216 L48,216 L48,40 L112,40",
    "M176,168 L216,128 L176,88",
    "M104,128 L216,128",
)

/** ph-desk */
private val DESK = arrayOf(
    "M24,96 L232,96",
    "M56,96 L56,208",
    "M200,96 L200,208",
    "M88,136 L168,136",
)

/* --- Ban FILL cho tab dang chon (ph-*-fill) --- */

/** ph-house-fill: khoi nha dac, cua duoc khoet bang EvenOdd. */
private val HOUSE_FILL = arrayOf(
    "M40,216 L40,112 L128,40 L216,112 L216,216 Z M104,216 L104,164 L152,164 L152,216 Z",
)

/** ph-lightning-fill */
private val LIGHTNING_FILL = arrayOf(
    "M96,240 L112,144 L48,120 L160,16 L144,112 L208,136 Z",
)

/** ph-shield-fill */
private val SHIELD_FILL = arrayOf(
    "M48,56 L128,32 L208,56 L208,120 C208,176 168,206 128,224 C88,206 48,176 48,120 Z",
)

/** ph-user-fill */
private val USER_FILL = arrayOf(
    "M128,36 A46,46 0 1 0 128,128 A46,46 0 1 0 128,36 Z",
    "M128,144 C86,144 50,172 38,212 L218,212 C206,172 170,144 128,144 Z",
)

/** ph-sparkle-fill */
private val SPARKLE_FILL = arrayOf(
    "M128,28 L148,108 L228,128 L148,148 L128,228 L108,148 L28,128 L108,108 Z",
)

/** Bo icon Phosphor da port. */
object Ph {
    val Lightbulb = phosphor("ph-lightbulb", REGULAR, *LIGHTBULB)
    val Drop = phosphor("ph-drop", REGULAR, *DROP)
    val Snowflake = phosphor("ph-snowflake", REGULAR, *SNOWFLAKE)
    val Thermometer = phosphor("ph-thermometer", REGULAR, *THERMOMETER)
    val DoorOpen = phosphor("ph-door-open", REGULAR, *DOOR_OPEN)
    val Door = phosphor("ph-door", REGULAR, *DOOR)
    val Shield = phosphor("ph-shield", REGULAR, *SHIELD)
    val ShieldCheck = phosphor("ph-shield-check", REGULAR, *SHIELD_CHECK)
    val Bell = phosphor("ph-bell", REGULAR, *BELL)
    val Moon = phosphor("ph-moon-stars", REGULAR, *MOON)
    val Sun = phosphor("ph-sun", REGULAR, *SUN)
    val SunHorizon = phosphor("ph-sun-horizon", REGULAR, *SUN_HORIZON)
    val House = phosphor("ph-house", REGULAR, *HOUSE)
    val Lightning = phosphor("ph-lightning", REGULAR, *LIGHTNING)
    val Battery = phosphor("ph-battery-charging", REGULAR, *BATTERY)
    val Plug = phosphor("ph-plug", REGULAR, *PLUG)
    val Bed = phosphor("ph-bed", REGULAR, *BED)
    val Baby = phosphor("ph-baby", REGULAR, *BABY)
    val Couch = phosphor("ph-couch", REGULAR, *COUCH)
    val Bathtub = phosphor("ph-bathtub", REGULAR, *BATHTUB)
    val CookingPot = phosphor("ph-cooking-pot", REGULAR, *COOKING_POT)
    val Washer = phosphor("ph-washing-machine", REGULAR, *WASHER)
    val Stairs = phosphor("ph-stairs", REGULAR, *STAIRS)
    val User = phosphor("ph-user", REGULAR, *USER)
    val Sparkle = phosphor("ph-sparkle", REGULAR, *SPARKLE)
    val Fire = phosphor("ph-fire-simple", REGULAR, *FIRE)
    val SignOut = phosphor("ph-sign-out", REGULAR, *SIGN_OUT)
    val Desk = phosphor("ph-desk", REGULAR, *DESK)

    /** Ban TO DAC cho tab dang chon. */
    val HouseFill = phosphorFill("ph-house-fill", *HOUSE_FILL)
    val LightningFill = phosphorFill("ph-lightning-fill", *LIGHTNING_FILL)
    val ShieldFill = phosphorFill("ph-shield-fill", *SHIELD_FILL)
    val UserFill = phosphorFill("ph-user-fill", *USER_FILL)
    val SparkleFill = phosphorFill("ph-sparkle-fill", *SPARKLE_FILL)
}
