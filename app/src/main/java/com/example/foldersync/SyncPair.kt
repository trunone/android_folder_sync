package com.example.foldersync

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SyncPair(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sourceUri: String,
    val destUri: String,
    val useHash: Boolean
)

object SyncPairRepository {
    private const val PREFS_NAME = "sync_prefs"
    private const val KEY_SYNC_PAIRS = "sync_pairs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAllSyncPairs(context: Context): List<SyncPair> {
        val jsonString = getPrefs(context).getString(KEY_SYNC_PAIRS, "[]")
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<SyncPair>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                SyncPair(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    sourceUri = obj.getString("sourceUri"),
                    destUri = obj.getString("destUri"),
                    useHash = obj.getBoolean("useHash")
                )
            )
        }
        return list
    }

    fun saveSyncPair(context: Context, pair: SyncPair) {
        val list = getAllSyncPairs(context).toMutableList()
        val index = list.indexOfFirst { it.id == pair.id }
        if (index != -1) {
            list[index] = pair
        } else {
            list.add(pair)
        }
        saveList(context, list)
    }

    fun deleteSyncPair(context: Context, id: String) {
        val list = getAllSyncPairs(context).toMutableList()
        list.removeAll { it.id == id }
        saveList(context, list)
    }

    fun getSyncPair(context: Context, id: String): SyncPair? {
        return getAllSyncPairs(context).find { it.id == id }
    }

    private fun saveList(context: Context, list: List<SyncPair>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("sourceUri", item.sourceUri)
            obj.put("destUri", item.destUri)
            obj.put("useHash", item.useHash)
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_SYNC_PAIRS, jsonArray.toString()).apply()
    }
}
