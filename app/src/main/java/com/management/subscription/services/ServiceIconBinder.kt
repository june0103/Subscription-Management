package com.management.subscription.services

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.management.subscription.R

object ServiceIconBinder {

    fun bind(
        context: Context,
        imageView: ImageView,
        badgeView: TextView,
        iconModel: ServiceIconModel
    ) {
        when (iconModel) {
            is ServiceIconModel.InstalledApp -> {
                val drawable = InstalledAppRepository.getInstance(context)
                    .getApplicationIcon(iconModel.packageName)
                if (drawable != null) {
                    badgeView.isVisible = false
                    badgeView.backgroundTintList = null
                    imageView.isVisible = true
                    imageView.setImageDrawable(drawable)
                } else {
                    imageView.isVisible = false
                    badgeView.isVisible = true
                    badgeView.text = "?"
                    badgeView.backgroundTintList = null
                    badgeView.setTextColor(ContextCompat.getColor(context, R.color.ink_3))
                }
            }

            is ServiceIconModel.BundledLogo -> {
                badgeView.isVisible = false
                badgeView.backgroundTintList = null
                imageView.isVisible = true
                imageView.setImageResource(iconModel.logoRes)
            }

            is ServiceIconModel.Badge -> {
                imageView.isVisible = false
                badgeView.isVisible = true
                badgeView.text = iconModel.text
                badgeView.backgroundTintList = null
                badgeView.setTextColor(ContextCompat.getColor(context, iconModel.accentColorRes))
            }
        }
    }
}
