package org.kaorun.nouto.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.core.content.edit
import org.json.JSONArray

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(
        "search_prefs",
        Context.MODE_PRIVATE
    )

    private val _searchHistory = MutableLiveData(loadHistory())
    val searchHistory: LiveData<List<String>> = _searchHistory

    fun addToHistory(query: String) {
        if (query.isBlank()) return
        val history = loadHistory().toMutableList()
        history.remove(query)
        history.add(0, query)
        val trimmed = history.take(10)
        saveHistory(trimmed)
        _searchHistory.value = trimmed
    }

    fun removeFromHistory(query: String) {
        val history = loadHistory().toMutableList()
        history.remove(query)
        saveHistory(history)
        _searchHistory.value = history
    }

    private fun saveHistory(history: List<String>) {
        prefs.edit { putString("history", JSONArray(history).toString()) }
    }

    private fun loadHistory(): List<String> {
        val raw = prefs.getString("history", "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val jsonArray = JSONArray(raw)
            List(jsonArray.length()) { index -> jsonArray.getString(index) }
                .filter { it.isNotBlank() }
        }.getOrElse {
            raw.split(",").filter { it.isNotBlank() }
        }
    }
}