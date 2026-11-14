package com.ondy.app.ui.screens.apps

import com.ondy.app.domain.model.AppInfo
import com.ondy.app.domain.repository.SelectedAppRepository
import com.ondy.app.testutil.MainDispatcherRule
import com.ondy.app.util.AppCache
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var selectedAppRepository: SelectedAppRepository
    private lateinit var appCache: AppCache
    private lateinit var viewModel: AppsViewModel

    @Before
    fun setup() {
        selectedAppRepository = mockk(relaxed = true)
        appCache = mockk(relaxed = true)
        viewModel = AppsViewModel(selectedAppRepository, appCache)
    }

    @Test
    fun `loadApps populates state and applies the default user filter`() = runTest {
        stubApps(
            AppInfo("com.test.app1", "Test App 1", isUserApp = true),
            AppInfo("com.test.app2", "Test App 2", isUserApp = false)
        )

        viewModel.loadApps()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.apps.size)
        assertEquals(1, state.filteredApps.size)
        assertTrue(state.filteredApps.single().isUserApp)
    }

    @Test
    fun `loadApps marks previously selected apps`() = runTest {
        stubApps(
            AppInfo("com.test.app1", "Test App 1", isUserApp = true)
        )
        coEvery { selectedAppRepository.getSelectedAppsList() } returns
            listOf("com.test.app1")

        viewModel.loadApps()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.apps.single().isSelected)
    }

    @Test
    fun `toggleAppSelection updates selection state`() = runTest {
        stubApps(AppInfo("com.test.app1", "Test App 1", isUserApp = true))
        viewModel.loadApps()
        advanceUntilIdle()
        coEvery { selectedAppRepository.toggleAppSelection(any()) } just Runs

        viewModel.toggleAppSelection("com.test.app1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.apps.single().isSelected)
        coVerify(exactly = 1) { selectedAppRepository.toggleAppSelection("com.test.app1") }
    }

    @Test
    fun `search query filters apps by name`() = runTest {
        stubApps(
            AppInfo("com.test.app1", "Test App 1", isUserApp = true),
            AppInfo("com.test.app2", "Another App", isUserApp = true)
        )
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Test")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredApps.size)
        assertEquals("Test App 1", state.filteredApps.single().appName)
    }

    @Test
    fun `filter system shows only system apps`() = runTest {
        stubApps(
            AppInfo("com.test.app1", "User App", isUserApp = true),
            AppInfo("com.test.app2", "System App", isUserApp = false)
        )
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.setFilter(AppFilter.SYSTEM)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredApps.size)
        assertFalse(state.filteredApps.single().isUserApp)
    }

    private fun stubApps(vararg apps: AppInfo) {
        coEvery { selectedAppRepository.getSelectedAppsList() } returns emptyList()
        coEvery { appCache.ensureLoaded(any()) } just Runs
        every { appCache.getAllApps() } returns apps.toList()
    }
}
