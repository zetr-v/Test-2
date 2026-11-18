package com.example.data

import android.net.Uri
import java.io.File
import java.util.UUID

enum class InputImageFormat(val badge: String) {
    PNG("PNG"),
    JPEG("JPG"),
}

enum class CompressionMode(val label: String) {
    LOSSY("Lossy"),
    LOSSLESS("Lossless"),
}

enum class ResizePreset(val maxDimension: Int?, val label: String) {
    ORIGINAL(null, "Original"),
    PX_2048(2048, "2048px"),
    PX_1080(1080, "1080px"),
    PX_720(720, "720px"),
}

data class ConversionSettings(
    val mode: CompressionMode = CompressionMode.LOSSY,
    val quality: Int = 85,
    val resizePreset: ResizePreset = ResizePreset.ORIGINAL,
)

sealed interface ItemConversionStatus {
    data object Queued : ItemConversionStatus
    data object Converting : ItemConversionStatus
    data class Converted(
        val outputFile: File,
        val outputFileName: String,
        val outputSizeBytes: Long,
        val outputWidth: Int,
        val outputHeight: Int,
        val modeUsed: CompressionMode,
        val qualityUsed: Int,
        val savedToGallery: Boolean = false,
    ) : ItemConversionStatus

    data class Failed(val message: String) : ItemConversionStatus
}

data class ImageConversionItem(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: Uri,
    val displayName: String,
    val sourceFormat: InputImageFormat,
    val originalSizeBytes: Long,
    val originalWidth: Int,
    val originalHeight: Int,
    val status: ItemConversionStatus = ItemConversionStatus.Queued,
)

data class HistoryWebpFile(
    val file: File,
    val fileName: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val lastModifiedMillis: Long,
)

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) {
        return if (kb < 10.0) String.format("%.1f KB", kb) else String.format("%.0f KB", kb)
    }
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}

fun formatSavingsPercent(originalBytes: Long, outputBytes: Long): String {
    if (originalBytes <= 0L) return "0%"
    val diffPercent = ((originalBytes - outputBytes) * 100.0 / originalBytes)
    val rounded = kotlin.math.round(diffPercent).toInt()
    return when {
        rounded > 0 -> "-$rounded%"
        rounded < 0 -> "+${kotlin.math.abs(rounded)}%"
        else -> "0%"
    }
}
