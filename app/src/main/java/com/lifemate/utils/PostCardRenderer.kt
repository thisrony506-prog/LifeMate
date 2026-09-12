package com.lifemate.utils

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import java.io.File
import java.util.UUID

/** One local photo, composited with editable text. No network, account, or posting SDK. */
data class PostDesign(
    val text: String = "Make today a little brighter.", val palette: String = "Rose", val font: String = "Modern",
    val effect: String = "Original", val format: String = "Portrait", val position: String = "Center",
    val ink: String = "White", val size: Float = 58f, val shade: Float = .35f, val zoom: Float = 1f,
    val rotation: Int = 0, val alignment: String = "Center", val photo: String = ""
)
object PostCardRenderer {
    val palettes = listOf("Rose", "Lavender", "Ocean", "Jade", "Midnight")
    val fonts = listOf("Modern", "Serif", "Handwritten", "Bold", "Mono")
    val effects = listOf("Original", "Warm", "Mono", "Dreamy")
    val formats = listOf("Portrait", "Square", "Landscape")
    val inks = listOf("White", "Cream", "Ink", "Pink", "Gold")
    fun dimensions(format: String) = when (format) { "Square" -> 1080 to 1080; "Landscape" -> 1200 to 630; else -> 1080 to 1350 }

    /** Bounded input/decode, EXIF orientation, and stripped metadata in the private normalized copy. */
    fun importPhoto(context: Context, uri: Uri): File {
        val dir = File(context.filesDir, "studio").apply { mkdirs() }
        val raw = File(dir, "input-${UUID.randomUUID()}")
        var decoded: Bitmap? = null
        var oriented: Bitmap? = null
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "This photo could not be opened." }
                raw.outputStream().use { out ->
                    val buffer = ByteArray(8192); var total = 0
                    while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= 25 * 1024 * 1024) { "Choose a photo smaller than 25 MB." }; out.write(buffer, 0, n) }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(raw.path, bounds)
            require(bounds.outWidth in 1..100_000 && bounds.outHeight in 1..100_000) { "Choose a supported photo (JPEG, PNG or WebP)." }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
            val source = requireNotNull(BitmapFactory.decodeFile(raw.path, BitmapFactory.Options().apply { inSampleSize = sample })) { "Unable to decode this photo." }
            decoded = source
            val orientation = runCatching { ExifInterface(raw.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
            val matrix = Matrix().apply { when (orientation) {
                2 -> setScale(-1f, 1f); 3 -> setRotate(180f); 4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }; 6 -> setRotate(90f)
                7 -> { setRotate(-90f); postScale(-1f, 1f) }; 8 -> setRotate(-90f)
            } }
            val normalized = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            oriented = normalized
            val file = File(dir, "photo-${UUID.randomUUID()}.jpg")
            try { file.outputStream().use { check(normalized.compress(Bitmap.CompressFormat.JPEG, 92, it)) }; return file }
            catch (e: Exception) { file.delete(); throw e }
        } finally { raw.delete(); if (oriented !== decoded) oriented?.recycle(); decoded?.recycle() }
    }
    fun render(context: Context, design: PostDesign): File {
        val (w, h) = dimensions(design.format)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        try {
            val colors = when (design.palette) {
                "Lavender" -> intArrayOf(0xFF8A60BA.toInt(), 0xFF332559.toInt())
                "Ocean" -> intArrayOf(0xFF419AA9.toInt(), 0xFF123858.toInt())
                "Jade" -> intArrayOf(0xFF4A9375.toInt(), 0xFF173D33.toInt())
                "Midnight" -> intArrayOf(0xFF37466E.toInt(), 0xFF10182C.toInt())
                else -> intArrayOf(0xFFDC729D.toInt(), 0xFF7C244F.toInt())
            }
            paint.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors[0], colors[1], Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint); paint.shader = null
            val photo = if (design.photo.isNotBlank()) BitmapFactory.decodeFile(design.photo) else null
            if (design.photo.isNotBlank()) requireNotNull(photo) { "Photo is unavailable. Choose it again." }
            photo?.let {
                try {
                    val rotate = Matrix().apply { postRotate((design.rotation % 4) * 90f) }
                    val rotated = Bitmap.createBitmap(it, 0, 0, it.width, it.height, rotate, true)
                    try {
                        val scale = maxOf(w.toFloat() / rotated.width, h.toFloat() / rotated.height) * design.zoom.coerceIn(1f, 2.5f)
                        val dw = rotated.width * scale; val dh = rotated.height * scale
                        paint.colorFilter = when (design.effect) {
                            "Mono" -> ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                            "Warm" -> ColorMatrixColorFilter(floatArrayOf(1.08f,0f,0f,0f,8f, 0f,1f,0f,0f,2f, 0f,0f,.88f,0f,0f, 0f,0f,0f,1f,0f))
                            "Dreamy" -> ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(.55f) })
                            else -> null
                        }
                        canvas.drawBitmap(rotated, null, RectF((w-dw)/2, (h-dh)/2, (w+dw)/2, (h+dh)/2), paint)
                        paint.colorFilter = null
                    } finally { if (rotated !== it) rotated.recycle() }
                } finally { it.recycle() }
                canvas.drawColor(Color.argb((design.shade.coerceIn(0f, .8f)*255).toInt(), 0, 0, 0))
            } ?: run {
                paint.color = Color.argb(24,255,255,255)
                canvas.drawCircle(w*.9f,h*.1f,w*.38f,paint); canvas.drawCircle(w*.1f,h*.9f,w*.3f,paint)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; paint.color = Color.argb(100,255,255,255)
                canvas.drawRoundRect(40f,40f,w-40f,h-40f,28f,28f,paint); paint.style = Paint.Style.FILL
                if (design.palette == "Rose") {
                    paint.color = Color.argb(95,255,220,235)
                    repeat(18) { i -> canvas.drawCircle(65f + (i*173 % (w-130)), 65f + (i*239 % (h-130)), (4+i%4).toFloat(), paint) }
                }
            }
            val fontName = when(design.font) { "Serif" -> "serif"; "Handwritten" -> "cursive"; "Mono" -> "monospace"; else -> "sans-serif" }
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when(design.ink) { "Cream" -> 0xFFFFF2DB.toInt(); "Ink" -> 0xFF19243A.toInt(); "Pink" -> 0xFFFFBBDD.toInt(); "Gold" -> 0xFFFFDF88.toInt(); else -> Color.WHITE }
                textSize = design.size.coerceIn(28f, 90f)
                typeface = Typeface.create(fontName, if (design.font == "Bold") Typeface.BOLD else Typeface.NORMAL)
                setShadowLayer(3f,0f,2f,Color.argb(90,0,0,0))
            }
            val text = design.text.take(600)
            val maxHeight = h - 180
            var layout: StaticLayout
            do {
                layout = StaticLayout.Builder.obtain(text,0,text.length,textPaint,w-160)
                    .setAlignment(when(design.alignment) { "Left" -> Layout.Alignment.ALIGN_NORMAL; "Right" -> Layout.Alignment.ALIGN_OPPOSITE; else -> Layout.Alignment.ALIGN_CENTER })
                    .setIncludePad(false).setLineSpacing(8f,1f).build()
                if (layout.height <= maxHeight || textPaint.textSize <= 28f) break
                textPaint.textSize -= 2f
            } while (true)
            if (layout.height > maxHeight) layout = StaticLayout.Builder.obtain(text,0,text.length,textPaint,w-160)
                .setAlignment(layout.alignment).setIncludePad(false).setLineSpacing(8f,1f)
                .setMaxLines((maxHeight/(textPaint.fontSpacing+8f)).toInt().coerceAtLeast(1)).setEllipsize(TextUtils.TruncateAt.END).build()
            val y = when(design.position) { "Top" -> 90f; "Bottom" -> h-90f-layout.height; else -> (h-layout.height)/2f }
            canvas.save(); canvas.translate(80f,y); layout.draw(canvas); canvas.restore()
            val dir = File(context.cacheDir,"cards").apply { mkdirs() }
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.forEachIndexed { index, file -> if (index >= 30 || System.currentTimeMillis()-file.lastModified()>86400000) file.delete() }
            val file = File(dir,"LifeMate-post-${UUID.randomUUID()}.png")
            try { file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }; return file }
            catch (e: Exception) { file.delete(); throw e }
        } finally { bitmap.recycle() }
    }
}
