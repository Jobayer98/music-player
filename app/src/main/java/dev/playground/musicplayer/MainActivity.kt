package dev.playground.musicplayer

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.playground.musicplayer.ui.MusicApp
import dev.playground.musicplayer.ui.MusicTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels()
    private var hasPermission by mutableStateOf(false)

    private val audioPermission =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            hasPermission = result[audioPermission] == true
            if (hasPermission) viewModel.loadSongs()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        hasPermission = ContextCompat.checkSelfPermission(this, audioPermission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) viewModel.loadSongs() else requestAccess()

        setContent {
            MusicTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                MusicApp(
                    state = state,
                    hasPermission = hasPermission,
                    onGrant = ::requestAccess,
                    vm = viewModel,
                )
            }
        }
    }

    private fun requestAccess() {
        val perms = mutableListOf(audioPermission)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        requestPermissions.launch(perms.toTypedArray())
    }
}
