package com.ondy.app.data.repository

import com.ondy.app.data.local.dao.BlockedNotificationDao
import com.ondy.app.domain.model.BlockedNotification
import com.ondy.app.testutil.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = mockk<BlockedNotificationDao>()
    private lateinit var repository: NotificationRepositoryImpl

    @Before
    fun setup() {
        repository = NotificationRepositoryImpl(dao)
    }

    @Test
    fun `saveNotification upserts entity, prunes history and returns id`() = runTest {
        val notification = notification("test_key")
        coEvery { dao.upsert(any()) } returns 1L
        coEvery { dao.pruneTo(any()) } just Runs

        val id = repository.saveNotification(notification)

        assertEquals(1L, id)
        coVerify(exactly = 1) { dao.upsert(any()) }
        coVerify(exactly = 1) { dao.pruneTo(NotificationRepositoryImpl.MAX_HISTORY) }
    }

    @Test
    fun `saveNotification supplies a key when one is missing`() = runTest {
        coEvery { dao.upsert(any()) } returns 2L
        coEvery { dao.pruneTo(any()) } just Runs

        repository.saveNotification(notification(notificationKey = null))

        coVerify {
            dao.upsert(match { it.notificationKey.isNotBlank() })
        }
    }

    @Test
    fun `deleteNotification calls dao deleteById`() = runTest {
        coEvery { dao.deleteById(any()) } just Runs

        repository.deleteNotification(123L)

        coVerify(exactly = 1) { dao.deleteById(123L) }
    }

    @Test
    fun `deleteAllNotifications calls dao deleteAll`() = runTest {
        coEvery { dao.deleteAll() } just Runs

        repository.deleteAllNotifications()

        coVerify(exactly = 1) { dao.deleteAll() }
    }

    private fun notification(notificationKey: String?) = BlockedNotification(
        packageName = "com.test.app",
        appName = "Test App",
        title = "Test Title",
        content = "Test Content",
        timestamp = 1_000L,
        notificationKey = notificationKey
    )
}
