package dev.momro.fosspdf.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private const val MAX_RECENTS = 20

private val Context.recentsDataStore: DataStore<Preferences> by preferencesDataStore("recents")

class RecentRepository(private val context: Context) {

    private val key = stringPreferencesKey("recents")

    val recents: Flow<List<RecentPdf>> = context.recentsDataStore.data.map { prefs ->
        parse(prefs[key] ?: "[]")
    }

    suspend fun add(uri: String, name: String) {
        context.recentsDataStore.edit { prefs ->
            val list = parse(prefs[key] ?: "[]").toMutableList()
            list.removeAll { it.uri == uri }
            list.add(0, RecentPdf(uri, name, System.currentTimeMillis()))
            prefs[key] = serialize(list.take(MAX_RECENTS))
        }
    }

    suspend fun rename(uri: String, newName: String) {
        context.recentsDataStore.edit { prefs ->
            val list = parse(prefs[key] ?: "[]").map {
                if (it.uri == uri) it.copy(name = newName) else it
            }
            prefs[key] = serialize(list)
        }
    }

    private fun parse(json: String): List<RecentPdf> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RecentPdf(
                uri = o.getString("uri"),
                name = o.getString("name"),
                lastOpened = o.getLong("lastOpened"),
            )
        }
    }

    private fun serialize(list: List<RecentPdf>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("uri", it.uri)
                    .put("name", it.name)
                    .put("lastOpened", it.lastOpened)
            )
        }
        return arr.toString()
    }
}
