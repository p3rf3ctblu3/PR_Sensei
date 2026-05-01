package com.elyse.prsensei

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import kotlinx.serialization.json.Json
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import kotlinx.serialization.builtins.ListSerializer
import androidx.compose.runtime.mutableStateMapOf

data class PRRecord(
    val date: String,
    val weight: Double,
    val reps: Int,
    val wasAdjusted: Boolean = false
)

val GymWeightsViewModelFactory = viewModelFactory {
    initializer {
        // We retrieve the Application context directly from the extras
        val app = this[APPLICATION_KEY] as android.app.Application
        GymWeightsViewModel(DataStorageManager(app))
    }
}
class GymWeightsViewModel(private val storage: DataStorageManager) : ViewModel() {

    val plateList = mutableStateListOf<PlateType>()
    val exerciseList = mutableStateListOf("Squat", "Bench Press", "Deadlift")

    // Track available configs
    val savedConfigNames = mutableStateListOf<String>()
    val exerciseBaseCoeffs = mutableStateMapOf<String, Map<Int, Double>>()

    init {
        viewModelScope.launch {
            // 1. Load config names
            savedConfigNames.addAll(storage.getAllConfigNames())

            // 2. Load all coefficients
            val allSavedCoeffs = storage.getAllSavedCoefficients()
            allSavedCoeffs.forEach { (exercise, coeffs) ->
                exerciseCoeffs[exercise] = coeffs.toMutableMap()

                // 3. Load history for each exercise found
                val history = storage.loadPRHistory(exercise)
                prHistory[exercise] = history.toMutableList()
            }

            val savedExercises = storage.loadExerciseNames()
            if (!savedExercises.isNullOrEmpty()) {
                // IMPORTANT: Clear and replace only if we actually found saved data
                exerciseList.clear()
                exerciseList.addAll(savedExercises)
            }
            exerciseList.forEach { exercise ->
                loadBaseCoefficientsForExercise(exercise)
            }
        }
    }

    private fun loadDefaultPlates() {
        plateList.clear()
        plateList.addAll(listOf(
            PlateType(25.0, isEvenOnly = true, quantity = "0"),
            PlateType(20.0, isEvenOnly = true, quantity = "101"),
            PlateType(15.0, isEvenOnly = true, quantity = "2"),
            PlateType(10.0, isEvenOnly = true, quantity = "2"),
            PlateType(5.0, isEvenOnly = true, quantity = "2"),
            PlateType(2.5, isEvenOnly = true, quantity = "2"),
            PlateType(1.25, isEvenOnly = true, quantity = "2"),
            PlateType(0.5, isEvenOnly = true, quantity = "2")
        ))
    }

    //------------------------------------CONFIGURATION FUNCTIONS----------------------------------

    fun loadConfiguration(configName: String) {
        if (configName.isEmpty()) {
            loadDefaultPlates()
            return
        }

        viewModelScope.launch {
            val savedJson = storage.getConfig(configName) // Assuming your storage supports name-based lookup
            if (savedJson != null) {
                try {
                    val savedData = Json.decodeFromString(
                        ListSerializer(PlateType.serializer()),
                        savedJson
                    )
                    plateList.clear()
                    plateList.addAll(savedData)
                } catch (e: Exception) {
                    println("Error loading: ${e.message}")
                }
            }
        }
    }

    fun saveConfiguration(configName: String) {
        viewModelScope.launch {
            val jsonString = Json.encodeToString(
                ListSerializer(PlateType.serializer()),
                plateList.toList()
            )
            storage.saveConfig(configName, jsonString)

            // Update the dropdown list if it's a new name
            if (configName.isNotEmpty() && !savedConfigNames.contains(configName)) {
                savedConfigNames.add(configName)
            }
        }
    }

    // Call this from the UI when you want to delete a config
    fun deleteConfiguration(name: String) {
        viewModelScope.launch {
            storage.deleteConfig(name)
            savedConfigNames.remove(name)
        }
    }

    //---------------------COEFFICIENT FUNCTIONS----------------------------
    val defaultCoeffs = mapOf(
        1 to 1.000, 2 to 0.943, 3 to 0.906, 4 to 0.881, 5 to 0.856,
        6 to 0.831, 7 to 0.807, 8 to 0.786, 9 to 0.765, 10 to 0.744,
        11 to 0.723, 12 to 0.703, 13 to 0.688, 14 to 0.675, 15 to 0.662,
        16 to 0.650, 17 to 0.638, 18 to 0.627, 19 to 0.616, 20 to 0.606
    )

    // Store this in your ViewModel state
    val exerciseCoeffs = mutableStateMapOf<String, Map<Int, Double>>()

    fun updateExerciseBaseCoefficient(exerciseName: String, reps: Int, newValue: Double) {
        // 1. Get the existing base map for this exercise, or fallback to the global hardcoded defaults
        val currentBaseMap = exerciseBaseCoeffs[exerciseName] ?: defaultCoeffs
        val updatedBaseMap = currentBaseMap.toMutableMap()

        // 2. Update the specific rep value
        updatedBaseMap[reps] = newValue

        // 3. Update Memory
        exerciseBaseCoeffs[exerciseName] = updatedBaseMap

        val currentMap = exerciseCoeffs[exerciseName] ?: defaultCoeffs
        val newMap = currentMap.toMutableMap()
        newMap[reps] = newValue
        exerciseCoeffs [exerciseName] = newMap

        // 4. Save to Disk (You'll need to add this method to your Storage Manager)
        viewModelScope.launch {
            storage.saveExerciseBaseCoefficients(exerciseName, updatedBaseMap)
            storage.saveExerciseCoefficients(exerciseName, newMap)
        }
    }

    fun updateCoefficient(exerciseName: String, reps: Int, increment: Boolean) {
        val currentMap = exerciseCoeffs[exerciseName] ?: defaultCoeffs
        val newMap = currentMap.toMutableMap()
        val adjustment = if (increment) 0.01 else -0.01
        newMap[reps] = (newMap[reps] ?: 1.0) + adjustment

        // Update memory
        exerciseCoeffs[exerciseName] = newMap

        // Update disk (Use 'storage', not 'DataStorageManager')
        viewModelScope.launch {
            storage.saveExerciseCoefficients(exerciseName, newMap)
        }
    }

    fun loadCoefficientsForExercise(exercise: String) {
        viewModelScope.launch {
            val loaded = storage.loadExerciseCoefficients(exercise)
            if (loaded != null) {
                exerciseCoeffs[exercise] = loaded
            }
        }
    }

    fun loadBaseCoefficientsForExercise(exercise: String) {
        viewModelScope.launch {
            val loadedBase = storage.loadExerciseBaseCoefficients(exercise)
            if (loadedBase != null) {
                // Update the state map to trigger the UI refresh in CoefficientList
                exerciseBaseCoeffs[exercise] = loadedBase
            }
        }
    }

    //-----------------------------------PR HISTORY FUNCTIONS-------------------------------------
    val prHistory = mutableStateMapOf<String, MutableList<PRRecord>>()
    fun addPR(exercise: String, weight: Double, reps: Int, adjusted: Boolean) {
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        // 1. Create the record with the new 'wasAdjusted' flag
        val newRecord = PRRecord(
            date = currentDate,
            weight = weight,
            reps = reps,
            wasAdjusted = adjusted
        )

        viewModelScope.launch {
            // 2. Get current list from memory or storage
            val currentList = prHistory[exercise]?.toMutableList()
                ?: storage.loadPRHistory(exercise).toMutableList()

            // 3. Add to the START of the list (so newest PRs appear at the top)
            currentList.add(0, newRecord)

            // 4. Update the state map to trigger UI refresh
            prHistory[exercise] = currentList

            // 5. Persist to DataStore
            storage.savePRHistory(exercise, currentList)
        }
    }

    fun loadHistoryForExercise(exercise: String) {
        // Only load if we don't already have it, or always refresh if you prefer
        if (!prHistory.containsKey(exercise)) {
            viewModelScope.launch {
                val history = storage.loadPRHistory(exercise)
                prHistory[exercise] = history
            }
        }
    }
    fun deletePR(exerciseName: String, record: PRRecord) {
        viewModelScope.launch {
            val updatedHistory = prHistory[exerciseName]?.toMutableList() ?: mutableListOf()

            if (updatedHistory.remove(record)) {
                prHistory[exerciseName] = updatedHistory
                storage.savePRHistory(exerciseName, updatedHistory)

                // ONLY undo if the record actually changed the coefficient
                if (record.wasAdjusted) {
                    updateCoefficient(exerciseName, record.reps, increment = false)
                }
            }
        }
    }

    fun addExercise(name: String) {
        if (name.isNotBlank() && !exerciseList.contains(name)) {
            exerciseList.add(name)
            viewModelScope.launch {
                storage.saveExerciseNames(exerciseList.toList())
            }
        }
    }

    fun deleteExercise(name: String) {
        // 1. Remove from the UI list
        if (exerciseList.remove(name)) {
            viewModelScope.launch {
                // 2. Remove from the name list on disk
                storage.saveExerciseNames(exerciseList.toList())

                // 3. Remove the specific PR history and Coefficients from disk
                storage.deleteExerciseData(name)

                // 4. Clear from memory so it doesn't reappear if we don't restart
                prHistory.remove(name)
                exerciseCoeffs.remove(name)
            }
        }
    }
}

