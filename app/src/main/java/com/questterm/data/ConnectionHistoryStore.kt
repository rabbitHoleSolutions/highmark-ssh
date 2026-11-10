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
    }

    init {
        purgeDemoDefaultsIfPresent()
    }

    private fun purgeDemoDefaultsIfPresent() {
        val ids = prefs.getStringSet(KEY_PROFILE_IDS, emptySet()) ?: emptySet()
        if (ids.isEmpty()) return
        val demoIds = ids.filter { id ->
            val json = prefs.getString("$PREFIX_PROFILE$id", null)
            json?.contains("bandit.labs.overthewire.org") == true
        }
        if (demoIds.isNotEmpty()) {
            val remainingIds = (ids - demoIds.toSet()).toMutableSet()
            val editor = prefs.edit().putStringSet(KEY_PROFILE_IDS, remainingIds)
            demoIds.forEach { editor.remove("$PREFIX_PROFILE$it") }
            editor.apply()
        }
    }

    fun getAllProfiles(): List<ConnectionProfile> {
        val ids = prefs.getStringSet(KEY_PROFILE_IDS, emptySet()) ?: emptySet()
        return ids.mapNotNull { id ->
            prefs.getString("$PREFIX_PROFILE$id", null)?.let {
                gson.fromJson(it, ConnectionProfile::class.java)
            }?.let { profile ->
                // Profiles saved before key-auth support was added have no `authMethod`
                // in their stored JSON. ConnectionProfile has required constructor params,
                // so Gson deserializes it via unsafe field injection rather than the
                // constructor, leaving such gaps as a raw null despite the non-null Kotlin
                // type — normalize that back to the intended default.
                @Suppress("SENSELESS_COMPARISON")
                if (profile.authMethod == null) profile.copy(authMethod = AuthMethod.PASSWORD) else profile
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

        // Password and key are stored independently, so connecting with one method
        // never discards the other. A key in particular may already be installed on
        // the server and can't be recovered once dropped.
        //
        // Password: only a password login updates it, gated by the remember toggle.
        val encryptedPassword = when {
            profile.authMethod != AuthMethod.PASSWORD -> existing?.encryptedPassword
            rememberPassword -> profile.encryptedPassword
            else -> null
        }
        // Key: always persisted once present; kept when the incoming profile has none.
        val hasNewKey = profile.encryptedPrivateKey != null && profile.publicKeyRaw != null
        val encryptedPrivateKey = if (hasNewKey) profile.encryptedPrivateKey else existing?.encryptedPrivateKey
        val publicKeyRaw = if (hasNewKey) profile.publicKeyRaw else existing?.publicKeyRaw

        // Base on the existing profile when updating (preserves id/favorite status),
        // otherwise on the freshly-built profile.
        val toSave = (existing ?: profile).copy(
            authMethod = profile.authMethod,
            encryptedPassword = encryptedPassword,
            encryptedPrivateKey = encryptedPrivateKey,
            publicKeyRaw = publicKeyRaw,
            lastUsedTimestamp = System.currentTimeMillis(),
        )

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
