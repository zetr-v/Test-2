package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CompressionMode
import com.example.data.ConversionSettings

@Composable
fun HomeScreen(
    settings: ConversionSettings,
    queuedCount: Int,
    historyCount: Int,
    isScanningInput: Boolean,
    onBrowseImages: () -> Unit,
    onBrowseFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onPasteClipboard: () -> Unit,
    onLoadSample: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Minimal top header bar matching screenshot layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_top_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.testTag("home_history_button"),
                ) {
                    if (historyCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    Text(
                                        text = if (historyCount > 99) "99+" else historyCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = stringResource(R.string.cd_history),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = stringResource(R.string.cd_history),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("home_settings_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Hero icon + title + subtitle
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = stringResource(R.string.cd_hero_icon),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.home_hero_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.home_hero_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Dashed upload / picker dropzone card
        val dashColor = MaterialTheme.colorScheme.outline
        val cardShape = RoundedCornerShape(22.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .heightIn(min = 290.dp)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surface)
                .drawBehind {
                    val strokeWidth = 1.6.dp.toPx()
                    val cornerPx = 22.dp.toPx()
                    drawRoundRect(
                        color = dashColor,
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(10.dp.toPx(), 8.dp.toPx()),
                                0f,
                            ),
                        ),
                    )
                }
                .clickable(enabled = !isScanningInput, onClick = onBrowseImages)
                .padding(horizontal = 24.dp, vertical = 34.dp)
                .testTag("home_dropzone_card"),
            contentAlignment = Alignment.Center,
        ) {
            if (isScanningInput) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Reading PNG & JPEG images…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Elevated gallery+ icon container
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.cd_add_images),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(30.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.home_drop_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Underlined inline actions matching the reference image
                    val linkStyle = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline,
                        ),
                    )
                    val orPrefix = stringResource(R.string.home_or_prefix)
                    val browseLabel = stringResource(R.string.home_action_browse)
                    val filesLabel = stringResource(R.string.home_action_files)
                    val commaSep = stringResource(R.string.home_comma_sep)
                    val folderLabel = stringResource(R.string.home_action_folder)
                    val orPaste = stringResource(R.string.home_or_paste)
                    val pasteLabel = stringResource(R.string.home_action_paste)

                    val actionAnnotated = buildAnnotatedString {
                        append(orPrefix)
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = "browse",
                                styles = linkStyle,
                                linkInteractionListener = { onBrowseImages() },
                            ),
                        ) {
                            append(browseLabel)
                        }
                        append(commaSep)
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = "files",
                                styles = linkStyle,
                                linkInteractionListener = { onBrowseFiles() },
                            ),
                        ) {
                            append(filesLabel)
                        }
                        append(commaSep)
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = "folder",
                                styles = linkStyle,
                                linkInteractionListener = { onPickFolder() },
                            ),
                        ) {
                            append(folderLabel)
                        }
                        append(orPaste)
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = "paste",
                                styles = linkStyle,
                                linkInteractionListener = { onPasteClipboard() },
                            ),
                        ) {
                            append(pasteLabel)
                        }
                    }

                    Text(
                        text = actionAnnotated,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.widthIn(max = 310.dp),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Format pills: [ PNG ] [ JPEG ] -> [ WEBP ]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FormatPill(text = stringResource(R.string.badge_png), highlighted = false)
                        FormatPill(text = stringResource(R.string.badge_jpeg), highlighted = false)
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        FormatPill(text = stringResource(R.string.badge_webp), highlighted = true)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Little adjustment: compact preset chip showing active WebP output settings
        val modeSummary = if (settings.mode == CompressionMode.LOSSY) {
            "${settings.mode.label} ${settings.quality}%"
        } else {
            settings.mode.label
        }

        Surface(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .testTag("home_preset_pill"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(
                        R.string.home_preset_summary,
                        modeSummary,
                        settings.resizePreset.label,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        androidx.compose.material3.TextButton(
            onClick = onLoadSample,
            enabled = !isScanningInput,
            modifier = Modifier.testTag("home_load_sample_button"),
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.home_load_sample),
                style = MaterialTheme.typography.labelMedium,
            )
        }

        if (queuedCount > 0) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onOpenQueue,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("home_open_queue_button"),
            ) {
                Text(stringResource(R.string.home_view_queue, queuedCount))
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FormatPill(
    text: String,
    highlighted: Boolean,
) {
    val bgColor = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val borderColor = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val textColor = if (highlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
