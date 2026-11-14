package com.ondy.app.data.backup

import com.ondy.app.domain.model.BackupData
import com.ondy.app.domain.model.ScheduleTime
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Reads and writes the portable JSON backup format.
 *
 * The format is intentionally small and stable:
 *
 * ```json
 * {
 *   "version": 1,
 *   "exportedAt": 1730000000000,
 *   "selectedApps": ["com.example.app"],
 *   "scheduleTimes": [{ "hour": 9, "minute": 30 }]
 * }
 * ```
 *
 * Decoding is deliberately forgiving: unknown fields are ignored, duplicate
 * entries are collapsed, and out-of-range schedule entries are dropped. Only a
 * wrong version, a non-object document or missing data sections make an import
 * fail, so backups survive small future additions to the format.
 */
object BackupCodec {

    const val FORMAT_VERSION = 1

    /** Upper bounds protect the app from absurd or malicious files. */
    private const val MAX_APPS = 5_000
    private const val MAX_SCHEDULE_ENTRIES = 1_440

    private const val KEY_VERSION = "version"
    private const val KEY_EXPORTED_AT = "exportedAt"
    private const val KEY_SELECTED_APPS = "selectedApps"
    private const val KEY_SCHEDULE_TIMES = "scheduleTimes"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"

    fun encode(data: BackupData, exportedAt: Long = System.currentTimeMillis()): String {
        val apps = JSONArray().apply {
            data.selectedApps
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
                .forEach(::put)
        }

        val times = JSONArray().apply {
            data.scheduleTimes
                .asSequence()
                .filter { it.hour in 0..23 && it.minute in 0..59 }
                .distinctBy { it.hour to it.minute }
                .sortedWith(compareBy({ it.hour }, { it.minute }))
                .forEach { time ->
                    put(
                        JSONObject()
                            .put(KEY_HOUR, time.hour)
                            .put(KEY_MINUTE, time.minute)
                    )
                }
        }

        return JSONObject()
            .put(KEY_VERSION, FORMAT_VERSION)
            .put(KEY_EXPORTED_AT, exportedAt)
            .put(KEY_SELECTED_APPS, apps)
            .put(KEY_SCHEDULE_TIMES, times)
            .toString(2)
    }

    /**
     * @throws IllegalArgumentException when [json] is not a supported Ondy backup.
     */
    fun decode(json: String): BackupData {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Backup is not a JSON object", e)
        }

        val version = root.optInt(KEY_VERSION, -1)
        require(version == FORMAT_VERSION) {
            "Unsupported backup version: $version"
        }
        require(root.has(KEY_SELECTED_APPS) || root.has(KEY_SCHEDULE_TIMES)) {
            "Backup does not contain any Ondy data"
        }

        return BackupData(
            selectedApps = root.optJSONArray(KEY_SELECTED_APPS).toPackageNames(),
            scheduleTimes = root.optJSONArray(KEY_SCHEDULE_TIMES).toScheduleTimes()
        )
    }

    private fun JSONArray?.toPackageNames(): List<String> {
        if (this == null) return emptyList()

        val apps = LinkedHashSet<String>()
        val limit = minOf(length(), MAX_APPS)
        for (index in 0 until limit) {
            val value = optString(index).trim()
            if (value.isNotEmpty() && value.length <= MAX_PACKAGE_LENGTH) {
                apps += value
            }
        }
        return apps.toList()
    }

    private fun JSONArray?.toScheduleTimes(): List<ScheduleTime> {
        if (this == null) return emptyList()

        val times = LinkedHashMap<Pair<Int, Int>, ScheduleTime>()
        val limit = minOf(length(), MAX_SCHEDULE_ENTRIES)
        for (index in 0 until limit) {
            val entry = optJSONObject(index) ?: continue
            val hour = entry.optInt(KEY_HOUR, -1)
            val minute = entry.optInt(KEY_MINUTE, -1)
            if (hour in 0..23 && minute in 0..59) {
                times.putIfAbsent(hour to minute, ScheduleTime(hour = hour, minute = minute))
            }
        }
        return times.values.toList()
    }

    private const val MAX_PACKAGE_LENGTH = 255
}
