package com.eunio.healthapp.domain.usecase.support

import com.eunio.healthapp.domain.model.support.SupportRequest
import com.eunio.healthapp.domain.model.support.SupportRequestType
import com.eunio.healthapp.domain.repository.HelpSupportRepository
import kotlinx.datetime.Clock
import kotlin.random.Random

class SubmitSupportRequestUseCase(
    private val helpSupportRepository: HelpSupportRepository
) {
    suspend operator fun invoke(
        userId: String,
        type: SupportRequestType,
        subject: String,
        description: String,
        attachLogs: Boolean = false
    ): Result<String> {
        return try {
            val deviceInfo = helpSupportRepository.getDeviceInfo()
            val appInfo = helpSupportRepository.getAppInfo()
            
            val request = SupportRequest(
                id = Random.nextLong().toString(),
                userId = userId,
                type = type,
                subject = subject.trim(),
                description = description.trim(),
                deviceInfo = deviceInfo,
                appInfo = appInfo,
                attachLogs = attachLogs,
                createdAt = Clock.System.now()
            )
            
            helpSupportRepository.submitSupportRequest(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}