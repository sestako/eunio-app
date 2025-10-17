package com.eunio.healthapp.data.sync

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Android implementation of DailyLogMigrationFactory.
 */
actual object DailyLogMigrationFactory {
    actual fun create(): DailyLogMigration {
        val firestore = FirebaseFirestore.getInstance()
        return AndroidDailyLogMigration(firestore)
    }
}
