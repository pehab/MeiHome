package de.haberland.meihome

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import de.haberland.meihome.ui.MeiHomeApp

class MainActivity : ComponentActivity() {
    private lateinit var appUpdateManager: AppUpdateManager
    private var updateReadyToInstall by mutableStateOf(false)

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        // A cancelled flexible update is harmless; the next app start checks again.
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            updateReadyToInstall = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateUpdatedListener)
        checkForFlexibleUpdate()

        setContent {
            MeiHomeApp(
                updateReadyToInstall = updateReadyToInstall,
                onInstallUpdate = { appUpdateManager.completeUpdate() },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    updateReadyToInstall = true
                }
            }
        }
    }

    override fun onDestroy() {
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        }
        super.onDestroy()
    }

    private fun checkForFlexibleUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    updateReadyToInstall = true
                }

                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    )
                }
            }
        }
    }
}
