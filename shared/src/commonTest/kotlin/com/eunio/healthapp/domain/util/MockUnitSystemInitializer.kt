package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.manager.UnitSystemInitializer
import com.eunio.healthapp.domain.model.UnitSystem

/**
 * Mock implementation of UnitSystemInitializer for testing
 */
class MockUnitSystemInitializer : UnitSystemInitializer {
    var initializeForNewUserResult: Result<UnitSystem> = Result.success(UnitSystem.METRIC)
    var handleLocaleChangeResult: Result<UnitSystem> = Result.success(UnitSystem.METRIC)
    var currentLocaleUnitSystem: UnitSystem = UnitSystem.METRIC
    
    var initializeForNewUserCalled = false
    var handleLocaleChangeCalled = false
    var getUnitSystemForCurrentLocaleCalled = false
    
    override suspend fun initializeForNewUser(): Result<UnitSystem> {
        initializeForNewUserCalled = true
        return initializeForNewUserResult
    }
    
    override suspend fun handleLocaleChange(): Result<UnitSystem> {
        handleLocaleChangeCalled = true
        return handleLocaleChangeResult
    }
    
    override fun getUnitSystemForCurrentLocale(): UnitSystem {
        getUnitSystemForCurrentLocaleCalled = true
        return currentLocaleUnitSystem
    }
    
    fun reset() {
        initializeForNewUserCalled = false
        handleLocaleChangeCalled = false
        getUnitSystemForCurrentLocaleCalled = false
        initializeForNewUserResult = Result.success(UnitSystem.METRIC)
        handleLocaleChangeResult = Result.success(UnitSystem.METRIC)
        currentLocaleUnitSystem = UnitSystem.METRIC
    }
}