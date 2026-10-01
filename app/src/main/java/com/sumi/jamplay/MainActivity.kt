package com.sumi.jamplay

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import coil.load
import com.sumi.jamplay.databinding.ActivityMainBinding
import com.sumi.jamplay.ui.player.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.hypot

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, maxOf(safe.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHost.navController
        binding.bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = destination.id == R.id.playlist || destination.id == R.id.search
            updateMiniPlayerVisibility()
            updatePlayerBackground()
        }
        binding.root.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                updatePlayerBackground()
            }
        }
        binding.miniPlayer.root.setOnClickListener { openPlayer() }
        binding.miniPlayer.playPause.setOnClickListener { playerViewModel.togglePlayPause() }
        observePlayback()
        if (savedInstanceState == null) handlePlayerIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePlayerIntent(intent)
    }

    private fun handlePlayerIntent(intent: Intent) {
        if (intent.getBooleanExtra("navigate_to_player", false)) openPlayer()
    }

    private fun openPlayer() {
        navController.navigate(R.id.player, null, navOptions { launchSingleTop = true })
    }

    private fun updateMiniPlayerVisibility() {
        val destination = navController.currentDestination?.id
        val supported = destination == R.id.playlist || destination == R.id.search || destination == R.id.playlist_detail
        binding.miniPlayer.root.isVisible = supported && playerViewModel.currentTrack.value != null
    }

    private fun updatePlayerBackground() {
        if (navController.currentDestination?.id != R.id.player) {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.jamplay_background))
            return
        }
        val colors = intArrayOf(
            ColorUtils.compositeColors(0x66000000, playerViewModel.vibrantColor.value),
            ColorUtils.compositeColors(0x66000000, playerViewModel.lightVibrantColor.value)
        )
        // Activity 배경은 시스템 바 뒤까지 이어지고, 콘텐츠는 기존 inset 안에 배치한다.
        binding.root.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors).apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = (hypot(binding.root.width.toFloat(), binding.root.height.toFloat()) / 2).coerceAtLeast(1f)
        }
    }

    private fun observePlayback() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    playerViewModel.currentTrack.collect { track ->
                        updateMiniPlayerVisibility()
                        binding.miniPlayer.trackName.text = track?.name
                        binding.miniPlayer.artistName.text = track?.artistName
                        binding.miniPlayer.artwork.load(track?.artworkUrl)
                    }
                }
                launch {
                    playerViewModel.isPlaying.collect { playing ->
                        binding.miniPlayer.playPause.apply {
                            setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
                            contentDescription = getString(if (playing) R.string.pause else R.string.play)
                        }
                    }
                }
                launch {
                    combine(playerViewModel.vibrantColor, playerViewModel.lightVibrantColor) { first, second ->
                        intArrayOf(first, second)
                    }.collect { colors ->
                        binding.miniPlayer.root.background = GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT, colors
                        ).apply { cornerRadius = 12 * resources.displayMetrics.density }
                        updatePlayerBackground()
                    }
                }
            }
        }
    }
}
