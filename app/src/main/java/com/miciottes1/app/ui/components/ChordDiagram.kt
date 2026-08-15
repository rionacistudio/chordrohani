package com.miciottes1.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ponytail: data gitar standar (E A D G B e), 0=open, -1=muted, 1-5=fret
private val chordDatabase: Map<String, IntArray> = buildMap {
    // Major
    put("C",  intArrayOf(-1, 3, 2, 0, 1, 0))
    put("C#", intArrayOf(-1, 4, 6, 6, 5, 4))
    put("D",  intArrayOf(-1, -1, 0, 2, 3, 2))
    put("Eb", intArrayOf(-1, -1, 1, 3, 4, 3))
    put("E",  intArrayOf(0, 2, 2, 1, 0, 0))
    put("F",  intArrayOf(1, 3, 3, 2, 1, 1))
    put("F#", intArrayOf(2, 4, 4, 3, 2, 2))
    put("G",  intArrayOf(3, 2, 0, 0, 0, 3))
    put("G#", intArrayOf(4, 6, 6, 5, 4, 4))
    put("A",  intArrayOf(-1, 0, 2, 2, 2, 0))
    put("Bb", intArrayOf(-1, 1, 3, 3, 3, 1))
    put("B",  intArrayOf(-1, 2, 4, 4, 4, 2))

    // Minor
    put("Cm",  intArrayOf(-1, 3, 5, 5, 4, 3))
    put("C#m", intArrayOf(-1, 4, 6, 6, 5, 4)) // same shape as C# here for simplicity
    put("Dm",  intArrayOf(-1, -1, 0, 2, 3, 1))
    put("Ebm", intArrayOf(-1, -1, 1, 3, 4, 2))
    put("Em",  intArrayOf(0, 2, 2, 0, 0, 0))
    put("Fm",  intArrayOf(1, 3, 3, 1, 1, 1))
    put("F#m", intArrayOf(2, 4, 4, 2, 2, 2))
    put("Gm",  intArrayOf(3, 5, 5, 3, 3, 3))
    put("G#m", intArrayOf(4, 6, 6, 4, 4, 4))
    put("Am",  intArrayOf(-1, 0, 2, 2, 1, 0))
    put("Bbm", intArrayOf(-1, 1, 3, 3, 2, 1))
    put("Bm",  intArrayOf(-1, 2, 4, 4, 3, 2))

    // Dominant 7th
    put("C7",  intArrayOf(-1, 3, 2, 3, 1, 0))
    put("C#7", intArrayOf(-1, 4, 3, 4, 2, 1))
    put("D7",  intArrayOf(-1, -1, 0, 2, 1, 2))
    put("Eb7", intArrayOf(-1, -1, 1, 3, 1, 3))
    put("E7",  intArrayOf(0, 2, 0, 1, 0, 0))
    put("F7",  intArrayOf(1, 3, 1, 2, 1, 1))
    put("F#7", intArrayOf(2, 4, 2, 3, 2, 2))
    put("G7",  intArrayOf(3, 2, 0, 0, 0, 1))
    put("G#7", intArrayOf(4, 3, 1, 1, 1, 1))
    put("A7",  intArrayOf(-1, 0, 2, 0, 2, 0))
    put("Bb7", intArrayOf(-1, 1, 3, 1, 3, 1))
    put("B7",  intArrayOf(-1, 2, 1, 2, 0, 2))

    // Minor 7th
    put("Cm7",  intArrayOf(-1, 3, 5, 3, 4, 3))
    put("C#m7", intArrayOf(-1, 4, 6, 4, 5, 4))
    put("Dm7",  intArrayOf(-1, -1, 0, 2, 1, 1))
    put("Ebm7", intArrayOf(-1, -1, 1, 3, 2, 2))
    put("Em7",  intArrayOf(0, 2, 0, 0, 0, 0))
    put("Fm7",  intArrayOf(1, 3, 1, 1, 1, 1))
    put("F#m7", intArrayOf(2, 4, 2, 2, 2, 2))
    put("Gm7",  intArrayOf(3, 5, 3, 3, 3, 3))
    put("G#m7", intArrayOf(4, 6, 4, 4, 4, 4))
    put("Am7",  intArrayOf(-1, 0, 2, 0, 1, 0))
    put("Bbm7", intArrayOf(-1, 1, 3, 1, 2, 1))
    put("Bm7",  intArrayOf(-1, 2, 4, 2, 3, 2))

    // Major 7th
    put("Cmaj7",  intArrayOf(-1, 3, 2, 0, 0, 0))
    put("C#maj7", intArrayOf(-1, 4, 3, 1, 1, 1))
    put("Dmaj7",  intArrayOf(-1, -1, 0, 2, 2, 2))
    put("Ebmaj7", intArrayOf(-1, -1, 1, 3, 3, 3))
    put("Emaj7",  intArrayOf(0, 2, 1, 1, 0, 0))
    put("Fmaj7",  intArrayOf(1, 3, 2, 2, 1, 0))
    put("F#maj7", intArrayOf(2, 4, 3, 3, 2, 1))
    put("Gmaj7",  intArrayOf(3, 2, 0, 0, 0, 2))
    put("Amaj7",  intArrayOf(-1, 0, 2, 1, 2, 0))
    put("Bbmaj7", intArrayOf(-1, 1, 3, 2, 3, 1))
    put("Bmaj7",  intArrayOf(-1, 2, 4, 3, 4, 2))

    // Sus2
    put("Csus2", intArrayOf(-1, 3, 0, 0, 1, 3))
    put("Dsus2", intArrayOf(-1, -1, 0, 2, 3, 0))
    put("Esus2", intArrayOf(0, 2, 4, 4, 0, 0))
    put("Gsus2", intArrayOf(3, 0, 0, 0, 3, 3))
    put("Asus2", intArrayOf(-1, 0, 2, 2, 0, 0))

    // Sus4
    put("Csus4", intArrayOf(-1, 3, 3, 0, 1, 1))
    put("Dsus4", intArrayOf(-1, -1, 0, 2, 3, 3))
    put("Esus4", intArrayOf(0, 2, 2, 2, 0, 0))
    put("Gsus4", intArrayOf(3, 3, 0, 0, 1, 3))
    put("Asus4", intArrayOf(-1, 0, 2, 2, 3, 0))

    // Dim
    put("Cdim",  intArrayOf(-1, 3, 1, 2, 1, -1))
    put("C#dim", intArrayOf(-1, 4, 2, 3, 2, -1))
    put("Edim",  intArrayOf(0, 1, 2, 0, 2, 0))
    put("Gdim",  intArrayOf(3, -1, 2, -1, 2, 1))

    // Aug
    put("Caug",  intArrayOf(-1, 3, 2, 1, 1, 0))
    put("Eaug",  intArrayOf(0, 3, 2, 1, 1, 0))
    put("Aaug",  intArrayOf(-1, 0, 3, 2, 2, 1))

    // Add9
    put("Cadd9", intArrayOf(-1, 3, 2, 0, 3, 0))
    put("Dadd9", intArrayOf(-1, -1, 0, 2, 3, 0))
    put("Gadd9", intArrayOf(3, 2, 0, 0, 0, 4))
    put("Aadd9", intArrayOf(-1, 0, 2, 4, 2, 0))

    // Major 6th
    put("C6",   intArrayOf(-1, 3, 2, 2, 1, 0))
    put("C#6",  intArrayOf(-1, 4, 3, 3, 2, 1))
    put("D6",   intArrayOf(-1, -1, 0, 2, 1, 2))
    put("D#6",  intArrayOf(-1, -1, 1, 3, 2, 3))
    put("E6",   intArrayOf(0, 2, 2, 1, 2, 0))
    put("F6",   intArrayOf(1, 3, 2, 2, 1, 1))
    put("F#6",  intArrayOf(2, 4, 3, 3, 2, 2))
    put("G6",   intArrayOf(3, 2, 0, 0, 0, 3))
    put("G#6",  intArrayOf(4, 3, 1, 1, 1, 1))
    put("A6",   intArrayOf(-1, 0, 2, 2, 1, 2))
    put("A#6",  intArrayOf(-1, 1, 3, 3, 2, 3))
    put("B6",   intArrayOf(-1, 2, 4, 4, 3, 4))

    // Minor 6th
    put("Cm6",  intArrayOf(-1, 3, 1, 2, 1, -1))
    put("Dm6",  intArrayOf(-1, -1, 0, 2, 0, 2))
    put("D#m6", intArrayOf(-1, -1, 1, 3, 1, 3))
    put("Em6",  intArrayOf(0, 2, 2, 0, 2, 0))
    put("Fm6",  intArrayOf(1, 3, 1, 1, 1, 1))
    put("Gm6",  intArrayOf(3, 5, 3, 3, 3, 3))
    put("Am6",  intArrayOf(-1, 0, 2, 2, 1, 2))
    put("Bm6",  intArrayOf(-1, 2, 4, 4, 3, 4))

    // Dim7
    put("Cdim7",  intArrayOf(-1, 3, 1, 2, 1, 2))
    put("C#dim7", intArrayOf(-1, 4, 2, 3, 2, 3))
    put("Ddim7",  intArrayOf(-1, -1, 0, 1, 0, 1))
    put("D#dim7", intArrayOf(-1, -1, 1, 2, 1, 2))
    put("Edim7",  intArrayOf(0, 1, 2, 0, 2, 0))
    put("Fdim7",  intArrayOf(1, -1, 0, 1, 0, 1))
    put("F#dim7", intArrayOf(2, -1, 1, 2, 1, 2))
    put("Gdim7",  intArrayOf(3, -1, 2, 3, 2, 3))
    put("G#dim7", intArrayOf(4, -1, 3, 4, 3, 4))
    put("Adim7",  intArrayOf(-1, 0, 1, 2, 1, 2))
    put("Bbdim7", intArrayOf(-1, 1, 2, 3, 2, 3))
    put("Bdim7",  intArrayOf(-1, 2, 3, 4, 3, 4))

    // 7sus4
    put("C7sus4",  intArrayOf(-1, 3, 3, 3, 1, 0))
    put("D7sus4",  intArrayOf(-1, -1, 0, 2, 1, 3))
    put("E7sus4",  intArrayOf(0, 2, 0, 2, 0, 0))
    put("G7sus4",  intArrayOf(3, 3, 0, 3, 1, 3))
    put("A7sus4",  intArrayOf(-1, 0, 2, 0, 3, 0))

    // Minor 9th
    put("Cm9",   intArrayOf(-1, 3, 1, 3, 3, 3))
    put("C#m9",  intArrayOf(-1, 4, 2, 4, 4, 4))
    put("Dm9",   intArrayOf(-1, -1, 0, 2, 1, 0))
    put("Em9",   intArrayOf(0, 2, 0, 0, 0, 2))
    put("Fm9",   intArrayOf(1, 3, 1, 1, 1, 3))
    put("F#m9",  intArrayOf(2, 4, 2, 2, 2, 4))
    put("Gm9",   intArrayOf(3, 5, 3, 3, 3, 5))
    put("Am9",   intArrayOf(-1, 0, 2, 0, 1, 0))
    put("Bm9",   intArrayOf(-1, 2, 0, 2, 2, 2))

    // Major 9th
    put("Cmaj9",  intArrayOf(-1, 3, 2, 0, 0, 0))
    put("Dmaj9",  intArrayOf(-1, -1, 0, 2, 2, 0))
    put("Emaj9",  intArrayOf(0, 2, 1, 1, 0, 2))
    put("Fmaj9",  intArrayOf(-1, -1, 3, 2, 1, 0))
    put("Gmaj9",  intArrayOf(3, 2, 0, 0, 0, 2))
    put("Amaj9",  intArrayOf(-1, 0, 2, 1, 2, 0))

    // Dominant 9th
    put("C9",   intArrayOf(-1, 3, 2, 3, 3, 3))
    put("D9",   intArrayOf(-1, -1, 0, 2, 1, 0))
    put("E9",   intArrayOf(0, 2, 0, 1, 0, 2))
    put("F9",   intArrayOf(1, -1, 1, 1, 1, 3))
    put("G9",   intArrayOf(3, 2, 0, 0, 0, 2))
    put("A9",   intArrayOf(-1, 0, 2, 0, 2, 0))
    put("B9",   intArrayOf(-1, 2, 1, 2, 2, 2))

    // Add11
    put("Cadd9", intArrayOf(-1, 3, 2, 0, 3, 0))
    put("Dadd9", intArrayOf(-1, -1, 0, 2, 3, 0))
    put("Eadd9", intArrayOf(0, 2, 2, 1, 0, 2))
    put("Fadd9", intArrayOf(1, -1, 1, 2, 1, 1))
    put("Gadd9", intArrayOf(3, 2, 0, 0, 0, 4))
    put("Aadd9", intArrayOf(-1, 0, 2, 4, 2, 0))
    put("Badd9", intArrayOf(-1, 2, 1, 2, 4, 2))

    // Slash chords (common inversions)
    put("C/E",  intArrayOf(0, 3, 2, 0, 1, 0))
    put("C/G",  intArrayOf(3, 3, 2, 0, 1, 0))
    put("D/F#", intArrayOf(2, -1, 0, 2, 3, 2))
    put("D/A",  intArrayOf(-1, 0, 0, 2, 3, 2))
    put("E/G#", intArrayOf(4, 2, 2, 1, 0, 0))
    put("E/B",  intArrayOf(-1, 2, 2, 1, 0, 0))
    put("F/A",  intArrayOf(-1, 0, 3, 2, 1, 1))
    put("F/C",  intArrayOf(-1, 3, 3, 2, 1, 1))
    put("G/B",  intArrayOf(-1, 2, 0, 0, 0, 3))
    put("G/D",  intArrayOf(-1, -1, 0, 0, 0, 3))
    put("A/C#", intArrayOf(-1, 4, 2, 2, 2, 0))
    put("A/E",  intArrayOf(0, 0, 2, 2, 2, 0))
    put("B/A",  intArrayOf(-1, 0, 4, 4, 4, 2))
    put("B/D#", intArrayOf(-1, -1, 1, 1, 2, 2))
}

private val enharmonicRoot = mapOf(
    "Db" to "C#", "C#" to "Db",
    "Eb" to "D#", "D#" to "Eb",
    "Gb" to "F#", "F#" to "Gb",
    "Ab" to "G#", "G#" to "Ab",
    "Bb" to "A#", "A#" to "Bb",
)

/** Ambil root (C, C#, Bb, …) + sisa quality (m, 7, m7, …) dari token chord. */
private fun splitRootQuality(token: String): Pair<String, String>? {
    val base = token.split("/").first().trim()
    if (base.isEmpty()) return null
    val root = when {
        base.length >= 2 && (base[1] == '#' || base[1] == 'b') -> base.take(2)
        base.isNotEmpty() && base[0] in 'A'..'G' -> base.take(1)
        else -> return null
    }
    return root to base.drop(root.length)
}

private fun resolveChord(chord: String): String? {
    val base = chord.split("/").first().trim()
    if (base.isEmpty()) return null

    // 1. Exact match
    chordDatabase[base]?.let { return base }

    // 2. Case-insensitive exact
    chordDatabase.entries.firstOrNull { it.key.equals(base, ignoreCase = true) }?.key?.let { return it }

    // 3. Enharmonic root + quality (D#7 → Eb7, D#m → Ebm, A#m → Bbm, …)
    val (root, quality) = splitRootQuality(base) ?: return null
    val altRoot = enharmonicRoot[root]
    if (altRoot != null) {
        val alt = altRoot + quality
        chordDatabase[alt]?.let { return alt }
        chordDatabase.entries.firstOrNull { it.key.equals(alt, ignoreCase = true) }?.key?.let { return it }
    }

    // Voicing diminished gitar lazim memakai bentuk dim7 yang simetris.
    if (quality.equals("dim", ignoreCase = true)) {
        val dim7Candidates = listOfNotNull(root + "dim7", altRoot?.plus("dim7"))
        dim7Candidates.forEach { candidate ->
            chordDatabase[candidate]?.let { return candidate }
            chordDatabase.entries.firstOrNull {
                it.key.equals(candidate, ignoreCase = true)
            }?.key?.let { return it }
        }
    }

    return null
}

@Composable
fun ChordDiagramCard(chord: String) {
    val lookup = remember(chord) { resolveChord(chord) }
    val fallbackColor = MaterialTheme.colorScheme.surfaceVariant
    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            if (lookup != null) {
                val fingers = chordDatabase[lookup]!!
                val minFret = fingers.filter { it > 0 }.minOrNull() ?: 1
                val maxFret = fingers.maxOrNull() ?: 1
                val startFret = if (maxFret > 5) minFret else 1
                val showBarre = minFret >= 2 && fingers.count { it == minFret } >= 2
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChordCanvas(
                        fingers = fingers,
                        startFret = startFret,
                        showBarre = showBarre,
                        barreFret = if (showBarre) minFret else 0,
                        modifier = Modifier.size(width = 64.dp, height = 66.dp),
                    )
                    if (startFret > 1) {
                        Text(
                            text = "$startFret fr",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                }
            } else {
                Canvas(modifier = Modifier.size(width = 64.dp, height = 66.dp)) {
                    drawRect(fallbackColor, style = Fill)
                }
            }
            Text(
                text = chord,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ChordCanvas(
    fingers: IntArray,
    startFret: Int,
    showBarre: Boolean,
    barreFret: Int,
    modifier: Modifier = Modifier,
) {
    val dotColor = MaterialTheme.colorScheme.onSurface
    val lineColor = MaterialTheme.colorScheme.outline
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val numStrings = 6
        val numFrets = 5

        // Petak diagram mengikuti tinggi yang tersedia, grid lalu diratakan tengah
        val fretSpacing = (h - 14.dp.toPx()) / numFrets
        val stringSpacing = fretSpacing
        val fretW = stringSpacing * (numStrings - 1)
        val fretH = fretSpacing * numFrets

        val leftPad = (w - fretW) / 2f
        val topPad = 12.dp.toPx()

        fun stringX(i: Int) = leftPad + i * stringSpacing
        fun fretY(f: Int) = topPad + f * fretSpacing

        // Draw fret lines
        for (f in 0..numFrets) {
            val strokeWidth = if (f == 0 && startFret == 1 && !showBarre) 3.dp.toPx() else 1.dp.toPx()
            drawLine(lineColor, Offset(leftPad, fretY(f)), Offset(leftPad + fretW, fretY(f)), strokeWidth)
        }

        // Draw string lines
        for (s in 0 until numStrings) {
            drawLine(lineColor, Offset(stringX(s), topPad), Offset(stringX(s), topPad + fretH), 1.dp.toPx())
        }

        // Barre
        if (showBarre) {
            val displayFret = barreFret - startFret + 1
            val y = fretY(displayFret - 1) + fretSpacing * 0.5f
            val leftString = fingers.indexOfFirst { it == barreFret }.coerceIn(0, 5)
            val rightString = fingers.indexOfLast { it == barreFret }.coerceIn(0, 5)
            drawLine(
                color = dotColor,
                start = Offset(stringX(leftString), y),
                end = Offset(stringX(rightString), y),
                strokeWidth = fretSpacing * 0.55f,
                cap = StrokeCap.Round,
            )
        }

        // Finger dots, open, muted
        for (s in 0 until numStrings) {
            val f = fingers[s]
            val cx = stringX(s)
            when {
                f == -1 -> {
                    // Muted: X above nut
                    val ty = 4.dp.toPx()
                    drawLine(mutedColor, Offset(cx - 2.5.dp.toPx(), ty - 2.5.dp.toPx()), Offset(cx + 2.5.dp.toPx(), ty + 2.5.dp.toPx()), 1.1.dp.toPx())
                    drawLine(mutedColor, Offset(cx + 2.5.dp.toPx(), ty - 2.5.dp.toPx()), Offset(cx - 2.5.dp.toPx(), ty + 2.5.dp.toPx()), 1.1.dp.toPx())
                }
                f == 0 -> {
                    // Open: circle above nut
                    val cy = 5.dp.toPx()
                    drawCircle(dotColor, 2.6.dp.toPx(), Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(1.1.dp.toPx(), cap = StrokeCap.Round))
                }
                else -> {
                    if (showBarre && f == barreFret) continue
                    val displayFret = f - startFret + 1
                    if (displayFret in 1..numFrets) {
                        val cy = fretY(displayFret - 1) + fretSpacing * 0.5f
                        drawCircle(dotColor, fretSpacing * 0.36f, Offset(cx, cy))
                    }
                }
            }
        }
    }
}
