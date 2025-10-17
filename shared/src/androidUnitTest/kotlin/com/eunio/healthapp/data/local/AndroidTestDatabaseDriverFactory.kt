package com.eunio.healthapp.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.eunio.healthapp.database.EunioDatabase

class AndroidTestDatabaseDriverFactory : DatabaseDriverFactoryInterface {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EunioDatabase.Schema.create(driver)
        
        // For testing, we create the database with the latest schema
        // Migrations are tested separately
        
        return driver
    }
}