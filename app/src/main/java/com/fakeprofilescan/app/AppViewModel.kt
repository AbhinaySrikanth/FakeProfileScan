package com.fakeprofilescan.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fakeprofilescan.app.data.ApiResult
import com.fakeprofilescan.app.data.Assessment
import com.fakeprofilescan.app.data.AssessmentApi
import com.fakeprofilescan.app.data.FormState
import com.fakeprofilescan.app.data.HistoryStore
import com.fakeprofilescan.app.data.Settings
import com.fakeprofilescan.app.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

sealed interface SubmitState {
    data object Idle : SubmitState
    data object Loading : SubmitState
    data class Failed(val message: String, val retryable: Boolean) : SubmitState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val api = AssessmentApi()
    private val settingsStore = SettingsStore(app)
    private val historyStore = HistoryStore(app.filesDir)

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _form = MutableStateFlow(FormState())
    val form: StateFlow<FormState> = _form.asStateFlow()
    private val _showErrors = MutableStateFlow(false)
    val showErrors: StateFlow<Boolean> = _showErrors.asStateFlow()
    private val _serverErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val serverErrors: StateFlow<Map<String, String>> = _serverErrors.asStateFlow()

    private val _submit = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submit: StateFlow<SubmitState> = _submit.asStateFlow()

    /** The latest result that has not been saved to history yet. */
    private val _current = MutableStateFlow<Assessment?>(null)
    val current: StateFlow<Assessment?> = _current.asStateFlow()

    private val _history = MutableStateFlow<List<Assessment>>(emptyList())
    val history: StateFlow<List<Assessment>> = _history.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    /** One-shot: id of a freshly completed assessment to open. */
    val resultEvents = _events.receiveAsFlow()

    private var job: Job? = null

    init { reloadHistory() }

    private fun reloadHistory() = viewModelScope.launch {
        _history.value = withContext(Dispatchers.IO) { historyStore.load(_settings.value.retentionDays) }
    }

    fun edit(f: (FormState) -> FormState) {
        _form.update(f)
        _serverErrors.value = emptyMap()
    }

    fun newAssessment() {
        job?.cancel()
        _form.value = FormState(); _showErrors.value = false; _serverErrors.value = emptyMap(); _submit.value = SubmitState.Idle
    }

    fun submit() {
        if (_submit.value is SubmitState.Loading) return // guard against double taps
        _showErrors.value = true
        val form = _form.value
        if (form.errors().isNotEmpty()) return
        _submit.value = SubmitState.Loading
        job = viewModelScope.launch {
            when (val r = api.assess(_settings.value.apiUrl, form)) {
                is ApiResult.Ok -> {
                    val a = r.assessment.copy(handle = form.username.trim().takeIf { _settings.value.storeHandle })
                    _current.value = a
                    _submit.value = SubmitState.Idle
                    _events.send(a.id)
                }
                is ApiResult.Invalid -> { _serverErrors.value = r.fields; _submit.value = SubmitState.Failed("The server rejected some fields. Check the highlighted entries.", false) }
                ApiResult.NotConfigured -> _submit.value = SubmitState.Failed("No server is configured. Add a valid server address in Settings.", false)
                ApiResult.Offline -> _submit.value = SubmitState.Failed("Could not reach the server. Check your connection and the server address.", true)
                ApiResult.Timeout -> _submit.value = SubmitState.Failed("The server took too long to respond.", true)
                is ApiResult.Server -> _submit.value = SubmitState.Failed(r.message, true)
            }
        }
    }

    fun cancel() { job?.cancel(); _submit.value = SubmitState.Idle }
    fun dismissError() { if (_submit.value is SubmitState.Failed) _submit.value = SubmitState.Idle }

    fun find(id: String): Assessment? = _history.value.firstOrNull { it.id == id } ?: _current.value?.takeIf { it.id == id }
    fun isSaved(id: String) = _history.value.any { it.id == id }

    fun save(a: Assessment) = persist(listOf(a) + _history.value.filter { it.id != a.id })

    fun updateNote(id: String, note: String) {
        _current.update { if (it?.id == id) it.copy(note = note) else it }
        if (isSaved(id)) persist(_history.value.map { if (it.id == id) it.copy(note = note) else it })
    }

    fun setDecision(id: String, decision: String) {
        if (isSaved(id)) persist(_history.value.map { if (it.id == id) it.copy(decision = decision) else it })
    }

    /** The most recent earlier saved assessment of the same profile, for trend display. */
    fun previousFor(a: Assessment): Assessment? = a.handle?.let { h ->
        _history.value.filter { it.id != a.id && it.handle.equals(h, true) && it.assessedAt < a.assessedAt }.maxByOrNull { it.assessedAt }
    }

    fun delete(id: String) {
        _current.update { if (it?.id == id) null else it }
        persist(_history.value.filter { it.id != id })
    }

    fun clearHistory() { _history.value = emptyList(); viewModelScope.launch(Dispatchers.IO) { historyStore.clear() } }

    fun updateSettings(s: Settings) {
        settingsStore.save(s); _settings.value = s; reloadHistory()
    }

    fun exportJson(): String = JSONArray(_history.value.map { it.toJson() }).toString(2)

    private fun persist(list: List<Assessment>) {
        _history.value = list
        viewModelScope.launch(Dispatchers.IO) { historyStore.write(list) }
    }
}
