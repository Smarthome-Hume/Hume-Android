package com.smarthome.hume.core.ha

/**
 * Refresh rates ported from HomeAssistantManager.swift.
 *
 * Every entity that changes is placed in a bucket; only REALTIME entities push a
 * new state map immediately. Everything else is coalesced and flushed on its own
 * interval, which keeps 1594 entities from recomposing the whole screen.
 */
enum class UpdateBucket(val intervalMs: Long) {
    REALTIME(0L),
    TEN_SECONDS(10_000L),
    THIRTY_SECONDS(30_000L),
    FIVE_MINUTES(300_000L),
    THIRTY_MINUTES(1_800_000L),
    ONE_HOUR(3_600_000L),
    TWO_HOURS(7_200_000L),
    ONE_DAY(86_400_000L),
}

/** One row of Home Assistant's entity registry (config/entity_registry/list). */
data class RegistryEntry(
    val entityId: String,
    val name: String?,
    val areaId: String?,
    val deviceId: String?,
    val platform: String?,
)
