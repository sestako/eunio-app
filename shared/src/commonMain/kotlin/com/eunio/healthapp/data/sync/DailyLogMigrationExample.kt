package com.eunio.healthapp.data.sync

/**
 * Example usage of DailyLogMigration utility.
 * 
 * This file demonstrates various ways to use the migration utility
 * for migrating daily logs from legacy Firebase path to new path.
 */

/**
 * Example 1: Basic migration for a single user
 */
suspend fun basicMigrationExample(userId: String) {
    // Create migration instance using factory
    val migration = DailyLogMigrationFactory.create()
    
    // Run migration
    val result = migration.migrateLegacyLogs(userId)
    
    // Check results
    if (result.success) {
        println("✅ Migration completed successfully!")
        println("📦 Migrated: ${result.migratedCount} documents")
        if (result.skippedCount > 0) {
            println("⏭️  Skipped: ${result.skippedCount} documents (already exist)")
        }
    } else {
        println("❌ Migration failed")
        println("⚠️  Errors: ${result.errorCount}")
        result.errors.forEach { error ->
            println("   - $error")
        }
    }
}

/**
 * Example 2: Migration with detailed logging
 */
suspend fun migrationWithLogging(userId: String) {
    val migration = DailyLogMigrationFactory.create()
    
    println("Starting migration for user: $userId")
    
    val result = migration.migrateLegacyLogs(userId)
    
    // Print detailed summary
    println("\n" + "=".repeat(50))
    println("MIGRATION SUMMARY")
    println("=".repeat(50))
    println(result.summary())
    println("=".repeat(50))
}

/**
 * Example 3: Batch migration for multiple users
 */
suspend fun batchMigrationExample(userIds: List<String>) {
    val migration = DailyLogMigrationFactory.create()
    val results = mutableMapOf<String, MigrationResult>()
    
    println("Starting batch migration for ${userIds.size} users...")
    
    userIds.forEachIndexed { index, userId ->
        println("\n[${index + 1}/${userIds.size}] Migrating user: $userId")
        
        try {
            val result = migration.migrateLegacyLogs(userId)
            results[userId] = result
            
            when {
                result.success && result.migratedCount > 0 -> 
                    println("  ✅ Success: ${result.migratedCount} documents migrated")
                result.success && result.migratedCount == 0 -> 
                    println("  ℹ️  No legacy data found")
                else -> 
                    println("  ⚠️  Partial success: ${result.migratedCount} migrated, ${result.errorCount} errors")
            }
        } catch (e: Exception) {
            println("  ❌ Failed: ${e.message}")
        }
    }
    
    // Print overall summary
    println("\n" + "=".repeat(50))
    println("BATCH MIGRATION SUMMARY")
    println("=".repeat(50))
    
    val totalMigrated = results.values.sumOf { it.migratedCount }
    val totalSkipped = results.values.sumOf { it.skippedCount }
    val totalErrors = results.values.sumOf { it.errorCount }
    val successfulUsers = results.values.count { it.success }
    
    println("Users processed: ${userIds.size}")
    println("Successful migrations: $successfulUsers")
    println("Total documents migrated: $totalMigrated")
    println("Total documents skipped: $totalSkipped")
    println("Total errors: $totalErrors")
    println("=".repeat(50))
}

/**
 * Example 4: Migration with error handling and retry
 */
suspend fun migrationWithRetry(userId: String, maxRetries: Int = 3) {
    val migration = DailyLogMigrationFactory.create()
    var attempt = 0
    var result: MigrationResult? = null
    
    while (attempt < maxRetries) {
        attempt++
        println("Attempt $attempt of $maxRetries for user: $userId")
        
        try {
            result = migration.migrateLegacyLogs(userId)
            
            if (result.success) {
                println("✅ Migration successful on attempt $attempt")
                break
            } else if (result.migratedCount > 0) {
                println("⚠️  Partial success: ${result.migratedCount} migrated, ${result.errorCount} errors")
                println("   Some documents migrated, considering this a success")
                break
            } else {
                println("❌ Migration failed on attempt $attempt")
                if (attempt < maxRetries) {
                    println("   Retrying in 5 seconds...")
                    kotlinx.coroutines.delay(5000)
                }
            }
        } catch (e: Exception) {
            println("❌ Exception on attempt $attempt: ${e.message}")
            if (attempt < maxRetries) {
                println("   Retrying in 5 seconds...")
                kotlinx.coroutines.delay(5000)
            }
        }
    }
    
    if (result != null) {
        println("\nFinal result:")
        println(result.summary())
    } else {
        println("\n❌ Migration failed after $maxRetries attempts")
    }
}

/**
 * Example 5: Migration with progress callback
 */
suspend fun migrationWithProgress(
    userIds: List<String>,
    onProgress: (current: Int, total: Int, userId: String, result: MigrationResult) -> Unit
) {
    val migration = DailyLogMigrationFactory.create()
    val total = userIds.size
    
    userIds.forEachIndexed { index, userId ->
        val result = migration.migrateLegacyLogs(userId)
        onProgress(index + 1, total, userId, result)
    }
}

/**
 * Example 6: Conditional migration (only if legacy data exists)
 */
suspend fun conditionalMigrationExample(userId: String) {
    val migration = DailyLogMigrationFactory.create()
    
    // Run migration
    val result = migration.migrateLegacyLogs(userId)
    
    // Only report if there was actually data to migrate
    if (result.migratedCount > 0 || result.errorCount > 0) {
        println("Migration performed for user $userId:")
        println("  Migrated: ${result.migratedCount}")
        println("  Errors: ${result.errorCount}")
    } else {
        println("No legacy data found for user $userId - skipping")
    }
}

/**
 * Example 7: Migration verification
 */
suspend fun migrationWithVerification(userId: String) {
    val migration = DailyLogMigrationFactory.create()
    
    println("Step 1: Running migration...")
    val result = migration.migrateLegacyLogs(userId)
    
    println("Step 2: Verifying migration...")
    if (result.success && result.migratedCount > 0) {
        println("✅ Migration completed: ${result.migratedCount} documents")
        
        // In a real scenario, you would verify by querying both paths
        // and comparing the data
        println("Step 3: Verification would go here")
        println("   - Query legacy path")
        println("   - Query new path")
        println("   - Compare document counts and data")
    } else if (result.migratedCount == 0 && result.errorCount == 0) {
        println("ℹ️  No legacy data to migrate")
    } else {
        println("⚠️  Migration had issues:")
        println("   Migrated: ${result.migratedCount}")
        println("   Errors: ${result.errorCount}")
        result.errors.forEach { error ->
            println("   - $error")
        }
    }
}

/**
 * Example 8: Safe migration with dry-run concept
 * 
 * Note: The actual migration utility doesn't have a dry-run mode,
 * but you can check for existing data first
 */
suspend fun safeMigrationExample(userId: String) {
    val migration = DailyLogMigrationFactory.create()
    
    println("Performing migration for user: $userId")
    println("Note: Migration is idempotent - safe to run multiple times")
    
    val result = migration.migrateLegacyLogs(userId)
    
    when {
        result.migratedCount > 0 -> {
            println("✅ Migrated ${result.migratedCount} new documents")
        }
        result.skippedCount > 0 -> {
            println("ℹ️  All ${result.skippedCount} documents already migrated")
        }
        else -> {
            println("ℹ️  No legacy data found")
        }
    }
    
    if (result.errorCount > 0) {
        println("⚠️  ${result.errorCount} documents failed to migrate")
    }
}
