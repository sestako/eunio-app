package com.eunio.healthapp.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.eunio.healthapp.database.EunioDatabase

actual open class DatabaseDriverFactory(private val context: Context) : DatabaseDriverFactoryInterface {
    actual override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = EunioDatabase.Schema,
            context = context,
            name = "eunio.db",
            callback = AndroidSqliteDriver.Callback(
                schema = EunioDatabase.Schema,
                *DatabaseMigrations.getAllMigrations()
            )
        )
    }
}