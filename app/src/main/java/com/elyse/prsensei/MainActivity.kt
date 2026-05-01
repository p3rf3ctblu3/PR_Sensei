package com.elyse.prsensei

import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storageManager = DataStorageManager(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Just pass the manager
                    AppNavigator(storageManager)
                }
            }
        }
    }
}

@Composable
fun AppNavigator(storageManager: DataStorageManager) {
    // 1. The factory needs the storageManager to build the ViewModel
    val gymViewModel: GymWeightsViewModel = viewModel(
        factory = GymWeightsViewModelFactory
    )

    // 2. Setup Pager State
    val pages = listOf("calculator", "details")
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    var selectedExerciseForDetails by remember { mutableStateOf("Squat") }
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        GymWeights(
            onBack = { showSettings = false },
            viewModel = gymViewModel
        )
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { pageIndex ->
            when (pages[pageIndex]) {
                "calculator" -> {
                    PRCalculatorScreen(
                        onNavigateToSettings = { showSettings = true },
                        onNavigateToCoefficients = { exercise ->
                            selectedExerciseForDetails = exercise
                            // Programmatically swipe to details when chevron is clicked
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        viewModel = gymViewModel
                    )
                }
                "details" -> {
                    ViewExerciseDetails(
                        exerciseName = selectedExerciseForDetails,
                        viewModel = gymViewModel,
                        onBack = {
                            scope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                    )
                }
            }
        }
    }
}