package com.eunio.healthapp.data.sync

import com.eunio.healthapp.domain.util.NetworkConnectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Service for managing offline mode indicators and graceful degradation.
 * Provides UI state for offline scenarios and manages feature availability.
 */
class OfflineModeService(
    private val networkConnectivity: NetworkConnectivity,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    
    private val _offlineState = MutableStateFlow(OfflineState())
    val offlineState: StateFlow<OfflineState> = _offlineState.asStateFlow()
    
    private val _featureAvailability = MutableStateFlow(FeatureAvailability())
    val featureAvailability: StateFlow<FeatureAvailability> = _featureAvailability.asStateFlow()
    
    private var lastConnectedTime: Instant? = null
    private var connectionLostTime: Instant? = null
    
    init {
        // Set initial state based on current connectivity
        val isInitiallyConnected = networkConnectivity.isConnected()
        updateOfflineState(isInitiallyConnected)
        updateFeatureAvailability(isInitiallyConnected)
        
        startMonitoring()
    }
    
    /**
     * Starts monitoring network connectivity and updating offline state
     */
    private fun startMonitoring() {
        coroutineScope.launch {
            networkConnectivity.observeConnectivity()
                .distinctUntilChanged()
                .collect { isConnected ->
                    updateOfflineState(isConnected)
                    updateFeatureAvailability(isConnected)
                }
        }
    }
    
    /**
     * Updates the offline state based on connectivity
     */
    private fun updateOfflineState(isConnected: Boolean) {
        val currentTime = Clock.System.now()
        
        if (isConnected) {
            lastConnectedTime = currentTime
            connectionLostTime = null
            
            _offlineState.value = OfflineState(
                isOffline = false,
                lastConnectedTime = currentTime,
                offlineDuration = null,
                networkType = networkConnectivity.getNetworkType(),
                showOfflineIndicator = false
            )
        } else {
            if (connectionLostTime == null) {
                connectionLostTime = currentTime
            }
            
            val offlineDuration = lastConnectedTime?.let { currentTime - it }
            
            _offlineState.value = OfflineState(
                isOffline = true,
                lastConnectedTime = lastConnectedTime,
                connectionLostTime = connectionLostTime,
                offlineDuration = offlineDuration,
                networkType = com.eunio.healthapp.domain.util.NetworkType.NONE,
                showOfflineIndicator = true
            )
        }
    }
    
    /**
     * Updates feature availability based on connectivity and offline duration
     */
    private fun updateFeatureAvailability(isConnected: Boolean) {
        val currentState = _offlineState.value
        val offlineDuration = currentState.offlineDuration
        
        _featureAvailability.value = FeatureAvailability(
            canSync = isConnected,
            canGenerateReports = isConnected || (offlineDuration != null && offlineDuration < Duration.parse("1h")),
            canViewInsights = isConnected || hasLocalInsights(),
            canExportData = isConnected,
            canBackupData = isConnected,
            canAccessHelp = isConnected || hasOfflineHelp(),
            showLimitedFunctionalityWarning = !isConnected && (offlineDuration == null || offlineDuration > 30.seconds),
            degradedFeatures = getDegradedFeatures(isConnected, offlineDuration)
        )
    }
    
    /**
     * Checks if local insights are available
     */
    private fun hasLocalInsights(): Boolean {
        // This would check local database for cached insights
        // For now, return true as placeholder
        return true
    }
    
    /**
     * Checks if offline help content is available
     */
    private fun hasOfflineHelp(): Boolean {
        // This would check for cached help content
        // For now, return true as placeholder
        return true
    }
    
    /**
     * Gets list of features that are degraded in offline mode
     */
    private fun getDegradedFeatures(isConnected: Boolean, offlineDuration: Duration?): List<DegradedFeature> {
        if (isConnected) return emptyList()
        
        val degradedFeatures = mutableListOf<DegradedFeature>()
        
        degradedFeatures.add(
            DegradedFeature(
                feature = "Data Synchronization",
                reason = "No network connection",
                impact = "Changes will sync when connection is restored",
                severity = FeatureSeverity.HIGH
            )
        )
        
        degradedFeatures.add(
            DegradedFeature(
                feature = "Insight Generation",
                reason = "Requires server processing",
                impact = "New insights unavailable until online",
                severity = FeatureSeverity.MEDIUM
            )
        )
        
        if (offlineDuration != null && offlineDuration > Duration.parse("1h")) {
            degradedFeatures.add(
                DegradedFeature(
                    feature = "Report Generation",
                    reason = "Extended offline period",
                    impact = "Reports may be outdated",
                    severity = FeatureSeverity.LOW
                )
            )
        }
        
        return degradedFeatures
    }
    
    /**
     * Gets user-friendly offline message based on current state
     */
    fun getOfflineMessage(): OfflineMessage {
        val state = _offlineState.value
        
        return when {
            !state.isOffline -> OfflineMessage(
                title = "Connected",
                message = "All features available",
                type = OfflineMessageType.CONNECTED
            )
            
            state.offlineDuration == null || state.offlineDuration < 30.seconds -> OfflineMessage(
                title = "Connection Lost",
                message = "Trying to reconnect...",
                type = OfflineMessageType.RECONNECTING
            )
            
            state.offlineDuration < Duration.parse("5m") -> OfflineMessage(
                title = "Working Offline",
                message = "Your data is saved locally and will sync when connection is restored",
                type = OfflineMessageType.SHORT_OFFLINE
            )
            
            state.offlineDuration < Duration.parse("1h") -> OfflineMessage(
                title = "Extended Offline Mode",
                message = "Some features may be limited. Data will sync when you're back online",
                type = OfflineMessageType.EXTENDED_OFFLINE
            )
            
            else -> OfflineMessage(
                title = "Long Offline Period",
                message = "You've been offline for a while. Some data may be outdated",
                type = OfflineMessageType.LONG_OFFLINE
            )
        }
    }
    
    /**
     * Checks if a specific feature is available in current state
     */
    fun isFeatureAvailable(feature: AppFeature): Boolean {
        val availability = _featureAvailability.value
        
        return when (feature) {
            AppFeature.DATA_LOGGING -> true // Always available offline
            AppFeature.CALENDAR_VIEW -> true // Always available offline
            AppFeature.BBT_CHARTING -> true // Always available offline
            AppFeature.DATA_SYNC -> availability.canSync
            AppFeature.INSIGHT_GENERATION -> availability.canViewInsights
            AppFeature.REPORT_GENERATION -> availability.canGenerateReports
            AppFeature.DATA_EXPORT -> availability.canExportData
            AppFeature.HELP_CONTENT -> availability.canAccessHelp
        }
    }
}

/**
 * Represents the current offline state
 */
data class OfflineState(
    val isOffline: Boolean = false,
    val lastConnectedTime: Instant? = null,
    val connectionLostTime: Instant? = null,
    val offlineDuration: Duration? = null,
    val networkType: com.eunio.healthapp.domain.util.NetworkType = com.eunio.healthapp.domain.util.NetworkType.NONE,
    val showOfflineIndicator: Boolean = false
)

/**
 * Represents feature availability in current state
 */
data class FeatureAvailability(
    val canSync: Boolean = true,
    val canGenerateReports: Boolean = true,
    val canViewInsights: Boolean = true,
    val canExportData: Boolean = true,
    val canBackupData: Boolean = true,
    val canAccessHelp: Boolean = true,
    val showLimitedFunctionalityWarning: Boolean = false,
    val degradedFeatures: List<DegradedFeature> = emptyList()
)

/**
 * Represents a feature that has degraded functionality
 */
data class DegradedFeature(
    val feature: String,
    val reason: String,
    val impact: String,
    val severity: FeatureSeverity
)

/**
 * Severity levels for degraded features
 */
enum class FeatureSeverity {
    LOW, MEDIUM, HIGH
}

/**
 * Types of offline messages
 */
enum class OfflineMessageType {
    CONNECTED,
    RECONNECTING,
    SHORT_OFFLINE,
    EXTENDED_OFFLINE,
    LONG_OFFLINE
}

/**
 * Offline message for user display
 */
data class OfflineMessage(
    val title: String,
    val message: String,
    val type: OfflineMessageType
)

/**
 * App features that can be checked for availability
 */
enum class AppFeature {
    DATA_LOGGING,
    CALENDAR_VIEW,
    BBT_CHARTING,
    DATA_SYNC,
    INSIGHT_GENERATION,
    REPORT_GENERATION,
    DATA_EXPORT,
    HELP_CONTENT
}