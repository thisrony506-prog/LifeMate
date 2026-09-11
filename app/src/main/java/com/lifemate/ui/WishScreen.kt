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
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lifemate.database.LifeItem
import com.lifemate.utils.*
import kotlinx.coroutines.*
import java.io.File

@Composable fun WishScreen(item: LifeItem, vm: LifeViewModel) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(item.nickname.ifBlank { item.title }) }
    var tone by rememberSaveable { mutableStateOf("Friendly") }
    var message by rememberSaveable { mutableStateOf(Wishes.generate(name, item.relationship, tone)) }
    var template by rememberSaveable { mutableStateOf("Botanical") }
    var style by rememberSaveable { mutableStateOf("Serif") }
    var card by remember { mutableStateOf<File?>(null) }
    var rendering by remember { mutableStateOf(true) }
    val saveImage = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val file = card
        if (uri != null && file != null) vm.runAction("Greeting card saved") { context.contentResolver.openOutputStream(uri).use { output -> requireNotNull(output); file.inputStream().use { it.copyTo(output) } } }
    }
    LaunchedEffect(name, message, template, style) {
        rendering = true
        delay(350)
        try { card = withContext(Dispatchers.IO) { GreetingCard.render(context, name, message, template, style == "Serif") } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { vm.message("The card couldn't be created. Try a shorter message.") }
        finally { rendering = false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeading("Make their day", "A personal wish. A little extra love.")
        SoftCard(color = MaterialTheme.colorScheme.primaryContainer) { Text("Made privately, on your device. These are offline templates—not AI. Nothing is ever sent automatically.", style = MaterialTheme.typography.bodyMedium) }
        Field(name, { name = it.take(100) }, "Their name")
        Text("Find the right words", style = MaterialTheme.typography.titleLarge)
        ChoiceChips(Wishes.tones, tone) { tone = it }
        OutlinedButton({ message = Wishes.generate(name, item.relationship, tone) }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AutoAwesome, null); Text(" Generate $tone wish") }
        Field(message, { message = it.take(600) }, "Make it your own (up to 600 characters)", singleLine = false, minLines = 5)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton({ (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Birthday wish", message)); vm.message("Wish copied") }, Modifier.weight(1f)) { Icon(Icons.Outlined.ContentCopy, null); Text(" Copy") }
            Button({ try { shareText(context, message) } catch (_: Exception) { vm.message("No sharing app is available.") } }, Modifier.weight(1f)) { Icon(Icons.Outlined.Share, null); Text(" Share wish") }
        }
        HorizontalDivider()
        SectionHeading("A card to keep")
        Text("Template & background", style = MaterialTheme.typography.bodyMedium)
        ChoiceChips(GreetingCard.templates, template) { template = it }
        ChoiceChips(listOf("Serif", "Modern"), style) { style = it }
        Box(Modifier.fillMaxWidth().aspectRatio(.8f).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            card?.let { AsyncImage(it, "Birthday card for $name: $message", Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            if (rendering) CircularProgressIndicator()
        }
        Text("Preview the complete card before sharing. Long messages may be shortened to fit.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton({ saveImage.launch("LifeMate-birthday.png") }, enabled = card != null && !rendering, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Download, null); Text(" Save image") }
            Button({ card?.let { try { shareFile(context, it, "image/png") } catch (_: Exception) { vm.message("Unable to share the card.") } } }, enabled = card != null && !rendering, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Share, null); Text(" Share card") }
        }
    }
}
