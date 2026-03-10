package com.questterm.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("questterm_connection_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val maxHistory = 8

    companion object {
        private const val KEY_PROFILE_IDS = "profile_ids"
        private const val PREFIX_PROFILE = "profile_"
        private const val KEY_SEEDED = "default_connections_seeded"
    }

    init {
        seedDefaultsIfNeeded()
    }

    private fun seedDefaultsIfNeeded() {
        if (prefs.getBoolean(KEY_SEEDED, false)) return
        val ids = prefs.getStringSet(KEY_PROFILE_IDS, emptySet()) ?: emptySet()
        if (ids.isNotEmpty()) {
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
            return
        }

        val demo = ConnectionProfile(
            host = "bandit.labs.overthewire.org",
            port = 2220,
            username = "bandit0",
            isFavorite = true,
        ).withPassword("bandit0")

        val newIds = mutableSetOf(demo.id)
        prefs.edit()
            .putStringSet(KEY_PROFILE_IDS, newIds)
            .putString("$PREFIX_PROFILE${demo.id}", gson.toJson(demo))
            .putBoolean(KEY_SEEDED, true)
            .apply()
    }

    fun getAllProfiles(): List<ConnectionProfile> {
        val ids = prefs.getStringSet(KEY_PROFILE_IDS, emptySet()) ?: emptySet()
        return ids.mapNotNull { id ->
            prefs.getString("$PREFIX_PROFILE$id", null)?.let {
                gson.fromJson(it, ConnectionProfile::class.java)
            }
        }.sortedWith(
            compareByDescending<ConnectionProfile> { it.isFavorite }
                .thenByDescending { it.lastUsedTimestamp }
        )
    }

    fun getFavorites(): List<ConnectionProfile> =
        getAllProfiles().filter { it.isFavorite }

    fun getRecentNonFavorites(limit: Int = maxHistory): List<ConnectionProfile> =
        getAllProfiles().filter { !it.isFavorite }.take(limit)

    fun saveProfile(profile: ConnectionProfile, rememberPassword: Boolean) {
        // Find existing profile with same host/port/username
        val existing = getAllProfiles().find {
            it.host == profile.host && it.port == profile.port && it.username == profile.username
        }

        val toSave = if (existing != null) {
            // Update existing: preserve favorite status, update timestamp, update password if requested
            existing.copy(
                encryptedPassword = if (rememberPassword) profile.encryptedPassword else null,
                lastUsedTimestamp = System.currentTimeMillis()
            )
        } else {
            // New profile
            profile.copy(
                encryptedPassword = if (rememberPassword) profile.encryptedPassword else null,
                isFavorite = false
            )
        }

        // Save profile
        val ids = prefs.getStringSet(KEY_PROFILE_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        ids.add(toSave.id)

        prefs.edit()
            .putStringSet(KEY_PROFILE_IDS, ids)
            .putString("$PREFIX_PROFILE${toSave.id}", gson.toJson(toSave))
            .apply()

        pruneHistory()
    }

    fun deleteProfile(id: String) {
        val ids = prefs.getStringSet(KEY_PROFILE_IDS, emptySet())?.toMutableSet() ?: return
        ids.remove(id)

        prefs.edit()
            .putStringSet(KEY_PROFILE_IDS, ids)
            .remove("$PREFIX_PROFILE$id")
            .apply()
    }

    fun toggleFavorite(id: String) {
        val profile = getAllProfiles().find { it.id == id } ?: return
        val updated = profile.copy(isFavorite = !profile.isFavorite)

        prefs.edit()
            .putString("$PREFIX_PROFILE$id", gson.toJson(updated))
            .apply()
    }

    // Auto-prune: keep favorites + last 8 non-favorites
    private fun pruneHistory() {
        val all = getAllProfiles()
        val favorites = all.filter { it.isFavorite }
        val nonFavorites = all.filter { !it.isFavorite }
            .sortedByDescending { it.lastUsedTimestamp }
            .take(maxHistory)

        val toKeep = (favorites + nonFavorites).map { it.id }.toSet()
        val toRemove = all.filter { it.id !in toKeep }

        if (toRemove.isNotEmpty()) {
            val ids = toKeep.toMutableSet()
            val editor = prefs.edit().putStringSet(KEY_PROFILE_IDS, ids)
            toRemove.forEach { editor.remove("$PREFIX_PROFILE${it.id}") }
            editor.apply()

            Log.d("ConnectionHistoryStore", "Pruned ${toRemove.size} old profiles")
        }
    }
}
