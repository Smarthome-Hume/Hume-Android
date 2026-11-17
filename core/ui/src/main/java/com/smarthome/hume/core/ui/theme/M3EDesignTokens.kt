package com.smarthome.hume.core.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Token giao dien duy nhat toan he thong (2026-11-18, vong chuan hoa token).
 *
 * Can cu: `~/workspace/audit-ui-tokens-2026-11-18.md` — moi so deu do san o do,
 * khong do lai. Android `feat/m3e-dashboard` @ 36ea51c; iOS doi chieu o
 * `feat/dynamic-rooms` @ 62ca547 (v79).
 *
 * NGUYEN TAC COT LOI — khe theo TI LE, khong cat so dp tuyet doi:
 * Vong cat 18/11 da chung minh cat khe/padding 40-50% trong khi chu giu
 * nguyen se lam the "day sat" (CostCard: khe label->gia tri 2dp so voi
 * gia tri 20sp, ti le 0,10 — bang 1/2 iOS). Tu day moi khe doc = he so x
 * co chu cua thanh phan ke (xem [Ratio]); muon the ngan lai thi THU NHO
 * thanh phan theo ti le, khong bop khe. He so lay tu ti le goc iOS
 * (vd khe trong stat = 0,20 x value) roi lam tron ve so dp sach.
 *
 * QUY TAC LINEHEIGHT: moi style dung lineHeight = fontSize x 1,219
 * (line height tu nhien cua Montserrat: ascent 968 / descent -251 / UPM
 * 1000, do tu Hume/Fonts/Montserrat-*.ttf). KHONG dung lineHeight mac
 * dinh M3 (1,43-1,5x, vd 14/20, 12/16), KHONG dung so le tu dat
 * (Stat2 LH 20 = 1,0x, PBox LH 15,5). iOS khong set lineHeight nen
 * SwiftUI dung tu nhien 1,219x — Android phai set tuong minh moi khop.
 *
 * FONT FAMILY: Android dung Montserrat cho moi style (chuan app, doi
 * qua ThemeStore.fontFamily). iOS PackSheet dung `.system(size:...)`
 * thay vi Montserrat — do la lech da co tu truoc, token chi chot CO CHU
 * / lineHeight, khong doi font family iOS o day.
 */

/** Mot cap co chu + lineHeight (lineHeight LUON = fontSize x 1,219). */
data class M3ETextSpec(val fontSize: TextUnit, val lineHeight: TextUnit)

object M3ETokens {

    /** Ti le lineHeight tu nhien cua Montserrat (xem header file). */
    const val LineHeightRatio: Float = 1.219f

    /** LineHeight tu nhien cho mot co chu bat ky (dung khi khong co spec san). */
    fun naturalLineHeight(fontSize: TextUnit): TextUnit = fontSize * LineHeightRatio

    // ---------- Thang chu (fontSize / lineHeight = size x 1,219) ----------
    object Text {
        /** Title the — chuan da chot voi user 18/11 (17sp, ca Android + iOS). */
        val CardTitle = M3ETextSpec(17.sp, 20.7.sp)

        /** Title muc (M3ESectionTitle, vd "Goi y cho ban", "Phong") — 16sp. */
        val SectionTitle = M3ETextSpec(16.sp, 19.5.sp)

        /**
         * Title TRANG (pill "Nang luong" EnergyScreen 20sp, Powerwall 18sp).
         * Khong phai title the — chua co chuan rieng, ghi lai de worker sau
         * quyet (audit muc 11.8). Tam giu 20/24,4 theo EnergyScreen hien tai.
         */
        val PageTitle = M3ETextSpec(20.sp, 24.4.sp)

        /** Title sheet (LightsSheet / ChartDetail / RoomSheet / EcoDialog). */
        val SheetTitle = M3ETextSpec(24.sp, 29.3.sp)

        /** Body chinh. */
        val Body = M3ETextSpec(14.sp, 17.1.sp)

        /** Body nho / sub. */
        val BodySmall = M3ETextSpec(12.sp, 14.6.sp)

        /** Caption / nhan nho nhat. */
        val Caption = M3ETextSpec(11.sp, 13.4.sp)

        /** Nhan stat (vd "Dien luoi" trong CostCard). */
        val StatLabel = M3ETextSpec(12.sp, 14.6.sp)

        /**
         * Gia tri stat trong CostCard — 16sp (DA THU tu 20sp o phan B de
         * khe bop truoc do tro lai dung ti le; xem [CostCard]). Gia tri
         * stat o the khac van 20sp = [StatValueLarge].
         */
        val StatValue = M3ETextSpec(16.sp, 19.5.sp)

        /** Gia tri stat goc iOS / cac the khac (20sp, vd WeekCard tong kWh 32sp thi xem Hero). */
        val StatValueLarge = M3ETextSpec(20.sp, 24.4.sp)

        /** Nhan PBox trong CostCard (vd "Gia mua"). */
        val PBoxLabel = M3ETextSpec(11.sp, 13.4.sp)

        /** Gia tri PBox trong CostCard — 12sp (da thu tu 14sp o phan B). */
        val PBoxValue = M3ETextSpec(12.sp, 14.6.sp)

        /** Chu chip / badge / pill. */
        val ChipText = M3ETextSpec(11.sp, 13.4.sp)

        /** Chu nut chinh. */
        val ButtonText = M3ETextSpec(14.sp, 17.1.sp)

        /** Chu nut phu / nut nho. */
        val ButtonTextSmall = M3ETextSpec(12.sp, 14.6.sp)

        /** So hero trong the (tong kWh WeekCard / DonutCard). */
        val HeroNumber = M3ETextSpec(32.sp, 39.0.sp)

        /** So hero lon (gio con lai BatteryCard). */
        val HeroLarge = M3ETextSpec(45.sp, 54.9.sp)

        /** So hero cuc lon (Dishwasher). */
        val DisplayHero = M3ETextSpec(57.sp, 69.5.sp)
    }

    // ---------- Thang spacing (dp tuyet doi) ----------
    object Spacing {
        val Xs: Dp = 4.dp
        val Sm: Dp = 8.dp
        val Md: Dp = 12.dp
        val Lg: Dp = 16.dp
        val Xl: Dp = 20.dp

        /**
         * Khe giua cac the. GIA TRI MIRROR cua `M3ECardGap` trong
         * `core/ui/.../components/M3EPageSpacing.kt` (10dp, user cat
         * 14->10 ngay 22/03) — gia tri chuan van nam o do, day chi la
         * ban ghi de token file day du, khong dung thay the.
         * iOS `M3ESpacing.cardGap` van 14pt (lech da co, chua quyet).
         */
        val CardGap: Dp = 10.dp

        /** Khe ngang giua cac o trong mot hang (Stat2 / PBox row). */
        val RowGap: Dp = 10.dp
    }

    // ---------- Quy tac ti le khe / thanh phan ----------
    //
    // Cach dung: khe(dp) = he so x co chu(sp) cua thanh phan ke, lam tron
    // ve so dp sach gan nhat. He so chon sao cho khe khong qua ngan so voi
    // thanh phan — nguong toi thieu: khe title->noi dung >= 0,6 x co title.
    // Moi he so deu co goc iOS (audit muc 9): khong dat so le tu nghi ra.
    object Ratio {
        /** Khe title the -> noi dung = 0,60 x co title (iOS 12/20). */
        const val TitleToContent: Float = 0.60f

        /** Khe nhan -> gia tri trong stat = 0,20 x co gia tri (iOS 4/20). */
        const val LabelToValue: Float = 0.20f

        /**
         * Khe giua 2 cum trong the = 0,70 x co gia tri cua cum SAU
         * (iOS Stat2->PBox 10/14 = 0,71).
         */
        const val SectionGap: Float = 0.70f

        /** Padding trong o stat = 0,70 x co gia tri (iOS Stat2 14/20). */
        const val StatPadding: Float = 0.70f

        /** Padding trong o PBox = 0,85 x co gia tri (iOS 12/14 = 0,86). */
        const val PBoxPadding: Float = 0.85f

        /** Khe nhan -> gia tri trong PBox = 0,30 x co gia tri (iOS 4/14 = 0,29). */
        const val PBoxLabelToValue: Float = 0.30f

        /** contentPadding the = 1,00 x co title (iOS 20/20). */
        const val CardPadding: Float = 1.00f
    }

    // ---------- Bo goc ----------
    object Corner {
        /** The chuan (HumeShapes.medium). */
        val Card: Dp = 28.dp

        /** The lon (Week / Cost / Power / Devices / Sunsynk / Water / Flow...). */
        val CardLarge: Dp = 32.dp

        /** DonutCard. */
        val Donut: Dp = 40.dp

        /** Stat2 / PBox. */
        val Stat: Dp = 22.dp

        /** The noi sheet (Room / Lights / Chart / Pack). */
        val SheetInner: Dp = 20.dp

        /** MiniBox / FlowNode. */
        val SmallBox: Dp = 20.dp

        /** WaterSensorCard. */
        val Sensor: Dp = 26.dp

        /** InnerCard PackSheet. */
        val InnerCard: Dp = 18.dp

        /** SocCell / ValueCellNum PackSheet. */
        val Cell: Dp = 16.dp

        /** Hop ClimateCard / nen Stepper. */
        val ClimateBox: Dp = 24.dp

        /**
         * EnvTile bo 30dp — NGOAI LE, giu nguyen, khong doi theo token.
         * iOS EnvTile bo 24pt (lech 6pt da co tu truoc, audit muc 11.5);
         * vong cat 18/11 da giu 30dp khi cat padding EnvTile 14->8dp.
         */
        val EnvTile: Dp = 30.dp
    }

    // ---------- Icon size ----------
    object Icon {
        /** SuggestCard icon (da cat 30->22dp vong truoc). */
        val SuggestCard: Dp = 22.dp

        /** EnvTile: box 36dp / icon 20dp (da cat 48->36 / 24->20). */
        val EnvTileBox: Dp = 36.dp
        val EnvTile: Dp = 20.dp

        /** Stepper (Solar) nut 32dp tron; StepperButton (Climate) 34dp. */
        val StepperButton: Dp = 32.dp
        val ClimateStepperButton: Dp = 34.dp
        val ClimateStepperIcon: Dp = 18.dp

        /** Nut nguon dieu hoa 40dp / icon 20dp (da cat 54->40 / 26->20). */
        val ClimatePowerButton: Dp = 40.dp
        val ClimatePowerIcon: Dp = 20.dp

        /** Box icon chuan cac the (Expander / FlowNode / WaterSensor). */
        val IconBox: Dp = 44.dp

        /** Box icon RoomCard / DeviceRow. */
        val IconBoxLarge: Dp = 48.dp

        /** Box icon DonutCard. */
        val IconBoxDonut: Dp = 52.dp

        /** Icon chuan trong box 44/48dp. */
        val Standard: Dp = 24.dp

        /** Icon PackHeader (box 44dp, bo 14dp). */
        val PackHeader: Dp = 26.dp

        /** Icon InnerCard (box 30dp) / ValueCell (13dp). */
        val InnerCard: Dp = 15.dp
        val ValueCell: Dp = 13.dp

        /** Icon pin ngang 30x18dp (BatteryCard / FlowNode). */
        val BatteryIconWidth: Dp = 30.dp
        val BatteryIconHeight: Dp = 18.dp

        /** Chevron Expander (Android MsIcon 26dp; iOS box 40 + icon 24 — khac kieu). */
        val Chevron: Dp = 26.dp

        /** Icon banner / dialog. */
        val Banner: Dp = 20.dp
        val Dialog: Dp = 24.dp
    }

    // ---------- minHeight nut & chip ----------
    object MinHeight {
        /** Nut Huy / Xac nhan inline (da cat 40->32dp; iOS 40pt). */
        val ConfirmCancelButton: Dp = 32.dp

        /** Chip MO/DONG DeviceRow (padding doc 4dp -> cao 21,4dp; iOS ~27,4pt). */
        val OpenCloseChip: Dp = 21.4.dp

        /** Chip che do ClimateMode (da cat 44->34dp; iOS 44pt). */
        val ModeChip: Dp = 34.dp
    }

    // ---------- Chieu cao co dinh ----------
    object Height {
        /** WeekChartD (iOS 150pt; Android cat 150->120 theo lenh user 22/03). */
        val WeekChart: Dp = 120.dp

        /** Donut (iOS 140pt; Android cat 140->120). */
        val Donut: Dp = 120.dp

        /** AreaChart / PowerBarChart ChartDetailSheet. */
        val AreaChart: Dp = 180.dp
        val PowerBarChart: Dp = 180.dp

        /** CellChart PackSheet. */
        val CellChart: Dp = 76.dp

        /** topGap sheet (Room / Lights / Chart / Pack): the cao = screenH - 183 - 8. */
        val SheetTopGap: Dp = 183.dp
    }

    // ---------- CostCard ("Chi phi dien") — ap dung mau dau tien ----------
    //
    // Muc dich phan B (18/11): khoi phuc ti le khe/thanh phan sau vong cat
    // manh 31d213e (207,5 -> 147,5dp). Cach lam: THU NHO gia tri trong
    // (Stat2 20->16sp, PBox 14->12sp, lineHeight ve tu nhien 1,219x) roi
    // tinh lai moi khe = he so [Ratio] x co chu MOI, lam tron ve dp sach.
    // Ket qua: cao moi ~183,8dp (van -23,7dp so voi goc 207,5dp), moi ti le
    // ve gan iOS (bang doi chieu day du trong bao cao phan B).
    object CostCard {
        val CardCorner: Dp = Corner.CardLarge // 32dp
        /** 1,00 x title 17 = 17dp. */
        val ContentPadding: Dp = 17.dp

        /** 0,60 x title 17 = 10,2 -> 10dp. */
        val TitleGap: Dp = 10.dp

        /** Khe ngang hang Stat2 / PBox (khong doi so voi truoc cat). */
        val RowGap: Dp = Spacing.RowGap // 10dp

        val StatCorner: Dp = Corner.Stat // 22dp
        /** 0,70 x value 16 = 11,2 -> 11dp. */
        val StatPadding: Dp = 11.dp

        /** 0,20 x value 16 = 3,2 -> 3dp. */
        val StatLabelGap: Dp = 3.dp

        /** Gia tri Stat2: 16/19,5 (thu tu 20sp; LH cu bi bop 24,4->20). */
        val StatValueSize: TextUnit = Text.StatValue.fontSize
        val StatValueLineHeight: TextUnit = Text.StatValue.lineHeight

        /** Khe so -> don vi (giu 4dp nhu truoc; ti le 4/16 = 0,25). */
        val UnitGap: Dp = 4.dp

        /** 0,70 x PBox value 12 = 8,4 -> 8dp. */
        val SectionGap: Dp = 8.dp

        val PBoxCorner: Dp = Corner.Stat // 22dp
        /** 0,85 x value 12 = 10,2 -> 10dp. */
        val PBoxPadding: Dp = 10.dp

        /** 0,30 x value 12 = 3,6 -> 4dp. */
        val PBoxLabelGap: Dp = 4.dp

        /** Gia tri PBox: 12/14,6 (thu tu 14sp; LH cu bi bop 17,1->15,5). */
        val PBoxValueSize: TextUnit = Text.PBoxValue.fontSize
        val PBoxValueLineHeight: TextUnit = Text.PBoxValue.lineHeight
    }
}

/**
 * NGOAI LE con ton tai (ghi lai de worker sau quyet, khong tu sua o day):
 * - `EnergyConsTab` Column spacedBy 14dp: ngoai le duy nhat giua cac tab,
 *   khong dung M3ECardGap 10dp (audit muc 5). Chi doi ve 10dp khi co lenh.
 * - EnvTile bo 30dp: xem [M3ETokens.Corner.EnvTile].
 * - Sheet Android bo 20dp vs iOS 60pt: co tinh khac (man Samsung bo ~22dp).
 * - The mo phong 3D, Slider do sang den, khoi mau den: LOAI TRU khoi
 *   vong chuan hoa, khong token hoa o day.
 */
