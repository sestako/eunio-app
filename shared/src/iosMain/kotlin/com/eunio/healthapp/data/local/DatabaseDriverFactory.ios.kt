package com.eunio.healthapp.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.eunio.healthapp.database.EunioDatabase

actual open class DatabaseDriverFactory : DatabaseDriverFactoryInterface {
    actual override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = EunioDatabase.Schema,
            name = "eunio.db"
        ).also { driver ->
            // Apply migrations manually for iOS
            DatabaseMigrations.getAllMigrations().forEach { migration ->
                migration.block(driver)
            }
        }
    }
}