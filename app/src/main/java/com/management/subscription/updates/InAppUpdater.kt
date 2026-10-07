package com.management.subscription.updates

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.management.subscription.BuildConfig
import com.management.subscription.analytics.Analytics
import com.management.subscription.analytics.AnalyticsEvent
import com.management.subscription.data.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 홈 배너가 보여 줄 앱 업데이트 상태 */
sealed interface AppUpdateState {
    data object None : AppUpdateState
    /** 새 버전이 있다. [업데이트]를 누르면 뒤에서 내려받는다. */
    data class Available(val versionCode: Int) : AppUpdateState
    data object Downloading : AppUpdateState
    /** 다 받았다. [다시 시작]을 누르면 설치하고 앱이 다시 켜진다. */
    data object ReadyToInstall : AppUpdateState
}

/**
 * Google Play 인앱 업데이트(유연한 업데이트). 새 버전이 있으면 홈 배너로 알리고,
 * 앱을 쓰는 동안 뒤에서 내려받은 뒤 사용자가 고를 때 설치한다.
 *
 * Play 스토어에서 설치한 앱에서만 새 버전이 잡힌다. 디버그 빌드를 -PFAKE_APP_UPDATE=true로
 * 만들면 Play 라이브러리의 가짜 관리자로 같은 흐름을 에뮬레이터에서 확인할 수 있다.
 */
class InAppUpdater(
    private val activity: ComponentActivity,
    private val settingsRepository: SettingsRepository,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>
) {
    private val manager: AppUpdateManager =
        if (BuildConfig.FAKE_APP_UPDATE) {
            FakeAppUpdateManager(activity).apply { setUpdateAvailable(BuildConfig.VERSION_CODE + 1) }
        } else {
            AppUpdateManagerFactory.create(activity)
        }

    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.None)
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    private var latestInfo: AppUpdateInfo? = null
    private var started = false

    private val listener = InstallStateUpdatedListener { installState ->
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADING, InstallStatus.PENDING -> _state.value = AppUpdateState.Downloading
            InstallStatus.DOWNLOADED -> {
                if (_state.value != AppUpdateState.ReadyToInstall) {
                    Analytics.log(AnalyticsEvent.AppUpdate("downloaded"))
                }
                _state.value = AppUpdateState.ReadyToInstall
            }
            InstallStatus.FAILED -> {
                Analytics.log(AnalyticsEvent.AppUpdate("failed"))
                _state.value = AppUpdateState.None
            }
            InstallStatus.CANCELED -> _state.value = AppUpdateState.None
            else -> Unit
        }
    }

    /** 앱이 켜지고 첫 화면이 자리 잡은 뒤 한 번 부른다. */
    fun start() {
        if (started) return
        started = true
        manager.registerListener(listener)
        refresh(fromStart = true)
    }

    /** 앱이 다시 앞에 올 때. 뒤에서 다 받아 둔 업데이트가 있으면 [다시 시작]을 보여 준다. */
    fun onResume() {
        if (started) refresh(fromStart = false)
    }

    fun stop() {
        if (started) manager.unregisterListener(listener)
    }

    private fun refresh(fromStart: Boolean) {
        activity.lifecycleScope.launch {
            val info = runCatching { manager.requestAppUpdateInfo() }.getOrNull() ?: return@launch
            latestInfo = info
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _state.value = AppUpdateState.ReadyToInstall
                return@launch
            }
            if (!fromStart || _state.value != AppUpdateState.None) return@launch
            val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE))
            if (!available) return@launch

            val versionCode = info.availableVersionCode()
            val settings = settingsRepository.getSettings()
            val today = LocalDate.now().toEpochDay()
            val dismissedRecently = settings.updateDismissedVersionCode == versionCode &&
                today - settings.updateDismissedEpochDay < DISMISS_DAYS
            if (!dismissedRecently) _state.value = AppUpdateState.Available(versionCode)
        }
    }

    /** 배너의 [업데이트]. Play의 업데이트 확인 창을 띄운다. */
    fun startUpdate() {
        val info = latestInfo ?: return
        Analytics.log(AnalyticsEvent.AppUpdate("accept"))
        val started = runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE))
        }.getOrDefault(false)
        if (!started) return
        (manager as? FakeAppUpdateManager)?.let(::simulateDownload)
    }

    /** 확인 창에서 취소한 경우(ActivityResult가 OK가 아님) */
    fun onUpdateFlowCanceled() {
        Analytics.log(AnalyticsEvent.AppUpdate("canceled"))
        _state.value = AppUpdateState.None
    }

    /** 배너의 [다시 시작]. 설치하면서 앱이 다시 켜진다. */
    fun completeUpdate() {
        Analytics.log(AnalyticsEvent.AppUpdate("install"))
        manager.completeUpdate()
    }

    /** 배너의 닫기. 같은 버전은 며칠 동안 다시 묻지 않는다. */
    fun dismiss() {
        val current = _state.value as? AppUpdateState.Available ?: return
        Analytics.log(AnalyticsEvent.AppUpdate("later"))
        _state.value = AppUpdateState.None
        activity.lifecycleScope.launch {
            settingsRepository.markUpdateDismissed(current.versionCode, LocalDate.now().toEpochDay())
        }
    }

    /** 가짜 관리자: 수락 → 다운로드 시작 → 완료를 차례로 흉내 낸다(디버그 확인용). */
    private fun simulateDownload(fake: FakeAppUpdateManager) {
        activity.lifecycleScope.launch {
            fake.userAcceptsUpdate()
            fake.downloadStarts()
            delay(1_500)
            fake.downloadCompletes()
        }
    }

    private companion object {
        const val DISMISS_DAYS = 3
    }
}
