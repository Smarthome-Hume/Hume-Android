package com.smarthome.hume.core.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
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
         * Title TRANG — CHOT 18/11 (vong audit title): moi title trang
         * cung 20/24,4 — pill "Nang luong" 20, Powerwall 18->20, "Hi,"
         * Home 22->20, "An ninh"/"Thong tin" 20, "Brief" 24->20,
         * RoomMgmtHeader 18->20, Login 22->20. iOS: pill = cardTitle
         * 20pt (khop), "Hi," = 22pt (lech, Android thong nhat 20
         * theo lenh user: title trang Nang luong va trang Home cung co).
         */
        val PageTitle = M3ETextSpec(20.sp, 24.4.sp)

        /**
         * Title sheet / dialog (LightsSheet / ChartDetail / RoomSheet /
         * EcoDialog / InsightPopup / dialog xac nhan / FontPickerSheet /
         * FabMenuConfigSheet / PowerwallSettingsSheet). CHOT 18/11.
         * Lech iOS ghi nhan: title InsightPopup iOS = headlineSmall
         * 20pt, header SettingsSheet iOS = cardTitle 20pt.
         */
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

        /** Gia tri cell/pack trong PowerwallPackSheet — 15sp (audit A-32). */
        val PackValue = M3ETextSpec(15.sp, 18.3.sp)

        /** Body thu nho — 13sp (HomeCards.kt:836, audit A-09). */
        val BodyTight = M3ETextSpec(13.sp, 15.8.sp)

        /** % hero trong InsightPopup ("$soc%", iOS 48pt — audit A-33). */
        val HeroPercent = M3ETextSpec(48.sp, 58.5.sp)
    }

    /** LetterSpacing — audit B-04 (M3ESectionTitle / M3ESectionHeader). */
    object LetterSpacing {
        /** SectionTitle / SectionHeader. */
        val Tight: TextUnit = (-0.1).sp

        /** SectionLabel. */
        val Tracking: TextUnit = 1.5.sp
    }

    // ---------- Thang spacing (dp tuyet doi) ----------
    object Spacing {
        /** Khe nho nhat (spacedBy 2dp — audit A-17, B-07). */
        val Xxs: Dp = 2.dp
        val Xs: Dp = 4.dp
        val Sm: Dp = 8.dp
        val Md: Dp = 12.dp
        val Lg: Dp = 16.dp
        val Xl: Dp = 20.dp
        /** Khe 24dp (EcoDialog padding, HomeFab, FabMenuCard — audit B-1). */
        val Xxl: Dp = 24.dp

        /**
         * Le ngang trang — 18dp o ca 4 tab (Home / Nang luong / An ninh /
         * Thong tin, audit A-06). Khac contentPadding the 20dp ([Xl]).
         */
        val PageMargin: Dp = 18.dp

        /** Bottom clearance trang — 120dp (audit A-06, B-09). */
        val PageBottom: Dp = 120.dp

        /** Padding doc lon (LoginScreen 32dp, FabMenuCard bottom 32dp — audit B-1). */
        val PageTop: Dp = 32.dp

        /**
         * Khe giua cac the (10dp, user cat 14->10 ngay 22/03).
         * Gia tri chuan nam o day; `m3eScreenBottomPadding()` trong
         * `M3EPageSpacing.kt` dung thang token nay (alias cu `M3ECardGap`
         * da xoa o vong chuan hoa 18/11).
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

        /**
         * Tron het co — 999dp (audit B-2). 1 token thay 3 cach viet:
         * `RoundedCornerShape(99.dp)` / `999.dp` / `RoundedCornerShape(50)`
         * (percent). Gia tri 999dp = ban kinh lon hon moi kich thuoc thuc te.
         */
        val Pill: Dp = 999.dp

        /** Bo o rat nho 10dp (DeviceEdit / RoomManagement / EnergyInsightsCard / MeScreen — audit B-2). */
        val XSmall: Dp = 10.dp

        /** Bo cuc nho 4dp (audit A-22: EnergyWaterTab:295 dung Spacing.Xs lam bo — sai ngu nghia, can token Corner dung ten). */
        val XxSmall: Dp = 4.dp

        /** Bo nho 8dp (audit A-22: WeekChartD:187 dung Spacing.Sm lam bo — sai ngu nghia). */
        val Tiny: Dp = 8.dp

        /**
         * Bo o nho 12dp — dich Q4(b) lam tron bo 14dp ve 12dp.
         * LU Y DAT TEN: audit §b de xuat `Corner.Small` = 16dp nhung ban do
         * da bi loai (bo 16dp noi ve [Cell] da co, khong can token rieng);
         * o day `Small` = 12dp cho vua thang (XSmall 10 < Small 12 <
         * Cell 16). Bo 12dp o A-22 (EnergyInsightsCard:290 dung
         * Spacing.Md lam bo) cung dung token nay.
         */
        val Small: Dp = 12.dp

        /**
         * Bo tren sheet chung 44dp — Q3 user chot 10:49 18/11 (ke ca
         * PowerwallSettingsSheet dang 60dp). Bang gia tri
         * `HumeShapes.extraLarge` 44dp — day la token chuan cho bo tren
         * sheet (audit C-12).
         */
        val SheetTop: Dp = 44.dp

        /** Bo popup 24dp (InsightPopup / ChartDetailSheet — audit A-05). Trung gia tri [ClimateBox] nhung khac ngu nghia. */
        val Popup: Dp = 24.dp

        /** Bo dialog 28dp (AlertDialog BriefEdgeHost — audit B-2, C-13). Trung gia tri [Card] nhung khac ngu nghia. */
        val Dialog: Dp = 28.dp

        /** Bo switch 16dp (M3ESwitch — audit B-03). Rieng cho switch, khong muon [Cell]. */
        val Switch: Dp = 16.dp

        /** Bo text field 20dp (M3ETextField — audit B-02). Trung gia tri [SheetInner] nhung khac ngu nghia. */
        val TextField: Dp = 20.dp
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

        /** Icon rat nho 16dp (`.size(16.dp)` x14 toan app — audit B-20). */
        val XSmall: Dp = 16.dp

        /** Icon nho 20dp (`.size(20.dp)` x21 toan app — audit B-20). */
        val Small: Dp = 20.dp
    }

    // ---------- minHeight nut & chip ----------
    object MinHeight {
        /** Nut Huy / Xac nhan inline (giam 6dp vong 18/11: 40->34dp; padding doc 10->9dp de noi dung 32,6dp < min 34dp). */
        val ConfirmCancelButton: Dp = 34.dp

        /**
         * Chip MO/DONG DeviceRow (giam 6dp vong 18/11: 27,4->21,4dp).
         * DeviceRow dung token nay qua heightIn(min) +
         * padding doc [M3ETokens.RoomSheet.OpenCloseChipPaddingV] (4dp x 2 +
         * LH 13,4 = 21,4dp).
         */
        val OpenCloseChip: Dp = 21.4.dp

        /** Chip che do ClimateMode (giam 6dp vong 18/11: 44->38dp; hop modes 14+38+14 = 66dp). */
        val ModeChip: Dp = 38.dp

        /** Cao toi thieu snackbar (M3ESnackbar — audit C-10). */
        val Snackbar: Dp = 48.dp
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

        /** FAB chinh 56x56dp (HomeFab — audit C-08). */
        val Fab: Dp = 56.dp

        /** Nut lon 56dp (Button M3 LoginScreen — audit A-18). Cung gia tri [Fab] nhung khac ngu nghia. */
        val ButtonLarge: Dp = 56.dp

        /** Cao toi thieu tab chuyen EsubGroup (audit B-07). */
        val EsubGroup: Dp = 46.dp
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
        /**
         * CHOT 18/11 (vong audit title): 20dp = Spacing.Xl, dong nhat
         * moi the trong tab + iOS cardPadding 20pt (CostCard iOS dung
         * mac dinh 20pt). Bo gia tri 17dp phan B (1,00 x title 17):
         * quy tac ti le lay tu iOS 20/20 voi title iOS 20pt, trong khi
         * padding iOS van 20pt — so tuyet doi iOS thang ti le. The
         * cao them ~6dp (183,8 -> ~189,8dp), chap nhan de dong nhat.
         */
        val ContentPadding: Dp = Spacing.Xl // 20dp

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
        /** Khe doc LazyColumn giua cac the con (giam 6dp vong 18/11: 12->6dp, user 10:14). */
        val CardGap: Dp = 6.dp

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

        /** O tang cuong rua bat 70dp, vong tron trong 40dp, icon 18dp (giam 6dp vong 18/11: 76->70dp). */
        val DishwasherBoost: Dp = 70.dp
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

        /**
         * Cham mau hien tai 28dp / thanh hue 26dp (giam 6dp vong 18/11:
         * cham 32->28dp, hue 28->26dp; khoi mau 68->62dp).
         * Token MOI — truoc day la literal trong khoi mau den.
         */
        val LightColorDot: Dp = 28.dp
        val LightHueHeight: Dp = 26.dp

        /** Padding doc chip hieu ung den (iOS 8dp -> chip 30,6dp). */
        val LightEffectChipPaddingV: Dp = 8.dp

        /** Padding nut Huy/Xac nhan: ngang 12 / doc 9 (giam 6dp vong 18/11: doc 10->9dp; nut max(34; 9+14,6+9=32,6) = 34dp). */
        val ConfirmCancelPaddingH: Dp = 12.dp
        val ConfirmCancelPaddingV: Dp = 9.dp

        /** Padding doc chip MO/DONG (giam 6dp vong 18/11: 7->4dp; chip 4+13,4+4 = 21,4dp). */
        val OpenCloseChipPaddingV: Dp = 4.dp
    }

    // ---------- Switch chuan (M3ESwitch) — audit B-03 ----------
    object Switch {
        val Width: Dp = 52.dp
        val Height: Dp = 32.dp
    }

    // ---------- Navbar — audit C-06 ----------
    object Navbar {
        /** Cao navbar 78dp (6+4+…+6). */
        val Height: Dp = 78.dp

        /** Bo ngoai navbar = bo active 20 + khe 10. */
        val OuterCorner: Dp = 30.dp

        /**
         * Bo pill active — GIU 20dp nhu hien tai, chi token hoa, khong doi
         * gia tri (user chot 10:47 18/11: giua 16/20/24, giu 20dp).
         */
        val ActiveCorner: Dp = 20.dp

        /** Icon tab khi chon 27dp (khi khong chon 24dp = [Icon.Standard]). */
        val IconSelected: Dp = 27.dp
    }

    // ---------- Mau trang thai / bieu do / camera — audit A-13, A-30, B-5 ----------
    //
    // Gia tri hien tai, KHONG doi mau nao. Tach theo ngu nghia, khong gop
    // theo hex (vd #F59E0B xuat hien o 3 ngu nghia khac nhau).
    object StatusColors {
        /** Trang thai ket noi (HomeCards.kt:207-209). */
        object Connection {
            val Connected: Color = Color(0xFF22C55E)
            val Connecting: Color = Color(0xFFF59E0B)
            val Disconnected: Color = Color(0xFFEF4444)
        }

        /** Pin / luoi trong EnergyConsTab.kt:367,369. */
        val Battery: Color = Color(0xFFD97706)
        val Grid: Color = Color(0xFF9AA5B1)

        /** Dang ghi hinh (CameraFeedCard.kt:211, SecurityScreen.kt:745). */
        val Recording: Color = Color(0xFFE53935)

        /** Thoi tiet trong BriefScreen.kt:303-304,567. */
        object Weather {
            val Sunny: Color = Color(0xFFF9A825)
            val Rain: Color = Color(0xFF1565C0)
            val Rank0: Color = Color(0xFFEF6C00)
        }
    }

    /** Palette bieu do (EnergyConsTab.kt:451-454). */
    object ChartColors {
        val Palette: List<Color> = listOf(
            Color(0xFFE8734A),
            Color(0xFF5B8DEF),
            Color(0xFFE8A838),
            Color(0xFF9B7EDE),
        )
    }

    /** Gradient nen camera — surface toi co dinh, khong theo theme sang/toi (CameraFeedCard.kt:122, SecurityScreen.kt:317,601). */
    object CameraColors {
        val Gradient: List<Color> = listOf(
            Color(0xFF2B3A4A),
            Color(0xFF1A2430),
            Color(0xFF24303D),
        )
    }

    // ---------- Alpha — audit A-14, A-15, A-31, B-02, B-10, B-12, C-05 ----------
    object Alpha {
        /** Nen tint icon (EnergyFlowCard). */
        const val TintBg: Float = 0.16f

        /** Nen muc do nghiem trong (EnergyInsightsCard). */
        const val SeverityBg: Float = 0.12f

        /** Track (WavyBatteryBar, EnergyFlowCard). */
        const val Track: Float = 0.45f

        /** Scrim co dinh o root (M3ERootScreen.kt:410). */
        const val Scrim: Float = 0.45f

        /** Dau gradient scrim (M3ERootScreen.kt:375). */
        const val ScrimTop: Float = 0.95f

        /** Chu phu. */
        const val SecondaryText: Float = 0.75f

        /** Chu phu bac 3. */
        const val TertiaryText: Float = 0.35f

        /** Chu khi disabled (M3ETextField). */
        const val DisabledText: Float = 0.6f

        /** Placeholder (M3ETextField). */
        const val Placeholder: Float = 0.55f

        /** Nen kinh chart tuan (WeekChartD). */
        const val Glass: Float = 0.72f

        /** Nhan bieu do (WeekChartD). */
        const val ChartLabel: Float = 0.7f

        /** Border (DeviceEdit, M3ETextField). */
        const val Border: Float = 0.45f
    }

    // ---------- Border / Divider — audit A-14, A-15, B-02, B-03 ----------
    object Border {
        val Thin: Dp = 1.dp
        val Medium: Dp = 1.5.dp

        /** Khi focus (M3ETextField) / vien switch (M3ESwitch). */
        val Focused: Dp = 2.dp
    }

    /** Divider rieng (khong gop vao [Border] — day 1dp nhung la chieu day duong ke, khong phai vien). */
    object Divider {
        val Thickness: Dp = 1.dp
    }

    // ---------- Shadow — audit B-10, B-13, C-08, C-10 ----------
    object Shadow {
        /** FAB (HomeFab). */
        val Fab: Dp = 8.dp

        /** Chart nho (WeekChartD). Cung gia tri [Fab] nhung khac ngu canh. */
        val Small: Dp = 8.dp

        /** Snackbar (M3ESnackbar). */
        val Snackbar: Dp = 6.dp

        /** Avatar lon (AvatarViewer). */
        val Avatar: Dp = 28.dp
    }

    // ---------- Dialog he thong — audit A-24, C-11, C-13 ----------
    object Dialog {
        /** Padding noi dung dialog (EcoDialog 24dp). */
        val Padding: Dp = 24.dp

        /** Padding nut dialog (SecurityScreen: 20/28 x 12dp — 28dp la ngoai le 1 cho, lay 20dp chuan). */
        val ButtonPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    }

    // ---------- Snackbar — audit C-10 ----------
    object Snackbar {
        val Corner: Dp = 20.dp
        val Padding: PaddingValues = PaddingValues(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
    }
}

/**
 * NGOAI LE con ton tai (ghi lai de worker sau quyet, khong tu sua o day):
 * - `EnergyConsTab` Column spacedBy 14dp: ngoai le duy nhat giua cac tab,
 *   khong dung Spacing.CardGap 10dp (audit muc 5). Chi doi ve 10dp khi co lenh.
 * - EnvTile bo 30dp: xem [M3ETokens.Corner.EnvTile].
 * - Sheet Android bo 20dp vs iOS 60pt: co tinh khac (man Samsung bo ~22dp).
 * - The mo phong 3D, Slider do sang den, khoi mau den: LOAI TRU khoi
 *   vong chuan hoa, khong token hoa o day.
 */
