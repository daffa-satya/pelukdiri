package com.makhp.pelukdiri.features.dashboard

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makhp.pelukdiri.core.domain.engine.CognitiveQuestionGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStatsScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is DashboardUiState.Success) {
            if (state.exportedFile != null) {
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "com.makhp.pelukdiri.fileprovider",
                    state.exportedFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Export ZIP"))
                viewModel.clearExportResult()
            }
            state.exportError?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearExportResult()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updatePermissionStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("App Usage Statistics") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is DashboardUiState.Success) {
                val state = uiState as DashboardUiState.Success
                ExtendedFloatingActionButton(
                    onClick = { viewModel.exportDatabase() },
                    icon = { 
                        if (state.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            // Using a simple text for icon if icons are not available, 
                            // but usually icons are part of material3
                            Text("📦") 
                        }
                    },
                    text = { Text("Export DB") },
                    expanded = !state.isExporting
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DashboardUiState.Success -> {
                    SuccessContent(
                        state = state,
                        onGrantPermission = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        onRefresh = { viewModel.forceRefresh() },
                        onBackfill = { viewModel.backfillHistory() }
                    )
                }
                is DashboardUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadData() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    state: DashboardUiState.Success,
    onGrantPermission: () -> Unit,
    onRefresh: () -> Unit,
    onBackfill: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!state.isPermissionGranted) {
            AlertCard(
                message = "Permission is required to view usage statistics.",
                actionLabel = "Grant Permission",
                onAction = onGrantPermission
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!state.isAccessibilityEnabled) {
            AlertCard(
                message = "Layanan Aksesibilitas PELUKDIRI belum aktif. Aktifkan agar intervensi dapat berjalan.",
                actionLabel = "Aktifkan",
                onAction = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!state.isBatteryOptimizationIgnored) {
            AlertCard(
                message = "Battery optimization is active and may kill background sync. Please whitelist the app for reliable data collection.",
                actionLabel = "Whitelist App",
                onAction = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                OutlinedButton(
                    onClick = onBackfill,
                    enabled = !state.isBackfilling && !state.isRefreshing && !state.isHistoryBackfilled,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    if (state.isBackfilling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (state.isHistoryBackfilled) "Backfilled" else "Backfill")
                    }
                }

                Button(
                    onClick = onRefresh,
                    enabled = !state.isRefreshing && !state.isBackfilling
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Force Sync")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        QuestionTesterCard()
    }
}

@Composable
fun QuestionTesterCard(
    modifier: Modifier = Modifier,
    generator: CognitiveQuestionGenerator = remember { CognitiveQuestionGenerator() }
) {
    var selectedLevel by remember { mutableIntStateOf(1) }
    var currentQuestion by remember { mutableStateOf(generator.generateQuestion(1)) }
    var userAnswer by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🧪 Cognitive Engine Playground",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                (1..5).forEach { lvl ->
                    FilterChip(
                        selected = selectedLevel == lvl,
                        onClick = {
                            selectedLevel = lvl
                            currentQuestion = generator.generateQuestion(lvl)
                            userAnswer = ""
                            feedbackMessage = ""
                        },
                        label = { Text("Lvl $lvl") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentQuestion.expression,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = userAnswer,
                onValueChange = { userAnswer = it },
                label = { Text("Ketik Jawaban") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        feedbackMessage = if (userAnswer.trim() == currentQuestion.correctAnswer.toString()) {
                            "✅ BENAR! Great job."
                        } else {
                            "❌ SALAH! Kunci: ${currentQuestion.correctAnswer}"
                        }
                    }
                ) {
                    Text("Cek Jawaban")
                }

                OutlinedButton(
                    onClick = {
                        currentQuestion = generator.generateQuestion(selectedLevel)
                        userAnswer = ""
                        feedbackMessage = ""
                    }
                ) {
                    Text("Acak Soal Baru")
                }
            }

            if (feedbackMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = feedbackMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AlertCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
