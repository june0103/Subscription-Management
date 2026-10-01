package com.management.subscription.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.management.subscription.R

/** 카드 안에 쌓인 행 사이에 line 색 1dp 구분선을 긋는다. 마지막 행 아래에는 긋지 않는다. */
class LineDividerDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply { color = ContextCompat.getColor(context, R.color.line) }
    private val height = context.resources.displayMetrics.density.coerceAtLeast(1f)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val last = (parent.adapter?.itemCount ?: 0) - 1
        if (position in 0 until last) outRect.bottom = height.toInt()
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val last = (parent.adapter?.itemCount ?: 0) - 1
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (parent.getChildAdapterPosition(child) >= last) continue
            val top = child.bottom.toFloat()
            canvas.drawRect(
                parent.paddingLeft.toFloat(),
                top,
                (parent.width - parent.paddingRight).toFloat(),
                top + height,
                paint
            )
        }
    }
}
