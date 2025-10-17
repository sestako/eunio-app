package com.eunio.healthapp.domain.repository

import com.eunio.healthapp.domain.model.support.*
import kotlinx.coroutines.flow.Flow

interface HelpSupportRepository {
    suspend fun getHelpCategories(): Result<List<HelpCategory>>
    suspend fun searchFAQs(query: String): Result<List<FAQ>>
    suspend fun getFAQsByCategory(categoryId: String): Result<List<FAQ>>
    
    suspend fun submitSupportRequest(request: SupportRequest): Result<String>
    suspend fun getSupportRequests(userId: String): Result<List<SupportRequest>>
    
    suspend fun getTutorials(): Result<List<Tutorial>>
    suspend fun getTutorialsByCategory(category: TutorialCategory): Result<List<Tutorial>>
    suspend fun markTutorialCompleted(tutorialId: String, stepId: String? = null): Result<Unit>
    
    suspend fun getDeviceInfo(): DeviceInfo
    suspend fun getAppInfo(): AppInfo
    suspend fun collectDiagnosticLogs(): Result<String>
}