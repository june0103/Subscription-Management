package com.management.subscription

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
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
    }

    override fun onDestroy() {
        homeBannerAdController.hide(binding.adBannerContainer)
        super.onDestroy()
    }

    fun openEditor(subscriptionId: String? = null) {
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

    companion object {
        private const val TAG = "AdMobSetup"
    }
}

object SubscriptionEditorArgs {
    const val KEY_SUBSCRIPTION_ID = "subscriptionId"
}
