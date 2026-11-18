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
        /** SuggestCard icon (cat 30->22dp vong 04:30 18/11, tang lai 22->24dp
         * theo lenh user 09:47 18/11 — 24dp = [Standard]; van nho hon cot
         * chu (31,3dp) nen khong doi chieu cao the. */
        val SuggestCard: Dp = 24.dp

        /** EnvTile: box 44dp / icon 22dp (giam 6dp vong 18/11: 48/24 -> 44/22, ti le theo the 76->70dp). */
        val EnvTileBox: Dp = 44.dp
        val EnvTile: Dp = 22.dp

        /** Stepper (Solar) nut 32dp tron; StepperButton (Climate) 36dp (giam 6dp vong 18/11: 42->36). */
        val StepperButton: Dp = 32.dp
        val ClimateStepperButton: Dp = 36.dp
        val ClimateStepperIcon: Dp = 20.dp

        /** Nut nguon dieu hoa 48dp / icon 24dp (giam 6dp vong 18/11: 54/26 -> 48/24). */
        val ClimatePowerButton: Dp = 48.dp
        val ClimatePowerIcon: Dp = 24.dp

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
        /** Nut Huy / Xac nhan inline (khoi phuc tu 32dp sau cat 18/11; iOS 40pt). */
        val ConfirmCancelButton: Dp = 40.dp

        /**
         * Chip MO/DONG DeviceRow (khoi phuc tu 21,4dp sau cat 18/11; iOS 27,4pt).
         * DA NOI DAY 18/11: DeviceRow dung token nay qua heightIn(min) +
         * padding doc [M3ETokens.RoomSheet.OpenCloseChipPaddingV] (7dp x 2 +
         * LH 13,4 = 27,4dp) — khong con la token chet.
         */
        val OpenCloseChip: Dp = 27.4.dp

        /** Chip che do ClimateMode (giam 6dp vong 18/11: 44->38dp; hop modes 14+38+14 = 66dp). */
        val ModeChip: Dp = 38.dp
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

    // ---------- RoomSheet (the con trong sheet phong) — khoi phuc parity iOS ----------
    //
    // Muc dich (18/11): khoi phuc cac so iOS sau vong cat manh 36ea51c
    // (EnvTile/DeviceRow/Climate/rua bat/den). KHONG doi [Spacing.Sm] (8dp,
    // dung chung 17 cho trong feature/home) hay [Icon.IconBox] (44dp, dung
    // chung 4 file energy) — moi so RoomSheet nam rieng o day.
    // Can cu: `~/workspace/roomsheet-measure-2026-11-18.md` muc 6.1.
    // Ratio.SectionGap (0,70) chi la san chong bop khe, KHONG tai tao duoc
    // iOS o 3/4 khe muc (CardGap/ExpandedGap/GenericGap/DishwasherGap) —
    // cac khe do dung so iOS truc tiep o day.
    object RoomSheet {
        /** Khe doc LazyColumn giua cac the con (iOS VStack spacing 12pt). */
        val CardGap: Dp = 12.dp

        /** Khe tren tieu de "Thiet bi" (iOS 4pt). */
        val DevicesTitleGap: Dp = 4.dp

        /** Padding EnvTile (giam 6dp vong 18/11: 14->13dp; the 13+44+13 = 70dp). */
        val EnvTilePadding: Dp = 13.dp

        /** Padding hop tren ClimateCard: ngang 16 / doc 14 (iOS). */
        val ClimatePaddingH: Dp = 16.dp
        val ClimatePaddingV: Dp = 14.dp

        /** Khe giua 2 hop dieu hoa (iOS 10pt). */
        val ClimateGap: Dp = 10.dp

        /** Padding trong stepper nhiet do (iOS 6dp); khe giua cac nut stepper (iOS 12dp). */
        val StepperPadding: Dp = 6.dp
        val StepperGap: Dp = 12.dp

        /** Padding ngang quanh so nhiet do stepper (iOS 8dp). */
        val TempPaddingH: Dp = 8.dp

        /** Padding doc DeviceRow (giam 6dp vong 18/11: 14->13dp; the 13+44+13 = 70dp; ngang 16dp giu nguyen). */
        val DeviceRowPaddingV: Dp = 13.dp

        /**
         * Box icon / glyph rieng DeviceRow (giam 6dp vong 18/11: 48/24 -> 44/22).
         * TACH MOI — khong doi [Icon.IconBoxLarge]/[Icon.Standard] goc vi
         * dung chung: IconBoxLarge con dung o RoomCard (:202/:239) +
         * BatteryCard, Standard con dung o RoomCard/RoomExpandedCards/energy.
         */
        val DeviceRowIconBox: Dp = 44.dp
        val DeviceRowIcon: Dp = 22.dp

        /** Khe tren slot mo rong (iOS VStack spacing 12pt). */
        val ExpandedGap: Dp = 12.dp

        /** Khe giua cac dong GenericExpandedView (iOS 8pt). */
        val GenericGap: Dp = 8.dp

        /** Khe doc tong the rua bat (iOS 14pt). */
        val DishwasherGap: Dp = 14.dp

        /** Khe nhan -> so hero rua bat (iOS 2dp). */
        val DishwasherHeroGap: Dp = 2.dp

        /** Khe doc SegmentFlow rua bat (iOS 10pt). */
        val DishwasherSegmentGap: Dp = 10.dp

        /** Khe icon -> nhan trong 1 doan rua (iOS 6dp). */
        val DishwasherSegmentLabelGap: Dp = 6.dp

        /** Node tron 1 doan rua (iOS 24pt; icon 11dp giu nguyen). */
        val DishwasherSegmentNode: Dp = 24.dp

        /** Padding doc meta pill rua bat (iOS 8dp -> pill 33,1dp). */
        val DishwasherMetaPillPaddingV: Dp = 8.dp

        /** Khe khoi chuong trinh rua bat (iOS 10pt). */
        val DishwasherProgramGap: Dp = 10.dp

        /** Khe khoi nuoc mem / hen gio rua bat (iOS 10pt). */
        val DishwasherSectionGap: Dp = 10.dp

        /** Vong tron chuong trinh rua bat 56dp / icon 26dp (iOS). */
        val DishwasherProgramCircle: Dp = 56.dp
        val DishwasherProgramIcon: Dp = 26.dp

        /** Khe icon -> nhan trong 1 item chuong trinh (iOS 4dp). */
        val DishwasherProgramItemGap: Dp = 4.dp

        /** Padding doc chip chuong trinh rua bat (iOS 4dp -> chip 25,1dp). */
        val DishwasherProgramChipPaddingV: Dp = 4.dp

        /** O tang cuong rua bat 76dp, vong tron trong 40dp, icon 18dp (iOS). */
        val DishwasherBoost: Dp = 76.dp
        val DishwasherBoostCircle: Dp = 40.dp
        val DishwasherBoostIcon: Dp = 18.dp

        /** Nut stepper rua bat 56dp / icon 20dp (iOS). */
        val DishwasherStepper: Dp = 56.dp
        val DishwasherStepperIcon: Dp = 20.dp

        /** Nut nguon rua bat 56dp; nut Dung 60dp (iOS; icon 24dp giu nguyen). */
        val DishwasherPower: Dp = 56.dp
        val DishwasherStop: Dp = 60.dp

        /** Padding doc nut Bat dau rua (iOS 16dp -> nut 49,1dp). */
        val DishwasherStartPaddingV: Dp = 16.dp

        /**
         * Khe doc tong the den — GIU 8dp, LECH CO CHU Y so voi iOS 14pt.
         * Slider M3 toi thieu 48dp (iOS ~31pt, +17dp) + khoi mau Android
         * 2 hang (65dp vs iOS 1 hang ~32pt) da cong +47dp; khoi phuc khe
         * 14 se day den len 199,7dp (+53 so voi iOS). Tong den co hieu ung
         * sau sua: 187,7dp.
         */
        val LightGap: Dp = 8.dp

        /** Khe trong khoi mau / khoi hieu ung den (iOS 8pt). */
        val LightInnerGap: Dp = 8.dp

        /** Padding doc chip hieu ung den (iOS 8dp -> chip 30,6dp). */
        val LightEffectChipPaddingV: Dp = 8.dp

        /** Padding nut Huy/Xac nhan: ngang 12 / doc 10 (iOS; nut 40dp). */
        val ConfirmCancelPaddingH: Dp = 12.dp
        val ConfirmCancelPaddingV: Dp = 10.dp

        /** Padding doc chip MO/DONG (iOS 7dp x 2 + LH 13,4 = 27,4dp). */
        val OpenCloseChipPaddingV: Dp = 7.dp
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
