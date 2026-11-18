package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.CompressionMode
import com.example.data.ConversionSettings
import com.example.data.HistoryWebpFile
import com.example.data.ImageConversionItem
import com.example.data.ImageConverterEngine
import com.example.data.ItemConversionStatus
import com.example.data.ResizePreset
import com.example.data.SettingsRepository
import com.example.ui.theme.StudioThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class AppScreen {
    HOME,
    WORKSPACE,
    HISTORY,
}

data class MainUiState(
    val activeScreen: AppScreen = AppScreen.HOME,
    val items: List<ImageConversionItem> = emptyList(),
    val settings: ConversionSettings = ConversionSettings(),
    val themeMode: StudioThemeMode = StudioThemeMode.DARK,
    val isScanningInput: Boolean = false,
    val isConverting: Boolean = false,
    val conversionCurrent: Int = 0,
    val conversionTotal: Int = 0,
    val showSettingsSheet: Boolean = false,
    val previewItem: ImageConversionItem? = null,
    val historyFiles: List<HistoryWebpFile> = emptyList(),
    val pendingSingleSaveFile: Pair<File, String>? = null,
    val snackbarMessage: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = ImageConverterEngine(application)
    private val settingsRepo = SettingsRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.settingsFlow.collectLatest { saved ->
                _uiState.update { it.copy(settings = saved) }
            }
        }
        viewModelScope.launch {
            settingsRepo.themeModeFlow.collectLatest { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        refreshHistory()
    }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.HISTORY) {
            refreshHistory()
        }
        _uiState.update { it.copy(activeScreen = screen) }
    }

    fun navigateBackToHome() {
        _uiState.update { it.copy(activeScreen = AppScreen.HOME) }
    }

    fun setShowSettingsSheet(show: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = show) }
    }

    fun setPreviewItem(item: ImageConversionItem?) {
        _uiState.update { it.copy(previewItem = item) }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun updateCompressionMode(mode: CompressionMode) {
        val next = _uiState.value.settings.copy(mode = mode)
        _uiState.update { it.copy(settings = next) }
        viewModelScope.launch { settingsRepo.updateSettings(next) }
    }

    fun updateQuality(quality: Int) {
        val next = _uiState.value.settings.copy(quality = quality.coerceIn(1, 100))
        _uiState.update { it.copy(settings = next) }
        viewModelScope.launch { settingsRepo.updateSettings(next) }
    }

    fun updateResizePreset(preset: ResizePreset) {
        val next = _uiState.value.settings.copy(resizePreset = preset)
        _uiState.update { it.copy(settings = next) }
        viewModelScope.launch { settingsRepo.updateSettings(next) }
    }

    fun updateThemeMode(mode: StudioThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch { settingsRepo.updateThemeMode(mode) }
    }

    fun addImageUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningInput = true) }
            val inspected = engine.inspectUris(uris)
            appendInspectedItems(inspected)
        }
    }

    fun addFolderTree(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningInput = true) }
            val inspected = engine.scanFolderTree(treeUri)
            appendInspectedItems(inspected)
        }
    }

    fun pasteFromClipboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningInput = true) }
            val inspected = engine.inspectFromClipboard()
            if (inspected.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isScanningInput = false,
                        snackbarMessage = getApplication<Application>().getString(R.string.msg_clipboard_empty),
                    )
                }
            } else {
                appendInspectedItems(inspected)
            }
        }
    }

    fun loadSampleImages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningInput = true) }
            val inspected = engine.loadBundledSamples()
            appendInspectedItems(inspected)
        }
    }

    private fun appendInspectedItems(inspected: List<ImageConversionItem>) {
        val app = getApplication<Application>()
        if (inspected.isEmpty()) {
            _uiState.update {
                it.copy(
                    isScanningInput = false,
                    snackbarMessage = app.getString(R.string.msg_no_supported_images),
                )
            }
            return
        }

        _uiState.update { current ->
            val existingUris = current.items.map { it.sourceUri }.toSet()
            val fresh = inspected.filter { it.sourceUri !in existingUris }
            val toAdd = fresh.ifEmpty { inspected }
            current.copy(
                isScanningInput = false,
                activeScreen = AppScreen.WORKSPACE,
                items = current.items + toAdd,
                snackbarMessage = app.getString(R.string.msg_added_images, toAdd.size),
            )
        }
    }

    fun removeItem(id: String) {
        _uiState.update { state ->
            val updated = state.items.filterNot { it.id == id }
            state.copy(
                items = updated,
                activeScreen = if (updated.isEmpty() && state.activeScreen == AppScreen.WORKSPACE) {
                    AppScreen.HOME
                } else {
                    state.activeScreen
                },
            )
        }
    }

    fun clearQueue() {
        _uiState.update {
            it.copy(
                items = emptyList(),
                activeScreen = AppScreen.HOME,
            )
        }
    }

    fun convertBatch(reconvertAll: Boolean = false) {
        val state = _uiState.value
        if (state.isConverting || state.items.isEmpty()) return

        val targets = if (reconvertAll) {
            state.items
        } else {
            state.items.filter { it.status !is ItemConversionStatus.Converted }
        }
        if (targets.isEmpty()) return

        val targetIds = targets.map { it.id }.toSet()
        val settingsSnapshot = state.settings

        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    isConverting = true,
                    conversionCurrent = 0,
                    conversionTotal = targets.size,
                )
            }

            var completed = 0
            var successCount = 0
            var autoSavedCount = 0

            for (target in targets) {
                if (target.id !in targetIds) continue

                _uiState.update { current ->
                    current.copy(
                        items = current.items.map { item ->
                            if (item.id == target.id) {
                                item.copy(status = ItemConversionStatus.Converting)
                            } else {
                                item
                            }
                        },
                    )
                }

                val result = engine.convertToWebp(target, settingsSnapshot)
                if (result is ItemConversionStatus.Converted) {
                    successCount++
                    if (result.savedToGallery) autoSavedCount++
                }
                completed++

                _uiState.update { current ->
                    current.copy(
                        conversionCurrent = completed,
                        items = current.items.map { item ->
                            if (item.id == target.id) {
                                item.copy(status = result)
                            } else {
                                item
                            }
                        },
                    )
                }
            }

            refreshHistory()
            val app = getApplication<Application>()
            _uiState.update { current ->
                current.copy(
                    isConverting = false,
                    snackbarMessage = when {
                        autoSavedCount > 0 -> app.getString(R.string.msg_converted_and_saved, successCount)
                        successCount > 0 -> app.getString(R.string.msg_converted_done, successCount)
                        else -> null
                    },
                )
            }
        }
    }

    fun saveAllToGallery(onNeedFolderPicker: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _uiState.update {
                it.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_use_folder_export))
            }
            onNeedFolderPicker()
            return
        }

        val items = _uiState.value.items
        viewModelScope.launch {
            val saved = engine.saveItemsToGallery(items)
            _uiState.update { current ->
                current.copy(
                    items = current.items.map { item ->
                        val status = item.status
                        if (status is ItemConversionStatus.Converted && saved > 0) {
                            item.copy(status = status.copy(savedToGallery = true))
                        } else {
                            item
                        }
                    },
                    snackbarMessage = if (saved > 0) {
                        getApplication<Application>().getString(R.string.msg_saved_gallery, saved)
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun saveSingleFileToGalleryOrExport(
        file: File,
        fileName: String,
        onNeedSaveAs: (String) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _uiState.update { it.copy(pendingSingleSaveFile = file to fileName) }
            onNeedSaveAs(fileName)
            return
        }
        viewModelScope.launch {
            val ok = engine.saveFileToGallery(file, fileName)
            if (ok) {
                _uiState.update {
                    it.copy(
                        items = it.items.map { item ->
                            val status = item.status
                            if (status is ItemConversionStatus.Converted && status.outputFile == file) {
                                item.copy(status = status.copy(savedToGallery = true))
                            } else {
                                item
                            }
                        },
                        snackbarMessage = getApplication<Application>().getString(R.string.msg_saved_single),
                    )
                }
            }
        }
    }

    fun exportAllToFolder(treeUri: Uri) {
        val files = _uiState.value.items.mapNotNull { item ->
            val converted = item.status as? ItemConversionStatus.Converted ?: return@mapNotNull null
            converted.outputFile to converted.outputFileName
        }
        if (files.isEmpty()) return

        viewModelScope.launch {
            val exported = engine.exportFilesToFolder(treeUri, files)
            _uiState.update {
                it.copy(
                    snackbarMessage = getApplication<Application>().getString(
                        R.string.msg_exported_folder,
                        exported,
                    ),
                )
            }
        }
    }

    fun prepareSingleSaveAs(file: File, fileName: String) {
        _uiState.update { it.copy(pendingSingleSaveFile = file to fileName) }
    }

    fun completeSingleSaveAs(destUri: Uri) {
        val pending = _uiState.value.pendingSingleSaveFile ?: return
        _uiState.update { it.copy(pendingSingleSaveFile = null) }
        viewModelScope.launch {
            val ok = engine.writeFileToUri(pending.first, destUri)
            if (ok) {
                _uiState.update {
                    it.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_saved_single))
                }
            }
        }
    }

    fun buildShareAllIntent(): Intent? {
        val files = _uiState.value.items.mapNotNull {
            (it.status as? ItemConversionStatus.Converted)?.outputFile
        }
        return engine.buildShareIntent(files)
    }

    fun buildShareSingleIntent(file: File): Intent? = engine.buildShareIntent(listOf(file))

    fun refreshHistory() {
        viewModelScope.launch {
            val files = engine.loadHistoryFiles()
            _uiState.update { it.copy(historyFiles = files) }
        }
    }

    fun deleteHistoryFile(file: File) {
        viewModelScope.launch {
            engine.deleteHistoryFile(file)
            refreshHistory()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            engine.clearHistoryFiles()
            refreshHistory()
        }
    }
}
