package com.management.subscription.ads

import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.management.subscription.BuildConfig

class HomeBannerAdController(
    private val activity: AppCompatActivity
) {

    private var adView: AdView? = null
    private var lastAdWidthDp: Int = -1

    fun show(container: FrameLayout) {
        val adWidthDp = calculateAdWidthDp(container)
        if (adWidthDp <= 0) {
            Log.w(TAG, "Skipping banner load because widthDp=$adWidthDp")
            return
        }

        if (adView != null && lastAdWidthDp == adWidthDp) {
            container.isVisible = true
            Log.d(TAG, "Reusing home banner widthDp=$adWidthDp")
            return
        }

        destroyBanner()
        container.removeAllViews()
        container.isVisible = false

        val nextAdView = AdView(activity).apply {
            adUnitId = BuildConfig.ADMOB_HOME_BANNER_AD_UNIT_ID
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    activity,
                    adWidthDp
                )
            )
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    if (adView !== this@apply) return
                    container.removeAllViews()
                    container.addView(
                        this@apply,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER
                        )
                    )
                    container.isVisible = true
                    Log.d(
                        TAG,
                        "Home banner loaded. unit=${maskedAdUnitId()} widthDp=$adWidthDp"
                    )
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (adView !== this@apply) return
                    Log.w(
                        TAG,
                        "Home banner failed. unit=${maskedAdUnitId()} widthDp=$adWidthDp " +
                            "code=${error.code} domain=${error.domain} message=${error.message} " +
                            "adapter=${error.responseInfo?.mediationAdapterClassName} " +
                            "responseId=${error.responseInfo?.responseId}"
                    )
                    container.removeAllViews()
                    container.isVisible = false
                    destroyBanner()
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Home banner impression")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Home banner clicked")
                }
            }
        }

        adView = nextAdView
        lastAdWidthDp = adWidthDp
        Log.d(
            TAG,
            "Loading home banner. unit=${nextAdView.maskedAdUnitId()} widthDp=$adWidthDp testAds=${BuildConfig.USE_TEST_ADS}"
        )
        nextAdView.setOnPaidEventListener {
            Log.d(TAG, "Home banner paid event received")
        }
        nextAdView.loadAd(AdRequest.Builder().build())
    }

    fun hide(container: FrameLayout) {
        destroyBanner()
        container.removeAllViews()
        container.isVisible = false
    }

    private fun destroyBanner() {
        adView?.destroy()
        adView = null
        lastAdWidthDp = -1
    }

    private fun calculateAdWidthDp(container: FrameLayout): Int {
        val displayMetrics = activity.resources.displayMetrics
        val adWidthPixels = container.width.takeIf { it > 0 }
            ?: activity.findViewById<View>(android.R.id.content)?.width?.takeIf { it > 0 }
            ?: displayMetrics.widthPixels
        return (adWidthPixels / displayMetrics.density).toInt().coerceAtLeast(1)
    }

    companion object {
        private const val TAG = "HomeBannerAd"
    }

    private fun AdView.maskedAdUnitId(): String {
        return adUnitId.takeIf { it.length > 8 }
            ?.let { "${it.take(8)}..." }
            ?: adUnitId
    }
}
