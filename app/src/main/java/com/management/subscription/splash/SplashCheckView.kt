package com.management.subscription.splash

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.management.subscription.R

/**
 * 시작 화면 로고 위에 코럴색 체크를 그려 나가는 뷰.
 *
 * 좌표는 로고 원본(512 단위) 기준이다. 뷰는 288dp 캔버스이고 그 가운데 152dp에 로고가 있으므로
 * splash_icon.png와 같은 자리·크기로 겹친다. [progress]를 0→1로 바꾸면 체크가 왼쪽 끝부터 그려진다.
 *
 * 그림자의 흐림(BlurMaskFilter)은 하드웨어 가속으로 그릴 수 없어서, 점점 넓고 옅어지는 선 몇 겹으로
 * 흉내 낸다. 그림자·체크·하이라이트가 모두 같은 진행도로 그려지므로 그림자가 체크보다 앞서지 않고,
 * 전부 GPU에서 그려져 앱이 막 켜지는 바쁜 순간에도 가볍다.
 */
class SplashCheckView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val checkPaint = stroke()
    private val highlightPaint = stroke().apply {
        color = ContextCompat.getColor(context, R.color.splash_check_highlight)
    }
    private val shadowPaints = List(SHADOW_LAYERS) { stroke() }

    private val checkPath = Path()
    private val highlightPath = Path()
    private val shadowPath = Path()
    private val segment = Path()
    private val warmUpPaints by lazy(LazyThreadSafetyMode.NONE) {
        (shadowPaints + checkPaint + highlightPaint).map { Paint(it).apply { alpha = 1 } }
    }
    private val warmUpPaths by lazy(LazyThreadSafetyMode.NONE) {
        List(shadowPaints.size) { shadowPath } + checkPath + highlightPath
    }
    private val measure = PathMeasure()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val logoSize = w * LOGO_FRACTION
        val unit = logoSize / 512f
        val offset = (w - logoSize) / 2f
        fun Path.points(vararg p: Float) {
            reset()
            moveTo(offset + p[0] * unit, offset + p[1] * unit)
            lineTo(offset + p[2] * unit, offset + p[3] * unit)
            lineTo(offset + p[4] * unit, offset + p[5] * unit)
        }
        checkPath.points(156f, 296f, 229f, 357f, 356f, 229f)
        highlightPath.points(162f, 290f, 229f, 345f, 350f, 223f)
        shadowPath.points(161f, 304f, 234f, 365f, 361f, 237f)

        checkPaint.strokeWidth = 42f * unit
        checkPaint.shader = LinearGradient(
            offset + 156f * unit, offset + 229f * unit,
            offset + 356f * unit, offset + 357f * unit,
            ContextCompat.getColor(context, R.color.splash_check_start),
            ContextCompat.getColor(context, R.color.splash_check_end),
            Shader.TileMode.CLAMP
        )
        highlightPaint.strokeWidth = 8f * unit

        // 원본 그림자(굵기 46, 흐림 17, 불투명도 45%)를 넓은 것부터 겹쳐 그려 가장자리가 부드럽게 빠지게 한다.
        val shadowColor = ContextCompat.getColor(context, R.color.splash_check_shadow)
        val layerAlpha = (Color.alpha(shadowColor) * 1.1f / SHADOW_LAYERS).toInt()
        shadowPaints.forEachIndexed { i, paint ->
            val spread = 1f - i / (SHADOW_LAYERS - 1f) // 1 → 0
            paint.strokeWidth = (46f + 34f * spread) * unit
            paint.color = shadowColor
            paint.alpha = layerAlpha
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (progress <= 0f) {
            // 시작 전(정지 로고)에는 완성된 체크를 눈에 안 보이는 투명도로 미리 그려 둔다.
            // 그라데이션·굵은 선을 GPU가 처음 그릴 때의 준비 비용을 여기서 치러, 체크가 출발하는 순간 멈칫하지 않게 한다.
            warmUpPaints.forEachIndexed { i, paint -> canvas.drawPath(warmUpPaths[i], paint) }
            return
        }
        partial(shadowPath).let { path -> shadowPaints.forEach { canvas.drawPath(path, it) } }
        canvas.drawPath(partial(checkPath), checkPaint)
        canvas.drawPath(partial(highlightPath), highlightPaint)
    }

    /** [path]의 앞에서부터 [progress]만큼을 잘라 낸다(같은 Path 객체를 재사용). */
    private fun partial(path: Path): Path {
        measure.setPath(path, false)
        segment.reset()
        measure.getSegment(0f, measure.length * progress, segment, true)
        return segment
    }

    private fun stroke() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private companion object {
        /** splash_icon.png: 288dp 캔버스 가운데 152dp가 로고 */
        const val LOGO_FRACTION = 152f / 288f
        const val SHADOW_LAYERS = 5
    }
}
