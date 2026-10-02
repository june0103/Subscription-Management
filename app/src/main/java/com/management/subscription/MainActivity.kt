package com.management.subscription

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
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
    private var currentScreenName: String? = null

    private val homeBannerAdController by lazy(LazyThreadSafetyMode.NONE) {
        HomeBannerAdController(this)
    }

    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.calendarFragment,
        R.id.settingsFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentDestinationId = destination.id
            currentScreenName = screenNames[destination.id]
            // Firebase는 앱이 앞에 있을 때만 화면 조회를 받는다. 시작 직후 화면은 onResume에서 보낸다.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) logCurrentScreen()
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
        configureAdRequestSettings()
        MobileAds.initialize(this) {
            mobileAdsInitialized = true
            Log.d(TAG, "Mobile Ads initialized. useTestAds=${BuildConfig.USE_TEST_ADS}")
            updateBannerVisibility()
        }
        lifecycleScope.launch {
            ReminderScheduler.getInstance(applicationContext).sync()
        }
        if (savedInstanceState == null) logNotificationOpen(intent)
    }

    override fun onResume() {
        super.onResume()
        logCurrentScreen()
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

    fun openCalendarTab(focusDate: LocalDate) {
        pendingCalendarFocusDate = focusDate
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
