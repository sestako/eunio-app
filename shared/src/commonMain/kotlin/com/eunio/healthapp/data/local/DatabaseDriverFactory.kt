package com.eunio.healthapp.data.local

import app.cash.sqldelight.db.SqlDriver

expect open class DatabaseDriverFactory : DatabaseDriverFactoryInterface {
    override fun createDriver(): SqlDriver
}