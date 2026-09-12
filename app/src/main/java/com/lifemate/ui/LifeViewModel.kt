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
    private val completionKeys by lazy { completions.map { it.itemId to it.date }.toHashSet() }
    private val completionDates by lazy { completions.groupBy { it.itemId }.mapValues { (_,rows) -> rows.map { LocalDate.parse(it.date) }.toSet() } }
    private val groupedMedia by lazy { attachments.groupBy { it.itemId } }
    fun prepareIndexes() { completionKeys.size; completionDates.size; groupedMedia.size }
    fun done(item: LifeItem, day: LocalDate = LocalDate.now()) = (item.id to day.toString()) in completionKeys
    fun completed(item: LifeItem, day: LocalDate = LocalDate.now()): Boolean = when {
        item.kind in setOf(Kind.MISSION, Kind.GOAL) -> progress(item) >= 1f
        item.repeat == Repeat.ONCE -> done(item, LocalDate.parse(item.date))
        else -> done(item, day)
    }
    fun dates(item: LifeItem) = completionDates[item.id].orEmpty()
    fun progress(item: LifeItem): Float = if (item.kind == Kind.MISSION) (dates(item).count { Schedule.occurs(item.spec(), it) }.toFloat() / item.duration).coerceIn(0f, 1f) else item.progress / 100f
    fun media(item: LifeItem) = groupedMedia[item.id].orEmpty()
}
class LifeViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as LifeMateApp
    val repo = app.repository
    val feedback = com.lifemate.utils.FeedbackSounds(app)
    val posts = repo.dao.posts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun playSuccess() { feedback.play("success",state.value.preferences.soundEffects) }
    override fun onCleared() { feedback.close();super.onCleared() }
    private val updateRepository = com.lifemate.updates.UpdateRepository(application)
    private val updateState = MutableStateFlow(com.lifemate.updates.UpdateState(release = updateRepository.cached()))
    val updates = updateState.asStateFlow()
    private val apkDownloader = com.lifemate.updates.ApkUpdateDownloader(application)
    private val downloadState = MutableStateFlow(com.lifemate.updates.ApkDownloadState())
    val apkDownload = downloadState.asStateFlow()
    private var downloadJob: Job? = null
    fun downloadUpdate() {
        val release = updateState.value.release ?: return
        if (downloadJob?.isCompleted == false || !release.newerThan(com.lifemate.BuildConfig.VERSION_CODE)) return
        downloadState.value = com.lifemate.updates.ApkDownloadState(release.code,com.lifemate.updates.DownloadPhase.DOWNLOADING,total=release.bytes)
        downloadJob = viewModelScope.launch {
            try {
                apkDownloader.download(release) { downloadState.value = it }
                downloadState.value = com.lifemate.updates.ApkDownloadState(release.code,com.lifemate.updates.DownloadPhase.READY,release.bytes,release.bytes,"Verified and ready to install.")
            } catch (_: TimeoutCancellationException) {
                downloadState.value = com.lifemate.updates.ApkDownloadState(release.code,com.lifemate.updates.DownloadPhase.ERROR,message="Download timed out. Reconnect and retry.")
            } catch (e: CancellationException) {
                downloadState.value = com.lifemate.updates.ApkDownloadState(release.code,message="Download stopped. Tap Download update to restart; the required update is still in force.")
                throw e
            } catch (e: Exception) {
                downloadState.value = com.lifemate.updates.ApkDownloadState(release.code,com.lifemate.updates.DownloadPhase.ERROR,message=if (e is IllegalStateException) e.message else "Download failed. Check your connection and storage, then retry.")
            } finally { downloadJob = null }
        }
    }
    fun stopUpdateDownload() { downloadJob?.cancel() }
    suspend fun verifiedUpdateFile(): java.io.File {
        val release = updateState.value.release ?: error("Check for updates first.")
        try { return apkDownloader.verifyReady(release) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            downloadState.value = com.lifemate.updates.ApkDownloadState(release.code,com.lifemate.updates.DownloadPhase.ERROR,message="The saved update is unavailable or failed verification. Download it again.")
            throw e
        }
    }
    fun checkUpdates(manual: Boolean = false) {
        if (updateState.value.checking || (!manual && !updateRepository.due())) return
        updateState.value = updateState.value.copy(checking = true, message = null)
        viewModelScope.launch {
            try {
                val release = updateRepository.fetch()
                val message = when {
                    release == null -> "No signed update has been published yet. Please check again later."
                    release.newerThan(com.lifemate.BuildConfig.VERSION_CODE) -> "A new official release is available."
                    else -> "You already have this release or a newer version."
                }
                updateState.value = com.lifemate.updates.UpdateState(release, message = message)
            } catch (e: CancellationException) { updateState.value = updateState.value.copy(checking = false); throw e }
            catch (_: Exception) { updateState.value = updateState.value.copy(checking = false, message = "Couldn't check for updates. Check your connection or try again later. Your offline features still work.") }
        }
    }
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    val busy = MutableStateFlow(false)
    private var activeActions = 0
    val state = combine(repo.dao.profile(), repo.dao.items(), repo.dao.completions(), repo.dao.attachments(), app.preferences.flow) { p, i, c, a, s -> LifeState(false, p, i, c, a, s).also { it.prepareIndexes() } }
        .flowOn(Dispatchers.Default)
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
    fun toggle(item: LifeItem, day: LocalDate = LocalDate.now()) = runAction {
        val completed=repo.toggle(item,day)
        val missionFinished=completed && item.kind==Kind.MISSION && repo.dao.completedDates(item.id).count { item.occurs(LocalDate.parse(it)) }>=item.duration
        if(completed) withContext(Dispatchers.Main) {
            feedback.play(if(missionFinished) "mission" else "task",state.value.preferences.soundEffects)
        }
    }
    fun delete(item: LifeItem, after: () -> Unit) = runAction("Deleted", after) { repo.delete(item) }
    fun attach(item: LifeItem, uri: Uri) = runAction("Attachment saved on this device") { repo.attach(item.id, uri) }
    fun preference(key: String, value: String) = runAction { app.preferences.set(key, value); app.scheduler.reconcile() }
    fun preference(key: String, value: Boolean) = runAction {
        app.preferences.set(key, value)
        if (!value && key in setOf("voiceReminders", "notifications")) app.stopService(android.content.Intent(app, com.lifemate.notifications.VoiceReminderService::class.java))
        app.scheduler.reconcile()
    }
    fun export(uri: Uri) = runAction("Backup exported. Store it somewhere private.") { BackupManager(repo).export(uri) }
    fun restore(uri: Uri, after: () -> Unit) = runAction("Backup restored", after) { BackupManager(repo).restore(uri) }
}
