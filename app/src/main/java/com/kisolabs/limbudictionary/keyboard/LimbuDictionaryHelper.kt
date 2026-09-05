package com.kisolabs.limbudictionary.keyboard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

data class LimbuWord(
    val word: String,
    var frequency: Int,
    var lastUsedTime: Long = System.currentTimeMillis()
)

object LimbuDictionaryHelper {

    @Volatile
    private var words: MutableList<LimbuWord> = mutableListOf()
    private const val FILE_NAME = "limbu_words_user.txt"
    private const val TAG = "LimbuDict"

    // Custom background scope replacing delicate GlobalScope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val limbuAlphabetOrder = listOf(
        'ᤀ', 'ᤁ', 'ᤂ', 'ᤃ', 'ᤄ', 'ᤅ',
        'ᤆ', 'ᤇ', 'ᤈ', 'ᤉ', 'ᤊ', 'ᤋ',
        'ᤌ', 'ᤍ', 'ᤎ', 'ᤏ', 'ᤐ', 'ᤑ',
        'ᤒ', 'ᤓ', 'ᤔ', 'ᤕ', 'ᤖ', 'ᤗ',
        'ᤘ', 'ᤙ', 'ᤚ', 'ᤛ', 'ᤜ'
    ).withIndex().associate { it.value to it.index }

    fun load(context: Context) {
        if (words.isNotEmpty()) {
            Log.d(TAG, "Dictionary already active with ${words.size} words.")
            return
        }

        val userFile = File(context.filesDir, FILE_NAME)

        if (!userFile.exists()) {
            val defaultLines = runCatching {
                context.assets.open("limbu_words.txt")
                    .bufferedReader()
                    .readLines()
            }.getOrDefault(emptyList())

            parseAndSetWords(defaultLines)
            saveToFile(userFile)
            Log.d(TAG, "Initialized default asset dictionary with ${words.size} words.")
        } else {
            parseAndSetWords(userFile.readLines())
            Log.d(TAG, "Loaded user dictionary file with ${words.size} words.")
        }
    }

    fun updateWordsFromRemote(context: Context, rawText: String) {
        if (rawText.isBlank()) return

        val prefs = context.getSharedPreferences("dictionary_cache", Context.MODE_PRIVATE)
        prefs.edit().putString("limbu_words_data", rawText).apply()

        val remoteLines = rawText.lines()
        val parsedRemote = parseLinesToWords(remoteLines)

        parsedRemote.forEach { remote ->
            val existing = words.find { it.word.equals(remote.word, ignoreCase = true) }
            if (existing == null) {
                words.add(remote)
            }
        }

        scope.launch {
            val userFile = File(context.filesDir, FILE_NAME)
            saveToFile(userFile)
        }
    }

    private fun parseAndSetWords(rawLines: List<String>) {
        words = parseLinesToWords(rawLines).distinctBy { it.word }.toMutableList()
    }

    private fun parseLinesToWords(rawLines: List<String>): List<LimbuWord> {
        return rawLines.mapNotNull { line ->
            val cleanLine = line.replace(Regex("<[^>]*>"), "").replace("-", "").trim()
            if (cleanLine.isEmpty()) return@mapNotNull null

            val parts = cleanLine.split(",")
            val wordText = parts[0].trim()
            val freq = parts.getOrNull(1)?.toIntOrNull() ?: 10
            val lastUsed = parts.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis()

            if (wordText.isNotEmpty()) LimbuWord(wordText, freq, lastUsed) else null
        }
    }

    fun recordWordSelection(context: Context, selectedWord: String) {
        val clean = selectedWord.trim()
        if (clean.isBlank()) return

        val now = System.currentTimeMillis()
        val existing = words.find { it.word.equals(clean, ignoreCase = true) }

        if (existing != null) {
            existing.frequency += 5
            existing.lastUsedTime = now
            Log.d(TAG, "Updated Word: $clean | Freq: ${existing.frequency}")
        } else {
            // Learn new word automatically when typed or submitted
            words.add(LimbuWord(clean, 20, now))
            Log.d(TAG, "Learned New Word: $clean | Freq: 20")
        }

        scope.launch {
            val userFile = File(context.filesDir, FILE_NAME)
            saveToFile(userFile)
        }
    }

    fun getSuggestions(query: String, max: Int = 4): List<String> {
        if (query.isEmpty()) return emptyList()

        return words
            .filter { it.word.startsWith(query, ignoreCase = true) }
            .sortedWith { w1, w2 ->
                val score1 = calculateScore(w1, query)
                val score2 = calculateScore(w2, query)
                if (score1 != score2) {
                    score2.compareTo(score1)
                } else {
                    compareLimbuWords(w1.word, w2.word)
                }
            }
            .take(max)
            .map { it.word }
    }

    private fun calculateScore(item: LimbuWord, query: String): Double {
        var score = item.frequency.toDouble()

        // Recency Decay: boost words used within the last 24 hours
        val hoursSinceUsed = (System.currentTimeMillis() - item.lastUsedTime) / (1000.0 * 60 * 60)
        if (hoursSinceUsed < 24.0) {
            score += (24.0 - hoursSinceUsed) * 2.0
        }

        // Exact match priority
        if (item.word.equals(query, ignoreCase = true)) {
            score += 1000.0
        }

        // Penalty for long words relative to typed prefix length
        val lengthDiff = item.word.length - query.length
        score -= (lengthDiff * 5.0)

        return score
    }

    private fun saveToFile(file: File) {
        runCatching {
            file.bufferedWriter().use { writer ->
                words.forEach { item ->
                    writer.write("${item.word},${item.frequency},${item.lastUsedTime}")
                    writer.newLine()
                }
            }
        }
    }

    private fun compareLimbuWords(word1: String, word2: String): Int {
        val minLen = minOf(word1.length, word2.length)
        for (i in 0 until minLen) {
            val char1 = word1[i]
            val char2 = word2[i]
            if (char1 != char2) {
                val rank1 = limbuAlphabetOrder[char1] ?: char1.code
                val rank2 = limbuAlphabetOrder[char2] ?: char2.code
                return rank1.compareTo(rank2)
            }
        }
        return word1.length.compareTo(word2.length)
    }
}
