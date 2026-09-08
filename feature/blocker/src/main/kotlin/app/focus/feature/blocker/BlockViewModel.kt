package app.focus.feature.blocker

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.domain.model.BypassState
import app.focus.domain.model.EventLog
import app.focus.domain.model.EventType
import app.focus.domain.model.LockMode
import app.focus.domain.model.Profile
import app.focus.domain.model.bypassflow.BypassFlow
import app.focus.domain.usecase.EventLogRepository
import app.focus.domain.usecase.GrantBypassUseCase
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val grantBypassUseCase: GrantBypassUseCase,
    private val eventLogRepository: EventLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockUiState())
    val uiState: StateFlow<BlockUiState> = _uiState.asStateFlow()

    private var bypassFlow: BypassFlow? = null
    private var profile: Profile? = null
    private var targetPackages: List<String> = emptyList()
    private var delayJob: Job? = null
    private var breathingJob: Job? = null

    fun initialize(args: BlockInitArgs) {
        viewModelScope.launch {
            val loadedProfile = profileRepository.getProfile(args.profileId)
            val session = sessionRepository.observeActiveSession().firstOrNull()
            profile = loadedProfile
            targetPackages = session?.targetPackagesSnapshot ?: emptyList()
            val bypassConfig = loadedProfile?.toBypassFlowConfig()
            val remaining = loadedProfile?.let { bypassesRemaining(it, args.bypassesUsed) } ?: 0

            _uiState.value = BlockUiState(
                sessionId = args.sessionId,
                profileId = args.profileId,
                blockedPackage = args.blockedPackage,
                appName = args.blockedAppName,
                profileName = args.profileName,
                remainingMillis = args.remainingMillis,
                attemptNumber = args.attemptNumber,
                lockMode = args.lockMode,
                bypassesRemaining = remaining,
                bypassLimitReached = remaining == 0 && (loadedProfile?.bypassLimitPerSession ?: 0) >= 0,
            )

            if (loadedProfile != null && bypassConfig != null) {
                bypassFlow = BypassFlow(
                    config = bypassConfig,
                    packageName = args.blockedPackage,
                    initialBypassesUsed = args.bypassesUsed,
                )
            }
        }
    }

    fun onAction(action: BlockAction) {
        when (action) {
            BlockAction.ReturnToWork, BlockAction.OpenFocus -> Unit
            BlockAction.StartBypass -> startBypass()
            is BlockAction.SubmitBypassReason -> submitReason(action.text)
            is BlockAction.InputPhraseChar -> inputPhraseChar(action.char)
            BlockAction.CancelBypass -> cancelBypass()
        }
    }

    fun resetBypassIfInProgress() {
        if (bypassFlow?.isInProgress() == true) {
            cancelBypass()
        }
    }

    private fun startBypass() {
        val flow = bypassFlow ?: return
        val state = flow.start()
        if (state is BypassState.Denied) {
            _uiState.update { it.copy(bypassLimitReached = true, bypassStep = null) }
            return
        }
        viewModelScope.launch {
            eventLogRepository.log(
                EventLog(
                    timestamp = System.currentTimeMillis(),
                    sessionId = _uiState.value.sessionId,
                    type = EventType.BYPASS_STARTED,
                    packageName = _uiState.value.blockedPackage,
                    payload = null,
                ),
            )
        }
        updateBypassStep(state)
        when (state) {
            is BypassState.Delay -> startDelayTicker()
            is BypassState.Breathing -> startBreathingTicker()
            is BypassState.Granted -> onBypassGranted(reason = null)
            else -> Unit
        }
    }

    companion object {
        private const val DELAY_TICK_MS = 1_000L
        private const val BREATHING_TICK_MS = 100L
    }

    private fun startDelayTicker() {
        delayJob?.cancel()
        delayJob = viewModelScope.launch {
            while (bypassFlow?.getState() is BypassState.Delay) {
                delay(DELAY_TICK_MS)
                val newState = bypassFlow?.tickDelay() ?: break
                updateBypassStep(newState)
                if (newState !is BypassState.Delay) {
                    when (newState) {
                        is BypassState.Breathing -> startBreathingTicker()
                        is BypassState.Granted -> onBypassGranted(reason = null)
                        else -> Unit
                    }
                    return@launch
                }
            }
        }
    }

    private fun startBreathingTicker() {
        breathingJob?.cancel()
        breathingJob = viewModelScope.launch {
            while (bypassFlow?.getState() is BypassState.Breathing) {
                delay(BREATHING_TICK_MS)
                val done = bypassFlow?.tickBreathing(BREATHING_TICK_MS) == true
                updateBypassStep(bypassFlow?.getState())
                if (done && bypassFlow?.getState() is BypassState.Granted) {
                    onBypassGranted(reason = null)
                    return@launch
                }
            }
        }
    }

    private fun submitReason(text: String) {
        val flow = bypassFlow ?: return
        val accepted = flow.submitReason(text)
        val state = flow.getState()
        updateBypassStep(state)
        when {
            state is BypassState.Granted -> onBypassGranted(reason = text)
            state is BypassState.Phrase -> Unit
            !accepted -> Unit
        }
    }

    private fun inputPhraseChar(char: Char) {
        val flow = bypassFlow ?: return
        val state = flow.inputPhraseCharacter(char)
        if (state != null) {
            updateBypassStep(state)
            if (state is BypassState.Granted) {
                onBypassGranted(reason = null)
            }
        }
    }

    private fun cancelBypass() {
        delayJob?.cancel()
        breathingJob?.cancel()
        bypassFlow?.resetToIdle()
        _uiState.update { it.copy(bypassStep = null) }
    }

    private fun updateBypassStep(state: BypassState?) {
        _uiState.update { it.copy(bypassStep = state) }
    }

    private fun onBypassGranted(reason: String?) {
        delayJob?.cancel()
        breathingJob?.cancel()
        val currentProfile = profile ?: return
        val state = _uiState.value
        viewModelScope.launch {
            runCatching {
                grantBypassUseCase.execute(
                    sessionId = state.sessionId,
                    packageName = state.blockedPackage,
                    reason = reason,
                    profile = currentProfile,
                    targetPackages = targetPackages,
                )
            }.onSuccess { result ->
                notifyAccessWindowGranted(result.expiresAt)
                val newUsed = (currentProfile.bypassLimitPerSession - state.bypassesRemaining.coerceAtLeast(0)) + 1
                _uiState.update { current ->
                    current.copy(
                        bypassStep = BypassState.Granted,
                        bypassGranted = true,
                        bypassesRemaining = bypassesRemaining(currentProfile, newUsed),
                        bypassLimitReached = currentProfile.bypassLimitPerSession >= 0 &&
                            newUsed >= currentProfile.bypassLimitPerSession,
                    )
                }
            }.onFailure {
                cancelBypass()
            }
        }
    }

    fun launchBlockedAppIntent(): Intent? {
        val packageName = _uiState.value.blockedPackage
        return runCatching {
            context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }.getOrNull()
    }

    private fun notifyAccessWindowGranted(expiresAt: Long) {
        val state = _uiState.value
        val intent = Intent().apply {
            setClassName(context.packageName, "app.focus.service.focus.FocusForegroundService")
            action = "app.focus.service.ACCESS_WINDOW_GRANTED"
            putExtra("sessionId", state.sessionId)
            putExtra("blockedPackage", state.blockedPackage)
            putExtra("blockedAppName", state.appName)
            putExtra("expiresAt", expiresAt)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun onCleared() {
        delayJob?.cancel()
        breathingJob?.cancel()
        super.onCleared()
    }
}
