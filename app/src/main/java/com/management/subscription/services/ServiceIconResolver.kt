package com.management.subscription.services

import android.content.Context
import com.management.subscription.data.SubscriptionPreview

object ServiceIconResolver {

    fun resolve(
        context: Context,
        subscription: SubscriptionPreview
    ): ServiceIconModel {
        val installedAppRepository = InstalledAppRepository.getInstance(context)
        val linkedPackageName = subscription.linkedPackageName

        if (!linkedPackageName.isNullOrBlank() &&
            installedAppRepository.isPackageInstalled(linkedPackageName)
        ) {
            return ServiceIconModel.InstalledApp(linkedPackageName)
        }

        val service = SubscriptionServiceCatalog.findByKey(subscription.serviceKey)
        if (service != null) {
            return ServiceIconModel.BundledLogo(service.key, service.logoRes)
        }

        return ServiceIconModel.Badge(
            text = subscription.badge,
            accentColorRes = subscription.accentColorRes
        )
    }
}
