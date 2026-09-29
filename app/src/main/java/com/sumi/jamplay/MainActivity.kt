package com.sumi.jamplay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sumi.jamplay.service.MusicPlayerService
import com.sumi.jamplay.ui.player.MiniPlayerScreen
import com.sumi.jamplay.ui.player.PlayerScreen
import com.sumi.jamplay.ui.player.PlayerViewModel
import com.sumi.jamplay.ui.playlist.PlaylistDetailScreen
import com.sumi.jamplay.ui.playlist.PlaylistScreen
import com.sumi.jamplay.ui.playlist.PlaylistSelectScreen
import com.sumi.jamplay.ui.playlist.PlaylistViewModel
import com.sumi.jamplay.ui.search.SearchScreen
import com.sumi.jamplay.ui.search.SearchViewViewModel
import com.sumi.jamplay.ui.theme.JamPlayBackground
import com.sumi.jamplay.ui.theme.JamPlayTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var musicService: MusicPlayerService? = null
    private var isBound = false
    private var playerCommandJob: Job? = null

    private val playerViewModel: PlayerViewModel by viewModels()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val bd = binder as MusicPlayerService.LocalBinder
            musicService = bd.getService()
            isBound = true
            musicService?.let { playerViewModel.bindService(it) }
            observePlayerCommands()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerCommandJob?.cancel()
            playerCommandJob = null
            playerViewModel.unbindService()
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navigateToPlayer = savedInstanceState == null && intent?.getBooleanExtra("navigate_to_player", false) == true

        // 음악 서비스 시작 (앱 실행 시 백그라운드에서 재생 준비)
        Intent(this, MusicPlayerService::class.java).also { intent ->
            startService(intent)
            isBound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                Color.Transparent.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.dark(
                Color.Transparent.toArgb()
            )
        )

        setContent {
            JamPlayTheme {
                val navController = rememberNavController()
                MainScreen(navController, playerViewModel)
                LaunchedEffect(navigateToPlayer) {
                    if (navigateToPlayer) {
                        navController.navigate("player")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        playerCommandJob?.cancel()
        playerViewModel.unbindService()
        musicService = null
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun observePlayerCommands() {
        val service = musicService ?: return
        playerCommandJob?.cancel()
        playerCommandJob = lifecycleScope.launch {
            playerViewModel.playerCommand.collect { command ->
                when (command) {
                    is PlayerViewModel.PlayerCommand.Play -> service.play(command.track, command.tracks)
                    PlayerViewModel.PlayerCommand.TogglePlay -> service.togglePlayPause()
                    PlayerViewModel.PlayerCommand.SkipNext -> service.skipNext()
                    PlayerViewModel.PlayerCommand.SkipPrevious -> service.skipPrevious()
                    is PlayerViewModel.PlayerCommand.Seek -> service.seekTo(command.position)
                    PlayerViewModel.PlayerCommand.ToggleShuffle -> service.toggleShuffle()
                    PlayerViewModel.PlayerCommand.ToggleRepeat -> service.toggleRepeat()
                }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavHostController, playerViewModel: PlayerViewModel) {
    val searchViewModel: SearchViewViewModel = hiltViewModel()
    val playlistViewModel: PlaylistViewModel = hiltViewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize()
            .background(JamPlayBackground)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
            ),
        bottomBar = {
            // BottomNavigation + MiniPlayer 숨김
            if (currentDestination == "playlist" || currentDestination == "search") {
                Column {
                    MiniPlayerScreen(
                        viewModel = playerViewModel
                    ) {
                        // 클릭 시 PlayerScreen으로 전환
                        navController.navigate("player") {
                            launchSingleTop = true
                        }
                    }
                    BottomNavigationBar(navController)
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "playlist",
            modifier = Modifier.background(Color.Transparent)
        ) {
            composable("playlist") {
                PlaylistScreen(
                    padding = padding,
                    playlistViewModel = playlistViewModel,
                    onPlaylistClick = { playlistId ->
                        navController.navigate("playlistDetail/$playlistId")
                    }
                )
            }
            composable("search") {
                SearchScreen(
                    padding = padding,
                    searchViewModel = searchViewModel,
                    playerViewModel = playerViewModel,
                    onTrackClick = { track, trackList ->
                        playerViewModel.play(track, trackList)
                        navController.navigate("player")
                    }
                )
            }
            composable("player") {
                PlayerScreen(
                    padding = padding,
                    playerViewModel = playerViewModel,
                    playlistViewModel = playlistViewModel,
                    onAddToPlaylist = { navController.navigate("playlistSelect") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("playlistSelect") {
                PlaylistSelectScreen(
                    padding = padding,
                    playlistViewModel = playlistViewModel,
                    playerViewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "playlistDetail/{playlistId}",
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                var initialized by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(playlistId) {
                    if (!initialized) {
                        playlistViewModel.setPlaylistId(playlistId)
                        initialized = true
                    }
                }
                PlaylistDetailScreen(
                    playlistViewModel = playlistViewModel,
                    playerViewModel = playerViewModel,
                    onTrackClick = { track, trackList ->
                        playerViewModel.play(track, trackList)
                        navController.navigate("player")
                    },
                    onMiniPlayerClick = {
                        // 클릭 시 PlayerScreen으로 전환
                        navController.navigate("player") {
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem(stringResource(R.string.playlist_title), "playlist", Icons.AutoMirrored.Filled.List),
        BottomNavItem(stringResource(R.string.search_title), "search", Icons.Default.Search),
    )

    NavigationBar(
        modifier = Modifier
            .drawBehind {
                // 맨 윗줄 구분선
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        containerColor = JamPlayBackground,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val selected = currentDestination == item.route
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
            )
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(300)
            )

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo("playlist") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.graphicsLayer(
                                scaleX = scale,
                                scaleY = scale
                            ),
                            tint = if (selected)
                                Color.White
                            else
                                Color.White.copy(alpha = 0.6f)
                        )
                        if (indicatorAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 6.dp)
                                    .height(2.dp)
                                    .width(18.dp)
                                    .alpha(indicatorAlpha)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF9B6BFF), Color(0xFF4B1F9C))
                                        ),
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                },
                label = {
                    Text(
                        item.label,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.White.copy(alpha = 0.5f)
                )
            )
        }
    }
}

data class BottomNavItem(val label: String, val route: String, val icon: ImageVector)