package com.management.subscription

import android.content.Intent
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.management.subscription.data.SettingsRepository
import com.management.subscription.notifications.NotificationPermissionHelper
import com.management.subscription.util.SubscriptionFormatters
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.management.subscription.notifications.ReminderNotifier
import com.management.subscription.notifications.ReminderScheduler
import com.management.subscription.ads.HomeBannerAdController
import com.management.subscription.analytics.Analytics
import com.management.subscription.analytics.AnalyticsEvent
import com.management.subscription.databinding.ActivityMainBinding
import com.management.subscription.splash.SplashIntro
import kotlinx.coroutines.launch
import com.management.subscription.data.BillingCycle
import com.management.subscription.subscriptionlist.SubscriptionCycleListArgs
import android.util.Log
import java.time.LocalDate

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pendingCalendarFocusDate: LocalDate? = null
    private var currentDestinationId: Int? = null
    private var mobileAdsInitialized = false
    private var adsStarted = false
    private var currentScreenName: String? = null
    /** openCalendarTab으로 들어갈 때의 진입 경로. 하단 탭으로 들어오면 비어 있다. */
    private var pendingCalendarEntry: String? = null

    private val settingsRepository by lazy(LazyThreadSafetyMode.NONE) {
        SettingsRepository.getInstance(applicationContext)
    }

    /** 권한 요청을 띄운 곳(after_save / home_banner). 결과 이벤트에 붙인다. */
    private var permissionRequestSource = "after_save"

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Analytics.log(AnalyticsEvent.NotificationPermission(granted, source = permissionRequestSource))
            if (granted) {
                enableReminders()
            } else if (NotificationPermissionHelper.isBlockedAfterDenial(this)) {
                // 시스템이 더 이상 권한 창을 띄우지 않는다. 휴대폰 설정에서 직접 켜도록 안내한다.
                showOpenSettingsSnackbar(permissionRequestSource)
            } else {
                Snackbar.make(binding.root, R.string.notification_prompt_denied, Snackbar.LENGTH_LONG).show()
            }
        }

    private val homeBannerAdController by lazy(LazyThreadSafetyMode.NONE) {
        HomeBannerAdController(this)
    }

    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.calendarFragment,
        R.id.settingsFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // 시스템 시작 화면(Theme.Gudok.Starting)을 붙이고 앱 테마로 바꾼다. super.onCreate보다 먼저 불러야 한다.
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()
        val playIntro = SplashIntro.shouldPlay(intent, isFreshStart = savedInstanceState == null)
        if (playIntro) {
            SplashIntro.play(this, splashScreen, onFinished = ::startAds)
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            currentDestinationId = destination.id
            currentScreenName = screenNameFor(destination.id, arguments)
            // Firebase는 앱이 앞에 있을 때만 화면 조회를 받는다. 시작 직후 화면은 onResume에서 보낸다.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                logCurrentScreen()
                if (destination.id == R.id.calendarFragment) {
                    Analytics.log(AnalyticsEvent.CalendarOpen(pendingCalendarEntry ?: "tab"))
                }
            }
            if (destination.id == R.id.calendarFragment) pendingCalendarEntry = null
            binding.bottomNavigation.isVisible = destination.id in topLevelDestinations
            binding.bottomNavigationDivider.isVisible = binding.bottomNavigation.isVisible
            updateBannerVisibility()
        }
        binding.adBannerContainer.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            val widthChanged = (right - left) != (oldRight - oldLeft)
            if (widthChanged && currentDestinationId == R.id.homeFragment && mobileAdsInitialized) {
                homeBannerAdController.show(binding.adBannerContainer)
            }
        }

        ReminderNotifier.ensureChannel(this)
        // 광고 SDK 초기화는 웹뷰를 띄우느라 메인 스레드를 수백 ms 붙잡는다.
        // 시작 애니메이션이 있으면 끝난 뒤에 시작해 체크가 끊기지 않게 한다.
        if (!playIntro) startAds()
        lifecycleScope.launch {
            ReminderScheduler.getInstance(applicationContext).sync()
        }
        if (savedInstanceState == null) logNotificationOpen(intent)
    }

    private fun startAds() {
        if (adsStarted || isFinishing || isDestroyed) return
        adsStarted = true
        configureAdRequestSettings()
        MobileAds.initialize(this) {
            mobileAdsInitialized = true
            Log.d(TAG, "Mobile Ads initialized. useTestAds=${BuildConfig.USE_TEST_ADS}")
            updateBannerVisibility()
        }
    }

    override fun onResume() {
        super.onResume()
        logCurrentScreen()
        if (enableRemindersOnReturn) {
            enableRemindersOnReturn = false
            if (NotificationPermissionHelper.hasNotificationPermission(this) &&
                NotificationManagerCompat.from(this).areNotificationsEnabled()
            ) {
                enableReminders()
            }
        }
        // 알림을 실제로 받는 상태인지(앱 설정 + 시스템 권한·알림 허용)를 사용자 속성으로 둔다.
        lifecycleScope.launch {
            val settings = settingsRepository.getSettings()
            val receiving = settings.notificationsEnabled &&
                NotificationPermissionHelper.hasNotificationPermission(this@MainActivity) &&
                NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled()
            Analytics.setUserProperty(Analytics.PROPERTY_NOTIFICATIONS_ON, receiving.toString())
        }
    }

    /**
     * 새 구독을 저장한 직후. 결제 알림이 꺼져 있고 아직 한 번도 묻지 않았으면
     * "결제 전에 알려 드릴까요?"를 한 번 묻는다. 알림은 기본으로 꺼져 있고 권한도 필요해서,
     * 설정 화면에 가지 않는 사용자는 핵심 기능을 영영 못 쓰게 되기 때문이다.
     */
    fun onSubscriptionAdded() {
        lifecycleScope.launch {
            val settings = settingsRepository.getSettings()
            if (settings.notificationsEnabled || settings.notificationPromptShown) return@launch
            settingsRepository.markNotificationPromptShown()
            val time = SubscriptionFormatters.reminderTime(settings.reminderHour, settings.reminderMinute)
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.notification_prompt_title)
                .setMessage(getString(R.string.notification_prompt_message, time))
                .setPositiveButton(R.string.notification_prompt_accept) { _, _ ->
                    Analytics.log(AnalyticsEvent.NotificationPrompt("accept", source = "after_save"))
                    turnOnReminders(source = "after_save")
                }
                .setNegativeButton(R.string.notification_prompt_later) { _, _ ->
                    Analytics.log(AnalyticsEvent.NotificationPrompt("later", source = "after_save"))
                }
                .show()
        }
    }

    /** 홈의 "결제 알림이 꺼져 있어요" 배너. accept면 켜고, 어느 쪽이든 다시 묻지 않는다. */
    fun onReminderBannerAnswered(accept: Boolean) {
        Analytics.log(
            AnalyticsEvent.NotificationPrompt(if (accept) "accept" else "later", source = "home_banner")
        )
        lifecycleScope.launch { settingsRepository.markNotificationPromptShown() }
        if (accept) turnOnReminders(source = "home_banner")
    }

    /** 권한이 있으면 바로 켜고, 없으면(Android 13+) 시스템 권한부터 묻는다. */
    private fun turnOnReminders(source: String) {
        if (NotificationPermissionHelper.hasNotificationPermission(this)) {
            enableReminders()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionRequestSource = source
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** 설정 화면에서 알림을 허용하고 돌아오면 바로 켜기 위해 기억해 둔다. */
    private var enableRemindersOnReturn = false

    private fun showOpenSettingsSnackbar(source: String) {
        Snackbar.make(binding.root, R.string.notification_blocked_message, Snackbar.LENGTH_LONG)
            .setAction(R.string.notification_open_settings) {
                Analytics.log(AnalyticsEvent.NotificationSettingsOpen(source))
                enableRemindersOnReturn = true
                NotificationPermissionHelper.openAppNotificationSettings(this)
            }
            .show()
    }

    private fun enableReminders() {
        Analytics.log(AnalyticsEvent.NotificationsToggled(enabled = true))
        lifecycleScope.launch {
            settingsRepository.setNotificationsEnabled(true)
            ReminderScheduler.getInstance(applicationContext).sync()
            Analytics.setUserProperty(Analytics.PROPERTY_NOTIFICATIONS_ON, "true")
        }
        Snackbar.make(binding.root, R.string.notification_prompt_enabled, Snackbar.LENGTH_SHORT).show()
    }

    private fun logCurrentScreen() {
        currentScreenName?.let { Analytics.log(AnalyticsEvent.ScreenView(it)) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        logNotificationOpen(intent)
    }

    private fun logNotificationOpen(intent: Intent?) {
        val count = intent?.getIntExtra(ReminderNotifier.EXTRA_NOTIFICATION_COUNT, 0) ?: 0
        if (count > 0) {
            Analytics.log(AnalyticsEvent.NotificationOpen(count))
            intent?.removeExtra(ReminderNotifier.EXTRA_NOTIFICATION_COUNT)
        }
    }

    override fun onDestroy() {
        homeBannerAdController.hide(binding.adBannerContainer)
        super.onDestroy()
    }

    /** entry: 어디서 열었는지(분석용). 새 구독은 fab / empty_state, 기존 구독은 home / calendar / list */
    fun openEditor(subscriptionId: String? = null, entry: String) {
        Analytics.log(
            if (subscriptionId == null) AnalyticsEvent.SubscriptionAddStart(entry)
            else AnalyticsEvent.SubscriptionOpen(entry)
        )
        val navController = findNavController(R.id.mainNavHost)
        navController.navigate(
            R.id.subscriptionEditorFragment,
            bundleOf(SubscriptionEditorArgs.KEY_SUBSCRIPTION_ID to subscriptionId)
        )
    }

    /** entry: 분석용 진입 경로(today_banner / view_all) */
    fun openCalendarTab(focusDate: LocalDate, entry: String) {
        pendingCalendarFocusDate = focusDate
        pendingCalendarEntry = entry
        binding.bottomNavigation.selectedItemId = R.id.calendarFragment
    }

    fun openSubscriptionList(cycle: BillingCycle) {
        val navController = findNavController(R.id.mainNavHost)
        navController.navigate(
            R.id.subscriptionCycleListFragment,
            bundleOf(SubscriptionCycleListArgs.KEY_BILLING_CYCLE to cycle.name)
        )
    }

    fun consumePendingCalendarFocusDate(): LocalDate? {
        val focusDate = pendingCalendarFocusDate
        pendingCalendarFocusDate = null
        return focusDate
    }

    /** targetSdk 35+는 edge-to-edge가 강제되므로 상태바·내비게이션바·키보드 영역만큼 여백을 준다. */
    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootMain) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun updateBannerVisibility() {
        val shouldShowBanner = currentDestinationId == R.id.homeFragment && mobileAdsInitialized
        Log.d(
            TAG,
            "updateBannerVisibility destination=$currentDestinationId shouldShowBanner=$shouldShowBanner"
        )
        if (shouldShowBanner) {
            homeBannerAdController.show(binding.adBannerContainer)
        } else if (currentDestinationId == R.id.homeFragment) {
            homeBannerAdController.reserve(binding.adBannerContainer)
        } else {
            homeBannerAdController.hide(binding.adBannerContainer)
        }
    }

    private fun configureAdRequestSettings() {
        val configuredTestDeviceIds = BuildConfig.ADMOB_TEST_DEVICE_IDS
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { if (it.equals("EMULATOR", ignoreCase = true)) AdRequest.DEVICE_ID_EMULATOR else it }

        val testDeviceIds = buildList {
            if (BuildConfig.USE_TEST_ADS) {
                add(AdRequest.DEVICE_ID_EMULATOR)
            }
            addAll(configuredTestDeviceIds)
        }.distinct()

        if (testDeviceIds.isNotEmpty()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
            )
        }

        Log.d(
            TAG,
            "Ad request settings configured. useTestAds=${BuildConfig.USE_TEST_ADS}, testDevices=${testDeviceIds.size}"
        )
    }

    /** 월간·연간 목록은 같은 화면이라 어느 목록인지 이름에 붙인다. */
    private fun screenNameFor(destinationId: Int, arguments: Bundle?): String? {
        if (destinationId == R.id.subscriptionCycleListFragment) {
            val cycle = arguments?.getString(SubscriptionCycleListArgs.KEY_BILLING_CYCLE)
                ?.lowercase() ?: return screenNames[destinationId]
            return "subscription_list_$cycle"
        }
        return screenNames[destinationId]
    }

    private val screenNames = mapOf(
        R.id.homeFragment to "home",
        R.id.calendarFragment to "calendar",
        R.id.settingsFragment to "settings",
        R.id.subscriptionEditorFragment to "editor",
        R.id.subscriptionDiscoveryFragment to "discovery",
        R.id.subscriptionCycleListFragment to "subscription_list"
    )

    companion object {
        private const val TAG = "AdMobSetup"
    }
}

object SubscriptionEditorArgs {
    const val KEY_SUBSCRIPTION_ID = "subscriptionId"
}
