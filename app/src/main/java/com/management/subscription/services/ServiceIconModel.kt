package com.management.subscription.services

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

sealed interface ServiceIconModel {

    data class InstalledApp(
        val packageName: String
    ) : ServiceIconModel

    data class BundledLogo(
        val serviceKey: String,
        @DrawableRes val logoRes: Int
    ) : ServiceIconModel

    data class Badge(
        val text: String,
        @ColorRes val accentColorRes: Int
    ) : ServiceIconModel
}
