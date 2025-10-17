package com.eunio.healthapp.data.local

import app.cash.sqldelight.db.SqlDriver

interface DatabaseDriverFactoryInterface {
    fun createDriver(): SqlDriver
}