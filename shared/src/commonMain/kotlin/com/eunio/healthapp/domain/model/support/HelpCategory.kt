package com.eunio.healthapp.domain.model.support

import kotlinx.serialization.Serializable

@Serializable
data class HelpCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val faqs: List<FAQ> = emptyList()
)

@Serializable
data class FAQ(
    val id: String,
    val question: String,
    val answer: String,
    val tags: List<String> = emptyList(),
    val isExpanded: Boolean = false
)

enum class SupportCategory(val displayName: String) {
    GETTING_STARTED("Getting Started"),
    CYCLE_TRACKING("Cycle Tracking"),
    NOTIFICATIONS("Notifications & Reminders"),
    DATA_PRIVACY("Data & Privacy"),
    SYNC_BACKUP("Sync & Backup"),
    TROUBLESHOOTING("Troubleshooting"),
    ACCOUNT_SETTINGS("Account & Settings")
}