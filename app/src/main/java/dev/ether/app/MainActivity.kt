package dev.ether.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import dev.ether.app.ui.EtherRoot
import dev.ether.app.ui.theme.EtherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EtherTheme {
                // Exposes Compose testTags as resource-ids so Maestro flows can select them.
                Box(Modifier.semantics { testTagsAsResourceId = true }) { EtherRoot() }
            }
        }
    }
}
