package com.example.app

import android.content.Context
import android.graphics.Bitmap
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

object BitmapBlurUtil {
    fun blur(context: Context, src: Bitmap, radius: Float = 25f): Bitmap {
        val bitmap = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val blurred = gaussianBlur(pixels, w, h, radius)
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(blurred, 0, w, 0, 0, w, h)
        return result
    }

    private fun gaussianBlur(pixels: IntArray, width: Int, height: Int, radius: Float): IntArray {
        val blurred = IntArray(pixels.size)
        val kernelSize = radius.toInt() * 2 + 1
        val kernel = FloatArray(kernelSize)
        val sigma = radius / 3
        var sum = 0f

        for (i in 0 until kernelSize) {
            val x = i - radius.toInt()
            kernel[i] = exp(-(x * x) / (2 * sigma * sigma)).toFloat()
            sum += kernel[i]
        }

        for (i in kernel.indices) {
            kernel[i] /= sum
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0f
                var g = 0f
                var b = 0f

                for (i in 0 until kernelSize) {
                    val px = min(width - 1, max(0, x + i - radius.toInt()))
                    val color = pixels[y * width + px]
                    r += ((color shr 16 and 0xFF) * kernel[i])
                    g += ((color shr 8 and 0xFF) * kernel[i])
                    b += ((color and 0xFF) * kernel[i])
                }

                blurred[y * width + x] =
                    (0xFF shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
            }
        }

        return blurred
    }
}
