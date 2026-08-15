package com.miciottes1.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miciottes1.app.ui.components.ChordDiagramCard

@Composable
private fun MoreScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        Surface(
            onClick = onBack,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
fun ChordDictionaryScreen(onBack: () -> Unit) {
    val chords = remember {
        listOf(
            "C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B",
            "Cm", "C#m", "Dm", "Ebm", "Em", "Fm", "F#m", "Gm", "G#m", "Am", "Bbm", "Bm",
            "C7", "D7", "E7", "F7", "G7", "A7", "B7", "Cmaj7", "Dmaj7", "Emaj7", "Gmaj7", "Amaj7",
            "Csus2", "Dsus2", "Asus2", "Csus4", "Dsus4", "Asus4", "Cdim", "Ddim7", "Caug", "Aaug",
        )
    }
    Column(modifier = Modifier.fillMaxSize()) {
        MoreScreenHeader("Kamus Chord", onBack)
        Text(
            text = "Diagram chord gitar dan posisi jari",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(92.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(chords) { chord -> ChordDiagramCard(chord) }
        }
    }
}

@Composable
fun RequestChordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var songName by remember { mutableStateOf("") }
    var mediaLink by remember { mutableStateOf("") }
    val canSend = songName.isNotBlank() && mediaLink.isNotBlank()

    Column(modifier = Modifier.fillMaxSize()) {
        MoreScreenHeader("Request Chord Gitar", onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            item {
                Text(
                    "Kirim detail lagu yang belum tersedia. Permintaan akan dibuka melalui aplikasi email.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedTextField(
                    value = songName,
                    onValueChange = { songName = it },
                    label = { Text("Nama Lagu") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = mediaLink,
                    onValueChange = { mediaLink = it },
                    label = { Text("Link Video/Musik") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(
                    enabled = canSend,
                    onClick = {
                        val subject = "Request Chord Gitar: ${songName.trim()}"
                        val body = "Nama lagu: ${songName.trim()}\nLink video/musik: ${mediaLink.trim()}"
                        val uri = Uri.parse(
                            "mailto:rionacistudio@gmail.com?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}",
                        )
                        val intent = Intent(Intent.ACTION_SENDTO, uri)
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Aplikasi email tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Text("Kirim", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun DonationScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        MoreScreenHeader("Donasi", onBack)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                    Icon(
                        Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(96.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text("QRIS belum tersedia", fontWeight = FontWeight.ExtraBold)
            Text(
                "Kirim gambar QRIS agar dapat ditampilkan di halaman ini.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
