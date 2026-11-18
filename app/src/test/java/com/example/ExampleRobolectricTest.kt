package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.CompressionMode
import com.example.data.ConversionSettings
import com.example.data.ImageConverterEngine
import com.example.data.InputImageFormat
import com.example.data.ItemConversionStatus
import com.example.data.ResizePreset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WebP Studio", appName)
    }

    @Test
    fun `inspect and convert PNG and JPEG to WebP`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = ImageConverterEngine(context)

        val pngFile = File(context.cacheDir, "sample_test.png")
        val jpgFile = File(context.cacheDir, "sample_test.jpg")

        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.parseColor("#E59235"))
        }
        FileOutputStream(pngFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileOutputStream(jpgFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bitmap.recycle()

        val inspected = engine.inspectUris(listOf(Uri.fromFile(pngFile), Uri.fromFile(jpgFile)))
        assertEquals(2, inspected.size)
        assertEquals(InputImageFormat.PNG, inspected[0].sourceFormat)
        assertEquals(InputImageFormat.JPEG, inspected[1].sourceFormat)

        val settings = ConversionSettings(
            mode = CompressionMode.LOSSY,
            quality = 80,
            resizePreset = ResizePreset.ORIGINAL,
        )

        val pngResult = engine.convertToWebp(inspected[0], settings)
        assertTrue(pngResult is ItemConversionStatus.Converted)
        val convertedPng = pngResult as ItemConversionStatus.Converted
        assertTrue(convertedPng.outputFile.exists())
        assertTrue(convertedPng.outputFileName.endsWith(".webp"))
        assertEquals(120, convertedPng.outputWidth)
        assertEquals(80, convertedPng.outputHeight)

        val jpgResult = engine.convertToWebp(inspected[1], settings)
        assertTrue(jpgResult is ItemConversionStatus.Converted)
        val convertedJpg = jpgResult as ItemConversionStatus.Converted
        assertTrue(convertedJpg.outputFile.exists())
        assertTrue(convertedJpg.outputFileName.endsWith(".webp"))

        pngFile.delete()
        jpgFile.delete()
        convertedPng.outputFile.delete()
        convertedJpg.outputFile.delete()
    }

    @Test
    fun `loadBundledSamples finds and inspects bundled sample image`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = ImageConverterEngine(context)
        val samples = engine.loadBundledSamples()
        assertTrue("Expected at least 1 bundled sample image", samples.isNotEmpty())
        assertEquals("015.png", samples.first().displayName)
    }
}
