package com.smarthome.hume.core.model

/**
 * Logic map cam bien chuyen dong -> phong / camera Frigate, dung chung cho
 * app (AppHomeRepository) va feature/home. (Chuyen tu HomeCards 2026-09-30
 * de app module cung dung duoc khi gan nhan doi tuong Frigate.)
 */

/**
 * Map entity cam bien chuyen dong -> ten phong de goi y theo phong.
 * (entity_id HA khong dau, vd binary_sensor.cam_bien_pir_phong_tho_occupancy)
 */
fun roomNameForSensor(entityId: String): String? {
    val id = entityId.lowercase()
    return when {
        "phong_ngu_be" in id || "tre_em" in id -> "Phòng Trẻ Em"
        "phong_ngu" in id || "bedroom" in id -> "Phòng Ngủ"
        "phong_tho" in id || "worship" in id -> "Phòng Thờ"
        "phong_tam" in id || "nha_tam" in id || "ve_sinh" in id || "bath" in id -> "Phòng Tắm"
        "phong_khach" in id || "living" in id -> "Phòng Khách"
        "bep" in id || "phong_an" in id || "kitchen" in id -> "Phòng Bếp"
        "giat" in id || "washing" in id || "laundry" in id -> "Phòng Giặt"
        "hanh_lang" in id || "hall" in id -> "Hành Lang"
        // Ngoai troi/san: phai co truoc khi fallback null de map dung cam
        // ngoai troi thay vi roi ve camera dau tien.
        "ngoai_troi" in id || "ngoai" in id || "outdoor" in id ||
            "san_truoc" in id || "san_sau" in id || "sanh" in id -> "Ngoài trời"
        else -> null
    }
}

/** Key camera Frigate/app (khop AppSecurityRepository.CAMERAS). */
val FRIGATE_CAMERA_KEYS = listOf("living", "kitchen", "outdoor", "server", "bedroom")

/** Key camera Frigate gan nhat voi sensor chuyen dong (null neu khong map duoc). */
fun cameraKeyForSensor(sensorId: String): String? {
    val id = sensorId.lowercase()
    // 1. Key camera xuat hien truc tiep trong entity_id.
    FRIGATE_CAMERA_KEYS.firstOrNull { it in id }?.let { return it }
    // 2. Theo phong cua sensor — dung mapping user chot 2026-09-30:
    // ngoai troi->outdoor; phong khach->living; phong an/hanh lang T1->kitchen;
    // phong ngu (ca phong ngu be)->bedroom; tang 3 (phong tho)->server.
    // Phong tam / phong giat KHONG co camera -> null (khong doan bua).
    return when (roomNameForSensor(sensorId)) {
        "Phòng Khách" -> "living"
        "Phòng Bếp" -> "kitchen"
        "Phòng Ngủ", "Phòng Trẻ Em" -> "bedroom"
        "Phòng Thờ" -> "server"
        "Ngoài trời" -> "outdoor"
        "Hành Lang" -> "kitchen"
        else -> null
    }
}

/** Ten hien thi tieng Viet cua camera Frigate theo key (khop AppSecurityRepository.CAMERAS). */
fun cameraNameVi(key: String): String = when (key) {
    "living" -> "Phòng khách"
    "kitchen" -> "Phòng ăn"
    "outdoor" -> "Ngoài trời"
    "server" -> "Phòng thờ"
    "bedroom" -> "Phòng ngủ"
    else -> key
}

/** Nhan doi tuong Frigate (person, car, dog...) -> tieng Viet cho goi y. */
fun frigateLabelVi(label: String): String = when (label.lowercase()) {
    "person" -> "người"
    "car" -> "ô tô"
    "vehicle" -> "phương tiện"
    "truck" -> "xe tải"
    "bus" -> "xe buýt"
    "motorcycle", "motorbike" -> "xe máy"
    "bicycle" -> "xe đạp"
    "dog" -> "chó"
    "cat" -> "mèo"
    "bird" -> "chim"
    else -> "vật thể"
}
