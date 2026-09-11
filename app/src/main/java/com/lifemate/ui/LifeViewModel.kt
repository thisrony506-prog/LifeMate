package com.lifemate.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifemate.LifeMateApp
import com.lifemate.data.*
import com.lifemate.database.*
import com.lifemate.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.time.LocalDate

data class LifeState(val loading: Boolean = true, val profile: Profile? = null, val items: List<LifeItem> = emptyList(),
    val completions: List<Completion> = emptyList(), val attachments: List<Attachment> = emptyList(), val preferences: Preferences = Preferences(), val error: String? = null) {
    fun done(item: LifeItem, day: LocalDate = LocalDate.now()) = completions.any { it.itemId == item.id && it.date == day.toString() }
    fun dates(item: LifeItem) = completions.filter { it.itemId == item.id }.map { LocalDate.parse(it.date) }.toSet()
    fun progress(item: LifeItem): Float = if (item.kind == Kind.MISSION) (dates(item).count { item.occurs(it) }.toFloat() / item.duration).coerceIn(0f, 1f) else item.progress / 100f
    fun media(item: LifeItem) = attachments.filter { it.itemId == item.id }
}
class LifeViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as LifeMateApp
    val repo = app.repository
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    val busy = MutableStateFlow(false)
    private var activeActions = 0
    val state = combine(repo.dao.profile(), repo.dao.items(), repo.dao.completions(), repo.dao.attachments(), app.preferences.flow) { p, i, c, a, s -> LifeState(false, p, i, c, a, s) }
        .catch { emit(LifeState(loading = false, error = "Your data couldn't be opened. Restart the app or contact support. Your files have not been deleted.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LifeState())
    fun message(text: String) { viewModelScope.launch { messages.send(text) } }
    fun runAction(success: String? = null, after: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            activeActions++; busy.value = true
            try { withContext(Dispatchers.IO) { block() }; success?.let { messages.send(it) }; after() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { messages.send(e.message?.take(180) ?: "Something went wrong. Please try again.") }
            finally { activeActions--; busy.value = activeActions > 0 }
        }
    }
    fun save(item: LifeItem, after: () -> Unit) = runAction("${item.kind.label} saved", after) { repo.save(item) }
    fun toggle(item: LifeItem, day: LocalDate = LocalDate.now()) = runAction { repo.toggle(item, day) }
    fun delete(item: LifeItem, after: () -> Unit) = runAction("Deleted", after) { repo.delete(item) }
    fun attach(item: LifeItem, uri: Uri) = runAction("Attachment saved on this device") { repo.attach(item.id, uri) }
    fun preference(key: String, value: String) = runAction { app.preferences.set(key, value); app.scheduler.reconcile() }
    fun preference(key: String, value: Boolean) = runAction { app.preferences.set(key, value); app.scheduler.reconcile() }
    fun export(uri: Uri) = runAction("Backup exported. Store it somewhere private.") { BackupManager(repo).export(uri) }
    fun restore(uri: Uri, after: () -> Unit) = runAction("Backup restored", after) { BackupManager(repo).restore(uri) }
}
