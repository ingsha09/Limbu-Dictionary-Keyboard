package com.kisolabs.limbudictionary.keyboard

import android.content.Context

object LimbuDictionaryHelper {
    
    @Volatile
    private var words: List<String> = emptyList()

    private val limbuAlphabetOrder = listOf(
        'ᤀ', 'ᤁ', 'ᤂ', 'ᤃ', 'ᤄ', 'ᤅ',
        'ᤆ', 'ᤇ', 'ᤈ', 'ᤉ', 'ᤊ', 'ᤋ',
        'ᤌ', 'ᤍ', 'ᤎ', 'ᤏ', 'ᤐ', 'ᤑ',
        'ᤒ', 'ᤓ', 'ᤔ', 'ᤕ', 'ᤖ', 'ᤗ',
        'ᤘ', 'ᤙ', 'ᤚ', 'ᤛ', 'ᤜ'
    ).withIndex().associate { it.value to it.index }

    fun load(context: Context) {
        if (words.isNotEmpty()) return

        // 1. Initial Priority: Load bundled assets/limbu_words.txt first
        runCatching {
            context.assets.open("limbu_words.txt")
                .bufferedReader()
                .useLines { lines ->
                    parseAndSetWords(lines.toList())
                }
        }

        // 2. Secondary Priority: Override with cached network response if available
        val prefs = context.getSharedPreferences("dictionary_cache", Context.MODE_PRIVATE)
        val cachedWordsText = prefs.getString("limbu_words_data", null)

        if (!cachedWordsText.isNullOrEmpty()) {
            parseAndSetWords(cachedWordsText.lines())
        }
    }

    fun updateWordsFromRemote(context: Context, rawText: String) {
        if (rawText.isBlank()) return
        val prefs = context.getSharedPreferences("dictionary_cache", Context.MODE_PRIVATE)
        prefs.edit().putString("limbu_words_data", rawText).apply()
        parseAndSetWords(rawText.lines())
    }

    private fun parseAndSetWords(rawLines: List<String>) {
        words = rawLines.map { line ->
            line.replace(Regex("<[^>]*>"), "")
                .replace("-", "")
                .trim()
        }
        .filter { it.isNotEmpty() }
        .distinct()
    }

    fun getSuggestions(query: String, max: Int = 4): List<String> {
        if (query.isEmpty()) return emptyList()

        return words
            .filter { it.startsWith(query, ignoreCase = true) }
            .sortedWith { word1, word2 -> compareLimbuWords(word1, word2) }
            .take(max)
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
