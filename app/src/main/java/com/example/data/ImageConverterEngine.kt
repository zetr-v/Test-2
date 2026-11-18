package com.example.data

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque
import java.util.Locale
import java.util.UUID

class ImageConverterEngine(private val context: Context) {

    private val convertedDir: File
        get() = File(context.filesDir, "converted").apply { mkdirs() }

    private val pastedCacheDir: File
        get() = File(context.cacheDir, "pasted").apply { mkdirs() }

    suspend fun inspectUris(uris: List<Uri>): List<ImageConversionItem> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri -> inspectSingleUri(uri) }
    }

    suspend fun scanFolderTree(treeUri: Uri, maxFiles: Int = 200): List<ImageConversionItem> =
        withContext(Dispatchers.IO) {
            val discoveredUris = mutableListOf<Uri>()
            val queue = ArrayDeque<Pair<String, Int>>()
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            queue.add(rootDocId to 0)

            while (queue.isNotEmpty() && discoveredUris.size < maxFiles) {
                val (docId, depth) = queue.removeFirst()
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                )
                runCatching {
                    context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                        val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        if (idIdx < 0) return@use
                        while (cursor.moveToNext() && discoveredUris.size < maxFiles) {
                            val childId = cursor.getString(idIdx) ?: continue
                            val name = if (nameIdx >= 0 && !cursor.isNull(nameIdx)) cursor.getString(nameIdx) ?: "" else ""
                            val mime = if (mimeIdx >= 0 && !cursor.isNull(mimeIdx)) cursor.getString(mimeIdx) ?: "" else ""
                            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                                if (depth < 3) {
                                    queue.add(childId to (depth + 1))
                                }
                            } else if (isSupportedMimeOrName(mime, name) || mime.isEmpty()) {
                                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                                discoveredUris.add(docUri)
                            }
                        }
                    }
                }
            }
            discoveredUris.mapNotNull { inspectSingleUri(it) }
        }

    suspend fun inspectFromClipboard(): List<ImageConversionItem> = withContext(Dispatchers.IO) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return@withContext emptyList()
        val clip = clipboard.primaryClip ?: return@withContext emptyList()
        val results = mutableListOf<ImageConversionItem>()

        for (i in 0 until clip.itemCount) {
            val uri = clip.getItemAt(i).uri ?: continue
            // Copy to local cache immediately so transient clipboard URI grants don't expire
            val copiedUri = copyUriToPastedCache(uri) ?: continue
            inspectSingleUri(copiedUri)?.let { results.add(it) }
        }
        results
    }

    suspend fun loadBundledSamples(): List<ImageConversionItem> = withContext(Dispatchers.IO) {
        val sampleNames = runCatching { context.assets.list("samples")?.toList().orEmpty() }
            .getOrDefault(emptyList())
            .filter { isSupportedMimeOrName("", it) }
        if (sampleNames.isEmpty()) return@withContext emptyList()

        val samplesCacheDir = File(context.cacheDir, "samples").apply { mkdirs() }
        val results = mutableListOf<ImageConversionItem>()

        for (name in sampleNames) {
            runCatching {
                val outFile = File(samplesCacheDir, name)
                if (!outFile.exists() || outFile.length() == 0L) {
                    context.assets.open("samples/$name").use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                inspectSingleUri(Uri.fromFile(outFile))?.let { results.add(it) }
            }
        }
        results
    }

    private fun copyUriToPastedCache(sourceUri: Uri): Uri? {
        return runCatching {
            val mime = context.contentResolver.getType(sourceUri)
            val ext = when {
                mime?.contains("png", ignoreCase = true) == true -> "png"
                mime?.contains("jpeg", ignoreCase = true) == true ||
                    mime?.contains("jpg", ignoreCase = true) == true -> "jpg"
                else -> "img"
            }
            val file = File(pastedCacheDir, "clipboard_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(file)
        }.getOrNull()
    }

    private fun inspectSingleUri(uri: Uri): ImageConversionItem? {
        return runCatching {
            val resolver = context.contentResolver
            val mimeFromResolver = if (uri.scheme == "file") null else resolver.getType(uri)
            val (displayName, reportedSize) = queryNameAndSize(uri)

            // Decode bounds & actual MIME from stream header
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val inputStream = openStreamForUri(uri) ?: return null
            inputStream.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }

            val detectedMime = bounds.outMimeType ?: mimeFromResolver ?: ""
            val format = resolveInputFormat(detectedMime, displayName) ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val actualSize = if (reportedSize > 0L) {
                reportedSize
            } else {
                measureUriByteSize(uri)
            }

            ImageConversionItem(
                sourceUri = uri,
                displayName = displayName.ifBlank {
                    "image_${UUID.randomUUID().toString().take(6)}.${format.badge.lowercase(Locale.US)}"
                },
                sourceFormat = format,
                originalSizeBytes = actualSize,
                originalWidth = bounds.outWidth,
                originalHeight = bounds.outHeight,
            )
        }.getOrNull()
    }

    suspend fun convertToWebp(
        item: ImageConversionItem,
        settings: ConversionSettings,
    ): ItemConversionStatus = withContext(Dispatchers.Default) {
        runCatching {
            val orientation = readExifOrientation(item.sourceUri)
            val maxDim = settings.resizePreset.maxDimension

            val sampleSize = if (maxDim != null) {
                calculateInSampleSize(item.originalWidth, item.originalHeight, maxDim, maxDim)
            } else {
                1
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = openStreamForUri(item.sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: error("Could not decode image")

            val orientedBitmap = applyExifOrientation(decoded, orientation)
            if (orientedBitmap !== decoded) {
                decoded.recycle()
            }

            val finalBitmap = if (maxDim != null) {
                scaleDownIfNeeded(orientedBitmap, maxDim)
            } else {
                orientedBitmap
            }
            if (finalBitmap !== orientedBitmap) {
                orientedBitmap.recycle()
            }

            val outWidth = finalBitmap.width
            val outHeight = finalBitmap.height
            val baseName = sanitizeBaseName(item.displayName)
            val outputFileName = "$baseName.webp"
            val outputFile = uniqueOutputFile(baseName)

            val compressFormat = resolveWebpCompressFormat(settings.mode)
            val compressQuality = if (settings.mode == CompressionMode.LOSSLESS) 100 else settings.quality.coerceIn(1, 100)

            FileOutputStream(outputFile).use { fos ->
                val ok = finalBitmap.compress(compressFormat, compressQuality, fos)
                if (!ok) {
                    error("WebP encoding failed")
                }
            }
            finalBitmap.recycle()

            val autoSaved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveFileToMediaStore(outputFile, outputFile.name)
            } else {
                false
            }

            ItemConversionStatus.Converted(
                outputFile = outputFile,
                outputFileName = outputFileName,
                outputSizeBytes = outputFile.length(),
                outputWidth = outWidth,
                outputHeight = outHeight,
                modeUsed = settings.mode,
                qualityUsed = compressQuality,
                savedToGallery = autoSaved,
            )
        }.getOrElse { throwable ->
            ItemConversionStatus.Failed(throwable.localizedMessage ?: "Conversion failed")
        }
    }

    suspend fun saveItemsToGallery(items: List<ImageConversionItem>): Int = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext 0
        }
        var savedCount = 0
        for (item in items) {
            val status = item.status as? ItemConversionStatus.Converted ?: continue
            if (status.savedToGallery || !status.outputFile.exists()) continue
            val ok = saveFileToMediaStore(status.outputFile, status.outputFileName)
            if (ok) savedCount++
        }
        savedCount
    }

    suspend fun saveFileToGallery(file: File, displayName: String): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !file.exists()) {
            return@withContext false
        }
        saveFileToMediaStore(file, displayName)
    }

    private fun saveFileToMediaStore(file: File, displayName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/WebP Studio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: return false
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        }.getOrDefault(false)
    }

    suspend fun exportFilesToFolder(treeUri: Uri, files: List<Pair<File, String>>): Int =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri),
            )
            var exported = 0
            for ((file, desiredName) in files) {
                if (!file.exists()) continue
                runCatching {
                    val createdUri = DocumentsContract.createDocument(
                        resolver,
                        parentDocUri,
                        "image/webp",
                        desiredName,
                    ) ?: return@runCatching
                    resolver.openOutputStream(createdUri)?.use { out ->
                        file.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    exported++
                }
            }
            exported
        }

    suspend fun writeFileToUri(sourceFile: File, destUri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: return@withContext false
            true
        }.getOrDefault(false)
    }

    suspend fun loadHistoryFiles(): List<HistoryWebpFile> = withContext(Dispatchers.IO) {
        val files = convertedDir.listFiles { file ->
            file.isFile && file.extension.equals("webp", ignoreCase = true)
        } ?: emptyArray()

        files.sortedByDescending { it.lastModified() }.map { file ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            HistoryWebpFile(
                file = file,
                fileName = file.name,
                sizeBytes = file.length(),
                width = bounds.outWidth.coerceAtLeast(0),
                height = bounds.outHeight.coerceAtLeast(0),
                lastModifiedMillis = file.lastModified(),
            )
        }
    }

    suspend fun deleteHistoryFile(file: File) = withContext(Dispatchers.IO) {
        runCatching { file.delete() }
    }

    suspend fun clearHistoryFiles() = withContext(Dispatchers.IO) {
        convertedDir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    fun buildShareIntent(files: List<File>): Intent? {
        val validFiles = files.filter { it.exists() }
        if (validFiles.isEmpty()) return null
        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList(validFiles.map { FileProvider.getUriForFile(context, authority, it) })

        return if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/webp"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/webp"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    private fun openStreamForUri(uri: Uri) = if (uri.scheme == "file") {
        uri.path?.let { File(it).inputStream() }
    } else {
        context.contentResolver.openInputStream(uri)
    }

    private fun queryNameAndSize(uri: Uri): Pair<String, Long> {
        if (uri.scheme == "file") {
            val f = uri.path?.let { File(it) }
            return (f?.name ?: "image") to (f?.length() ?: 0L)
        }
        var name = ""
        var size = 0L
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: ""
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }
        }
        return name to size
    }

    private fun measureUriByteSize(uri: Uri): Long {
        return runCatching {
            openStreamForUri(uri)?.use { input ->
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                }
                total
            } ?: 0L
        }.getOrDefault(0L)
    }

    private fun readExifOrientation(uri: Uri): Int {
        return runCatching {
            openStreamForUri(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / maxSide.toFloat()
        val targetWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun resolveWebpCompressFormat(mode: CompressionMode): Bitmap.CompressFormat {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (mode) {
                CompressionMode.LOSSY -> Bitmap.CompressFormat.WEBP_LOSSY
                CompressionMode.LOSSLESS -> Bitmap.CompressFormat.WEBP_LOSSLESS
            }
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
    }

    private fun isSupportedMimeOrName(mime: String, fileName: String): Boolean {
        val lowerMime = mime.lowercase(Locale.US)
        if (lowerMime == "image/png" || lowerMime == "image/jpeg" || lowerMime == "image/jpg") {
            return true
        }
        val lowerName = fileName.lowercase(Locale.US)
        return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
    }

    private fun resolveInputFormat(mime: String, fileName: String): InputImageFormat? {
        val lowerMime = mime.lowercase(Locale.US)
        when (lowerMime) {
            "image/png" -> return InputImageFormat.PNG
            "image/jpeg", "image/jpg" -> return InputImageFormat.JPEG
        }
        val lowerName = fileName.lowercase(Locale.US)
        return when {
            lowerName.endsWith(".png") -> InputImageFormat.PNG
            lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") -> InputImageFormat.JPEG
            else -> null
        }
    }

    private fun sanitizeBaseName(displayName: String): String {
        val stripped = displayName
            .replace(Regex("\\.(png|jpe?g)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .trim('_')
        return stripped.ifBlank { "converted_image" }
    }

    private fun uniqueOutputFile(baseName: String): File {
        var candidate = File(convertedDir, "$baseName.webp")
        if (!candidate.exists()) return candidate
        var counter = 1
        while (candidate.exists()) {
            candidate = File(convertedDir, "${baseName}_$counter.webp")
            counter++
        }
        return candidate
    }
}
