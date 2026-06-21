package co.hermesdispatch.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import co.hermesdispatch.app.ui.nav.AppNav
import co.hermesdispatch.app.ui.theme.HermesDispatchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val deepLinkSessionId = mutableStateOf<String?>(null)

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        deepLinkSessionId.value = intent?.getStringExtra(EXTRA_SESSION_ID)
        setContent {
            HermesDispatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNav(
                        deepLinkSessionId = deepLinkSessionId.value,
                        onDeepLinkHandled = { deepLinkSessionId.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkSessionId.value = intent.getStringExtra(EXTRA_SESSION_ID)
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val EXTRA_SESSION_ID = "co.hermesdispatch.app.SESSION_ID"
    }
}
