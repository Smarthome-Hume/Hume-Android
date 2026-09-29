package com.smarthome.hume.core.model

/** Camera Frigate co dinh (port tu SecurityView.swift). */
data class SecurityCamera(
    val key: String,
    val name: String,
)

enum class SensorKind { Door, Motion, Smoke, Leak }

/** Mot cam bien trong grid An ninh (demo v4: .scard). */
data class SensorUi(
    val entityId: String,
    val name: String,
    val iconKey: String,
    val kind: SensorKind,
    /** true = trang thai bao dong (cua mo / phat hien chuyen dong). */
    val isOn: Boolean = false,
    val lastChange: String = "",
    /** true = cam bien canh bao (khoi/nuoc) — dung tertiaryContainer khi on. */
    val warn: Boolean = false,
)

/** Mot clip ghi hinh Frigate (demo v4: .rec). */
data class RecordingUi(
    val id: String,
    val timeLabel: String,
    val dateLabel: String,
    /** Duong dan file mp4 da tai ve local (FrigateStore) — null neu chua tai xong. */
    val clipPath: String? = null,
)

data class SecurityUiState(
    val cameras: List<SecurityCamera> = emptyList(),
    val doorSensors: List<SensorUi> = emptyList(),
    val motionSensors: List<SensorUi> = emptyList(),
    val envSensors: List<SensorUi> = emptyList(),
    /** key = camera.key */
    val recordings: Map<String, List<RecordingUi>> = emptyMap(),
    /** Camera dang tai clip (key = camera.key) — hien spinner o nut 'Tai 10 clip'. */
    val downloadingCams: Set<String> = emptySet(),
)
