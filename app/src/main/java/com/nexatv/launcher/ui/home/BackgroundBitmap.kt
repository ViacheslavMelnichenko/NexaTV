// Copyright 2026 Viacheslav Melnichenko. Licensed under the Apache License, Version 2.0.
package com.nexatv.launcher.ui.home

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.nexatv.launcher.data.BackgroundBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decode off-main, at bounded resolution. Blur is baked once, never a per-frame GPU effect. */
internal suspend fun loadBackground(assets: AssetManager, path: String, blur: BackgroundBlur): ImageBitmap =
    withContext(Dispatchers.Default) {
        val source = ImageDecoder.createSource(assets, path)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = minOf(info.size.width, if (blur == BackgroundBlur.NONE) 1920 else 480)
            decoder.setTargetSize(width, (info.size.height.toLong() * width / info.size.width).toInt())
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        if (blur == BackgroundBlur.NONE) {
            bitmap.asImageBitmap()
        } else {
            val result = blurBitmap(bitmap, if (blur == BackgroundBlur.SOFT) 2 else 6)
            bitmap.recycle()
            result.asImageBitmap()
        }
    }

/** Two separable box passes on a small image; memory and cost do not depend on the TV's 4K surface. */
private fun blurBitmap(bitmap: Bitmap, radius: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    val scratch = IntArray(pixels.size)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    repeat(2) {
        blurPass(pixels, scratch, width, height, radius)
        blurPass(scratch, pixels, height, width, radius)
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

/** Sliding box average, transposed on output so the same bounded pass handles both axes. */
private fun blurPass(input: IntArray, output: IntArray, width: Int, height: Int, radius: Int) {
    val count = radius * 2 + 1
    for (y in 0 until height) {
        var red = 0
        var green = 0
        var blue = 0
        for (offset in -radius..radius) {
            val color = input[y * width + offset.coerceIn(0, width - 1)]
            red += color shr 16 and 255
            green += color shr 8 and 255
            blue += color and 255
        }
        for (x in 0 until width) {
            output[x * height + y] =
                (255 shl 24) or ((red / count) shl 16) or ((green / count) shl 8) or (blue / count)
            val outgoing = input[y * width + (x - radius).coerceIn(0, width - 1)]
            val incoming = input[y * width + (x + radius + 1).coerceIn(0, width - 1)]
            red += (incoming shr 16 and 255) - (outgoing shr 16 and 255)
            green += (incoming shr 8 and 255) - (outgoing shr 8 and 255)
            blue += (incoming and 255) - (outgoing and 255)
        }
    }
}
