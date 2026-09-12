package com.lifemate.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lifemate.database.LifeItem
import com.lifemate.utils.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File

private fun PostDesign.json() = JSONObject().put("text",text).put("palette",palette).put("font",font).put("effect",effect)
    .put("format",format).put("position",position).put("ink",ink).put("size",size.toDouble()).put("shade",shade.toDouble())
    .put("zoom",zoom.toDouble()).put("rotation",rotation).put("alignment",alignment).put("photo",photo).toString()
private fun designFromJson(json: String, fallback: PostDesign): PostDesign = runCatching {
    val o = JSONObject(json)
    PostDesign(o.optString("text",fallback.text).take(600),o.optString("palette","Rose"),o.optString("font","Modern"),
        o.optString("effect","Original"),o.optString("format","Portrait"),o.optString("position","Center"),o.optString("ink","White"),
        o.optDouble("size",58.0).toFloat().coerceIn(28f,90f),o.optDouble("shade",.35).toFloat().coerceIn(0f,.8f),
        o.optDouble("zoom",1.0).toFloat().coerceIn(1f,2.5f),o.optInt("rotation",0),o.optString("alignment","Center"),o.optString("photo",""))
}.getOrDefault(fallback)

@Composable fun CardStudioScreen(vm: LifeViewModel, birthday: LifeItem? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val draftId = birthday?.id ?: "social-post"
    val person = birthday?.nickname?.ifBlank { birthday.title } ?: "friend"
    val default = remember(draftId) { PostDesign(text = if (birthday != null) Wishes.generate(person,birthday.relationship,"Short") else "Make today\na little brighter.", format = if (birthday != null) "Portrait" else "Square") }
    var saved by rememberSaveable(draftId) { mutableStateOf(runCatching { vm.app.secure.readStudioDraft(draftId) }.getOrNull() ?: default.json()) }
    var design by remember(draftId) { mutableStateOf(designFromJson(saved, default)) }
    var tone by rememberSaveable(draftId) { mutableStateOf("Short") }
    var name by rememberSaveable(draftId) { mutableStateOf(person) }
    var tab by rememberSaveable { mutableStateOf("Text") }
    var card by remember { mutableStateOf<File?>(null) }
    var readyDesign by remember { mutableStateOf<PostDesign?>(null) }
    var renderRevision by remember { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val ready = card != null && readyDesign == design && !importing
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val output = card
        if (uri != null && output != null) vm.runAction("Image saved") {
            context.contentResolver.openOutputStream(uri).use { stream -> requireNotNull(stream); output.inputStream().use { it.copyTo(stream) } }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importing = true; error = null
            try {
                val photo = withContext(Dispatchers.IO) { PostCardRenderer.importPhoto(context,uri) }
                design = design.copy(photo = photo.path, rotation = 0, zoom = 1f)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Photo could not be opened." }
            finally { importing = false }
        }
    }
    LaunchedEffect(design, renderRevision) {
        saved = design.json(); error = null
        delay(450)
        try {
            withContext(Dispatchers.IO) {
                val old = vm.app.secure.readStudioDraft(draftId)?.let { designFromJson(it,default).photo }
                vm.app.secure.saveStudioDraft(draftId,saved)
                if (!old.isNullOrBlank() && old != design.photo) {
                    val previous = File(old)
                    if (previous.parentFile?.canonicalFile == File(context.filesDir,"studio").canonicalFile) previous.delete()
                }
            }
            val rendered = withContext(Dispatchers.IO) { PostCardRenderer.render(context,design) }
            card = rendered; readyDesign = design
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "Unable to make this image. Try again with a smaller photo." }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeading(if (birthday == null) "Your photo studio" else "Make their day", "A little creativity. Something worth sharing.")
        SoftCard(Modifier.fillMaxWidth(), MaterialTheme.colorScheme.primaryContainer) {
            Text(if (birthday == null) "Create a Facebook-ready photo post" else "Birthday wishes, with a personal touch", style = MaterialTheme.typography.titleMedium)
            Text("Local editing only. Choose a photo, add your words, then save or share it yourself. No automatic posting.", style = MaterialTheme.typography.bodyMedium)
        }
        val (width,height) = PostCardRenderer.dimensions(design.format)
        Box(Modifier.fillMaxWidth().aspectRatio(width.toFloat()/height).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).testTag("studio-preview"), contentAlignment = Alignment.Center) {
            card?.let { AsyncImage(it,"Your card preview: ${design.text}",Modifier.fillMaxSize(),contentScale = ContentScale.Fit) }
            if (!ready && error == null) CircularProgressIndicator()
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton({ renderRevision++ }) { Text("Try preview again") } }
        Text("$width × $height PNG · Live preview. Long text shrinks to fit and may be shortened; review before sharing.", style = MaterialTheme.typography.bodyMedium)
        ChoiceChips(listOf("Text","Photo","Style","Layout"),tab) { tab = it }
        SoftCard(Modifier.fillMaxWidth()) {
            when(tab) {
                "Text" -> {
                    if (birthday != null) {
                        Field(name,{ name = it.take(100) },"Their name")
                        ChoiceChips(Wishes.tones,tone) { tone = it }
                        OutlinedButton({ design = design.copy(text = Wishes.generate(name,birthday.relationship,tone).take(600)) },Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AutoAwesome,null); Text(" Generate $tone wish") }
                    }
                    Field(design.text,{ design = design.copy(text = it.take(600)) },"Your words (up to 600 characters)",singleLine = false,minLines = 4)
                    ChoiceChips(PostCardRenderer.fonts,design.font) { design = design.copy(font = it) }
                    Text("Text size · ${design.size.toInt()}")
                    Slider(design.size,{ design = design.copy(size = it) },valueRange = 28f..90f, modifier = Modifier.testTag("studio-text-size"))
                    ChoiceChips(PostCardRenderer.inks,design.ink) { design = design.copy(ink = it) }
                    TextButton({ (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("LifeMate words",design.text)); vm.message("Text copied") }) { Text("Copy text") }
                    TextButton({ try { shareText(context,design.text) } catch (_: Exception) { error = "No sharing app is available." } }) { Text("Share text") }
                }
                "Photo" -> {
                    Button({ picker.launch(arrayOf("image/*")) },enabled = !importing,modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AddPhotoAlternate,null); Text(if (importing) " Importing…" else " Choose a photo") }
                    Text("System picker only · up to 25 MB. Original photo stays unchanged; location metadata is not included in the exported image.",style = MaterialTheme.typography.bodyMedium)
                    if (design.photo.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ design = design.copy(rotation = (design.rotation+1)%4) }) { Text("Rotate 90°") }
                            TextButton({ design = design.copy(photo = "") }) { Text("Remove photo") }
                        }
                        Text("Crop zoom")
                        Slider(design.zoom,{ design = design.copy(zoom = it) },valueRange = 1f..2.5f)
                        Text("Dark overlay · keeps words readable")
                        Slider(design.shade,{ design = design.copy(shade = it) },valueRange = 0f..0.8f)
                    }
                }
                "Style" -> {
                    Text("Color story",style = MaterialTheme.typography.titleMedium)
                    ChoiceChips(PostCardRenderer.palettes,design.palette) { design = design.copy(palette = it) }
                    Text("Gradient backgrounds appear when no photo is selected.",style = MaterialTheme.typography.bodyMedium)
                    Text("Photo effect",style = MaterialTheme.typography.titleMedium)
                    ChoiceChips(PostCardRenderer.effects,design.effect) { design = design.copy(effect = it) }
                    if (design.photo.isBlank()) Text("Choose a photo to see photo effects.")
                }
                else -> {
                    Text("Post format",style = MaterialTheme.typography.titleMedium)
                    ChoiceChips(PostCardRenderer.formats,design.format) { design = design.copy(format = it) }
                    Text("Text placement")
                    ChoiceChips(listOf("Top","Center","Bottom"),design.position) { design = design.copy(position = it) }
                    Text("Text alignment")
                    ChoiceChips(listOf("Left","Center","Right"),design.alignment) { design = design.copy(alignment = it) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton({ save.launch("LifeMate-${if(birthday == null) "post" else "birthday"}.png") },enabled = ready,modifier = Modifier.weight(1f)) { Text("Save image") }
            Button({ card?.let { try { shareFile(context,it,"image/png") } catch (_: Exception) { error = "No sharing app is available. Save the image instead." } } },enabled = ready,modifier = Modifier.weight(1f).testTag("studio-share")) { Text("Share image") }
        }
        Text("To post on Facebook, choose Facebook in Share (if installed), or save this image and attach it to a new post. Review the audience before posting.",style = MaterialTheme.typography.bodyMedium)
        Text("Your text draft is encrypted on this device. Studio drafts/photos are not part of record backups; save finished images separately. Remove all drafts with Settings → Delete all data.",style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
    }
}
