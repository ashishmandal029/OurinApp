package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.api.DownloadResult
import com.example.data.HistoryEntity
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = Modifier.fillMaxSize()
    ) {
        // Main bottom-tab shell
        composable("main") {
            MainShell(
                viewModel = mainViewModel,
                onPlayVideo = { result ->
                    val route = "player" +
                            "?videoUrl=${Uri.encode(result.url)}" +
                            "&watchUrl=${Uri.encode(result.watchUrl)}" +
                            "&title=${Uri.encode(result.title)}" +
                            "&series=${Uri.encode(result.series ?: "")}" +
                            "&season=${result.seasonNumber ?: -1}" +
                            "&episode=${result.episodeNumber ?: -1}" +
                            "&quality=${Uri.encode(result.quality ?: "")}" +
                            "&format=${Uri.encode(result.format ?: "")}" +
                            "&uploader=${Uri.encode(result.author ?: "")}" +
                            "&duration=${result.duration ?: 0L}" +
                            "&expiresAt=${System.currentTimeMillis() / 1000 + 3600}"
                    navController.navigate(route)
                },
                onPlayHistoryItem = { entity ->
                    val route = "player" +
                            "?videoUrl=${Uri.encode(entity.downloadUrl ?: "")}" +
                            "&watchUrl=${Uri.encode(entity.watchUrl ?: "")}" +
                            "&title=${Uri.encode(entity.title)}" +
                            "&series=${Uri.encode(entity.seriesName ?: "")}" +
                            "&season=${entity.seasonNumber ?: -1}" +
                            "&episode=${entity.episodeNumber ?: -1}" +
                            "&quality=${Uri.encode(entity.quality ?: "")}" +
                            "&format=${Uri.encode(entity.format ?: "")}" +
                            "&uploader=${Uri.encode(entity.uploader ?: "")}" +
                            "&duration=${entity.duration}" +
                            "&expiresAt=${entity.timestamp / 1000 + 3600}"
                    navController.navigate(route)
                }
            )
        }

        // Dedicated Immersive ExoPlayer screen
        composable(
            route = "player?videoUrl={videoUrl}&watchUrl={watchUrl}&title={title}&series={series}&season={season}&episode={episode}&quality={quality}&format={format}&uploader={uploader}&duration={duration}&expiresAt={expiresAt}",
            arguments = listOf(
                navArgument("videoUrl") { type = NavType.StringType },
                navArgument("watchUrl") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("series") { type = NavType.StringType; nullable = true },
                navArgument("season") { type = NavType.IntType; defaultValue = -1 },
                navArgument("episode") { type = NavType.IntType; defaultValue = -1 },
                navArgument("quality") { type = NavType.StringType; nullable = true },
                navArgument("format") { type = NavType.StringType; nullable = true },
                navArgument("uploader") { type = NavType.StringType; nullable = true },
                navArgument("duration") { type = NavType.LongType; defaultValue = 0L },
                navArgument("expiresAt") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val videoUrl = args?.getString("videoUrl") ?: ""
            val watchUrl = args?.getString("watchUrl") ?: ""
            val title = args?.getString("title") ?: "Video Stream"
            val series = args?.getString("series").let { if (it.isNullOrEmpty()) null else it }
            val seasonVal = args?.getInt("season") ?: -1
            val episodeVal = args?.getInt("episode") ?: -1
            val quality = args?.getString("quality").let { if (it.isNullOrEmpty()) null else it }
            val format = args?.getString("format").let { if (it.isNullOrEmpty()) null else it }
            val uploader = args?.getString("uploader").let { if (it.isNullOrEmpty()) null else it }
            val duration = args?.getLong("duration") ?: 0L
            val expiresAt = args?.getOnLongClickValue("expiresAt") ?: 0L

            NativePlayerView(
                videoUrl = videoUrl,
                watchUrl = watchUrl,
                title = title,
                seriesName = series,
                seasonNumber = if (seasonVal == -1) null else seasonVal,
                episodeNumber = if (episodeVal == -1) null else episodeVal,
                quality = quality,
                format = format,
                uploader = uploader,
                duration = duration,
                expiresAt = if (expiresAt == 0L) null else expiresAt,
                viewModel = mainViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

// Helper to handle safe long reading from bundle
fun Bundle.getOnLongClickValue(key: String): Long {
    return this.getLong(key, 0L)
}

@Composable
fun MainShell(
    viewModel: MainViewModel,
    onPlayVideo: (DownloadResult) -> Unit,
    onPlayHistoryItem: (HistoryEntity) -> Unit
) {
    var selectedScreen by remember { mutableStateOf("downloader") } // downloader, library, settings

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedScreen == "downloader",
                    onClick = { selectedScreen = "downloader" },
                    icon = { Icon(imageVector = Icons.Default.Download, contentDescription = "Downloader") },
                    label = {
                        Text(
                            text = "Downloader",
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPurpleText,
                        selectedTextColor = BentoPurpleText,
                        indicatorColor = BentoPurpleAccent,
                        unselectedIconColor = BentoTextMuted,
                        unselectedTextColor = BentoTextMuted
                    ),
                    modifier = Modifier.testTag("tab_downloader")
                )

                NavigationBarItem(
                    selected = selectedScreen == "library",
                    onClick = { selectedScreen = "library" },
                    icon = { Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = "My Library") },
                    label = {
                        Text(
                            text = "My Library",
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPurpleText,
                        selectedTextColor = BentoPurpleText,
                        indicatorColor = BentoPurpleAccent,
                        unselectedIconColor = BentoTextMuted,
                        unselectedTextColor = BentoTextMuted
                    ),
                    modifier = Modifier.testTag("tab_library")
                )

                NavigationBarItem(
                    selected = selectedScreen == "settings",
                    onClick = { selectedScreen = "settings" },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    label = {
                        Text(
                            text = "Settings",
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPurpleText,
                        selectedTextColor = BentoPurpleText,
                        indicatorColor = BentoPurpleAccent,
                        unselectedIconColor = BentoTextMuted,
                        unselectedTextColor = BentoTextMuted
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        },
        containerColor = BentoBg,
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedScreen) {
                "downloader" -> {
                    HomeView(
                        viewModel = viewModel,
                        onPlayOnline = onPlayVideo,
                        onNavigateToSettings = { selectedScreen = "settings" }
                    )
                }
                "library" -> {
                    HistoryView(
                        viewModel = viewModel,
                        onPlayClick = onPlayHistoryItem
                    )
                }
                "settings" -> {
                    SettingsView(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
