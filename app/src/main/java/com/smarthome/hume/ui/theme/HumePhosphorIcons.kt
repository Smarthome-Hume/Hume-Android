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
 * index.html cua ban HTML nap dung hai weight cua Phosphor Icons 2.1.1:
 *   <link ... @phosphor-icons/web@2.1.1/src/thin/style.css>
 *   <link ... @phosphor-icons/web@2.1.1/src/regular/style.css>
 * tuc la Phosphor REGULAR (net 16 tren khung 256) va THIN (net 8), khong dung
 * ban fill/duotone. Moi icon trong bundle la <i class="ph ph-...">.
 *
 * Material Rounded ben Android la ban FILLED bo goc - do la ly do man hinh day
 * icon dac va tho. Nen o day khung 256 cua Phosphor duoc dung lai nguyen ven,
 * moi icon noi dung ve bang NET, dau va khop deu bo tron.
 *
 *  - Regular: net 16 - dung cho moi icon noi dung.
 *  - Thin:    net 8  - dung cho icon phu, khi can mang hon.
 *  - Fill:    to DAC - CHI dung cho tab dang chon tren navbar.
 */
private const val REGULAR = 16f
private const val THIN = 8f

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

/* --- Bo sung: thay cho cac icon Material dac con sot lai --- */

/** ph-caret-right */
private val CARET_RIGHT = arrayOf("M96,48 L176,128 L96,208")

/** ph-caret-left */
private val CARET_LEFT = arrayOf("M160,48 L80,128 L160,208")

/** ph-caret-up */
private val CARET_UP = arrayOf("M48,168 L128,88 L208,168")

/** ph-caret-down */
private val CARET_DOWN = arrayOf("M208,88 L128,168 L48,88")

/** ph-download-simple */
private val DOWNLOAD = arrayOf(
    "M74,102 L128,156 L182,102",
    "M128,32 L128,156",
    "M40,200 L216,200",
)

/** ph-play */
private val PLAY = arrayOf("M72,40 L200,128 L72,216 Z")

/** ph-video-camera */
private val VIDEO_CAMERA = arrayOf(
    "M24,72 L164,72 L164,184 L24,184 Z",
    "M164,116 L232,80 L232,176 L164,140",
)

/** ph-person-simple-walk */
private val WALK = arrayOf(
    "M132,32 A16,16 0 1 0 132,64 A16,16 0 1 0 132,32",
    "M120,80 L120,132 L92,224",
    "M120,104 L172,124",
    "M120,132 L156,224",
)

/** ph-identification-badge */
private val BADGE = arrayOf(
    "M40,48 L216,48 L216,208 L40,208 Z",
    "M96,80 L160,80",
    "M128,116 A20,20 0 1 0 128,156 A20,20 0 1 0 128,116",
    "M96,184 A40,40 0 0 1 160,184",
)

/** ph-envelope-simple */
private val ENVELOPE = arrayOf(
    "M32,64 L224,64 L224,192 L32,192 Z",
    "M32,64 L128,144 L224,64",
)

/** ph-phone */
private val PHONE = arrayOf(
    "M92,40 L120,96 L96,120 C112,152 104,144 136,160 L160,136 L216,164 C216,196 196,216 168,216 C96,208 48,160 40,88 C40,60 60,40 92,40 Z",
)

/** ph-map-pin */
private val MAP_PIN = arrayOf(
    "M128,32 A72,72 0 0 1 200,104 C200,160 128,224 128,224 C128,224 56,160 56,104 A72,72 0 0 1 128,32 Z",
    "M128,80 A24,24 0 1 0 128,128 A24,24 0 1 0 128,80",
)

/** ph-copy */
private val COPY = arrayOf(
    "M80,80 L216,80 L216,216 L80,216 Z",
    "M176,80 L176,40 L40,40 L40,176 L80,176",
)

/** ph-pencil-simple */
private val PENCIL = arrayOf(
    "M96,216 L40,216 L40,160 L160,40 L216,96 Z",
    "M136,64 L192,120",
)

/** ph-chat-circle-dots */
private val CHAT = arrayOf(
    "M128,32 A96,96 0 1 0 128,224 A96,96 0 1 0 128,32",
    "M96,128 L96,128",
    "M128,128 L128,128",
    "M160,128 L160,128",
)

/** ph-device-mobile */
private val DEVICE_MOBILE = arrayOf(
    "M64,24 L192,24 L192,232 L64,232 Z",
    "M104,48 L152,48",
)

/** ph-magic-wand */
private val MAGIC_WAND = arrayOf(
    "M56,200 L200,56",
    "M176,32 L184,56 L208,64 L184,72 L176,96 L168,72 L144,64 L168,56 Z",
    "M64,48 L64,80",
    "M48,64 L80,64",
    "M176,176 L176,208",
    "M160,192 L192,192",
)

/** ph-x */
private val X = arrayOf("M64,64 L192,192", "M192,64 L64,192")

/** ph-plus */
private val PLUS = arrayOf("M40,128 L216,128", "M128,40 L128,216")

/** ph-minus */
private val MINUS = arrayOf("M40,128 L216,128")

/** ph-check */
private val CHECK = arrayOf("M216,72 L104,184 L48,128")

/** ph-lock-simple */
private val LOCK = arrayOf(
    "M64,112 L192,112 L192,208 L64,208 Z",
    "M92,112 L92,72 A36,36 0 0 1 164,72 L164,112",
)

/** ph-gear */
private val GEAR = arrayOf(
    "M128,88 A40,40 0 1 0 128,168 A40,40 0 1 0 128,88",
    "M128,24 L128,56",
    "M128,200 L128,232",
    "M24,128 L56,128",
    "M200,128 L232,128",
    "M52,52 L74,74",
    "M182,182 L204,204",
    "M204,52 L182,74",
    "M74,182 L52,204",
)

/** ph-warning */
private val WARNING = arrayOf(
    "M128,32 L232,216 L24,216 Z",
    "M128,88 L128,148",
    "M128,184 L128,184",
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

    val CaretRight = phosphor("ph-caret-right", REGULAR, *CARET_RIGHT)
    val CaretLeft = phosphor("ph-caret-left", REGULAR, *CARET_LEFT)
    val CaretUp = phosphor("ph-caret-up", REGULAR, *CARET_UP)
    val CaretDown = phosphor("ph-caret-down", REGULAR, *CARET_DOWN)
    val Download = phosphor("ph-download-simple", REGULAR, *DOWNLOAD)
    val Play = phosphor("ph-play", REGULAR, *PLAY)
    val VideoCamera = phosphor("ph-video-camera", REGULAR, *VIDEO_CAMERA)
    val Walk = phosphor("ph-person-simple-walk", REGULAR, *WALK)
    val Badge = phosphor("ph-identification-badge", REGULAR, *BADGE)
    val Envelope = phosphor("ph-envelope-simple", REGULAR, *ENVELOPE)
    val Phone = phosphor("ph-phone", REGULAR, *PHONE)
    val MapPin = phosphor("ph-map-pin", REGULAR, *MAP_PIN)
    val Copy = phosphor("ph-copy", REGULAR, *COPY)
    val Pencil = phosphor("ph-pencil-simple", REGULAR, *PENCIL)
    val Chat = phosphor("ph-chat-circle-dots", REGULAR, *CHAT)
    val DeviceMobile = phosphor("ph-device-mobile", REGULAR, *DEVICE_MOBILE)
    val MagicWand = phosphor("ph-magic-wand", REGULAR, *MAGIC_WAND)
    val X = phosphor("ph-x", REGULAR, *X)
    val Plus = phosphor("ph-plus", REGULAR, *PLUS)
    val Minus = phosphor("ph-minus", REGULAR, *MINUS)
    val Check = phosphor("ph-check", REGULAR, *CHECK)
    val Lock = phosphor("ph-lock-simple", REGULAR, *LOCK)
    val Gear = phosphor("ph-gear", REGULAR, *GEAR)
    val Warning = phosphor("ph-warning", REGULAR, *WARNING)

    /** Ban net MANH hon (Phosphor thin) cho icon phu. */
    val CaretRightThin = phosphor("ph-caret-right-thin", THIN, *CARET_RIGHT)
    val CaretUpThin = phosphor("ph-caret-up-thin", THIN, *CARET_UP)
    val CaretDownThin = phosphor("ph-caret-down-thin", THIN, *CARET_DOWN)

    /** Ban TO DAC cho tab dang chon. */
    val HouseFill = phosphorFill("ph-house-fill", *HOUSE_FILL)
    val LightningFill = phosphorFill("ph-lightning-fill", *LIGHTNING_FILL)
    val ShieldFill = phosphorFill("ph-shield-fill", *SHIELD_FILL)
    val UserFill = phosphorFill("ph-user-fill", *USER_FILL)
    val SparkleFill = phosphorFill("ph-sparkle-fill", *SPARKLE_FILL)
}
