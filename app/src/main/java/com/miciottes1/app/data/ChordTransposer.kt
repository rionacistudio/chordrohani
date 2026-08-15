package com.miciottes1.app.data

/**
 * Utilitas transpose chord & klasifikasi baris lagu.
 */
object ChordTransposer {

    private val NOTES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "Bb", "B")

    private val NOTE_INDEX = mapOf(
        "C" to 0, "B#" to 0,
        "C#" to 1, "Db" to 1,
        "D" to 2,
        "D#" to 3, "Eb" to 3,
        "E" to 4, "Fb" to 4,
        "F" to 5, "E#" to 5,
        "F#" to 6, "Gb" to 6,
        "G" to 7,
        "G#" to 8, "Ab" to 8,
        "A" to 9,
        "A#" to 10, "Bb" to 10,
        "B" to 11, "Cb" to 11,
    )

    private val chordTokenRegex = Regex(
        "^([A-Ga-g][#b]?)((?:maj|min|dim|aug|sus|add|m|M|\\d|[#b]\\d+|\\+|-|°|\\(|\\))*)(/([A-Ga-g][#b]?))?$",
        RegexOption.IGNORE_CASE,
    )

    private val blacklist = setOf("amin", "amen", "a", "e")

    private val decorationRegex = Regex("^[|¦‖•·∙●○◦.()\\[\\]{}\\-–—xXoO]+$")

    val DISPLAY_KEYS: List<String> = listOf(
        "A", "Bb", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#",
    )

    private fun normalizeNote(note: String): String {
        if (note.isEmpty()) return note
        val letter = note[0].uppercaseChar()
        if (note.length == 1) return letter.toString()
        val acc = when (note[1]) {
            '#', '♯' -> "#"
            'b', 'B', '♭' -> "b"
            else -> return letter.toString()
        }
        return "$letter$acc"
    }

    fun rootNote(token: String): String? {
        val match = chordTokenRegex.matchEntire(token.trim()) ?: return null
        return normalizeNote(match.groupValues[1])
    }

    fun stepsBetween(from: String, to: String): Int {
        val fromIdx = NOTE_INDEX[normalizeNote(from)] ?: return 0
        val toIdx = NOTE_INDEX[normalizeNote(to)] ?: return 0
        return ((toIdx - fromIdx) % 12 + 12) % 12
    }

    fun transposeNote(note: String, steps: Int): String {
        val idx = NOTE_INDEX[normalizeNote(note)] ?: return note
        val newIdx = ((idx + steps) % 12 + 12) % 12
        return NOTES[newIdx]
    }

    fun transposeChordToken(token: String, steps: Int): String {
        if (steps == 0) return token
        val t = token.trim()
        if (t.isEmpty()) return token
        val match = chordTokenRegex.matchEntire(t)
        if (match != null) {
            val root = transposeNote(match.groupValues[1], steps)
            val quality = match.groupValues[2]
            val bassRaw = match.groupValues[4]
            return buildString {
                append(root)
                append(quality)
                if (bassRaw.isNotEmpty()) {
                    append('/')
                    append(transposeNote(bassRaw, steps))
                }
            }
        }
        // ponytail: token tidak match regex (misal "GG" typo). Transpose tiap huruf root berurutan.
        val rootLetterRegex = Regex("[A-Ga-g][#b]?")
        return rootLetterRegex.replace(t) { m ->
            val noteIdx = NOTE_INDEX[normalizeNote(m.value)]
            if (noteIdx != null) transposeNote(m.value, steps) else m.value
        }
    }

    /**
     * Transpose semua chord di baris.
     * Pipe | di-split sebagai delimiter, chord yang menempel (A|D|D/F#|) ditangani.
     * Spasi dan posisi dipertahankan.
     */
    fun transposeLine(line: String, steps: Int): String {
        if (steps == 0 || line.isEmpty()) return line
        val result = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c.isWhitespace() || isDecorationChar(c)) {
                result.append(c)
                i++
            } else {
                // Baca token sampai ketemu spasi atau decoration
                val start = i
                while (i < line.length && !line[i].isWhitespace() && !isDecorationChar(line[i])) {
                    i++
                }
                val token = line.substring(start, i)
                result.append(transposeChordToken(token, steps))
            }
        }
        return result.toString()
    }

    private fun isDecorationChar(c: Char): Boolean {
        return c in "|¦‖•·∙●○◦.·–—-xXoO()[]{}"
    }

    private fun isDecoration(token: String): Boolean = decorationRegex.matches(token)

    fun isChordLine(line: String): Boolean {
        val tokens = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return false

        // Split tiap token by pipe, lalu flatten — tangani chord menempel: A|D|D/F#|
        val splitTokens = tokens.flatMap { tok ->
            tok.split('|', '¦', '‖').filter { it.isNotEmpty() }
        }
        if (splitTokens.isEmpty()) return false

        // Abaikan separator decoration
        val meaningful = splitTokens.filter { !isDecoration(it) }
        if (meaningful.isEmpty()) return false

        // Strip decoration dari tepi, cek apakah chord valid
        val cores = meaningful.map { stripDecorations(it) }.filter { it.isNotEmpty() }
        if (cores.isEmpty()) return false

        // Mayoritas token harus chord valid (toleransi typo data seperti "GG")
        val validCount = cores.count { isChordToken(it) }
        if (validCount == 0) return false
        if (validCount * 2 < cores.size) return false
        if (cores.size == 1 &&
            cores[0].lowercase() in blacklist &&
            cores[0] != "A" && cores[0] != "E"
        ) {
            return false
        }
        return true
    }

    /** Strip decoration di kiri & kanan, return core. */
    private fun stripDecorations(token: String): String {
        if (token.isEmpty()) return ""
        var start = 0
        var end = token.length
        while (start < end && isDecorationChar(token[start])) start++
        while (end > start && isDecorationChar(token[end - 1])) end--
        return if (end > start) token.substring(start, end) else ""
    }

    private fun isChordToken(token: String): Boolean {
        if (token.lowercase() in blacklist && token != "A" && token != "E") return false
        return chordTokenRegex.matches(token)
    }
}

enum class LineType { HEADER, CHORD, LYRIC, BLANK }

data class SongLine(val type: LineType, val text: String)

private fun isTitleLine(line: String): Boolean {
    val t = line.trim()
    if (t.isEmpty()) return false
    if (t.startsWith("Chord ", ignoreCase = true)) return true
    if (t.startsWith("Kunci ", ignoreCase = true)) return true
    return false
}

fun parseSongBody(body: String): List<SongLine> {
    val rawLines = body.lines()
    var start = 0
    while (start < rawLines.size) {
        val t = rawLines[start].trim()
        if (t.isEmpty() || isTitleLine(t)) {
            start++
            continue
        }
        break
    }
    return rawLines.drop(start).map { raw ->
        val trimmed = raw.trim()
        when {
            trimmed.isEmpty() -> SongLine(LineType.BLANK, "")
            trimmed.endsWith(":") -> SongLine(LineType.HEADER, trimmed.removeSuffix(":").trim())
            ChordTransposer.isChordLine(raw) -> SongLine(LineType.CHORD, raw.trimEnd())
            else -> SongLine(LineType.LYRIC, raw.trimEnd())
        }
    }
}
