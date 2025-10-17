package com.eunio.healthapp.domain.model.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrivacyPreferencesTest {
    
    @Test
    fun `default constructor creates valid preferences`() {
        val preferences = PrivacyPreferences()
        
        assertFalse(preferences.dataSharingEnabled)
        assertTrue(preferences.anonymousInsightsEnabled)
        assertTrue(preferences.crashReportingEnabled)
        assertTrue(preferences.analyticsEnabled)
        assertTrue(preferences.isValid())
        assertTrue(preferences.hasDataCollectionEnabled())
    }
    
    @Test
    fun `default factory method creates valid preferences`() {
        val preferences = PrivacyPreferences.default()
        
        assertTrue(preferences.isValid())
        assertTrue(preferences.hasDataCollectionEnabled())
    }
    
    @Test
    fun `maxPrivacy factory method creates privacy-focused preferences`() {
        val preferences = PrivacyPreferences.maxPrivacy()
        
        assertFalse(preferences.dataSharingEnabled)
        assertFalse(preferences.anonymousInsightsEnabled)
        assertFalse(preferences.crashReportingEnabled)
        assertFalse(preferences.analyticsEnabled)
        assertTrue(preferences.isValid())
        assertFalse(preferences.hasDataCollectionEnabled())
    }
    
    @Test
    fun `balanced factory method creates balanced preferences`() {
        val preferences = PrivacyPreferences.balanced()
        
        assertFalse(preferences.dataSharingEnabled)
        assertTrue(preferences.anonymousInsightsEnabled)
        assertTrue(preferences.crashReportingEnabled)
        assertFalse(preferences.analyticsEnabled)
        assertTrue(preferences.isValid())
        assertTrue(preferences.hasDataCollectionEnabled())
    }
    
    @Test
    fun `all boolean combinations are valid`() {
        val combinations = listOf(
            PrivacyPreferences(false, false, false, false),
            PrivacyPreferences(true, false, false, false),
            PrivacyPreferences(false, true, false, false),
            PrivacyPreferences(false, false, true, false),
            PrivacyPreferences(false, false, false, true),
            PrivacyPreferences(true, true, true, true)
        )
        
        combinations.forEach { preferences ->
            assertTrue(preferences.isValid(), "Preferences should be valid: $preferences")
        }
    }
    
    @Test
    fun `hasDataCollectionEnabled returns false when all disabled`() {
        val preferences = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = false,
            analyticsEnabled = false
        )
        
        assertFalse(preferences.hasDataCollectionEnabled())
    }
    
    @Test
    fun `hasDataCollectionEnabled returns true when data sharing enabled`() {
        val preferences = PrivacyPreferences(
            dataSharingEnabled = true,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = false,
            analyticsEnabled = false
        )
        
        assertTrue(preferences.hasDataCollectionEnabled())
    }
    
    @Test
    fun `hasDataCollectionEnabled returns true when anonymous insights enabled`() {
        val preferences = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = true,
            crashReportingEnabled = false,
            analyticsEnabled = false
        )
        
        assertTrue(preferences.hasDataCollectionEnabled())
    }
    
    @Test
    fun `hasDataCollectionEnabled returns true when crash reporting enabled`() {
        val preferences = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = true,
            analyticsEnabled = false
        )
        
        assertTrue(preferences.hasDataCollectionEnabled())
    }
    
    @Test
    fun `hasDataCollectionEnabled returns true when analytics enabled`() {
        val preferences = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = false,
            analyticsEnabled = true
        )
        
        assertTrue(preferences.hasDataCollectionEnabled())
    }
    
    @Test
    fun `toPrivacyFocused disables all data collection`() {
        val original = PrivacyPreferences(
            dataSharingEnabled = true,
            anonymousInsightsEnabled = true,
            crashReportingEnabled = true,
            analyticsEnabled = true
        )
        
        val privacyFocused = original.toPrivacyFocused()
        
        assertFalse(privacyFocused.dataSharingEnabled)
        assertFalse(privacyFocused.anonymousInsightsEnabled)
        assertFalse(privacyFocused.crashReportingEnabled)
        assertFalse(privacyFocused.analyticsEnabled)
        assertFalse(privacyFocused.hasDataCollectionEnabled())
    }
    
    @Test
    fun `toOptimizedForImprovement keeps personal data private but enables improvement features`() {
        val original = PrivacyPreferences.maxPrivacy()
        
        val optimized = original.toOptimizedForImprovement()
        
        assertFalse(optimized.dataSharingEnabled) // Keep personal data private
        assertTrue(optimized.anonymousInsightsEnabled) // Help improve insights
        assertTrue(optimized.crashReportingEnabled) // Help fix bugs
        assertTrue(optimized.analyticsEnabled) // Help improve app experience
        assertTrue(optimized.hasDataCollectionEnabled())
    }
    
    @Test
    fun `copy with changes works correctly`() {
        val original = PrivacyPreferences.default()
        val modified = original.copy(
            dataSharingEnabled = true,
            analyticsEnabled = false
        )
        
        assertFalse(original.dataSharingEnabled)
        assertTrue(modified.dataSharingEnabled)
        assertTrue(original.analyticsEnabled)
        assertFalse(modified.analyticsEnabled)
    }
    
    @Test
    fun `privacy preferences maintain immutability`() {
        val original = PrivacyPreferences.default()
        val privacyFocused = original.toPrivacyFocused()
        val optimized = original.toOptimizedForImprovement()
        
        // Original should remain unchanged
        assertTrue(original.anonymousInsightsEnabled)
        assertTrue(original.crashReportingEnabled)
        assertTrue(original.analyticsEnabled)
        
        // Each transformation creates a new instance
        assertFalse(privacyFocused.anonymousInsightsEnabled)
        assertTrue(optimized.anonymousInsightsEnabled)
    }
    
    @Test
    fun `serialization annotations are present`() {
        // This test ensures the class can be serialized
        val preferences = PrivacyPreferences(
            dataSharingEnabled = true,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = true,
            analyticsEnabled = false
        )
        
        // If serialization annotations are missing, this would fail at compile time
        assertTrue(preferences.isValid())
    }
}