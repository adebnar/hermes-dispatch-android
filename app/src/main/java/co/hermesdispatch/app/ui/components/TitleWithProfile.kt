package co.hermesdispatch.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow

/**
 * Top-bar title that also shows which profile the screen is scoped to. Tasks,
 * Inbox, and Scheduled are all per-profile; surfacing the active profile here
 * makes it obvious why something created under another profile isn't listed.
 */
@Composable
fun TitleWithProfile(title: String, profile: String) {
    Column {
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (profile.isNotBlank()) {
            Text(
                "Profile: $profile",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
