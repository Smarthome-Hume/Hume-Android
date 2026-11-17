package com.smarthome.hume.data

import com.smarthome.hume.core.data.HomeRepository
import com.smarthome.hume.core.ha.HistoryFetcher
import com.smarthome.hume.core.ha.HaEndpointMode
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.AlarmUi
import com.smarthome.hume.core.model.BatteryUi
import com.smarthome.hume.core.model.ClimateUi
import com.smarthome.hume.core.model.ConnectionState
import com.smarthome.hume.core.model.DeviceKind
import com.smarthome.hume.core.model.DeviceUi
import com.smarthome.hume.core.model.HomeEntity as LegacyEntity
import com.smarthome.hume.core.model.HomeNotification
import com.smarthome.hume.core.model.HomeUiState
import com.smarthome.hume.core.model.HumeConfig
import com.smarthome.hume.core.model.RoomBubbleConfig
import com.smarthome.hume.core.model.RoomUi
import com.smarthome.hume.core.model.SolarDay
import com.smarthome.hume.core.model.cameraKeyForSensor
import com.smarthome.hume.core.model.FRIGATE_CAMERA_KEYS
import com.smarthome.hume.core.model.cameraNameVi
import com.smarthome.hume.core.model.frigateLabelVi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.ZoneId

/**
 * Adapter: map HomeAssistantRepository cu (app module) sang HomeUiState moi.
 * Giu nguyen toan bo logic HA/WiFi hien tai; UI moi chi doc HomeRepository.
 */
class AppHomeRepository(
    private val ha: HomeAssistantRepository,
    private val scope: CoroutineScope,
    appContext: android.content.Context,
) : HomeRepository {

    private val _homeState = MutableStateFlow(HomeUiState())
    override val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    /** (Batch D, GAP-07) Trang thai tho HA cho the mo rong trong phong. */
    override val entityStates: StateFlow<Map<String, LegacyEntity>> = ha.entities

    /**
     * Room configs dong (port iOS RoomStore).
     * Cho RoomStore seed/load persisted data; Home does not invent fallback rooms.
     * goi setRoomConfigs() de cap nhat.
     */
    private val _roomConfigs = MutableStateFlow<List<com.smarthome.hume.core.model.RoomBubbleConfig>>(
        emptyList()
    )
    val roomConfigs: StateFlow<List<com.smarthome.hume.core.model.RoomBubbleConfig>> = _roomConfigs.asStateFlow()

    /** Cap nhat room configs tu RoomStore (goi khi roomsFlow thay doi). */
    fun setRoomConfigs(configs: List<com.smarthome.hume.core.model.RoomBubbleConfig>) {
        _roomConfigs.value = configs
    }

    private val weekCache = mutableMapOf<Long, Double>()
    private val snapshots = com.smarthome.hume.core.storage.DailySnapshotStore.get(appContext)

    companion object {
        /** Event Frigate trong N phut moi duoc tong hop thanh goi y (khong co PIR). */
        private const val FRIGATE_SUGGEST_MIN = 5
    }
    private val settingsStore = com.smarthome.hume.core.storage.SettingsStore(appContext)
    private val frigateStore = com.smarthome.hume.core.frigate.FrigateStore.get(appContext)
    /** Lan cuoi goi Frigate lay nhan doi tuong (tranh spam moi lan entity doi). */
    private var motionFetchAt = 0L
    private var motionFetchIds: Set<String> = emptySet()
    /** Goi y tong hop tu trigger Frigate truc tiep (khong qua PIR). */
    private var frigateMotions: List<HomeNotification> = emptyList()

    init {
        scope.launch {
            combine(ha.entities, ha.connected, _roomConfigs) { e, c, _ -> e to c }
                .collect { (entities, connected) ->
                    val connState = when {
                        connected -> ConnectionState.Connected
                        ha.isConnecting() -> ConnectionState.Connecting
                        else -> ConnectionState.Disconnected
                    }
                    val newState = buildState(entities, connected, connState)
                    _homeState.value = enrichMotionObjects(newState, connected)
                    // Realtime (2026-09-30): dang ky watched entities cho dashboard M3E
                    // (truoc day chi ViewModel cu goi setWatchedEntities, dashboard moi
                    // khong goi -> sensor roi vao bucket ONE_DAY, khong realtime).
                    updateWatched(newState)
                }
        }
        scope.launch {
            ha.connected.collectLatest { connected ->
                if (!connected) return@collectLatest
                refreshSolarWeek()
                while (isActive) {
                    delay(300_000L)
                    if (ha.connected.value) refreshSolarWeek()
                }
            }
        }
    }

    // ---------- actions ----------

    /**
     * Den trong config phong dong — dem bong den CHI dung danh sach
     * nay, khong dung light.* (2026-09-30, user yeu cau). Loai group entity
     * (vd: light.all_light, group phong ngu) de khong dem trung.
     */
    private val configuredLightIds: Set<String> get() =
        _roomConfigs.value
            .flatMap { it.devices }
            .map { it.entity }
            .filter { it.startsWith("light.") }
            .toSet()

    /** True neu entity la group (co attribute entity_id chua list member). */
    private fun isGroup(e: LegacyEntity): Boolean {
        val ids = e.attributes["entity_id"] as? kotlinx.serialization.json.JsonArray
        return ids != null && ids.isNotEmpty()
    }

    /** Dang ky realtime cho cac entity dashboard dang hien (2026-09-30). */
    private var lastWatched: Set<String> = emptySet()

    /** (N-03, v70) Dong BMS lam muot (EMA, alpha 0.2) + mau dong cuoi cho
     * cong thuc thoi gian pin — port iOS CurrentEMA (HomeBatteryCard giu
     * @State battEMA, cap nhat qua onChange(of: bmsCurr)). */
    private var battCurrentEma: Double? = null
    private var lastBmsCurr: Double? = null
    private fun updateWatched(s: HomeUiState) {
        val ids = HashSet<String>()
        // Tat ca thiet bi tren UI (config phong + climate) — bao gom sensor dien.
        s.searchDevices.forEach { ids.add(it.entityId) }
        s.lightsOn.forEach { ids.add(it.entityId) }
        _roomConfigs.value.forEach { room ->
            room.tempEntity?.takeIf { it.isNotBlank() }?.let(ids::add)
            room.humidityEntity?.takeIf { it.isNotBlank() }?.let(ids::add)
            room.devices.forEach { ids.add(it.entity) }
        }
        // Sensor cong suat cua o cam/cong tac co do cong suat: UI hien "· X W"
        // khi bat. Khong watch -> roi bucket ONE_DAY, WS giam toi 24h -> powerW
        // null/stale mai (bug 2026-09-30: cong tac nong lanh khong hien cong suat).
        _roomConfigs.value
            .flatMap { it.devices }
            .mapNotNull { it.powerEntity }
            .forEach { ids.add(it) }
        s.alarm?.let { ids.add(it.entityId) }
        // Nang luong: PV + pin.
        ids.add(HumeConfig.PV_POWER); ids.add(HumeConfig.PV_TODAY)
        ids.add(HumeConfig.BATTERY_SOC); ids.add(HumeConfig.BATTERY_POWER)
        // Flow tab Dien: cac sensor cong suat thoi gian thuc (deu la sensor.*
        // -> tu dong vao nhom telemetry 10s theo bucketFor). Truoc day thieu
        // nen gia tri luoi/nha co the stale, gay bug "duong chay khi hien 0".
        ids.add("sensor.aptomat_tong_power"); ids.add("sensor.cong_suat_nha")
        ids.add("sensor.solis_s6_eh1p_pv_power_1_3")
        ids.add("sensor.solis_s6_eh1p_pv_power_2_3")
        ids.add("sensor.aptomat_t1_power"); ids.add("sensor.aptomat_t2_power")
        ids.add("sensor.aptomat_t3_power")
        ids.add("sensor.battery_current_flow"); ids.add("sensor.energy_home_daily")
        ids.add("sensor.solis_s6_eh1p_today_battery_charge_energy_2")
        // (2026-10-22) Bo sensor.grid_status cu khoi watched: gridOn doc truc
        // tiep status_string_3 (port iOS d8df5d3), khong con noi nao dung
        // grid_status nua.
        // Trang thai inverter Solis — bao mat dien phai realtime (port iOS 65fe551).
        ids.add("sensor.solis_s6_eh1p_status_string_3")
        // Backup SOC: the pin doc nhung truoc day quen watch -> gia tri stale
        // tu luc mo app (nguyen nhan the pin mat cum thoi gian).
        ids.add(HumeConfig.BACKUP_SOC)
        // Nguong ep xa khi mat dien (port iOS f251d8e) + sensor trang thai inverter
        ids.add("sensor.solis_s6_eh1p_overdischarge_soc_2")
        ids.add("sensor.solis_s6_eh1p_status_string_3")
        // (N-03, v70 — port iOS PowerwallLiveData.timeCalcEntityIds, 31 entity)
        // Cong thuc thoi gian sac/xa moi doc BMS truc tiep, khong dung sensor
        // thoi gian cua Solis (da bo fallback) hay sensor nang luong SoH/DoD
        // (cong thuc cu dung, gio khong con dung) nen khong watch 4 sensor
        // do nua.
        ids.addAll(BATTERY_TIME_CALC_ENTITY_IDS)
        // (N-12 — port iOS PowerwallLiveData.entityIds, 205 entity, buoc bat
        // buoc truoc khi mo trang Powerwall) Toan bo sensor ESP32 BMS
        // (prefix powerwall_bms_): 4 pack x 46 key + master-only + system
        // + setting live. Trang Powerwall doc qua GroupEntityReader, KHONG
        // nam trong config phong/tab nao nen phai them tay vao day nhu the
        // rua bat (iOS rebuildWatchedSet §4f: ids.formUnion(entityIds)).
        // 31 entity N-03 la tap con cua set nay; ids la HashSet nen trung
        // lap tu triet tieu, khong duplicate.
        ids.addAll(POWERWALL_ENTITY_IDS)
        // (2026-10-22, E1) Flow 4 node: truoc day thieu cac sensor nay trong
        // watched -> roi bucket ONE_DAY, dong bang 24h trong khi node PV/Pin
        // van nhay 10s -> 4 node sai lech nhau tren may that (iOS da fix o
        // 3af820b, baseWatchedIds). Bo sung day du theo iOS:
        ids.add("sensor.solis_s6_eh1p_meter_active_power")
        ids.add("sensor.solis_s6_eh1p_household_load_power_2")
        ids.add("sensor.solis_s6_eh1p_backup_load_power_2")
        ids.add("sensor.solis_s6_eh1p_household_load_today_energy_2")
        ids.add("sensor.solis_s6_eh1p_backup_load_today_energy_2")
        ids.add("sensor.solis_s6_eh1p_today_battery_discharge_energy_2")
        ids.add("sensor.aptomat_tong_daily")
        // (Batch B, GAP-04 — port iOS PumpAlertMonitor) Bom may loc Rice Home:
        // khong watch -> roi bucket ONE_DAY, monitor app-level khong bao gio
        // thay doi trang thai kip thoi. binary_sensor da watch se vao REALTIME
        // theo bucketFor (khong phai sensor.* nen khong roi nhom 10s).
        ids.add("binary_sensor.r_ro_m_01")
        // (Batch D, GAP-08 — port iOS RoomExpandedCards/DishwasherLiveManager)
        // Nhom may rua bat Texgio: khong watch -> roi bucket ONE_DAY, the
        // mo rong stale (trang thai/giai doan/con lai/nhiet do/chuong trinh
        // + switch/tang cuong/number nuoc mem/hen gio).
        ids.add("sensor.texgio_tgfpcm795b_g_trang_thai") // DP 7
        ids.add("sensor.texgio_tgfpcm795b_g_giai_doan_rua") // DP 10
        ids.add("sensor.texgio_tgfpcm795b_g_thoi_gian_con_lai") // DP 9
        ids.add("sensor.texgio_tgfpcm795b_g_nhiet_do") // DP 11
        ids.add("sensor.texgio_tgfpcm795b_g_chuong_trinh") // DP 6
        ids.add("switch.texgio_tgfpcm795b_g_may_rua_bat") // DP 1
        ids.add("switch.texgio_tgfpcm795b_g_bat_dau") // DP 2
        ids.add("switch.texgio_tgfpcm795b_g_hoi_nuoc") // DP 108
        ids.add("switch.texgio_tgfpcm795b_g_chong_dia") // DP 102
        ids.add("switch.texgio_tgfpcm795b_g_khoa_tre_em") // DP 101
        ids.add("number.texgio_tgfpcm795b_g_hen_gio") // DP 8
        ids.add("number.texgio_tgfpcm795b_g_nuoc_mem") // DP 106
        // (Batch C, GAP-01 — port iOS EnergyWaterTab) Toan bo nhom nuoc RO
        // con lai: khong watch -> roi bucket ONE_DAY, tab Nuoc stale.
        // Bom binary_sensor.r_ro_m_01 da co tu Batch B, khong them trung.
        ids.add("sensor.r_ro_m_01"); ids.add("sensor.r_ro_m_01_2")
        ids.add("sensor.r_ro_m_01_4"); ids.add("sensor.r_ro_m_01_7")
        ids.add("sensor.r_ro_m_01_8")
        ids.add("binary_sensor.r_ro_m_01_2"); ids.add("binary_sensor.r_ro_m_01_3")
        // (Batch A, GAP-05 — port iOS baseWatchedIds) Sensor chi phi ma
        // AppEnergyRepository doc cho the Chi phi dien (grid_cost/home_cost/
        // evn_current_unit_price): truoc day khong watch -> roi bucket
        // ONE_DAY, gia/chi phi stale toi 24h sau khi HA doi.
        ids.add("sensor.grid_cost"); ids.add("sensor.home_cost")
        ids.add("sensor.evn_current_unit_price")
        // (N-01 — port iOS baseWatchedIds) grid_import_billing la mau so
        // cua gia mua dien (evnBuyRate); home_import_billing doi xung theo
        // iOS. Khong watch -> rate stale toi 24h sau khi HA doi.
        ids.add("sensor.grid_import_billing"); ids.add("sensor.home_import_billing")
        // BatteryInfo (tab Dien mat troi + popup): dien ap + gioi han dong BMS.
        ids.add("sensor.solis_s6_eh1p_battery_voltage_2")
        ids.add("sensor.solis_s6_eh1p_battery_charge_current_limitation_bms_2")
        ids.add("sensor.solis_s6_eh1p_battery_discharge_current_limitation_bms_2")
        // (2026-10-22, B1) Toan bo entity dieu khien 2 expander Sac/Xa pin
        // (specs trong AppEnergyRepository): truoc day chi co BACKUP_SOC duoc
        // watch, con lai roi ONE_DAY -> bam Stepper/switch xong UI khong nhan
        // state HA tra ve. iOS de ca nhom trong baseWatchedIds.
        ids.add("switch.allow_grid_to_charge_the_battery_2")
        ids.add("number.solis_s6_eh1p_battery_max_charge_current_2")
        ids.add("number.solis_s6_eh1p_grid_time_of_use_charge_battery_current_slot_1_2")
        ids.add("number.solis_s6_eh1p_grid_time_of_use_charge_cut_off_soc_slot_1_2")
        ids.add("switch.grid_time_of_use_charging_period_1_2")
        ids.add("time.solis_s6_eh1p_grid_time_of_use_charge_start_slot_1_2")
        ids.add("time.solis_s6_eh1p_grid_time_of_use_charge_end_slot_1_2")
        ids.add("number.solis_s6_eh1p_force_charge_soc_2")
        ids.add("number.solis_s6_eh1p_battery_max_discharge_current_2")
        ids.add("number.solis_s6_eh1p_off_grid_overdischarge_soc_2")
        ids.add("number.solis_s6_eh1p_overdischarge_soc_2")
        // An ninh: sensor cua/chuyen dong/khoi/nuoc.
        runCatching {
            ids.addAll(com.smarthome.hume.core.data.HumeGraph.get().securityRepository.sensorEntityIds)
        }
        if (ids != lastWatched) {
            lastWatched = ids
            ha.setWatchedEntities(ids)
        }
        // (2026-09-30) Khong pin bucket tay o day nua: luat telemetry thong nhat
        // nam trong HomeAssistantRepository.bucketFor (sensor.* -> TEN_SECONDS,
        // discrete -> REALTIME), ap dung cho moi man hinh.
    }

    override fun toggle(entityId: String) = ha.toggle(entityId)
    override fun setSwitch(entityId: String, on: Boolean) {
        val service = if (on) "turn_on" else "turn_off"
        ha.callService("switch", service, "{\"entity_id\":\"$entityId\"}", entityId)
    }
    override fun setNumber(entityId: String, value: Double) {
        ha.callService(
            "number", "set_value",
            "{\"entity_id\":\"$entityId\",\"value\":$value}", entityId,
        )
    }
    override fun setLightBrightness(entityId: String, percent: Int) =
        ha.setLightBrightness(entityId, percent)
    override fun setLightRgb(entityId: String, r: Int, g: Int, b: Int) =
        ha.setLightRgb(entityId, r, g, b)
    override fun setLightEffect(entityId: String, effect: String) =
        ha.setLightEffect(entityId, effect)
    override fun setLocalTuyaDp(deviceId: String, dp: Int, value: String) =
        ha.setLocalTuyaDp(deviceId, dp, value)
    override fun refreshDishwasherAfterCommand() = ha.refreshDishwasherAfterCommand()
    override fun setClimateTemp(entityId: String, tempC: Double) =
        ha.setClimateTemperature(entityId, tempC)
    override fun setHvacMode(entityId: String, mode: String) = ha.setHvacMode(entityId, mode)
    override fun toggleClimate(entityId: String) = ha.toggleClimate(entityId)
    override fun toggleRoomLights(entityIds: List<String>) = ha.toggleRoomLights(entityIds)
    override fun alarmArm(mode: String) {
        val entity = alarmEntityId() ?: return
        ha.alarmArm(entity, mode.trim().removePrefix("arm_"), HumeConfig.ALARM_CODE)
    }
    override fun alarmDisarm() {
        val entity = alarmEntityId() ?: return
        ha.alarmDisarm(entity, HumeConfig.ALARM_CODE)
    }
    override fun refreshSolarWeek() {
        scope.launch(Dispatchers.IO) { loadSolarWeek() }
    }

    // ---------- mapping ----------

    private fun alarmEntityId(): String? {
        val ids = ha.entities.value.keys
        return when {
            ids.contains(HumeConfig.ALARM_PRIMARY) -> HumeConfig.ALARM_PRIMARY
            ids.contains(HumeConfig.ALARM_FALLBACK) -> HumeConfig.ALARM_FALLBACK
            else -> ids.firstOrNull { it.startsWith("alarm_control_panel.") }
        }
    }

    private fun buildState(
        entities: Map<String, LegacyEntity>,
        connected: Boolean,
        connectionState: ConnectionState,
    ): HomeUiState {
        val cur = _homeState.value
        // Ten + avatar nguoi dung: lay person dau tien (khong hardcode id).
        val person = entities.values.firstOrNull { it.id.startsWith("person.") }
        val personName = person
            ?.attributes?.get("friendly_name")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() && !it.startsWith("person.") }
            ?: ""
        // Avatar: entity_picture cua person (HA tra ve duong dan tuong doi hoac URL day du).
        val rawPicture = person?.attributes?.get("entity_picture")?.jsonPrimitive?.contentOrNull
        val avatarUrl = rawPicture?.takeIf { it.isNotBlank() }?.let { pic ->
            if (pic.startsWith("http")) pic else ha.getBaseUrl().trimEnd('/') + pic
        }
        if (avatarUrl == null) {
            android.util.Log.d("AppHomeRepository", "Khong co avatar user (person entity_picture trong)")
        }
        // Key luu avatar theo user: giong logic ProfileScreen (user_id, fallback ten).
        val userKey = person?.attributes?.get("user_id")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: personName
        // Publish cho tab Toi (MeScreen) dung chung key avatar voi header trang Nha.
        runCatching { com.smarthome.hume.core.data.HumeGraph.get().userKey.value = userKey }

        val pvToday = entities[HumeConfig.PV_TODAY]?.numericState
        val solarNowKw = (entities[HumeConfig.PV_POWER]?.numericState ?: 0.0) / 1000.0

        val soc = (entities[HumeConfig.BATTERY_SOC]?.numericState ?: 0.0).toInt()
        val battPowerW = entities[HumeConfig.BATTERY_POWER]?.numericState ?: 0.0
        val battPowerKw = battPowerW / 1000.0
        // Cong thuc Hume goc: backupSoc tu number.solis_s6_eh1p_backup_soc_2, mac dinh 20
        val backupSoc = (entities[HumeConfig.BACKUP_SOC]?.numericState ?: 20.0).toInt()
        // Nguong ep xa khi mat dien (port iOS f251d8e, user 2026-10-09)
        val overdischargeSoc =
            (entities["sensor.solis_s6_eh1p_overdischarge_soc_2"]?.numericState ?: 5.0).toInt()
        // Trang thai luoi tu sensor inverter (port iOS d8df5d3)
        val gridStatusRaw = entities["sensor.solis_s6_eh1p_status_string_3"]?.state
            ?.lowercase()?.replace('-', ' ')?.replace('_', ' ') ?: ""
        // (2026-10-22, B3) Bo sung 4 case exact (off/0/false/no) cho khop
        // isGridOn cua iOS va AppEnergyRepository — thieu thi the pin giu
        // limit backupSoc trong khi FlowCard da coi la mat dien.
        val gridOn = !(gridStatusRaw == "off" || gridStatusRaw == "0" ||
            gridStatusRaw == "false" || gridStatusRaw == "no" ||
            "off grid" in gridStatusRaw || "no grid" in gridStatusRaw ||
            "outage" in gridStatusRaw || "mất" in gridStatusRaw || "mat dien" in gridStatusRaw)
        // Limit hieu dung: co dien -> backupSoc; mat dien -> overdischarge_soc
        val effectiveLimit = if (gridOn) backupSoc else overdischargeSoc
        // Hume goc: resting = power 0..5W -> khong hien thoi gian
        val resting = battPowerW in 0.0..5.0
        val discharging = battPowerW < 0.0
        val charging = !resting && !discharging
        // (N-03, v70 — port iOS Hume/M3E/Tabs/BatteryTimeCalc.swift +
        // HomeBatteryCard.swift:55-63,88-101) Thoi gian sac/xa tinh local tu
        // BMS (Ah/A), KHONG dung sensor Solis nua: sensor.battery_time_to_full_2
        // da ghi nhan bao ao (iOS bo fallback o v70), sensor.thoi_gian_pin_con_lai
        // khong phai duration. Cong thuc cu (kWh Solis / W tuc thoi, sac toi
        // 100%, xa phase 2 toi 0%) cung bo theo.
        //   1. Dong BMS lam muot bang EMA (alpha 0.2 moi lan HA day state moi).
        //   2. Cua so SOC thuc: sac toi setting_cutoff_soc live (97%),
        //      xa toi setting_discharge_cutoff_soc live (5%) — khong toi 100%/0%.
        //   3. Sac 2 pha CC/CV: CC toi goi CV roi CV dong giam tuyen tinh xuong
        //      nguong ngat setting_cutoff_c_rate (0.05 C, live).
        //      GOI CV (GIA DINH — ghi ro trong code iOS, copy nguyen, khong phai
        //      so do): SOC 90%. Ngoai ra neu cell cao nhat da cham nguong CV
        //      (cellMax >= min(cell-max live, CV pack / 16)) thi coi nhu dang
        //      trong pha CV roi.
        //   4. Xa giu cau truc 2 phase theo limit hieu dung (user 2026-10-09),
        //      nhung phase 2 dung o 5% (BMS ngat) thay vi 0% nhu cu.
        //   5. Mien tinh bang Ah/A nen thoi gian KHONG phu thuoc dien ap.
        //   6. Bien: dong ≈ 0 / thieu du lieu -> "—"; sac >= 97% -> "Đã đầy";
        //      xa <= 5% -> "Đã cạn".
        fun bms(id: String): Double? = entities[id]?.numericState
        val bmsSoc = bms("sensor.powerwall_bms_master_py_system_soc") ?: soc.toDouble()
        val bmsCurr = bms("sensor.powerwall_bms_master_py_system_current")
        val bmsCellMax = bms("sensor.powerwall_bms_master_cell_max")
        // Tong dung luong Ah + gioi han dong he thong = tong 4 pack (iOS
        // PowerwallLiveData.payload: master chi bao gioi han cua rieng no,
        // 31.2 A — khong phai tong he thong). Pack 2 khong co trong danh sach
        // iOS (chi master, pack_1, pack_3, pack_4) — copy nguyen.
        val bmsPacks = listOf("master", "pack_1", "pack_3", "pack_4")
        val fccAh = bmsPacks.sumOf { bms("sensor.powerwall_bms_${it}_full_charge_cap") ?: 0.0 }
        val limChgA = bms("sensor.powerwall_bms_master_py_chg_current_limit")
            ?: bmsPacks.sumOf { bms("sensor.powerwall_bms_${it}_charge_current_limit") ?: 0.0 }
        val limDisA =
            bmsPacks.sumOf { bms("sensor.powerwall_bms_${it}_discharge_current_limit") ?: 0.0 }
        val settings = batterySettingsFrom(entities)
        // EMA: cap nhat khi dong BMS DOI (giong iOS onChange(of: bmsCurr));
        // chua co mau dau thi dung dong live hien tai (1 mau dau). Dong mat
        // (entity thieu) thi giu EMA cu — fccAh = 0 da tra unknown nen khong
        // hien so cu.
        if (bmsCurr != null && bmsCurr != lastBmsCurr) {
            battCurrentEma = battCurrentEma?.let { 0.2 * bmsCurr + 0.8 * it } ?: bmsCurr
            lastBmsCurr = bmsCurr
        }
        val emaCurr = battCurrentEma ?: (bmsCurr ?: 0.0)
        // Chieu sac/xa van lay theo Solis flowW nhu iOS (resting 0..5W,
        // discharging < 0); chi dong + SOC + Ah + setting lay tu BMS.
        val battTime = batteryTimeCalc(
            soc = bmsSoc,
            currentEma = emaCurr,
            cellMax = bmsCellMax,
            fccAh = fccAh,
            limChgA = limChgA,
            limDisA = limDisA,
            settings = settings,
            dischargeLimitSoc = effectiveLimit.toDouble(),
            charging = charging,
            discharging = discharging,
        )
        // iOS HomeBatteryCard:88-101 — .minutes -> "H:MM" + gio ket thuc
        // (chi khi > 0 phut); .unknown -> "—"; .full -> "Đã đầy";
        // .empty -> "Đã cạn". Luc nghi khong hien thoi gian (null).
        val runtimeText: String? = if (resting) null else when (battTime) {
            is BatteryTime.Minutes ->
                "${battTime.minutes / 60}:${(battTime.minutes % 60).toString().padStart(2, '0')}"
            BatteryTime.Unknown -> "—"
            BatteryTime.Full -> "Đã đầy"
            BatteryTime.Empty -> "Đã cạn"
        }
        val endTime: String? =
            if (battTime is BatteryTime.Minutes && battTime.minutes > 0) {
                val end = java.time.LocalTime.now().plusMinutes(battTime.minutes.toLong())
                // 24h co dinh (port iOS 05827af, en_US_POSIX)
                String.format(java.util.Locale.US, "%02d:%02d", end.hour, end.minute)
            } else null

        val alarmId = alarmEntityId()
        val alarmEntity = alarmId?.let { entities[it] }
        val alarm = alarmEntity?.let {
            AlarmUi(
                entityId = alarmId,
                state = it.state,
                label = HumeConfig.alarmLabel(it.state),
                isArmed = it.state != "disarmed",
            )
        }

        val lightsOn = entities.values
            .filter { it.id in configuredLightIds && it.isOn && !isGroup(it) }
            .map { e ->
                DeviceUi(
                    entityId = e.id,
                    label = friendlyName(e),
                    sub = "",
                    iconKey = "bulb",
                    isOn = true,
                )
            }

        return HomeUiState(
            userName = personName,
            avatarUrl = avatarUrl,
            userKey = userKey,
            connected = connected,
            connectionState = connectionState,
            solarWeek = cur.solarWeek,
            solarTodayKwh = pvToday,
            solarNowKw = solarNowKw,
            battery = BatteryUi(
                soc = soc,
                powerKw = battPowerKw,
                powerW = battPowerW,
                backupSoc = backupSoc,
                timeText = runtimeText,
                endTime = endTime,
                effectiveLimit = effectiveLimit,
                overdischargeSoc = overdischargeSoc,
            ),
            alarm = alarm,
            lightsOn = lightsOn,
            rooms = _roomConfigs.value.map { buildRoom(it, entities) },
            notifications = buildNotifications(entities),
        ).let { st ->
            st.copy(searchDevices = buildSearchDevices(entities, st.rooms))
        }
    }

    /**
     * Gan nhan doi tuong Frigate (person/car/dog...) cho sensor chuyen dong
     * dang active, de the goi y mo ta "Có người hoạt động lúc 06:25".
     * Chi goi Frigate khi tap sensor thay doi hoac qua 60s (moi camera 1
     * request /api/events nhe, khong tai clip); event qua 15 phut thi bo qua.
     *
     * Them (2026-09-30, user: dung trigger event cua Frigate): camera co event
     * Frigate moi (<= FRIGATE_SUGGEST_MIN phut) nhung khong co sensor PIR nao
     * active -> tu tao thong bao "frigate:<cam>" de goi y dung camera do.
     */
    private suspend fun enrichMotionObjects(
        state: HomeUiState, connected: Boolean,
    ): HomeUiState {
        val motions = state.notifications.filter { it.title == "Phát hiện chuyển động" }
        val ids = motions.map { it.id }.toSet()
        if (ids.isEmpty() || !connected) {
            motionFetchIds = emptySet()
            frigateMotions = emptyList()
            return if (state.motionObjects.isEmpty()) state
            else state.copy(motionObjects = emptyMap())
        }
        val now = System.currentTimeMillis()
        // Goi y Frigate tong hop het han -> bat buoc fetch lai de loai bo.
        val syntheticsStale = frigateMotions.isNotEmpty() &&
            frigateMotions.all { (it.minutesAgo ?: 999) > FRIGATE_SUGGEST_MIN }
        if (ids == motionFetchIds && now - motionFetchAt < 60_000 && !syntheticsStale) {
            // Giu nhan cu, loc theo sensor dang active + goi y Frigate con han.
            val fresh = frigateMotions.filter { (it.minutesAgo ?: 999) <= FRIGATE_SUGGEST_MIN }
            val allIds = ids + fresh.map { it.id }
            val kept = _homeState.value.motionObjects.filterKeys { it in allIds }
            return state.copy(
                notifications = (state.notifications + fresh).take(12),
                motionObjects = kept,
            )
        }
        motionFetchIds = ids
        motionFetchAt = now
        val settings = runCatching { settingsStore.settings.first() }.getOrNull()
            ?: return state
        if (!settings.hasToken) return state
        val objects = mutableMapOf<String, String>()
        // Moi camera chi fetch 1 lan cho tat ca sensor map ve no.
        val byCam = motions.mapNotNull { n ->
            cameraKeyForSensor(n.id)?.let { cam -> cam to n.id }
        }.groupBy({ it.first }, { it.second })
        val freshSynthetics = mutableListOf<HomeNotification>()
        for (cam in FRIGATE_CAMERA_KEYS) {
            val (label, start) = runCatching {
                frigateStore.latestEvent(
                    cam, ha.getBaseUrl(), settings.haToken,
                    ha.endpoint.currentMode == HaEndpointMode.REMOTE &&
                        frigateStore.remoteConfig.remoteUrl.isNotBlank(),
                )
            }.getOrNull() ?: continue
            if (label.isBlank()) continue
            val ageSec = now / 1000 - start
            if (ageSec < 0) continue
            val vi = frigateLabelVi(label)
            val sensorIds = byCam[cam]
            if (sensorIds != null) {
                // Nhan doi tuong cho sensor dang active (event <= 15 phut).
                if (ageSec <= 900) sensorIds.forEach { objects[it] = vi }
            } else if (ageSec <= FRIGATE_SUGGEST_MIN * 60) {
                // Trigger truc tiep tu Frigate, khong co PIR nao: goi y tong hop.
                val mins = (ageSec / 60).toInt()
                val nid = "frigate:$cam"
                freshSynthetics += HomeNotification(
                    id = nid,
                    title = "Phát hiện chuyển động",
                    body = cameraNameVi(cam),
                    timeText = if (mins < 1) "vừa xong" else "$mins phút trước",
                    minutesAgo = mins,
                )
                objects[nid] = vi
            }
        }
        frigateMotions = freshSynthetics
        return state.copy(
            notifications = (state.notifications + freshSynthetics).take(12),
            motionObjects = objects,
        )
    }

    /**
     * Danh sach thiet bi day du (tung phuc vu tim kiem thiet bi, da xoa
     * theo GAP-12 — port iOS 487946e).
     * 1. Thiet bi tu config tinh (label dep) + climate moi phong.
     * 2. Tat ca entity light/switch/fan/cover/lock/climate con lai -> tu dong
     *    phan loai dung (dieu hoa / den / o cam / cong tac / quat / rem / khoa).
     * sub mang tu khoa loai de chip "Dieu hoa"/"O cam" luon match.
     */
    /**
     * Danh sach tim kiem (2026-09-30): CHI gom thiet bi co tren giao dien
     * (thiet bi + dieu hoa cua cac phong). Entity le khong thuoc phong nao
     * (khong hien tren UI) khong dua vao search theo yeu cau user.
     */
    private fun buildSearchDevices(
        entities: Map<String, LegacyEntity>,
        rooms: List<RoomUi>,
    ): List<DeviceUi> {
        val out = mutableListOf<DeviceUi>()
        rooms.forEach { r ->
            r.devices.forEach { d ->
                out.add(d.copy(sub = r.name))
            }
            r.climate?.let { c ->
                out.add(
                    DeviceUi(
                        entityId = c.entityId,
                        label = climateSearchLabel(entities[c.entityId]),
                        sub = r.name,
                        iconKey = "snowflake",
                        kind = DeviceKind.Climate,
                        isOn = c.isOn,
                    )
                )
            }
        }
        return out.distinctBy { it.entityId }
    }

    /** Label dieu hoa: dam bao luon chua "dieu hoa" de chip tim kiem match. */
    private fun climateSearchLabel(e: LegacyEntity?): String {
        if (e == null) return "Điều hoà"
        val n = friendlyName(e)
        return if (n.contains("điều hoà", ignoreCase = true) ||
            n.contains("điều hòa", ignoreCase = true)
        ) n else "Điều hoà"
    }

    /**
     * (2026-10-14, audit #9): Map trang thai dung cho tung loai thiet bi.
     * Cover: isOpen -> on; Lock: isLocked -> on; con lai: isOn.
     * (2026-11-17): LegacyEntity la data class HomeEntity nen khong co
     * subtype de `when (this) is ...` nhu sealed interface domain — phan
     * biet theo prefix entityId + state tho (cover chi "open" la mo,
     * lock chi "locked" la khoa; cac state chuyen tiep opening/closing/
     * locking/unlocking khong tinh la active).
     */
    private fun LegacyEntity.isActive(): Boolean = when {
        id.startsWith("cover.") -> state == "open"
        id.startsWith("lock.") -> state == "locked"
        else -> isOn
    }

    private fun buildRoom(room: RoomBubbleConfig, entities: Map<String, LegacyEntity>): RoomUi {
        val lights = room.devices.filter { it.entity.startsWith("light.") }
        val lightOn = lights.any { entities[it.entity]?.isOn == true }
        // (2026-10-14, audit #3): bo filter type=="toggle" — hien tat ca loai
        // thiet bi nhu iOS (toggle/fan/cover/lock/media_player/vacuum/humidifier)
        // (2026-10-22): loai "climate" khoi danh sach thiet bi — dieu hoa chi
        // hien bang ClimateCard rieng nhu iOS (HomeRooms.swift toggles khong
        // chua "climate"). Khong loc thi sheet render dieu hoa 2 lan + dem
        // thiet bi lech + cong suat phong bi cong trung climate 2 lan.
        val devices = room.devices
            .filter { it.type != "climate" }
            .map { d ->
                val e = entities[d.entity]
                DeviceUi(
                    entityId = d.entity,
                    label = d.label,
                    sub = d.sub,
                    iconKey = d.icon,
                    isOn = e?.isActive() == true,
                    powerW = d.powerEntity?.let { entities[it]?.numericState },
                    powerEntityId = d.powerEntity,
                )
            }
        val climateDevice = room.devices.firstOrNull { it.type == "climate" }
        val climateEntity = climateDevice?.let { entities[it.entity] }
        val climate = climateEntity?.let { e ->
            val modes = e.attributes["hvac_modes"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?.filter { it != "off" }
                .orEmpty()
            // Dieu hoa active khi state khac "off"/"unavailable" (cool/heat/auto/...)
            // — port tu iOS 7289baa (fix cong suat dieu hoa).
            val climateActive = e.state != "off" && e.state != "unavailable" &&
                e.state != "unknown" && e.state.isNotEmpty()
            val climatePowerEntity = climateDevice?.powerEntity
            ClimateUi(
                entityId = e.id,
                currentTemp = e.attributes["current_temperature"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
                targetTemp = e.attributes["temperature"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
                hvacMode = e.state,
                isOn = e.state != "off",
                modes = modes,
                powerW = if (climateActive)
                    climatePowerEntity?.let { entities[it]?.numericState }
                else null,
            )
        }
        // Tong cong suat phong = thiet bi dang bat + dieu hoa dang chay (iOS roomPowerW).
        val roomPowerW = devices
            .filter { it.isOn }
            .mapNotNull { it.powerW }
            .sum() + (climate?.powerW ?: 0.0)
        return RoomUi(
            key = room.key,
            name = room.label,
            iconKey = room.icon,
            tempC = entities[room.tempEntity]?.numericState,
            humidityPct = entities[room.humidityEntity]?.numericState,
            lightEntityId = lights.firstOrNull()?.entity,
            lightOn = lightOn,
            devicesOn = devices.count { it.isOn },
            deviceCount = devices.size,
            climate = climate,
            devices = devices,
            roomPowerW = roomPowerW,
        )
    }

    private fun friendlyName(e: LegacyEntity): String {
        val raw = e.attributes["friendly_name"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() } ?: e.id.substringAfter('.').replace('_', ' ')
        // Viet hoa ngan gon nhung ten con tieng Anh (friendly_name mac dinh
        // cua integration / fallback tu entity_id).
        return vietnameseShort(raw)
    }

    /**
     * Viet hoa ngan gon, ro nghia ten thiet bi con tieng Anh.
     * Ten da co dau tieng Viet duoc giu nguyen. Dich theo cum (dai truoc)
     * voi word-boundary; cum khong co trong tu dien giu nguyen.
     */
    private fun vietnameseShort(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return s
        // Da la tieng Viet (co ky tu co dau) -> giu nguyen
        if (s.any { it in '\u00c0'..'\u1ef9' }) return s
        var out = " ${s.lowercase()} "
        for ((en, vi) in VI_NAME_DICT) {
            out = out.replace(Regex("\\b${Regex.escape(en)}\\b"), vi)
        }
        // "l1".."l9" (kenh/line) -> chi giu so
        out = out.replace(Regex("\\bl([1-9])\\b"), "$1")
        out = out.replace(Regex("\\s+"), " ").trim()
        return out.replaceFirstChar { it.uppercase() }
    }

    /**
     * Tat ca sensor co mat tren giao dien (an ninh + the phong).
     * Thong bao CHI dung sensor trong danh sach nay (2026-09-30, user yeu cau).
     */
    private val uiSensorIds: Set<String> get() = buildSet {
        runCatching {
            addAll(com.smarthome.hume.core.data.HumeGraph.get().securityRepository.sensorEntityIds)
        }
        _roomConfigs.value.forEach { room ->
            room.tempEntity?.let { add(it) }
            room.humidityEntity?.let { add(it) }
            room.devices.filter { it.entity.startsWith("binary_sensor.") }
                .forEach { add(it.entity) }
        }
    }

    private fun buildNotifications(entities: Map<String, LegacyEntity>): List<HomeNotification> {
        val out = mutableListOf<HomeNotification>()
        entities.values
            .filter { it.id in uiSensorIds && it.id.startsWith("binary_sensor.") && it.state == "on" }
            .forEach { e ->
                val dc = e.attributes["device_class"]?.jsonPrimitive?.contentOrNull
                val name = friendlyName(e)
                val mins = e.minutesAgo()
                val time = when {
                    mins == null -> ""
                    mins < 1 -> "vừa xong"
                    mins < 60 -> "$mins phút trước"
                    else -> "${mins / 60} giờ trước"
                }
                when (dc) {
                    "door", "window", "garage_door", "opening" ->
                        out += HomeNotification(e.id, "Cửa đang mở", name, time, e.minutesAgo())
                    "motion", "occupancy", "presence" ->
                        out += HomeNotification(e.id, "Phát hiện chuyển động", name, time, e.minutesAgo())
                    "smoke" -> out += HomeNotification(e.id, "Cảnh báo khói!", name, time, e.minutesAgo())
                    "moisture" -> out += HomeNotification(e.id, "Phát hiện rò nước!", name, time, e.minutesAgo())
                }
            }
        return out.take(12)
    }

    /**
     * Pattern 3 buoc cua Hume goc (HomeViewModel.weekly):
     * 1. Ngay nao thieu -> fetch history tung ngay (song song), lay max.
     * 2. Ngay nao van thieu -> fetch 1 lan 7 ngay, trich max theo ngay,
     *    fallback = diem moi nhat truoc cuoi ngay.
     * 3. Luu DailySnapshotStore (persistent) + weekCache (memory).
     */
    private suspend fun loadSolarWeek() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val dayMs = 86_400_000L
        val labels = arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val dayStarts = (6 downTo 1).map { ago ->
            today.minusDays(ago.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        }

        // Buoc 1: doc cache truoc
        val resolved = HashMap<Long, Double>()
        val missing = dayStarts.filter { start ->
            val cached = snapshots.get(HumeConfig.PV_TODAY, start) ?: weekCache[start]
            if (cached != null && cached > 0) {
                resolved[start] = cached; false
            } else true
        }

        if (missing.isNotEmpty()) {
            // Buoc 1: moi ngay thieu = 1 query nho, chay song song
            val perDay: Map<Long, Double> = kotlinx.coroutines.coroutineScope {
                val deferreds = missing.map { start ->
                    async(Dispatchers.IO) {
                        val pts = runCatching {
                            HistoryFetcher.fetchRange(HumeConfig.PV_TODAY, start, start + dayMs)
                        }.getOrDefault(emptyList())
                        // sensor "today energy" reset moi ngay -> max trong ngay = san luong ngay do
                        start to (pts.maxOfOrNull { it.value } ?: 0.0)
                    }
                }
                deferreds.awaitAll().toMap()
            }
            perDay.forEach { (start, v) ->
                if (v > 0) {
                    snapshots.set(HumeConfig.PV_TODAY, start, v)
                    weekCache[start] = v
                    resolved[start] = v
                }
            }

            // Buoc 2: ngay nao van thieu -> keo 1 lan ca tuan
            val stillMissing = missing.filter { resolved[it] == null }
            if (stillMissing.isNotEmpty()) {
                val weekPts = runCatching {
                    HistoryFetcher.fetchRange(
                        HumeConfig.PV_TODAY,
                        dayStarts.first(),
                        dayStarts.last() + dayMs,
                    )
                }.getOrDefault(emptyList())
                if (weekPts.isNotEmpty()) {
                    stillMissing.forEach { start ->
                        val dayEnd = start + dayMs
                        val inDay = weekPts.filter { it.timeMs in start until dayEnd }
                        val v = inDay.maxOfOrNull { it.value }
                            ?: weekPts.filter { it.timeMs < dayEnd }
                                .maxByOrNull { it.timeMs }?.value
                            ?: 0.0
                        if (v > 0) {
                            snapshots.set(HumeConfig.PV_TODAY, start, v)
                            weekCache[start] = v
                            resolved[start] = v
                        }
                    }
                }
            }
            snapshots.prune()
        }

        val days = dayStarts.map { start ->
            val date = java.time.Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
            SolarDay(labels[date.dayOfWeek.value - 1], (resolved[start] ?: 0.0).toFloat())
        } + SolarDay(
            labels[today.dayOfWeek.value - 1],
            (ha.entities.value[HumeConfig.PV_TODAY]?.numericState ?: 0.0).toFloat(),
        )
        _homeState.value = _homeState.value.copy(solarWeek = days)
    }
}

/**
 * Tu dien Anh -> Viet ngan gon cho ten thiet bi (muc 5, 29/09).
 * Thu tu: cum dai truoc, tu don sau. Chi dung cho ten chua co dau
 * tieng Viet (ten thuong hieu nhu Solis/Aqara khong co trong nay
 * nen duoc giu nguyen).
 */
private val VI_NAME_DICT = listOf(
    // Cum chuyen biet
    "total pv power" to "tổng công suất điện mặt trời",
    "pv power" to "công suất điện mặt trời",
    "solar power" to "công suất điện mặt trời",
    "battery power" to "công suất pin",
    "battery soc" to "mức pin",
    "backup soc" to "pin dự phòng",
    "grid power" to "công suất lưới điện",
    "smart light" to "đèn thông minh",
    "smartlight" to "đèn thông minh",
    "ceiling light" to "đèn trần",
    "table lamp" to "đèn bàn",
    "desk lamp" to "đèn bàn học",
    "led strip" to "dải đèn led",
    "light strip" to "dải đèn",
    "night light" to "đèn ngủ",
    "motion sensor" to "cảm biến chuyển động",
    "door sensor" to "cảm biến cửa",
    "window sensor" to "cảm biến cửa sổ",
    "temperature sensor" to "cảm biến nhiệt độ",
    "humidity sensor" to "cảm biến độ ẩm",
    "smoke detector" to "báo khói",
    "smoke sensor" to "cảm biến khói",
    "water leak sensor" to "cảm biến rò rỉ nước",
    "leak sensor" to "cảm biến rò rỉ",
    "presence sensor" to "cảm biến hiện diện",
    "air conditioner" to "điều hòa",
    "air conditioning" to "điều hòa",
    "living room" to "phòng khách",
    "master bedroom" to "phòng ngủ chính",
    "kid bedroom" to "phòng trẻ em",
    "kids bedroom" to "phòng trẻ em",
    "bedroom" to "phòng ngủ",
    "dining room" to "phòng ăn",
    "prayer room" to "phòng thờ",
    "worship room" to "phòng thờ",
    "study room" to "phòng học",
    "kitchen" to "phòng bếp",
    "bathroom" to "phòng tắm",
    "office" to "phòng làm việc",
    "garage" to "nhà xe",
    "balcony" to "ban công",
    "terrace" to "sân thượng",
    "garden" to "sân vườn",
    "hallway" to "hành lang",
    "corridor" to "hành lang",
    "staircase" to "cầu thang",
    "stairs" to "cầu thang",
    "front door" to "cửa chính",
    "back door" to "cửa sau",
    "main door" to "cửa chính",
    "entrance" to "cửa chính",
    // Tu don
    "sensor" to "cảm biến",
    "detector" to "cảm biến",
    "lights" to "đèn",
    "light" to "đèn",
    "lamp" to "đèn",
    "bulb" to "bóng đèn",
    "switch" to "công tắc",
    "button" to "nút bấm",
    "remote" to "điều khiển",
    "dimmer" to "chiết áp",
    "plug" to "ổ cắm",
    "outlet" to "ổ cắm",
    "socket" to "ổ cắm",
    "fan" to "quạt",
    "heater" to "máy sưởi",
    "thermostat" to "điều nhiệt",
    "curtain" to "rèm",
    "blinds" to "rèm",
    "blind" to "rèm",
    "speaker" to "loa",
    "television" to "tivi",
    "tv" to "tivi",
    "camera" to "camera",
    "lock" to "khóa",
    "alarm" to "báo động",
    "siren" to "còi báo động",
    "smoke" to "khói",
    "leak" to "rò rỉ",
    "water" to "nước",
    "gas" to "gas",
    "motion" to "chuyển động",
    "presence" to "hiện diện",
    "door" to "cửa",
    "window" to "cửa sổ",
    "gate" to "cổng",
    "temperature" to "nhiệt độ",
    "humidity" to "độ ẩm",
    "temp" to "nhiệt độ",
    "power" to "công suất",
    "energy" to "điện năng",
    "voltage" to "điện áp",
    "current" to "dòng điện",
    "battery" to "pin",
    "solar" to "mặt trời",
    "pv" to "điện mặt trời",
    "grid" to "lưới điện",
    "backup" to "dự phòng",
    "total" to "tổng",
    "today" to "hôm nay",
    "daily" to "hằng ngày",
    "yesterday" to "hôm qua",
    "smart" to "thông minh",
    "ceiling" to "trần",
    "table" to "bàn",
    "desk" to "bàn",
    "wall" to "tường",
    "floor" to "sàn",
    "outdoor" to "ngoài trời",
    "indoor" to "trong nhà",
    "outside" to "ngoài trời",
    "inside" to "trong nhà",
    "front" to "trước",
    "back" to "sau",
    "left" to "trái",
    "right" to "phải",
    "main" to "chính",
    "room" to "phòng",
)

// ---------- BatteryTimeCalc v70 (N-03) ----------
// Port 1:1 tu iOS Hume/M3E/Tabs/BatteryTimeCalc.swift (v70) — cong thuc thoi
// gian sac/xa cho the "Hieu nang pin". Xem ghi chu dau file iOS ve 6 diem
// moi (EMA, cua so SOC live, 2 pha CC/CV, 2 phase xa, mien Ah/A, bien).
// parseDurationToEndTime/durationToMinutes cu (chi phuc vu fallback sensor
// Solis) da xoa theo — khong con noi nao dung.

/** Setting BMS live tren master — port 1:1 iOS PowerwallLiveData.settingEntityIds
 * (11 entity). Dung chung cho ca POWERWALL_ENTITY_IDS (full, N-12) va
 * BATTERY_TIME_CALC_ENTITY_IDS (N-03). charge_voltage_limit da co trong
 * packKeys nen KHONG lap lai o day (nhu iOS). */
private val POWERWALL_SETTING_ENTITY_IDS: List<String> = listOf(
    "setting_charge_cv", "setting_cutoff_soc", "setting_cutoff_c_rate",
    "setting_charge_limit_c_rate", "setting_discharge_cutoff_soc",
    "setting_discharge_limit_c_rate", "setting_cell_max_volt",
    "setting_cell_min_volt", "setting_cutoff_volt", "setting_undervoltage",
    "py_chg_current_limit",
).map { "sensor.powerwall_bms_master_$it" }

/** Toan bo entity Powerwall can watch — port 1:1 iOS PowerwallLiveData.entityIds
 * (N-12, buoc bat buoc truoc khi mo trang Powerwall — N-02 port song song).
 * iOS HomeAssistantManager.rebuildWatchedSet §4f: ids.formUnion(entityIds).
 * Tong 205 entity duy nhat: 4 pack x 46 key (184) + master-only 4
 * (temp/cell max-min id) + system 6 (py_system_* + py_protection/alarm_flags)
 * + setting live 11. Pack 2 (slave 2 / BMS1) la slot de trong nen iOS khong
 * watch — copy nguyen, khong them pack_2. Luu y: "~220" trong mo ta gap chi
 * la uoc luong; dem dung tu code iOS la 205.
 * 31 entity cua BATTERY_TIME_CALC_ENTITY_IDS (N-03) la tap con cua set nay. */
private val POWERWALL_ENTITY_IDS: List<String> = buildList {
    // Key sensor dung chung cho moi pack — port iOS PowerwallLiveData.packKeys
    // (46 key: 30 key co dinh + cell_1..cell_16).
    val packKeys = listOf(
        "soc", "soh", "voltage", "current",
        "temp_avg", "temp_cell_1", "temp_cell_2", "temp_cell_3", "temp_max", "temp_min",
        "cell_max", "cell_min", "cell_spread",
        "status", "error_id", "output",
        "full_charge_cap", "remaining_cap",
        "charge_current_limit", "charge_voltage_limit", "discharge_current_limit",
        "adc_battery_volt", "adc_fuse_volt", "adc_pack_volt",
        "board_temp", "fet_temp", "charge_fet", "discharge_fet", "precharge_fet",
        "firmware_version",
    ) + (1..16).map { "cell_$it" }
    for (p in listOf("master", "pack_1", "pack_3", "pack_4")) {
        for (key in packKeys) add("sensor.powerwall_bms_${p}_$key")
    }
    // Master-only: id cua cell/temp cao-thap.
    for (key in listOf("temp_max_cell_id", "temp_min_cell_id", "cell_max_id", "cell_min_id")) {
        add("sensor.powerwall_bms_master_$key")
    }
    // System-level (inverter-facing, Pylontech).
    for (key in listOf(
        "py_system_soc", "py_system_soh", "py_system_voltage",
        "py_system_current", "py_protection_flags", "py_alarm_flags",
    )) {
        add("sensor.powerwall_bms_master_$key")
    }
    addAll(POWERWALL_SETTING_ENTITY_IDS)
}

/** Entity toi thieu cho cong thuc thoi gian sac/xa — port 1:1 iOS
 * PowerwallLiveData.timeCalcEntityIds (31 thay vi 205 entity day du). */
private val BATTERY_TIME_CALC_ENTITY_IDS: List<String> = buildList {
    val mp = "sensor.powerwall_bms_master_"
    add(mp + "py_system_soc"); add(mp + "py_system_voltage")
    add(mp + "py_system_current"); add(mp + "cell_max")
    for (p in listOf("master", "pack_1", "pack_3", "pack_4")) {
        for (key in listOf(
            "full_charge_cap", "remaining_cap",
            "charge_current_limit", "discharge_current_limit"
        )) {
            add("sensor.powerwall_bms_${p}_$key")
        }
    }
    // Setting BMS live tren master — dung chung POWERWALL_SETTING_ENTITY_IDS
    // (port iOS PowerwallLiveData.settingEntityIds), khong lap lai literal.
    addAll(POWERWALL_SETTING_ENTITY_IDS)
}

/** Setting BMS — so CHINH luon la live tu sensor setting_* (master).
 * Fallback duoi day = gia tri hardcode cu trong iOS, chi dung khi entity live
 * thieu hoac unknown; da kiem chung 19:44 16/11: live khop 100% cac so nay. */
private data class BatterySettings(
    val chargeCV: Double = 54.4, // setting_charge_cv (V)
    val cutoffSoc: Double = 97.0, // setting_cutoff_soc (%)
    val cutoffCRate: Double = 0.05, // setting_cutoff_c_rate (C)
    val chargeLimitCRate: Double = 0.30, // setting_charge_limit_c_rate (C)
    val dischargeCutoffSoc: Double = 5.0, // setting_discharge_cutoff_soc (%)
    val dischargeLimitCRate: Double = 0.30, // setting_discharge_limit_c_rate (C)
    val cellMaxVolt: Double = 3.400, // setting_cell_max_volt (V)
    val cellMinVolt: Double = 3.000, // setting_cell_min_volt (V)
    val cutoffVolt: Double = 48.0, // setting_cutoff_volt (V)
    val undervoltage: Double = 40.0, // setting_undervoltage (V)
)

/** Doc setting live tu entity setting_* tren master; field nao thieu thi giu
 * fallback (port iOS BatterySettings.from(sys:) — key sys setCV/setCutSOC/
 * setCutC/setLimC_C/setDisCutSOC/setDisLimC_C/setCellMax/setCellMin/setCutV/
 * setUVP map 1:1 sang entity setting_* tuong ung). */
private fun batterySettingsFrom(entities: Map<String, LegacyEntity>): BatterySettings {
    val mp = "sensor.powerwall_bms_master_"
    fun num(key: String): Double? = entities[mp + key]?.numericState
    return BatterySettings(
        chargeCV = num("setting_charge_cv") ?: 54.4,
        cutoffSoc = num("setting_cutoff_soc") ?: 97.0,
        cutoffCRate = num("setting_cutoff_c_rate") ?: 0.05,
        chargeLimitCRate = num("setting_charge_limit_c_rate") ?: 0.30,
        dischargeCutoffSoc = num("setting_discharge_cutoff_soc") ?: 5.0,
        dischargeLimitCRate = num("setting_discharge_limit_c_rate") ?: 0.30,
        cellMaxVolt = num("setting_cell_max_volt") ?: 3.400,
        cellMinVolt = num("setting_cell_min_volt") ?: 3.000,
        cutoffVolt = num("setting_cutoff_volt") ?: 48.0,
        undervoltage = num("setting_undervoltage") ?: 40.0,
    )
}

/** Ket qua uoc luong thoi gian (hien thi o the pin) — port iOS BatteryTime. */
private sealed interface BatteryTime {
    data class Minutes(val minutes: Int) : BatteryTime
    data object Unknown : BatteryTime // "—": khong uoc luong duoc (dong ≈ 0 / thieu du lieu)
    data object Full : BatteryTime // "Đã đầy": SOC da o nguong ngat sac
    data object Empty : BatteryTime // "Đã cạn": SOC da o nguong ngat xa
}

/** Nguong deadband iOS CurrentEMA.deadband: duoi 2.0 A coi nhu khong co dong
 * (nghi / trickle). 2.0 A ≈ 100 W o 52 V. */
private const val BATTERY_TIME_DEADBAND_A = 2.0

/** Goi CV (GIA DINH — xem ghi chu dau file iOS): pha CV bat dau o SOC nay.
 * Port iOS BatteryTimeCalc.socCVKnee = 90.0. */
private const val BATTERY_TIME_SOC_CV_KNEE = 90.0

private fun batteryTimeCalc(
    soc: Double,
    currentEma: Double, // A, da EMA; sac +, xa − (lay theo chieu dang xet)
    cellMax: Double?, // dien ap cell cao nhat (V), de phat hien pha CV
    fccAh: Double, // Σ full_charge_cap (Ah)
    limChgA: Double, // gioi han dong sac he thong (A)
    limDisA: Double, // gioi han dong xa he thong (A)
    settings: BatterySettings,
    dischargeLimitSoc: Double, // limit hieu dung luc xa: backupSoc (co luoi) hoac overdischargeSoc (mat dien), %
    charging: Boolean,
    discharging: Boolean,
): BatteryTime {
    if (charging) return batteryChargeTime(soc, currentEma, cellMax, fccAh, limChgA, settings)
    if (discharging) {
        return batteryDischargeTime(soc, currentEma, fccAh, limDisA, settings, dischargeLimitSoc)
    }
    return BatteryTime.Unknown
}

/** Thoi gian sac day toi nguong ngat (phut): pha CC + pha CV (taper).
 * Port iOS BatteryTimeCalc.chargeTime. */
private fun batteryChargeTime(
    soc: Double,
    currentEma: Double,
    cellMax: Double?,
    fccAh: Double,
    limChgA: Double,
    settings: BatterySettings,
): BatteryTime {
    if (fccAh <= 0) return BatteryTime.Unknown
    if (currentEma <= BATTERY_TIME_DEADBAND_A) return BatteryTime.Unknown
    if (soc >= settings.cutoffSoc) return BatteryTime.Full
    val qTgt = settings.cutoffSoc / 100 * fccAh
    val qNow = soc / 100 * fccAh
    val dQ = qTgt - qNow
    if (dQ <= 0) return BatteryTime.Full
    val iCut = settings.cutoffCRate * fccAh
    val iCap = minOf(limChgA, settings.chargeLimitCRate * fccAh)
    val iCC = minOf(currentEma, iCap)
    if (iCC <= BATTERY_TIME_DEADBAND_A) return BatteryTime.Unknown
    // Vao pha CV khi cell cao nhat cham nguong HOAC SOC da qua goi.
    val cvCell = minOf(settings.cellMaxVolt, settings.chargeCV / 16)
    val qKnee = BATTERY_TIME_SOC_CV_KNEE / 100 * fccAh
    val inCV = (cellMax != null && cellMax >= cvCell) || qNow >= qKnee
    val dQcc = if (inCV) 0.0 else maxOf(0.0, qKnee - qNow)
    val dQcv = dQ - dQcc
    val tCC = dQcc / iCC // gio
    // Pha CV: dong giam tuyen tinh tu dong vao pha xuong nguong ngat
    // (neu dong vao da duoi nguong ngat thi khong con gi de giam).
    val iCVEnd = minOf(iCC, iCut)
    val iCVAvg = (iCC + iCVEnd) / 2
    val tCV = dQcv / maxOf(iCVAvg, BATTERY_TIME_DEADBAND_A) // gio
    return BatteryTime.Minutes(((tCC + tCV) * 60).roundToInt())
}

/** Thoi gian xa toi limit hieu dung / toi nguong ngat 5% (phut).
 * Port iOS BatteryTimeCalc.dischargeTime. */
private fun batteryDischargeTime(
    soc: Double,
    currentEma: Double,
    fccAh: Double,
    limDisA: Double,
    settings: BatterySettings,
    dischargeLimitSoc: Double,
): BatteryTime {
    if (fccAh <= 0) return BatteryTime.Unknown
    if (currentEma >= -BATTERY_TIME_DEADBAND_A) return BatteryTime.Unknown
    if (soc <= settings.dischargeCutoffSoc) return BatteryTime.Empty
    val iDis = minOf(kotlin.math.abs(currentEma), limDisA, settings.dischargeLimitCRate * fccAh)
    if (iDis <= BATTERY_TIME_DEADBAND_A) return BatteryTime.Unknown
    val qNow = soc / 100 * fccAh
    val dQ: Double
    if (soc > dischargeLimitSoc) {
        // Phase 1: phan su dung, toi limit hieu dung.
        dQ = qNow - dischargeLimitSoc / 100 * fccAh
    } else {
        // Phase 2: phan du tru, toi nguong ngat xa cua BMS (5%, live)
        // — cu di toi 0% la sai (BMS ngat truoc).
        dQ = qNow - settings.dischargeCutoffSoc / 100 * fccAh
    }
    if (dQ <= 0) return BatteryTime.Empty
    return BatteryTime.Minutes(((dQ / iDis) * 60).roundToInt())
}
