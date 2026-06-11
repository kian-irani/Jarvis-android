package com.kianirani.jarvis.brain.score

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Encodes [DeviceMetrics] into the node-registry `capabilities` JSON column and
 * back, so heartbeats carry the raw inputs of [BrainScoreCalculator] instead of
 * a pre-computed score (ADR-006: election must be recomputable on any node).
 */
object NodeMetricsCodec {

    @Serializable
    private data class MetricsDto(
        val ram_free_gb: Double,
        val cpu_cores: Int,
        val is_vps: Boolean = false,
        val battery_percent: Int = 100,
        val network_mbps: Double = 0.0,
        val on_battery: Boolean = false,
        val thermal_throttling: Boolean = false,
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(m: DeviceMetrics): String = json.encodeToString(
        MetricsDto.serializer(),
        MetricsDto(m.ramFreeGb, m.cpuCores, m.isVps, m.batteryPercent, m.networkMbps, m.isOnBattery, m.thermalThrottling),
    )

    /** Returns null when [capabilities] is not valid JSON or lacks metric keys. */
    fun decode(capabilities: String): DeviceMetrics? = try {
        val obj = json.parseToJsonElement(capabilities).jsonObject
        if ("ram_free_gb" !in obj) {
            null
        } else {
            val d = json.decodeFromJsonElement(MetricsDto.serializer(), obj)
            DeviceMetrics(d.ram_free_gb, d.cpu_cores, d.is_vps, d.battery_percent, d.network_mbps, d.on_battery, d.thermal_throttling)
        }
    } catch (_: Exception) {
        null
    }
}
