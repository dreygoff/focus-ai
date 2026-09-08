package app.focus.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.datastore.UserSettingsRepository
import app.focus.domain.model.LockMode
import app.focus.domain.model.SessionStatus
import app.focus.domain.model.Theme
import app.focus.domain.usecase.ClearStatisticsUseCase
import app.focus.domain.usecase.DeleteAllUserDataUseCase
import app.focus.domain.usecase.ExportStatsCsvUseCase
import app.focus.domain.usecase.SessionRepository
import app.focus.domain.usecase.StatsPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class SettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val sessionRepository: SessionRepository,
    private val exportStatsCsv: ExportStatsCsvUseCase,
    private val clearStatisticsUseCase: ClearStatisticsUseCase,
    private val deleteAllUserData: DeleteAllUserDataUseCase,
) : ViewModel() {

    data class UiState(
        val theme: Theme = Theme.SYSTEM,
        val dynamicColor: Boolean = true,
        val languageTag: String = "ru",
        val blockVibration: Boolean = false,
        val blockSound: Boolean = false,
        val quotesEnabled: Boolean = true,
        val hardLockActive: Boolean = false,
        val exportCsv: String? = null,
        val messageKey: String? = null,
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                combine(
                    userSettingsRepository.getTheme(),
                    userSettingsRepository.getDynamicColorEnabled(),
                    userSettingsRepository.getLanguageTag(),
                ) { theme, dynamicColor, languageTag ->
                    Triple(theme, dynamicColor, languageTag)
                },
                combine(
                    userSettingsRepository.getBlockVibrationEnabled(),
                    userSettingsRepository.getBlockSoundEnabled(),
                    userSettingsRepository.getQuotesEnabled(),
                ) { blockVibration, blockSound, quotesEnabled ->
                    Triple(blockVibration, blockSound, quotesEnabled)
                },
                sessionRepository.observeActiveSession(),
            ) { appearance, blocking, session ->
                SettingsSnapshot(
                    theme = appearance.first,
                    dynamicColor = appearance.second,
                    languageTag = appearance.third,
                    blockVibration = blocking.first,
                    blockSound = blocking.second,
                    quotesEnabled = blocking.third,
                    hardLockActive = session?.lockMode is LockMode.Hard &&
                        (session.status is SessionStatus.Running || session.status is SessionStatus.Paused),
                )
            }.collect { snapshot ->
                _uiState.update { current ->
                    current.copy(
                        theme = snapshot.theme,
                        dynamicColor = snapshot.dynamicColor,
                        languageTag = snapshot.languageTag,
                        blockVibration = snapshot.blockVibration,
                        blockSound = snapshot.blockSound,
                        quotesEnabled = snapshot.quotesEnabled,
                        hardLockActive = snapshot.hardLockActive,
                    )
                }
            }
        }
    }

    fun setTheme(theme: Theme) {
        viewModelScope.launch { userSettingsRepository.updateTheme(theme) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { userSettingsRepository.updateDynamicColorEnabled(enabled) }
    }

    fun setLanguageTag(tag: String) {
        viewModelScope.launch { userSettingsRepository.updateLanguageTag(tag) }
    }

    fun setBlockVibration(enabled: Boolean) {
        viewModelScope.launch { userSettingsRepository.updateBlockVibrationEnabled(enabled) }
    }

    fun setBlockSound(enabled: Boolean) {
        viewModelScope.launch { userSettingsRepository.updateBlockSoundEnabled(enabled) }
    }

    fun setQuotesEnabled(enabled: Boolean) {
        viewModelScope.launch { userSettingsRepository.updateQuotesEnabled(enabled) }
    }

    fun requestExport() {
        viewModelScope.launch {
            runCatching { exportStatsCsv.execute(StatsPeriod.MONTH) }
                .onSuccess { csv -> _uiState.update { it.copy(exportCsv = csv) } }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun clearExportRequest() {
        _uiState.update { it.copy(exportCsv = null) }
    }

    fun clearStatistics(onDone: () -> Unit = {}) {
        if (_uiState.value.hardLockActive) return
        viewModelScope.launch {
            runCatching { clearStatisticsUseCase.execute() }
                .onSuccess {
                    _uiState.update { it.copy(messageKey = "stats_cleared") }
                    onDone()
                }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun deleteAllData(onDone: () -> Unit = {}) {
        if (_uiState.value.hardLockActive) return
        viewModelScope.launch {
            runCatching { deleteAllUserData.execute() }
                .onSuccess {
                    _uiState.update { it.copy(messageKey = "data_deleted") }
                    onDone()
                }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(messageKey = null, errorMessage = null) }
    }

    private data class SettingsSnapshot(
        val theme: Theme,
        val dynamicColor: Boolean,
        val languageTag: String,
        val blockVibration: Boolean,
        val blockSound: Boolean,
        val quotesEnabled: Boolean,
        val hardLockActive: Boolean,
    )
}
