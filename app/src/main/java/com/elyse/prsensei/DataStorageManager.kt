package com.elyse.prsensei

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "gym_settings")

class DataStorageManager(private val context: Context) {
    private val gson = Gson()

    // --- JSON Serialization Helpers ---
    private fun serializeMapToJson(coeffs: Map<Int, Double>): String = gson.toJson(coeffs)

    private fun deserializeJsonToMap(json: String): Map<Int, Double> {
        val type = object : TypeToken<Map<Int, Double>>() {}.type
        return gson.fromJson(json, type)
    }

    // --- Configuration Methods ---
    suspend fun saveConfig(name: String, json: String) {
        val key = stringPreferencesKey("config_$name")
        context.dataStore.edit { prefs -> prefs[key] = json }
    }

    suspend fun getConfig(name: String): String? {
        return context.dataStore.data.map { it[stringPreferencesKey("config_$name")] }.firstOrNull()
    }

    suspend fun getAllConfigNames(): List<String> {
        return context.dataStore.data.map { prefs ->
            prefs.asMap().keys
                .filter { it.name.startsWith("config_") }
                .map { it.name.removePrefix("config_") }
        }.firstOrNull() ?: emptyList()
    }

    suspend fun deleteConfig(name: String) {
        context.dataStore.edit { it.remove(stringPreferencesKey("config_$name")) }
    }

    suspend fun saveExerciseBaseCoefficients(exercise: String, baseMap: Map<Int, Double>) {
        val key = stringPreferencesKey("base_coeffs_$exercise")
        val json = serializeMapToJson(baseMap)
        context.dataStore.edit { it[key] = json }
    }

    suspend fun loadExerciseBaseCoefficients(exercise: String): Map<Int, Double>? {
        val key = stringPreferencesKey("base_coeffs_$exercise")
        val json = context.dataStore.data.map { it[key] }.firstOrNull()
        return if (json != null) deserializeJsonToMap(json) else null
    }

    suspend fun deleteExerciseData(exercise: String) {
        context.dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("history_$exercise"))
            prefs.remove(stringPreferencesKey("coeffs_$exercise"))
            prefs.remove(stringPreferencesKey("base_coeffs_$exercise")) // Clear base too!
        }
    }

    // --- NEW: Coefficient Methods (Now using DataStore) ---
    suspend fun saveExerciseCoefficients(exercise: String, coeffs: Map<Int, Double>) {
        val key = stringPreferencesKey("coeffs_$exercise")
        val json = serializeMapToJson(coeffs)
        context.dataStore.edit { it[key] = json }
    }



    suspend fun loadExerciseCoefficients(exercise: String): Map<Int, Double>? {
        val key = stringPreferencesKey("coeffs_$exercise")
        val json = context.dataStore.data.map { it[key] }.firstOrNull()
        return if (json != null) deserializeJsonToMap(json) else null
    }

    suspend fun getAllSavedCoefficients(): Map<String, Map<Int, Double>> {
        val allPrefs = context.dataStore.data.firstOrNull() ?: return emptyMap()
        val results = mutableMapOf<String, Map<Int, Double>>()

        allPrefs.asMap().forEach { (key, value) ->
            if (key.name.startsWith("coeffs_")) {
                val exerciseName = key.name.removePrefix("coeffs_")
                val json = value as? String
                if (json != null) {
                    results[exerciseName] = deserializeJsonToMap(json)
                }
            }
        }
        return results
    }

    // --- NEW: PR History Methods (DataStore) ---
    suspend fun savePRHistory(exercise: String, history: List<PRRecord>) {
        val key = stringPreferencesKey("history_$exercise")
        val json = gson.toJson(history)
        context.dataStore.edit { it[key] = json }
    }

    suspend fun loadPRHistory(exercise: String): MutableList<PRRecord> {
        val key = stringPreferencesKey("history_$exercise")
        val json = context.dataStore.data.map { it[key] }.firstOrNull()

        return if (json != null) {
            val type = object : TypeToken<List<PRRecord>>() {}.type
            val list: List<PRRecord> = gson.fromJson(json, type)
            list.toMutableList()
        } else {
            mutableListOf()
        }
    }

    private val EXERCISES_KEY = stringPreferencesKey("custom_exercises")

    suspend fun saveExerciseNames(names: List<String>) {
        val jsonString = gson.run { toJson(names) }
        context.dataStore.edit { preferences ->
            preferences[EXERCISES_KEY] = jsonString
        }
    }

    suspend fun loadExerciseNames(): List<String>? { // Change return type to nullable
        val preferences = context.dataStore.data.firstOrNull()
        val jsonString = preferences?.get(EXERCISES_KEY) ?: return null

        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            null
        }
    }

}