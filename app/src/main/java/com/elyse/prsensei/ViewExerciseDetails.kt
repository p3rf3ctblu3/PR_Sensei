package com.elyse.prsensei

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Color as AndroidColor // Alias this for MPAndroidChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import androidx.compose.foundation.clickable // Fixes 'clickable'
import androidx.compose.material.icons.Icons // Fixes 'Icons'
import androidx.compose.material.icons.filled.ArrowDropDown // Fixes 'ArrowDropDown'

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewExerciseDetails(
    exerciseName: String,
    viewModel: GymWeightsViewModel,
    onBack: () -> Unit
) {
    // 1. Local state to track which exercise the dropdown has selected
    var currentExercise by remember { mutableStateOf(exerciseName) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Sync if the navigator forces a change (e.g., swiping from Calculator)
    LaunchedEffect(exerciseName) {
        currentExercise = exerciseName
        viewModel.loadHistoryForExercise(currentExercise)
        // Load the "Current" adjusted coefficients
        viewModel.loadCoefficientsForExercise(currentExercise)
        // NEW: Load the "Base" coefficients so the (+/-) diffs are correct
        viewModel.loadBaseCoefficientsForExercise(currentExercise)
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // --- Header Row with Back Button and Dropdown ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Back") }

            Spacer(modifier = Modifier.width(16.dp))

            // --- EXERCISE DROPDOWN ---
            Box {
                Row(
                    modifier = Modifier
                        .clickable { dropdownExpanded = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentExercise,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Exercise"
                    )
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    viewModel.exerciseList.forEach { exercise ->
                        DropdownMenuItem(
                            text = { Text(exercise) },
                            onClick = {
                                currentExercise = exercise
                                dropdownExpanded = false
                                viewModel.loadHistoryForExercise(exercise)
                            }
                        )
                    }
                }
            }
        }

        // --- Tab Switcher (Uses currentExercise) ---
        TabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("PRs") }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("Coefficients") }
            )
        }

        // --- Swipeable Pager Content (Passes currentExercise down) ---
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> {
                    Column {
                        PRGraph(
                            history = viewModel.prHistory[currentExercise] ?: emptyList(),
                            coeffs = viewModel.exerciseCoeffs[currentExercise] ?: viewModel.defaultCoeffs,
                            modifier = Modifier.fillMaxWidth().height(250.dp)
                        )
                        PRHistoryList(currentExercise, viewModel)
                    }
                }
                1 -> CoefficientList(currentExercise, viewModel)
            }
        }
    }
}

@Composable
fun CoefficientList(exerciseName: String, viewModel: GymWeightsViewModel) {
    val currentCoeffs = viewModel.exerciseCoeffs[exerciseName] ?: viewModel.defaultCoeffs
    // Look for the exercise-specific base, fallback to global defaults if never edited
    val baseCoeffs = viewModel.exerciseBaseCoeffs[exerciseName] ?: viewModel.defaultCoeffs

    var editingReps by remember { mutableStateOf<Int?>(null) }
    var newBaseValueText by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(currentCoeffs.toSortedMap().toList()) { (reps, currentValue) ->
            // Use the exercise-specific baseline
            val baseValue = baseCoeffs[reps] ?: (viewModel.defaultCoeffs[reps] ?: 1.0)
            val diff = currentValue - baseValue

            val diffText = if (diff >= 0) "(+%.3f)".format(diff) else "(%.3f)".format(diff)
            val diffColor = when {
                diff > 0 -> Color.Green
                diff < 0 -> Color.Red
                else -> Color.Gray
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        editingReps = reps
                        // Fix: Use the consistent variable name and current baseValue
                        newBaseValueText = baseValue.toString()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reps: $reps", style = MaterialTheme.typography.titleMedium)
                        Text(
                            // Fix: Refer to baseValue calculated above
                            "Base: %.3f".format(baseValue),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Current: ${"%.3f".format(currentValue)}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = diffText,
                            color = diffColor,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    // --- Edit Base Dialog ---
    if (editingReps != null) {
        AlertDialog(
            onDismissRequest = { editingReps = null },
            title = { Text("Edit Base Coefficient ($editingReps Reps)") },
            text = {
                Column {
                    Text("Change the starting baseline for this specific exercise.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        // Fix: Use the consistent variable name
                        value = newBaseValueText,
                        onValueChange = { newBaseValueText = it },
                        label = { Text("Base Value") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newVal = newBaseValueText.toDoubleOrNull()
                    if (newVal != null) {
                        // Fix: Call the exercise-specific update function
                        viewModel.updateExerciseBaseCoefficient(exerciseName, editingReps!!, newVal)
                    }
                    editingReps = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingReps = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PRHistoryList(exerciseName: String, viewModel: GymWeightsViewModel) {
    val history = viewModel.prHistory[exerciseName] ?: emptyList()
    var recordToDelete by remember { mutableStateOf<PRRecord?>(null) }

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete PR Entry") },
            text = { Text("Are you sure you want to delete this record?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePR(exerciseName, recordToDelete!!)
                    recordToDelete = null
                }) { Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("PR History is empty.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history) { record ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(record.date, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            text = "${record.weight}kg x ${record.reps}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { recordToDelete = record }) {
                            Text("✕", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Helper function to calculate 1RM
fun calculate1RM(weight: Double, reps: Int, coeffs: Map<Int, Double>): Double {
    if (reps == 1) return weight

    // Use your coefficient logic: weight / coeff[reps] or whatever your math is
    // Assuming a standard inverse coefficient approach:
    val coefficient = coeffs[reps] ?: 1.0 // Fallback to 1.0 if not found
    return weight / coefficient
}
@Composable
fun PRGraph(
    history: List<PRRecord>,
    coeffs: Map<Int, Double>,
    modifier: Modifier = Modifier
) {
    // We calculate entries here so they update whenever 'history' changes
    val entries = remember(history) {
        history.mapIndexed { index, record ->
            Entry(index.toFloat(), calculate1RM(record.weight, record.reps, coeffs).toFloat())
        }
    }
    val dates = remember(history) { history.map { it.date } }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("1RM (kg)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    // Set up static properties here
                }
            },
            // THIS IS THE KEY: The update block triggers when 'entries' or 'dates' change
            update = { chart ->
                val dataSet = LineDataSet(entries, "").apply {
                    color = AndroidColor.BLUE
                    setCircleColor(AndroidColor.BLUE)
                    valueTextSize = 10f
                }
                chart.data = LineData(dataSet)

                chart.xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(dates)
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    labelRotationAngle = -45f
                }

                // Refresh Y-Axis logic
                if (entries.isNotEmpty()) {
                    val minVal = entries.minOf { it.y }
                    val maxVal = entries.maxOf { it.y }
                    val range = maxVal - minVal
                    val buffer = if (range == 0f) 5f else range * 0.1f
                    chart.axisLeft.axisMinimum = minVal - buffer
                    chart.axisLeft.axisMaximum = maxVal + buffer
                }

                chart.notifyDataSetChanged() // Tell the chart data changed
                chart.invalidate()           // Redraw the view
            }
        )
    }
}