package com.lifemate.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lifemate.database.SocialPost
import com.lifemate.domain.*
import com.lifemate.data.BackendPostGenerator
import com.lifemate.utils.*
import kotlinx.coroutines.*
import java.io.File
import java.net.URI

@Composable fun FacebookPostsScreen(vm: LifeViewModel, navigate: (String)->Unit) {
    val posts by vm.posts.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val visible=remember(posts,query) { posts.filter { query.isBlank() || it.topic.contains(query,true) || it.caption.contains(query,true) } }
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Facebook Posts","Private drafts") }
        item { Button({navigate("post/new")},Modifier.fillMaxWidth()) {Icon(Icons.Outlined.Add,null); Text(" Create post")} }
        item { Field(query,{query=it},"Search posts") }
        if(visible.isEmpty()) item { EmptyState(null,"No Facebook posts","","Create post") {navigate("post/new")} }
        items(visible,key={it.id}) { post ->
            SoftCard(Modifier.fillMaxWidth().clickable {navigate("post/${post.id}")}) {
                if(post.photo.isNotBlank()) AsyncImage(File(post.photo),post.topic.ifBlank {post.type},Modifier.fillMaxWidth().height(150.dp),contentScale=ContentScale.Crop)
                Eyebrow(post.type)
                Text(post.topic.ifBlank { post.person.ifBlank {post.type} },style=MaterialTheme.typography.titleMedium)
                Text(post.caption,maxLines=3,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable fun PostComposerScreen(id: String, vm: LifeViewModel, navigate: (String)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var draft by rememberSaveable(id) { mutableStateOf(SocialPost()) }
    var loaded by rememberSaveable(id) { mutableStateOf(id=="new") }
    var missing by remember { mutableStateOf(false) }
    var tone by rememberSaveable { mutableStateOf("Short") }
    var variation by rememberSaveable { mutableIntStateOf(0) }
    var generating by remember { mutableStateOf(false) }
    var confirmAi by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(id) {
        if(!loaded) {
            val post=withContext(Dispatchers.IO) {vm.repo.dao.getPost(id)}
            if(post==null) missing=true else draft=post
            loaded=true
        }
    }
    val photo=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) vm.runAction {
            val path=vm.repo.postPhoto(uri)
            withContext(Dispatchers.Main) { draft=draft.copy(photo=path) }
        }
    }
    if(!loaded) { CircularProgressIndicator(Modifier.padding(24.dp)); return }
    if(missing) { EmptyState(null,"Post not found","","Facebook Posts") {navigate("posts")}; return }
    fun save(after: ()->Unit) {
        vm.runAction("Post saved",after) { vm.repo.savePost(draft); withContext(Dispatchers.Main) { vm.playSuccess() } }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        PageHeading(if(id=="new") "Create post" else "Edit post","")
        ChoiceChips(PostTemplates.types,draft.type) {draft=draft.copy(type=it)}
        Field(draft.topic,{draft=draft.copy(topic=it.take(160))},"Post topic")
        Field(draft.message,{draft=draft.copy(message=it.take(600))},"Short message",singleLine=false,minLines=2)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton({photo.launch(arrayOf("image/*"))},enabled=!busy) {Icon(Icons.Outlined.AddPhotoAlternate,null); Text(" Photo")}
            if(draft.photo.isNotBlank()) TextButton({draft=draft.copy(photo="")}) {Text("Remove photo")}
        }
        if(draft.photo.isNotBlank()) AsyncImage(File(draft.photo),"Post photo",Modifier.fillMaxWidth().height(180.dp),contentScale=ContentScale.Crop)
        Field(draft.person,{draft=draft.copy(person=it.take(100))},"Name (optional)")
        DateField("Date (optional)",draft.date,true) {draft=draft.copy(date=it)}
        ChoiceChips(PostTemplates.tones,tone) {tone=it}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button({ variation++; draft=draft.copy(caption=PostTemplates.caption(draft,tone,variation)) },Modifier.weight(1f)) {Text(if(draft.caption.isBlank()) "Create caption" else "Regenerate")}
            OutlinedButton({confirmAi=true},enabled=PostAiConfig.validEndpoint(state.preferences.postAiEndpoint) && !generating) {Text(if(generating) "Generating…" else "AI")}
        }
        Text(if(PostAiConfig.validEndpoint(state.preferences.postAiEndpoint)) "Templates stay offline. AI asks before sending text." else "Offline templates · Optional AI in Settings",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Field(draft.caption,{draft=draft.copy(caption=it.take(4000))},"Caption",singleLine=false,minLines=4,modifier=Modifier.testTag("post-caption"))
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton({(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Post",draft.caption));vm.message("Copied")},enabled=draft.caption.isNotBlank(),modifier=Modifier.weight(1f)) {Text("Copy")}
            OutlinedButton({try {shareText(context,draft.caption)} catch (_: Exception) {error="No sharing app available."}},enabled=draft.caption.isNotBlank(),modifier=Modifier.weight(1f)) {Text("Share")}
        }
        Button({save {navigate("posts")}},enabled=!busy && !generating,modifier=Modifier.fillMaxWidth().testTag("save-post")) {Text("Save post")}
        OutlinedButton({save {navigate("post-card/${draft.id}")}},enabled=!busy && !generating,modifier=Modifier.fillMaxWidth()) {Icon(Icons.Outlined.Palette,null);Text(" Create image")}
        Text("Share opens Android's chooser. Review your audience in Facebook; nothing is published automatically.",style=MaterialTheme.typography.bodyMedium)
        if(id!="new") TextButton({deleting=true}) {Text("Delete post",color=MaterialTheme.colorScheme.error)}
        Spacer(Modifier.height(20.dp))
    }
    if(deleting) ConfirmDialog("Delete post?","This removes the saved draft and its private photo.",onDismiss={deleting=false}) {
        deleting=false;vm.runAction("Post deleted",{navigate("posts")}) {vm.repo.deletePost(draft)}
    }
    if(confirmAi) ConfirmDialog("Generate with AI?","Send only this post's type, topic, message, name, date and tone to ${runCatching { URI(state.preferences.postAiEndpoint).host }.getOrDefault("your backend")}? Photos, saved captions and other records are not sent. Review these fields before continuing.","Send & generate",{confirmAi=false}) {
        confirmAi=false
        val request=draft.aiRequest(tone);val previousCaption=draft.caption;val endpoint=state.preferences.postAiEndpoint
        scope.launch {
            generating=true;error=null
            try { val caption=BackendPostGenerator(endpoint).generate(request);if(draft.aiRequest(tone)==request && draft.caption==previousCaption) draft=draft.copy(caption=caption) else error="Your draft changed. Generate again to keep your edits." }
            catch(e:CancellationException) {throw e}
            catch (_:Exception) {error="AI is unavailable. Your draft is unchanged; use offline templates."}
            finally {generating=false}
        }
    }
}

@Composable fun PostGraphicScreen(id: String, vm: LifeViewModel) {
    var post by remember(id) {mutableStateOf<SocialPost?>(null)}
    var loaded by remember(id) {mutableStateOf(false)}
    LaunchedEffect(id) {post=withContext(Dispatchers.IO) {vm.repo.dao.getPost(id)};loaded=true}
    val value=post
    if(value!=null) CardStudioScreen(vm,post=value)
    else if(!loaded) CircularProgressIndicator(Modifier.padding(24.dp))
    else EmptyState(null,"Post not found","")
}
