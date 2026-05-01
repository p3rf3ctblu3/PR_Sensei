package com.elyse.prsensei

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elyse.prsensei.logic.Calculator
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock

@Composable
fun PRCalculatorScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToCoefficients: (String) -> Unit,
    viewModel: GymWeightsViewModel = viewModel(factory = GymWeightsViewModelFactory)
) {
    val navyBlue = Color(0xFF0D1B2A)
    val whiteText = Color(0xFFFFFFFF)
    val blueButton = Color(0xFF4CC9F0)
    val gold = Color(0xFFFFD700)

    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var percentage by remember { mutableStateOf("1") }
    var minReps by remember { mutableStateOf("7") }
    var maxReps by remember { mutableStateOf("9") }
    var expandedSetIndex by remember { mutableStateOf<Int?>(null) }

    var selectedExercise by remember { mutableStateOf<String?>(null) }
    var expandedExercise by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<String?>(null) }

    var expandedConfig by remember { mutableStateOf(false) }
    var selectedConfig by remember { mutableStateOf("Barbell Default") }
    val savedConfigs = listOf("Barbell Default") + viewModel.savedConfigNames

    var adjustmentFeedback by remember { mutableStateOf<String?>(null) }
    var allowCoeffUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(selectedConfig) {
        val configToLoad = if (selectedConfig == "Barbell Default") "" else selectedConfig
        viewModel.loadConfiguration(configToLoad)
    }

    LaunchedEffect(selectedExercise) {
        if (selectedExercise != null) {
            viewModel.loadCoefficientsForExercise(selectedExercise!!)
        }
    }

    LaunchedEffect(selectedExercise) {
        allowCoeffUpdate = false // Reset the safety lock on exercise change
        if (selectedExercise != null) {
            viewModel.loadCoefficientsForExercise(selectedExercise!!)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(navyBlue)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.sensei),
            contentDescription = "Sensei Banner",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "PR Sensei",
                style = MaterialTheme.typography.headlineMedium,
                color = whiteText
            )

            // --- Exercise Selection ---
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedExercise = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = blueButton)
                ) {
                    Text(selectedExercise ?: "Select Exercise", color = whiteText)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null, tint = blueButton)
                }
                DropdownMenu(
                    expanded = expandedExercise,
                    onDismissRequest = { expandedExercise = false },
                    modifier = Modifier.background(Color(0xFF1B263B))
                ) {
                    viewModel.exerciseList.forEach { exercise ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        exercise,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(onClick = {
                                        expandedExercise = false
                                        onNavigateToCoefficients(exercise)
                                    }) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "View Coeffs",
                                            tint = Color.Gray
                                        )
                                    }

                                    val protectedExercises = listOf("Squat", "Bench Press", "Deadlift")
                                    if (exercise !in protectedExercises) {
                                        IconButton(onClick = {
                                            exerciseToDelete = exercise
                                            showDeleteDialog = true
                                            expandedExercise = false
                                        }) {
                                            Text(
                                                "X",
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                selectedExercise = exercise
                                expandedExercise = false
                            }
                        )
                    }
                    HorizontalDivider(color = Color.Gray)
                    DropdownMenuItem(
                        text = {
                            Text(
                                "+ Add Exercise",
                                fontWeight = FontWeight.Bold,
                                color = blueButton
                            )
                        },
                        onClick = { showAddDialog = true; expandedExercise = false }
                    )
                }
            }

            // --- Weight and Reps Row ---
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GymInput(
                    value = weight,
                    label = "Weight",
                    onValueChange = { weight = it },
                    modifier = Modifier.weight(1f)
                )
                GymInput(
                    value = reps,
                    label = "Reps",
                    onValueChange = { reps = it },
                    modifier = Modifier.weight(1f)
                )
            }

            // --- Config Selection ---
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { expandedConfig = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = blueButton,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Gym Weights: $selectedConfig")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(
                    expanded = expandedConfig,
                    onDismissRequest = { expandedConfig = false },
                    modifier = Modifier.background(Color(0xFF1B263B))
                ) {
                    savedConfigs.forEach { config ->
                        DropdownMenuItem(
                            text = { Text(config, color = Color.White) },
                            onClick = { selectedConfig = config; expandedConfig = false }
                        )
                    }
                    HorizontalDivider(color = Color.Gray)
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Edit Configurations",
                                fontWeight = FontWeight.Bold,
                                color = blueButton
                            )
                        },
                        onClick = { onNavigateToSettings(); expandedConfig = false }
                    )
                }
            }

            // --- Goal Inputs ---
            GymInput(
                value = percentage,
                label = "Target Increase %",
                onValueChange = { percentage = it }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GymInput(
                    value = minReps,
                    label = "Min Reps",
                    onValueChange = { minReps = it },
                    modifier = Modifier.weight(1f)
                )
                GymInput(
                    value = maxReps,
                    label = "Max Reps",
                    onValueChange = { maxReps = it },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.White.copy(alpha = 0.2f)
            )

            // --- Calculation Results ---
            val w = weight.toDoubleOrNull() ?: 0.0
            val r = reps.toIntOrNull() ?: 0
            val p = percentage.toDoubleOrNull() ?: 1.0
            val start = minReps.toIntOrNull() ?: 7
            val end = maxReps.toIntOrNull() ?: 9

            if (w > 0 && r > 0 && end >= start) {
                val currentCoeffs = selectedExercise?.let {
                    viewModel.exerciseCoeffs[it]
                } ?: viewModel.defaultCoeffs

                val target1RM = Calculator.getNew1RM(w, r, p, currentCoeffs)

                val suggestedSets = Calculator.getNextSet(
                    target1RM,
                    (start..end).toList(),
                    viewModel.plateList.toList(),
                    currentCoeffs
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A3971)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "SENSEI DEMANDS:",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFF8A062),
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        suggestedSets.forEachIndexed { index, set ->
                            Column(modifier = Modifier.clickable {
                                expandedSetIndex = if (expandedSetIndex == index) null else index
                                adjustmentFeedback = null
                            }) {
                                Text(
                                    "${set.weight} kg for ${set.reps} reps",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = whiteText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Rounding error: ${"%+.2f".format(set.error)} kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9C9C9C)
                                )

                                // --- Inside suggestedSets.forEachIndexed loop ---
                                if (expandedSetIndex == index) {
                                    if (selectedExercise != null) {
                                        // --- Header Row for Label and Toggle ---
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Adjust Coefficients",
                                                fontStyle = FontStyle.Italic,
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )

                                            // --- Smaller, Minimalist Safety Circle ---
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .then(
                                                        if (allowCoeffUpdate) {
                                                            Modifier.background(blueButton, shape = androidx.compose.foundation.shape.CircleShape)
                                                        } else {
                                                            Modifier.border(1.5.dp, Color.Gray, shape = androidx.compose.foundation.shape.CircleShape)
                                                        }
                                                    )
                                                    .clickable { allowCoeffUpdate = !allowCoeffUpdate },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (allowCoeffUpdate) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            if (adjustmentFeedback != null) {
                                                Text(
                                                    adjustmentFeedback!!,
                                                    color = gold,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                // --- NOT TODAY Button (Only enabled if toggle is blue) ---
                                                Button(
                                                    onClick = {
                                                        viewModel.updateCoefficient(selectedExercise!!, set.reps, false)
                                                        adjustmentFeedback = "Coefficient Reduced"
                                                        allowCoeffUpdate = false
                                                    },
                                                    enabled = allowCoeffUpdate,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = navyBlue,
                                                        disabledContainerColor = navyBlue.copy(alpha = 0.3f)
                                                    )
                                                ) { Text("Not Today") }

                                                // --- NEW PR Button (ALWAYS ENABLED) ---
                                                Button(
                                                    onClick = {
                                                        // 1. Always Save PR
                                                        viewModel.addPR(selectedExercise!!, set.weight, set.reps,allowCoeffUpdate)

                                                        // 2. Optionally Update Coeff
                                                        if (allowCoeffUpdate) {
                                                            viewModel.updateCoefficient(selectedExercise!!, set.reps, true)
                                                            adjustmentFeedback = "PR Saved & Coeff Augmented!"
                                                        } else {
                                                            adjustmentFeedback = "PR Saved (Coeff unchanged)"
                                                        }

                                                        allowCoeffUpdate = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = gold,
                                                        contentColor = Color.Black
                                                    )
                                                ) { Text("NEW PR") }
                                            }
                                        }
                                    }
                                }

                                if (index < suggestedSets.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Exercise") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addExercise(name)
                        selectedExercise = name
                        showAddDialog = false
                    }
                }) { Text("Add") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete?") },
            text = { Text("Are you sure you want to delete '$exerciseToDelete'?") },
            confirmButton = {
                TextButton(onClick = {
                    val exerciseName = exerciseToDelete!!
                    viewModel.deleteExercise(exerciseName)

                    // If the user is currently looking at the exercise they just deleted, reset selection
                    if (selectedExercise == exerciseName) {
                        selectedExercise = null
                    }

                    showDeleteDialog = false
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun GymInput(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color(0xFF4CC9F0),
            unfocusedLabelColor = Color.LightGray,
            focusedBorderColor = Color(0xFF4CC9F0),
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color.White
        )
    )
}