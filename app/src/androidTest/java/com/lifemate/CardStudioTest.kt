package com.lifemate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.utils.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CardStudioTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun rendersEveryPostShapeWithLongUnicodeText() {
        PostCardRenderer.formats.forEach { shape ->
            val file = PostCardRenderer.render(context,PostDesign(text="শুভ জন্মদিন! Happy birthday! ".repeat(30),format=shape,font="Handwritten"))
            val bitmap = BitmapFactory.decodeFile(file.path)
            val expected = PostCardRenderer.dimensions(shape)
            assertEquals(expected.first,bitmap.width); assertEquals(expected.second,bitmap.height)
            assertTrue(file.length()>1000); bitmap.recycle(); file.delete()
        }
    }
    @Test fun importsPrivateCopyAndAppliesPhotoEffects() {
        val original = File(context.cacheDir,"studio-source.png")
        val image = Bitmap.createBitmap(80,120,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        original.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }; image.recycle()
        val privateCopy = PostCardRenderer.importPhoto(context,Uri.fromFile(original))
        assertTrue(privateCopy.path.startsWith(File(context.filesDir,"studio").path))
        val file = PostCardRenderer.render(context,PostDesign(text="",photo=privateCopy.path,effect="Mono",rotation=1,shade=0f))
        val result = BitmapFactory.decodeFile(file.path); val pixel = result.getPixel(100,100)
        assertEquals(Color.red(pixel),Color.green(pixel)); assertEquals(Color.green(pixel),Color.blue(pixel))
        assertTrue(original.exists()); result.recycle(); file.delete(); privateCopy.delete(); original.delete()
    }
    @Test fun draftTextIsEncryptedAndRestorable() {
        val secure = SecureStore(context)
        secure.saveStudioDraft("test-card","private birthday words")
        assertEquals("private birthday words",secure.readStudioDraft("test-card"))
        val stored = context.getSharedPreferences("lifemate_secrets",0).getString("studio:test-card","")!!
        assertFalse(stored.contains("private birthday words"))
    }
    @Test fun invalidPhotoFailsWithoutLeavingTemporaryInput() {
        val file = File(context.cacheDir,"bad-photo").apply { writeText("not an image") }
        try { PostCardRenderer.importPhoto(context,Uri.fromFile(file)); fail("Must reject non-image") }
        catch (_: IllegalArgumentException) { }
        finally { file.delete() }
        assertFalse(File(context.filesDir,"studio").listFiles().orEmpty().any { it.name.startsWith("input-") })
    }
}
