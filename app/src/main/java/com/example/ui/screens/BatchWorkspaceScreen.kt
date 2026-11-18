package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CompressionMode
import com.example.data.ConversionSettings
import com.example.data.ImageConversionItem
import com.example.data.ItemConversionStatus
import com.example.data.formatFileSize
import com.example.data.formatSavingsPercent
import kotlin.math.roundToInt

@Composable
fun BatchWorkspaceScreen(
    items: List<ImageConversionItem>,
    settings: ConversionSettings,
    isConverting: Boolean,
    conversionCurrent: Int,
    conversionTotal: Int,
    onBack: () -> Unit,
    onAddImages: () -> Unit,
    onBrowseFiles: () -> Unit,
    onAddFolder: () -> Unit,
    onPasteClipboard: () -> Unit,
    onAddSample: () -> Unit,
    onClearQueue: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onPreviewItem: (ImageConversionItem) -> Unit,
    onShareSingle: (ItemConversionStatus.Converted) -> Unit,
    onSaveSingle: (ItemConversionStatus.Converted) -> Unit,
    onModeChange: (CompressionMode) -> Unit,
    onQualityChange: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onConvertAll: (reconvert: Boolean) -> Unit,
    onSaveAllToGallery: () -> Unit,
    onExportAllToFolder: () -> Unit,
    onShareAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var showAddMenu by remember { mutableStateOf(false) }

    val queuedCount = items.count { it.status !is ItemConversionStatus.Converted }
    val convertedItems = items.mapNotNull { item ->
        (item.status as? ItemConversionStatus.Converted)?.let { item to it }
    }
    val allConverted = items.isNotEmpty() && convertedItems.size == items.size

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("workspace_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
                Text(
                    text = stringResource(R.string.workspace_title, items.size),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton(
                        onClick = { showAddMenu = true },
                        enabled = !isConverting,
                        modifier = Modifier.testTag("workspace_add_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.workspace_add_more))
                    }
                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workspace_add_images)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                            },
                            onClick = {
                                showAddMenu = false
                                onAddImages()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Browse files") },
                            leadingIcon = {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                            },
                            onClick = {
                                showAddMenu = false
                                onBrowseFiles()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workspace_add_folder)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                            },
                            onClick = {
                                showAddMenu = false
                                onAddFolder()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workspace_paste_image)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                            },
                            onClick = {
                                showAddMenu = false
                                onPasteClipboard()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workspace_add_sample)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                            },
                            onClick = {
                                showAddMenu = false
                                onAddSample()
                            },
                        )
                    }
                }

                IconButton(
                    onClick = onClearQueue,
                    enabled = !isConverting,
                    modifier = Modifier.testTag("workspace_clear_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = stringResource(R.string.workspace_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Batch savings summary banner when items are converted
        AnimatedVisibility(visible = convertedItems.isNotEmpty()) {
            val totalOrig = convertedItems.sumOf { it.first.originalSizeBytes }
            val totalOut = convertedItems.sumOf { it.second.outputSizeBytes }
            val savedBytes = (totalOrig - totalOut).coerceAtLeast(0L)
            val savedPercent = formatSavingsPercent(totalOrig, totalOut)

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(
                                R.string.workspace_summary_saved,
                                formatFileSize(savedBytes),
                                savedPercent,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = "${formatFileSize(totalOrig)} → ${formatFileSize(totalOut)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        // Progress bar during conversion
        AnimatedVisibility(visible = isConverting) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val progress = if (conversionTotal > 0) {
                    conversionCurrent.toFloat() / conversionTotal.toFloat()
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                )
                Text(
                    text = stringResource(
                        R.string.workspace_converting,
                        conversionCurrent,
                        conversionTotal,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Image Queue List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.id }) { item ->
                ConversionQueueCard(
                    item = item,
                    isConvertingBatch = isConverting,
                    onClick = { onPreviewItem(item) },
                    onRemove = { onRemoveItem(item.id) },
                    onShare = { converted -> onShareSingle(converted) },
                    onSave = { converted -> onSaveSingle(converted) },
                )
            }
        }

        // Bottom Conversion Controls & Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Quick Compression Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompressionMode.entries.forEach { mode ->
                            val selected = settings.mode == mode
                            Surface(
                                onClick = { if (!isConverting) onModeChange(mode) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                ),
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = settings.resizePreset.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(
                            onClick = onOpenSettings,
                            enabled = !isConverting,
                            modifier = Modifier.testTag("workspace_settings_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = stringResource(R.string.cd_settings),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Inline Quality Slider for Lossy mode
                if (settings.mode == CompressionMode.LOSSY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Quality",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = settings.quality.toFloat(),
                            onValueChange = { onQualityChange(it.roundToInt()) },
                            valueRange = 10f..100f,
                            enabled = !isConverting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("workspace_quality_slider"),
                        )
                        Text(
                            text = "${settings.quality}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Primary Action Row
                if (!allConverted) {
                    Button(
                        onClick = { onConvertAll(false) },
                        enabled = !isConverting && queuedCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("convert_all_button"),
                    ) {
                        Text(
                            text = if (isConverting) {
                                stringResource(
                                    R.string.workspace_converting,
                                    conversionCurrent,
                                    conversionTotal,
                                )
                            } else {
                                stringResource(R.string.workspace_convert_all, queuedCount)
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                } else {
                    val allSavedToGallery = convertedItems.all { it.second.savedToGallery }

                    if (allSavedToGallery) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.workspace_saved_auto),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!allSavedToGallery) {
                            Button(
                                onClick = onSaveAllToGallery,
                                modifier = Modifier
                                    .weight(1.35f)
                                    .height(48.dp)
                                    .testTag("save_all_gallery_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SaveAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.workspace_save_gallery),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            OutlinedButton(
                                onClick = onExportAllToFolder,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_folder_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Folder",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            IconButton(
                                onClick = onShareAll,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("share_all_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = stringResource(R.string.workspace_share_all),
                                )
                            }
                        } else {
                            Button(
                                onClick = onShareAll,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("share_all_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.workspace_share_all))
                            }

                            OutlinedButton(
                                onClick = onExportAllToFolder,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_folder_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Folder",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { onConvertAll(true) },
                        enabled = !isConverting,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("reconvert_all_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.workspace_reconvert))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversionQueueCard(
    item: ImageConversionItem,
    isConvertingBatch: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onShare: (ItemConversionStatus.Converted) -> Unit,
    onSave: (ItemConversionStatus.Converted) -> Unit,
) {
    val converted = item.status as? ItemConversionStatus.Converted
    val thumbModel: Any = converted?.outputFile ?: item.sourceUri

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("queue_item_card_${item.id}"),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = thumbModel,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = converted?.outputFileName ?: item.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = item.sourceFormat.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    if (converted != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        )
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "WEBP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Text(
                        text = "${item.originalWidth}×${item.originalHeight}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                when (val status = item.status) {
                    ItemConversionStatus.Queued -> {
                        Text(
                            text = formatFileSize(item.originalSizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ItemConversionStatus.Converting -> {
                        Text(
                            text = "Converting to WebP…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    is ItemConversionStatus.Converted -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "${formatFileSize(item.originalSizeBytes)} → ${formatFileSize(status.outputSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatSavingsPercent(
                                    item.originalSizeBytes,
                                    status.outputSizeBytes,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    is ItemConversionStatus.Failed -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = status.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            when (val status = item.status) {
                ItemConversionStatus.Converting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                    )
                }
                is ItemConversionStatus.Converted -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onShare(status) }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.cd_share_item),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(onClick = { onSave(status) }) {
                            Icon(
                                imageVector = if (status.savedToGallery) {
                                    Icons.Outlined.CheckCircle
                                } else {
                                    Icons.Outlined.SaveAlt
                                },
                                contentDescription = stringResource(R.string.cd_save_item),
                                tint = if (status.savedToGallery) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                else -> {
                    IconButton(
                        onClick = onRemove,
                        enabled = !isConvertingBatch,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.cd_remove_item),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
