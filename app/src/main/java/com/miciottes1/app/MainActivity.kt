package com.miciottes1.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.miciottes1.app.data.ServiceListRepository
import com.miciottes1.app.data.SettingsRepository
import com.miciottes1.app.ui.screens.AlbumDetailScreen
import com.miciottes1.app.ui.screens.ChordDictionaryScreen
import com.miciottes1.app.ui.screens.DonationScreen
import com.miciottes1.app.ui.screens.FavoritesScreen
import com.miciottes1.app.ui.screens.HomeScreen
import com.miciottes1.app.ui.screens.RequestChordScreen
import com.miciottes1.app.ui.screens.ServiceListScreen
import com.miciottes1.app.ui.screens.SettingsScreen
import com.miciottes1.app.ui.screens.SongDetailScreen
import com.miciottes1.app.ui.theme.AgonAppTheme
import com.miciottes1.app.viewmodel.SongListViewModel
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okio.Path.Companion.toPath
import java.io.File

class MainActivity : ComponentActivity(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache").absolutePath.toPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ServiceListRepository.init(this)
        setContent {
            val context = LocalContext.current
            val settingsRepo = remember { SettingsRepository(context) }
            val themeMode by settingsRepo.themeModeFlow.collectAsState(initial = "system")
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            AgonAppTheme(darkTheme = darkTheme) {
                MainApp()
            }
        }
    }
}

private data class NavTab(
    val route: String,
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector,
)

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val listViewModel: SongListViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val tabRoutes = listOf("home", "list", "favorites", "settings")
    val showBottomBar = currentRoute in tabRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding),
            ) {
            composable("home") {
                HomeScreen(
                    viewModel = listViewModel,
                    onSongClick = { song ->
                        listViewModel.select(song)
                        navController.navigate("detail")
                    },
                    onAlbumClick = { name, image ->
                        listViewModel.selectAlbum(name, image)
                        navController.navigate("album_detail")
                    },
                    onChordDictionary = { navController.navigate("chord_dictionary") },
                    onRequestChord = { navController.navigate("request_chord") },
                    onDonation = { navController.navigate("donation") },
                )
            }
            composable("list") {
                ServiceListScreen(
                    viewModel = listViewModel,
                    onSongClick = { song ->
                        listViewModel.select(song)
                        navController.navigate("detail")
                    },
                )
            }
            composable("favorites") {
                FavoritesScreen(
                    viewModel = listViewModel,
                    onSongClick = { song ->
                        listViewModel.select(song)
                        navController.navigate("detail")
                    },
                )
            }
            composable("settings") { SettingsScreen() }
            composable("chord_dictionary") {
                ChordDictionaryScreen(onBack = { navController.popBackStack() })
            }
            composable("request_chord") {
                RequestChordScreen(onBack = { navController.popBackStack() })
            }
            composable("donation") {
                DonationScreen(onBack = { navController.popBackStack() })
            }
            composable("detail") {
                val song = listViewModel.selectedSong
                if (song != null) {
                    SongDetailScreen(
                        summary = song,
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    navController.popBackStack()
                }
            }
            composable("album_detail") {
                AlbumDetailScreen(
                    albumName = listViewModel.selectedAlbum,
                    albumImage = listViewModel.selectedAlbumImage,
                    viewModel = listViewModel,
                    onSongClick = { song ->
                        listViewModel.select(song)
                        navController.navigate("detail")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
        }

        if (showBottomBar) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                BottomNav(navController, currentRoute)
            }
        }
    }
}

@Composable
fun BottomNav(navController: NavHostController, currentRoute: String?) {
    val tabs = listOf(
        NavTab("home", "EXPLORE", Icons.Filled.Home, Icons.Outlined.Home),
        NavTab("list", "LIBRARY", Icons.AutoMirrored.Filled.QueueMusic, Icons.AutoMirrored.Outlined.QueueMusic),
        NavTab("favorites", "FAVORIT", Icons.Filled.Star, Icons.Outlined.StarOutline),
        NavTab("settings", "SETTING", Icons.Filled.Settings, Icons.Outlined.Settings),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF111111),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp),
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val color = if (selected) Color.White
                    else Color.White.copy(alpha = 0.5f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                navController.navigate(tab.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(horizontal = 8.dp),
                    ) {
                        Icon(
                            imageVector = if (selected) tab.iconSelected else tab.iconUnselected,
                            contentDescription = tab.label,
                            tint = color,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}
