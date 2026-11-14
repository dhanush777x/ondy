package com.ondy.app.data.backup

import com.ondy.app.domain.model.BackupData
import com.ondy.app.domain.model.ScheduleTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    @Test
    fun `round trip preserves apps and schedule`() {
        val original = BackupData(
            selectedApps = listOf("com.example.b", "com.example.a"),
            scheduleTimes = listOf(
                ScheduleTime(hour = 9, minute = 30),
                ScheduleTime(hour = 21, minute = 0)
            )
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(original, exportedAt = 1L))

        assertEquals(listOf("com.example.a", "com.example.b"), decoded.selectedApps)
        assertEquals(
            listOf(ScheduleTime(hour = 9, minute = 30), ScheduleTime(hour = 21, minute = 0)),
            decoded.scheduleTimes
        )
    }

    @Test
    fun `encode sorts and deduplicates entries`() {
        val data = BackupData(
            selectedApps = listOf("com.b", "com.a", "com.b", " "),
            scheduleTimes = listOf(
                ScheduleTime(hour = 18, minute = 5),
                ScheduleTime(hour = 18, minute = 5),
                ScheduleTime(hour = 7, minute = 0)
            )
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(data))

        assertEquals(listOf("com.a", "com.b"), decoded.selectedApps)
        assertEquals(2, decoded.scheduleTimes.size)
        assertEquals(7, decoded.scheduleTimes.first().hour)
    }

    @Test
    fun `decode drops out of range schedule entries`() {
        val json = """
            {
              "version": 1,
              "selectedApps": ["com.a"],
              "scheduleTimes": [
                {"hour": 25, "minute": 0},
                {"hour": 12, "minute": 60},
                {"hour": 12, "minute": 15},
                {"hour": "bad", "minute": 0}
              ]
            }
        """.trimIndent()

        val decoded = BackupCodec.decode(json)

        assertEquals(listOf(ScheduleTime(hour = 12, minute = 15)), decoded.scheduleTimes)
    }

    @Test
    fun `decode accepts an empty but valid backup`() {
        val decoded = BackupCodec.decode(
            """{"version":1,"selectedApps":[],"scheduleTimes":[]}"""
        )

        assertTrue(decoded.isEmpty)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects an unsupported version`() {
        BackupCodec.decode("""{"version":99,"selectedApps":[]}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects a document without ond y data`() {
        BackupCodec.decode("""{"version":1,"somethingElse":true}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects a non json document`() {
        BackupCodec.decode("not a backup")
    }
}
