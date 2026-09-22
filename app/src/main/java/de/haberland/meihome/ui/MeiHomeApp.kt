package de.haberland.meihome.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import de.haberland.meihome.ui.dashboard.DashboardScreen

@Composable
fun MeiHomeApp() {
    MaterialTheme {
        DashboardScreen()
    }
}
