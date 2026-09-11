package com.lifemate.utils

import android.content.*
import android.graphics.*
import android.media.*
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

fun shareText(context: Context, text: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share with someone"))
}
fun shareFile(context: Context, file: File, mime: String, view: Boolean = false) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = if (view) Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime) else Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).clipData = ClipData.newRawUri("LifeMate attachment", uri)
    context.startActivity(Intent.createChooser(intent, if (view) "Open attachment" else "Share attachment"))
}
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    var file: File? = null; private set
    fun start() {
        stop(discard = true)
        val target = File(context.filesDir, "media/${UUID.randomUUID()}.m4a").apply { parentFile?.mkdirs() }
        file = target
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        recorder = r
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC); r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); r.setAudioEncodingBitRate(96000); r.setAudioSamplingRate(44100)
            r.setOutputFile(target.absolutePath); r.setMaxDuration(10 * 60 * 1000); r.prepare(); r.start()
        } catch (e: Exception) { stop(true); throw e }
    }
    fun pause() { recorder?.pause() }
    fun resume() { recorder?.resume() }
    fun stop(discard: Boolean = false): File? {
        var valid = true
        try { recorder?.stop() } catch (_: RuntimeException) { valid = false }
        recorder?.release(); recorder = null
        val result = file
        if (discard || !valid) { result?.delete(); file = null; return null }
        file = null; return result
    }
}
/** Deterministic, offline templates. No external API or personal-data transfer. */
object Wishes {
    val tones = listOf("Friendly", "Emotional", "Funny", "Short", "Formal", "Romantic", "Best Friend")
    fun generate(name: String, relationship: String, tone: String): String = when (tone) {
        "Emotional" -> "Happy birthday, $name. Having you in my life means more than words can say. Thank you for the warmth, the memories, and the little moments that make everything brighter. May this next chapter bring you all the love you give to others."
        "Funny" -> "Happy birthday, $name! You're not getting older. You're becoming a limited edition. Here's to extra cake, questionable dance moves, and absolutely no counting candles!"
        "Short" -> "Happy birthday, $name! Wishing you a day full of love and a year full of possibility."
        "Formal" -> "Dear $name, wishing you a very happy birthday. It is a pleasure to have you in my life as my ${relationship.lowercase()}. May the coming year bring good fortune, fulfillment, and continued success."
        "Romantic" -> "Happy birthday, my love, $name. You make ordinary days feel extraordinary. I'd choose you in every chapter, in every lifetime. Here's to more laughter, more adventures, and more of us."
        "Best Friend" -> "Happy birthday, $name! My favorite person to do absolutely everything and absolutely nothing with. Thank you for being my safe place, my biggest cheerleader, and my partner in every adventure. Life is better with you."
        else -> "Happy birthday, $name! So grateful to call you my ${relationship.lowercase()}. May your day be filled with your favorite people, a little adventure, and a very big slice of cake. Here's to a wonderful year ahead!"
    }
}
object GreetingCard {
    val templates = listOf("Botanical", "Midnight", "Peach")
    fun render(context: Context, name: String, message: String, template: String, serif: Boolean): File {
        val bitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val dark = template == "Midnight"
        val bg = when (template) { "Midnight" -> Color.rgb(25, 43, 37); "Peach" -> Color.rgb(249, 225, 211); else -> Color.rgb(237, 242, 223) }
        val fg = if (dark) Color.rgb(225, 235, 193) else Color.rgb(35, 87, 64)
        canvas.drawColor(bg)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = fg; paint.alpha = 24
        canvas.drawCircle(1000f, 70f, 340f, paint); canvas.drawCircle(50f, 1260f, 270f, paint)
        paint.alpha = 255; paint.strokeWidth = 3f; paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(48f, 48f, 1032f, 1302f, 28f, 28f, paint); paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL); paint.textSize = 24f; paint.textAlign = Paint.Align.CENTER
        canvas.drawText("A LITTLE WISH, JUST FOR YOU", 540f, 230f, paint)
        paint.typeface = Typeface.create(if (serif) "serif" else "sans-serif", Typeface.NORMAL); paint.textSize = 86f
        canvas.drawText("Happy", 540f, 370f, paint); canvas.drawText("birthday.", 540f, 470f, paint)
        paint.textSize = 52f
        // Ellipsize extremely long names to keep the card shareable and readable.
        val displayName = android.text.TextUtils.ellipsize(name, android.text.TextPaint(paint), 840f, android.text.TextUtils.TruncateAt.END).toString()
        canvas.drawText(displayName, 540f, 585f, paint)
        val textPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fg; textSize = 31f; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
        val layout = android.text.StaticLayout.Builder.obtain(message, 0, message.length, textPaint, 800)
            .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).setLineSpacing(12f, 1f).setMaxLines(12).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
        canvas.save(); canvas.translate(140f, 680f); layout.draw(canvas); canvas.restore()
        paint.textSize = 22f; canvas.drawText("WITH LOVE  ·  MADE WITH LIFEMATE", 540f, 1210f, paint)
        val directory = File(context.cacheDir, "cards").apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24 * 60 * 60 * 1000 }?.forEach { it.delete() }
        val file = File(directory, "birthday-${UUID.randomUUID()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        return file
    }
}
