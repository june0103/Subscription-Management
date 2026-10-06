package com.management.subscription.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import androidx.core.splashscreen.SplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import com.management.subscription.databinding.ViewSplashOverlayBinding

/**
 * 시스템 시작 화면(체크 없는 로고)을 같은 자리에서 이어받아
 * 체크 그리기 → 로고가 살짝 올라가며 '구독체크'·소개 문구 등장 → 홈으로 페이드 순서로 보여준다.
 * 애니메이션은 약 1.3초(앱 준비가 늦으면 정지 로고로 최대 0.45초 더 기다림). 오버레이는 홈 화면 위에 덮여 있어서 그동안 홈은 뒤에서 미리 그려진다.
 */
object SplashIntro {

    /** 런처 아이콘으로 새로 켰을 때만 보여준다. 알림을 눌러 연 경우 등은 바로 화면을 보여준다. */
    fun shouldPlay(intent: Intent?, isFreshStart: Boolean): Boolean {
        if (!isFreshStart || intent == null) return false
        if (intent.action != Intent.ACTION_MAIN || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)) return false
        // 개발자 옵션·접근성에서 애니메이션을 끈 사용자는 기다리게 하지 않는다.
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }

    /** [onFinished]는 애니메이션이 끝나 홈이 드러난 뒤 불린다(미뤄 둔 무거운 초기화용). */
    fun play(activity: Activity, splashScreen: SplashScreen, onFinished: () -> Unit) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val binding = ViewSplashOverlayBinding.inflate(activity.layoutInflater, content, false)
        content.addView(binding.root)

        // 오버레이가 첫 화면에 그려진 뒤 시스템 시작 화면을 바로 걷는다. 둘은 같은 모습이라 이음새가 없다.
        splashScreen.setOnExitAnimationListener { provider -> provider.remove() }

        // 남색 배경 위에서는 상태바·내비게이션바 아이콘을 밝게 둔다.
        val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val lightStatusBars = insetsController.isAppearanceLightStatusBars
        val lightNavigationBars = insetsController.isAppearanceLightNavigationBars
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        binding.root.doOnPreDraw {
            // 첫 화면 직후에는 홈 화면 준비로 메인 스레드가 바쁘다(에뮬레이터 기준 약 0.4초).
            // 그 사이에는 시스템 시작 화면과 같은 정지 로고를 보여 주고, 한가해지면 체크를 시작한다.
            startWhenIdle(binding.root) {
                buildAnimation(binding).apply {
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            insetsController.isAppearanceLightStatusBars = lightStatusBars
                            insetsController.isAppearanceLightNavigationBars = lightNavigationBars
                            content.removeView(binding.root)
                            onFinished()
                        }
                    })
                    start()
                }
            }
        }
    }

    /** 메인 스레드 메시지 큐가 빌 때 [action]을 한 번 실행한다. 오래 바쁘면 [MAX_WAIT_MS] 뒤에 그냥 실행한다. */
    private fun startWhenIdle(view: View, action: () -> Unit) {
        var started = false
        fun runOnce() {
            if (started) return
            started = true
            action()
        }
        val fallback = Runnable { runOnce() }
        view.postDelayed(fallback, MAX_WAIT_MS)
        Looper.myQueue().addIdleHandler {
            view.removeCallbacks(fallback)
            runOnce()
            false
        }
    }

    private fun buildAnimation(binding: ViewSplashOverlayBinding): Animator {
        val density = binding.root.resources.displayMetrics.density
        val logoHalf = LOGO_DP / 2f * density
        val gap = TEXT_GAP_DP * density
        val textsHeight = binding.splashTexts.height.toFloat()

        // 로고 + 글자 묶음이 화면 가운데에 오도록 로고를 올린다.
        val logoShift = (gap + textsHeight) / 2f
        // 글자 묶음은 처음에 화면 가운데에 놓여 있으므로 로고(이동 후) 바로 아래로 옮긴다.
        val textsTop = logoHalf - logoShift + gap
        val textsBaseY = textsTop + textsHeight / 2f
        val rise = 14f * density
        binding.splashTexts.translationY = textsBaseY

        val standard = PathInterpolator(0.2f, 0f, 0f, 1f)

        val glow = ObjectAnimator.ofFloat(binding.splashGlow, View.ALPHA, 0.01f, 1f).apply {
            duration = 500
        }
        // 이름으로 찾는 ObjectAnimator는 릴리스 난독화에 깨지므로 직접 갱신한다.
        val check = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { binding.splashCheck.progress = it.animatedValue as Float }
            duration = 460
            // 펜으로 긋듯 천천히 출발해 가속했다가 끝에서 부드럽게 멈춘다.
            interpolator = PathInterpolator(0.45f, 0f, 0.2f, 1f)
        }
        val logoUp = ObjectAnimator.ofFloat(binding.splashLogo, View.TRANSLATION_Y, 0f, -logoShift).apply {
            startDelay = 400
            duration = 420
            interpolator = standard
        }
        val title = textIn(binding.splashTitle, rise, 460, standard)
        val tagline = textIn(binding.splashTagline, rise, 560, standard)
        // 화면 전체를 흐리게 하려면 오버레이를 한 장의 레이어로 그려야 한다. 레이어를 만드는 순간 잠깐 멈추므로
        // 글자가 다 나온 뒤 정지해 있는 동안 미리 만들어 둔다.
        val prepareFade = ValueAnimator.ofFloat(0f, 1f).apply {
            startDelay = 940
            duration = 1
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    binding.root.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    binding.root.buildLayer()
                }
            })
        }
        val fadeOut = ObjectAnimator.ofFloat(binding.root, View.ALPHA, 1f, 0f).apply {
            startDelay = 1060
            duration = 240
        }
        return AnimatorSet().apply { playTogether(glow, check, logoUp, title, tagline, prepareFade, fadeOut) }
    }

    private fun textIn(view: View, rise: Float, delay: Long, interpolator: PathInterpolator): Animator {
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
        val move = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, rise, 0f)
        return AnimatorSet().apply {
            playTogether(alpha, move)
            startDelay = delay
            duration = 360
            this.interpolator = interpolator
        }
    }

    /** splash_icon.png 안의 로고 크기 */
    private const val LOGO_DP = 152f
    private const val TEXT_GAP_DP = 28f
    private const val MAX_WAIT_MS = 450L
}
