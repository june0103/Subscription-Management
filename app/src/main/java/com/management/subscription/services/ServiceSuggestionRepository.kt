package com.management.subscription.services

import android.content.Context
import com.management.subscription.R

class ServiceSuggestionRepository private constructor(
    private val installedAppRepository: InstalledAppRepository
) {

    suspend fun preload() {
        installedAppRepository.getLauncherApps()
    }

    fun suggestionsFor(query: String, limit: Int = DEFAULT_LIMIT): List<ServiceSuggestionUiModel> {
        val queryKey = HangulSearchKey.from(query)
        if (queryKey.isBlank) {
            return emptyList()
        }

        return rankedSuggestionsFor(queryKey)
            .take(limit)
            .map(RankedServiceSuggestion::suggestion)
    }

    suspend fun discoverySuggestions(limit: Int = DEFAULT_DISCOVERY_LIMIT): List<ServiceSuggestionUiModel> {
        val installedApps = installedAppRepository.getLauncherApps()
        val ranked = linkedMapOf<String, RankedServiceSuggestion>()

        installedApps.forEach { app ->
            val matchedService = SubscriptionServiceCatalog.findInstalledAppMatch(
                label = app.label,
                packageName = app.packageName
            ) ?: return@forEach

            ranked[matchedService.key] = RankedServiceSuggestion(
                suggestion = ServiceSuggestionUiModel(
                    displayName = matchedService.displayName,
                    serviceKey = matchedService.key,
                    linkedPackageName = app.packageName,
                    sourceLabelRes = R.string.editor_suggestion_source_installed,
                    iconModel = ServiceIconModel.InstalledApp(app.packageName)
                ),
                isInstalled = true,
                matchRank = SearchMatchRank(tier = 0, prefixMatch = true)
            )
        }

        if (ranked.size < limit) {
            SubscriptionServiceCatalog.recommendedServices
                .asSequence()
                .filterNot { it.key in ranked.keys }
                .take(limit - ranked.size)
                .forEach { service ->
                    ranked[service.key] = RankedServiceSuggestion(
                        suggestion = catalogSuggestion(service),
                        isInstalled = false,
                        matchRank = SearchMatchRank(tier = 99, prefixMatch = false)
                    )
                }
        }

        return ranked.values
            .sortedWith(rankedSuggestionComparator())
            .take(limit)
            .map(RankedServiceSuggestion::suggestion)
    }

    fun resolveFreeTextIdentity(name: String): ResolvedServiceIdentity {
        val matchedService = SubscriptionServiceCatalog.findExactMatch(name)
        return ResolvedServiceIdentity(
            serviceKey = matchedService?.key,
            linkedPackageName = null
        )
    }

    private fun catalogSuggestion(service: SubscriptionServiceDefinition): ServiceSuggestionUiModel {
        return ServiceSuggestionUiModel(
            displayName = service.displayName,
            serviceKey = service.key,
            linkedPackageName = null,
            sourceLabelRes = R.string.editor_suggestion_source_catalog,
            iconModel = ServiceIconModel.BundledLogo(service.key, service.logoRes)
        )
    }

    private fun rankedSuggestionsFor(queryKey: HangulSearchKey): List<RankedServiceSuggestion> {
        val ranked = linkedMapOf<String, RankedServiceSuggestion>()
        val installedAppsByPackage = installedAppRepository
            .getCachedLauncherApps()
            .associateBy(InstalledAppInfo::packageName)

        installedAppRepository.getCachedLauncherApps().forEach { app ->
            val appRank = HangulSearchMatcher.match(queryKey, app.searchKey) ?: return@forEach
            val matchedService = SubscriptionServiceCatalog.findInstalledAppMatch(
                label = app.label,
                packageName = app.packageName
            )

            val serviceRank = matchedService?.searchKeys
                ?.mapNotNull { HangulSearchMatcher.match(queryKey, it) }
                ?.minWithOrNull(matchRankComparator())

            val bestRank = listOfNotNull(appRank, serviceRank)
                .minWithOrNull(matchRankComparator())
                ?: return@forEach

            val suggestion = if (matchedService != null) {
                ServiceSuggestionUiModel(
                    displayName = matchedService.displayName,
                    serviceKey = matchedService.key,
                    linkedPackageName = app.packageName,
                    sourceLabelRes = R.string.editor_suggestion_source_installed,
                    iconModel = ServiceIconModel.InstalledApp(app.packageName)
                )
            } else {
                ServiceSuggestionUiModel(
                    displayName = app.label,
                    serviceKey = null,
                    linkedPackageName = app.packageName,
                    sourceLabelRes = R.string.editor_suggestion_source_installed,
                    iconModel = ServiceIconModel.InstalledApp(app.packageName)
                )
            }

            mergeSuggestion(
                ranked = ranked,
                identityKey = matchedService?.key ?: "package:${app.packageName}",
                incoming = RankedServiceSuggestion(
                    suggestion = suggestion,
                    isInstalled = true,
                    matchRank = bestRank
                )
            )
        }

        SubscriptionServiceCatalog.allServices.forEach { service ->
            val serviceRank = service.searchKeys
                .mapNotNull { HangulSearchMatcher.match(queryKey, it) }
                .minWithOrNull(matchRankComparator())
                ?: return@forEach

            val installedApp = service.packageNames
                .firstNotNullOfOrNull(installedAppsByPackage::get)

            val incoming = if (installedApp != null) {
                RankedServiceSuggestion(
                    suggestion = ServiceSuggestionUiModel(
                        displayName = service.displayName,
                        serviceKey = service.key,
                        linkedPackageName = installedApp.packageName,
                        sourceLabelRes = R.string.editor_suggestion_source_installed,
                        iconModel = ServiceIconModel.InstalledApp(installedApp.packageName)
                    ),
                    isInstalled = true,
                    matchRank = serviceRank
                )
            } else {
                RankedServiceSuggestion(
                    suggestion = catalogSuggestion(service),
                    isInstalled = false,
                    matchRank = serviceRank
                )
            }

            mergeSuggestion(
                ranked = ranked,
                identityKey = service.key,
                incoming = incoming
            )
        }

        return ranked.values.sortedWith(rankedSuggestionComparator())
    }

    private fun mergeSuggestion(
        ranked: MutableMap<String, RankedServiceSuggestion>,
        identityKey: String,
        incoming: RankedServiceSuggestion
    ) {
        val current = ranked[identityKey]
        if (current == null) {
            ranked[identityKey] = incoming
            return
        }

        val preferredSuggestion = when {
            current.isInstalled && !incoming.isInstalled -> current.suggestion
            incoming.isInstalled && !current.isInstalled -> incoming.suggestion
            else -> chooseBetterRank(current, incoming).suggestion
        }

        ranked[identityKey] = RankedServiceSuggestion(
            suggestion = preferredSuggestion,
            isInstalled = current.isInstalled || incoming.isInstalled,
            matchRank = chooseBetterRank(current, incoming).matchRank
        )
    }

    private fun chooseBetterRank(
        first: RankedServiceSuggestion,
        second: RankedServiceSuggestion
    ): RankedServiceSuggestion {
        return listOf(first, second).minWithOrNull(
            compareBy<RankedServiceSuggestion>(
                { it.matchRank.tier },
                { if (it.matchRank.prefixMatch) 0 else 1 },
                { it.suggestion.displayName.length },
                { it.suggestion.displayName.lowercase() }
            )
        ) ?: first
    }

    private fun rankedSuggestionComparator(): Comparator<RankedServiceSuggestion> {
        return compareBy<RankedServiceSuggestion>(
            { if (it.isInstalled) 0 else 1 },
            { it.matchRank.tier },
            { if (it.matchRank.prefixMatch) 0 else 1 },
            { it.suggestion.displayName.length },
            { it.suggestion.displayName.lowercase() }
        )
    }

    private fun matchRankComparator(): Comparator<SearchMatchRank> {
        return compareBy<SearchMatchRank>(
            { it.tier },
            { if (it.prefixMatch) 0 else 1 }
        )
    }

    companion object {
        private const val DEFAULT_LIMIT = 8
        private const val DEFAULT_DISCOVERY_LIMIT = 24

        @Volatile
        private var instance: ServiceSuggestionRepository? = null

        fun getInstance(context: Context): ServiceSuggestionRepository {
            return instance ?: synchronized(this) {
                instance ?: ServiceSuggestionRepository(
                    InstalledAppRepository.getInstance(context.applicationContext)
                ).also { instance = it }
            }
        }
    }
}
