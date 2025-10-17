package com.eunio.healthapp.domain.model.support

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class SupportRequest(
    val id: String,
    val userId: String,
    val type: SupportRequestType,
    val subject: String,
    val description: String,
    val deviceInfo: DeviceInfo,
    val appInfo: AppInfo,
    val attachLogs: Boolean = false,
    val createdAt: Instant,
    val status: SupportRequestStatus = SupportRequestStatus.PENDING
)

@Serializable
data class DeviceInfo(
    val platform: String,
    val osVersion: String,
    val deviceModel: String,
    val screenSize: String,
    val locale: String
)

@Serializable
data class AppInfo(
    val version: String,
    val buildNumber: String,
    val installDate: Instant?,
    val lastUpdateDate: Instant?
)

enum class SupportRequestType(val displayName: String) {
    GENERAL_INQUIRY("General Inquiry"),
    BUG_REPORT("Bug Report"),
    FEATURE_REQUEST("Feature Request"),
    ACCOUNT_ISSUE("Account Issue"),
    DATA_CONCERN("Data Concern"),
    TECHNICAL_SUPPORT("Technical Support")
}

enum class SupportRequestStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}