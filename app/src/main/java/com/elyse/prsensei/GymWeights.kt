package com.elyse.prsensei

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Serializable
data class PlateType(
    val weight: Double,
    val isExclusive: Boolean = false,
    val isEvenOnly: Boolean = false,
    val quantity: String = "1"
)

@Composable
fun GymWeights(
    onBack: () -> Unit,
    viewModel: GymWeightsViewModel = viewModel(factory = GymWeightsViewModelFactory)
) {
    val plateList = viewModel.plateList
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val fadeAlpha = remember { Animatable(1f) }

    var configName by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val savedConfigs = remember(viewModel.savedConfigNames) {
        listOf("Barbell Default") + viewModel.savedConfigNames
    }

    LaunchedEffect(configName) {
        viewModel.loadConfiguration(configName)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(16.dp)
                        .graphicsLayer { alpha = fadeAlpha.value }
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(data.visuals.message, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(configName.ifEmpty { "Barbell Default" })
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    savedConfigs.forEach { config ->
                        DropdownMenuItem(
                            text = { Text(config) },
                            onClick = {
                                configName = if (config == "Barbell Default") "" else config
                                expanded = false
                            }
                        )
                    }
                }
            }

            Text("Edit Configuration", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = configName,
                onValueChange = { configName = it },
                label = { Text("Configuration Name") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(plateList) { plate ->
                    PlateInputRow(
                        plate = plate,
                        onCountChange = { newCount ->
                            val index = plateList.indexOf(plate)
                            plateList[index] = plate.copy(quantity = newCount)
                        },
                        onToggleExclusive = {
                            val index = plateList.indexOf(plate)
                            plateList[index] = plate.copy(isExclusive = !plate.isExclusive)
                        },
                        onDelete = { plateList.remove(plate) }
                    )
                }
                item {
                    Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Add Custom Weight")
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.saveConfiguration(configName)
                    scope.launch {
                        fadeAlpha.snapTo(1f)
                        val job = launch { snackbarHostState.showSnackbar("SAVED") }
                        delay(2000)
                        fadeAlpha.animateTo(0f, animationSpec = tween(500))
                        job.cancel()
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Configuration")
            }

            if (configName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Configuration")
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Configuration") },
            text = { Text("Are you sure you want to delete the '$configName' configuration?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    viewModel.deleteConfiguration(configName)
                    scope.launch {
                        fadeAlpha.snapTo(1f)
                        val job = launch { snackbarHostState.showSnackbar("DELETED") }
                        delay(2000)
                        fadeAlpha.animateTo(0f, animationSpec = tween(500))
                        job.cancel()
                        onBack()
                    }
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDialog) {
        var newWeightText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Custom Weight") },
            text = { OutlinedTextField(value = newWeightText, onValueChange = { newWeightText = it }, label = { Text("Weight (kg)") }) },
            confirmButton = {
                Button(onClick = {
                    val weight = newWeightText.toDoubleOrNull()
                    if (weight != null) {
                        plateList.add(PlateType(weight = weight, quantity = "1"))
                        plateList.sortBy { it.weight }
                    }
                    showDialog = false
                }) { Text("Add") }
            }
        )
    }
}

@Composable
fun PlateInputRow(plate: PlateType, onCountChange: (String) -> Unit, onToggleExclusive: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(20.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape).background(if (plate.isExclusive) Color(0xFF4CC9F0) else Color.Transparent).clickable { onToggleExclusive() })
        Text("${plate.weight} kg", modifier = Modifier.width(60.dp).padding(start = 8.dp))
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(value = if (plate.quantity == "-1") "" else plate.quantity, onValueChange = onCountChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Qty", color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = MaterialTheme.typography.bodySmall.fontSize) })
        }
        IconButton(onClick = onDelete) { Text("X", color = Color.Gray, fontWeight = FontWeight.Bold) }
    }
}