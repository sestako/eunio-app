package com.eunio.healthapp.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.eunio.healthapp.database.EunioDatabase

class TestDatabaseDriverFactory : DatabaseDriverFactoryInterface {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EunioDatabase.Schema.create(driver)
        
        // Apply any migrations for testing
        DatabaseMigrations.getAllMigrations().forEach { migration ->
            migration.block(driver)
        }
        
        return driver
    }
}