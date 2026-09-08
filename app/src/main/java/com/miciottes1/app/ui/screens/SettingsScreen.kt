package com.miciottes1.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miciottes1.app.data.SettingsRepository
import com.miciottes1.app.data.SongRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val songRepo = remember { SongRepository(context) }
    val themeMode by settingsRepo.themeModeFlow.collectAsState(initial = "system")
    val fontSize by settingsRepo.fontSizeFlow.collectAsState(initial = 15f)
    val scrollSpeed by settingsRepo.scrollSpeedFlow.collectAsState(initial = 2)
    val scope = rememberCoroutineScope()

    var songCount by remember { mutableStateOf<Int?>(null) }
    var lastSync by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var resetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        songCount = songRepo.count()
        lastSync = songRepo.getLastSync()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Pengaturan",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = "Sesuaikan tampilan dan preferensi aplikasi",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ---------- Tema ----------
        SettingCard(title = "Tema Aplikasi") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(
                    "light" to Icons.Default.LightMode,
                    "system" to Icons.Default.BrightnessAuto,
                    "dark" to Icons.Default.DarkMode,
                ).forEach { (mode, icon) ->
                    val label = when (mode) {
                        "light" -> "Terang"
                        "system" -> "Sistem"
                        else -> "Gelap"
                    }
                    val selected = themeMode == mode
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable {
                                scope.launch { settingsRepo.setThemeMode(mode) }
                            },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // ---------- Ukuran font ----------
        SettingCard(title = "Ukuran Font Chord") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FormatSize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
                Slider(
                    value = fontSize,
                    onValueChange = { scope.launch { settingsRepo.setFontSize(it.roundToInt().toFloat()) } },
                    valueRange = 11f..26f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                Text(
                    text = "${fontSize.roundToInt()}sp",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "Contoh: Am  F  C  G",
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // ---------- Kecepatan autoscroll ----------
        SettingCard(title = "Kecepatan Autoscroll Awal") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
                Slider(
                    value = scrollSpeed.toFloat(),
                    onValueChange = { scope.launch { settingsRepo.setScrollSpeed(it.roundToInt()) } },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                Text(
                    text = "${scrollSpeed}x",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // ---------- Status Data ----------
        SettingCard(title = "Status Data") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = if (songCount == null) "Menghitung\u2026" else "${songCount!!.toLocaleString()} lagu tersimpan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Sync terakhir: ${lastSync?.take(16) ?: "belum pernah"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                        .clickable(enabled = !syncing) {
                            syncing = true
                            scope.launch {
                                runCatching { songRepo.sync() }
                                songCount = songRepo.count()
                                lastSync = songRepo.getLastSync()
                                syncing = false
                            }
                        }
                        .padding(horizontal = 14.dp),
                ) {
                    if (syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        Text(
                            text = "Sync Sekarang",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // ---------- Reset Default ----------
        SettingCard(title = "Reset Pengaturan") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Kembalikan tema, font, dan kecepatan ke bawaan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                        .background(if (resetConfirm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface)
                        .clickable {
                            if (resetConfirm) {
                                scope.launch {
                                    settingsRepo.resetToDefaults()
                                    resetConfirm = false
                                }
                            } else {
                                resetConfirm = true
                            }
                        }
                        .padding(horizontal = 14.dp),
                ) {
                    Text(
                        text = if (resetConfirm) "Yakin?" else "Reset",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (resetConfirm) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // ---------- Kontak ----------
        SettingCard(title = "Kontak & Sosial") {
            listOf(
                "Instagram" to "https://instagram.com/rionacistudio",
                "WhatsApp" to "https://wa.me/6281234567890",
            ).forEach { (label, url) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                        .padding(vertical = 8.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }

        // ---------- Privasi ----------
        SettingCard(title = "Kebijakan Privasi") {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Doxord tidak mengumpulkan data pribadi. Data chord tersimpan di perangkat dan disinkronkan dari server publik. Favorit dan daftar pelayanan hanya tersimpan lokal di HP kamu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }

        // ---------- Tentang ----------
        SettingCard(title = "Tentang Aplikasi") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp)),
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "Doxord v1.0",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Kumpulan chord lagu rohani untuk pelayanan. Data dimuat dari Supabase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun Int.toLocaleString(): String = java.text.DecimalFormat("#,###").format(this)

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}
