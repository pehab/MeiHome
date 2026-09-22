package de.haberland.meihome.ui.dashboard

import androidx.lifecycle.ViewModel
import de.haberland.meihome.data.preferences.DashboardPreferencesRepository
import kotlinx.coroutines.flow.StateFlow

class DashboardViewModel(
    preferencesRepository: DashboardPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = preferencesRepository.dashboardState
}
