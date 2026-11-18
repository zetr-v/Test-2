package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.components.ImagePreviewDialog
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.screens.BatchWorkspaceScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.StudioThemeMode
import com.example.ui.theme.WebPStudioTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val isDark = when (uiState.themeMode) {
                StudioThemeMode.DARK -> true
                StudioThemeMode.LIGHT -> false
                StudioThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(isDark) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                )
            }

            WebPStudioTheme(themeMode = uiState.themeMode) {
                WebPStudioApp(
                    uiState = uiState,
                    viewModel = viewModel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                list.orEmpty()
            }
            else -> emptyList()
        }
        if (uris.isNotEmpty()) {
            viewModel.addImageUris(uris)
        }
    }
}

@Composable
private fun WebPStudioApp(
    uiState: MainUiState,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImageUris(uris)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImageUris(uris)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.addFolderTree(treeUri)
        }
    }

    val exportFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.exportAllToFolder(treeUri)
        }
    }

    val createSingleWebpLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/webp"),
    ) { destUri ->
        if (destUri != null) {
            viewModel.completeSingleSaveAs(destUri)
        }
    }

    val launchImageBrowse = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    val launchFileBrowse = {
        documentPickerLauncher.launch(arrayOf("image/png", "image/jpeg"))
    }

    val launchFolderPick = {
        folderPickerLauncher.launch(null)
    }

    val launchShareIntent: (Intent?) -> Unit = { shareIntent ->
        if (shareIntent != null) {
            context.startActivity(Intent.createChooser(shareIntent, "Share WebP"))
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.activeScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { screen ->
            when (screen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        settings = uiState.settings,
                        queuedCount = uiState.items.size,
                        historyCount = uiState.historyFiles.size,
                        isScanningInput = uiState.isScanningInput,
                        onBrowseImages = launchImageBrowse,
                        onBrowseFiles = launchFileBrowse,
                        onPickFolder = launchFolderPick,
                        onPasteClipboard = { viewModel.pasteFromClipboard() },
                        onLoadSample = { viewModel.loadSampleImages() },
                        onOpenSettings = { viewModel.setShowSettingsSheet(true) },
                        onOpenHistory = { viewModel.navigateTo(AppScreen.HISTORY) },
                        onOpenQueue = { viewModel.navigateTo(AppScreen.WORKSPACE) },
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }

                AppScreen.WORKSPACE -> {
                    BatchWorkspaceScreen(
                        items = uiState.items,
                        settings = uiState.settings,
                        isConverting = uiState.isConverting,
                        conversionCurrent = uiState.conversionCurrent,
                        conversionTotal = uiState.conversionTotal,
                        onBack = { viewModel.navigateBackToHome() },
                        onAddImages = launchImageBrowse,
                        onBrowseFiles = launchFileBrowse,
                        onAddFolder = launchFolderPick,
                        onPasteClipboard = { viewModel.pasteFromClipboard() },
                        onAddSample = { viewModel.loadSampleImages() },
                        onClearQueue = { viewModel.clearQueue() },
                        onRemoveItem = { viewModel.removeItem(it) },
                        onPreviewItem = { viewModel.setPreviewItem(it) },
                        onShareSingle = { converted ->
                            launchShareIntent(viewModel.buildShareSingleIntent(converted.outputFile))
                        },
                        onSaveSingle = { converted ->
                            viewModel.saveSingleFileToGalleryOrExport(
                                file = converted.outputFile,
                                fileName = converted.outputFileName,
                                onNeedSaveAs = { suggestedName ->
                                    createSingleWebpLauncher.launch(suggestedName)
                                },
                            )
                        },
                        onModeChange = { viewModel.updateCompressionMode(it) },
                        onQualityChange = { viewModel.updateQuality(it) },
                        onOpenSettings = { viewModel.setShowSettingsSheet(true) },
                        onConvertAll = { reconvert -> viewModel.convertBatch(reconvertAll = reconvert) },
                        onSaveAllToGallery = {
                            viewModel.saveAllToGallery(
                                onNeedFolderPicker = { exportFolderLauncher.launch(null) },
                            )
                        },
                        onExportAllToFolder = { exportFolderLauncher.launch(null) },
                        onShareAll = { launchShareIntent(viewModel.buildShareAllIntent()) },
                    )
                }

                AppScreen.HISTORY -> {
                    HistoryScreen(
                        files = uiState.historyFiles,
                        onBack = { viewModel.navigateBackToHome() },
                        onShareFile = { file ->
                            launchShareIntent(viewModel.buildShareSingleIntent(file))
                        },
                        onSaveFile = { file, fileName ->
                            viewModel.saveSingleFileToGalleryOrExport(
                                file = file,
                                fileName = fileName,
                                onNeedSaveAs = { suggestedName ->
                                    createSingleWebpLauncher.launch(suggestedName)
                                },
                            )
                        },
                        onDeleteFile = { viewModel.deleteHistoryFile(it) },
                        onClearAll = { viewModel.clearHistory() },
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            }
        }
    }

    if (uiState.showSettingsSheet) {
        SettingsBottomSheet(
            settings = uiState.settings,
            themeMode = uiState.themeMode,
            onModeChange = { viewModel.updateCompressionMode(it) },
            onQualityChange = { viewModel.updateQuality(it) },
            onResizeChange = { viewModel.updateResizePreset(it) },
            onThemeModeChange = { viewModel.updateThemeMode(it) },
            onDismiss = { viewModel.setShowSettingsSheet(false) },
        )
    }

    val previewItem = uiState.previewItem
    if (previewItem != null) {
        ImagePreviewDialog(
            item = previewItem,
            onDismiss = { viewModel.setPreviewItem(null) },
            onShare = { converted ->
                launchShareIntent(viewModel.buildShareSingleIntent(converted.outputFile))
            },
            onSave = { converted ->
                viewModel.saveSingleFileToGalleryOrExport(
                    file = converted.outputFile,
                    fileName = converted.outputFileName,
                    onNeedSaveAs = { suggestedName ->
                        createSingleWebpLauncher.launch(suggestedName)
                    },
                )
            },
        )
    }
}
