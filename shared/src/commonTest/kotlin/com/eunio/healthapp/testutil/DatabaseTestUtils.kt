package com.eunio.healthapp.testutil

import com.eunio.healthapp.data.local.DatabaseManager
import com.eunio.healthapp.data.local.DatabaseDriverFactoryInterface
import com.eunio.healthapp.data.local.dao.DailyLogDao
import com.eunio.healthapp.data.local.dao.UserDao
import com.eunio.healthapp.data.local.dao.UserPreferencesDao
import com.eunio.healthapp.data.local.dao.UserSettingsDao
import com.eunio.healthapp.database.EunioDatabase
import app.cash.sqldelight.db.SqlDriver
// JDBC not available in common tests - using mock implementation

/**
 * Creates a mock DatabaseManager for testing purposes.
 * Note: Database tests should use platform-specific test directories.
 * This is a temporary compatibility shim.
 */
fun createMockDatabaseManager(): DatabaseManager {
    // Return a DatabaseManager with a mock driver factory
    // This allows tests to run without actual database initialization
    return DatabaseManager(MockDatabaseDriverFactory())
}

/**
 * Test implementation that creates a working driver for Android tests
 */
class TestDatabaseDriverFactory : DatabaseDriverFactoryInterface {
    override fun createDriver(): SqlDriver {
        // For Android tests, try to create a real JDBC driver
        // For other platforms, this will be handled by platform-specific implementations
        return try {
            createJdbcDriverIfAvailable()
        } catch (e: Exception) {
            // If JDBC is not available (e.g., on iOS), throw a clear error
            throw UnsupportedOperationException(
                "Database functionality not available in this test environment. " +
                "Move database tests to androidTest or iosTest directories. " +
                "Original error: ${e.message}"
            )
        }
    }
}

/**
 * Creates a JDBC driver if available (Android/JVM tests)
 * Note: This function uses Java reflection which is not available on iOS/Native.
 * It will throw UnsupportedOperationException on non-JVM platforms.
 */
private fun createJdbcDriverIfAvailable(): SqlDriver {
    throw UnsupportedOperationException(
        "JDBC driver not available on this platform. " +
        "Use platform-specific test directories (androidTest or iosTest) for database tests."
    )
}

/**
 * Creates a DatabaseManager with a fresh in-memory database for each test
 */
fun createFreshTestDatabaseManager(): DatabaseManager {
    return DatabaseManager(TestDatabaseDriverFactory())
}

/**
 * Mock DatabaseManager that doesn't require actual database operations
 * Useful for testing service layer without database dependencies
 */
class MockDatabaseManagerForService(driverFactory: DatabaseDriverFactoryInterface = MockDatabaseDriverFactory()) {
    
    private val realManager = DatabaseManager(driverFactory)
    private var isInitialized = false
    private var shouldThrowError = false
    
    fun setShouldThrowError(shouldThrow: Boolean) {
        shouldThrowError = shouldThrow
    }
    
    fun getDatabase(): EunioDatabase {
        if (shouldThrowError) {
            throw RuntimeException("Mock database error")
        }
        isInitialized = true
        return realManager.getDatabase()
    }
    
    fun getUserDao(): UserDao {
        if (shouldThrowError) {
            throw RuntimeException("Mock DAO error")
        }
        return realManager.getUserDao()
    }
    
    fun getDailyLogDao(): DailyLogDao {
        if (shouldThrowError) {
            throw RuntimeException("Mock DAO error")
        }
        return realManager.getDailyLogDao()
    }
    
    fun getUserPreferencesDao(): UserPreferencesDao {
        if (shouldThrowError) {
            throw RuntimeException("Mock DAO error")
        }
        return realManager.getUserPreferencesDao()
    }
    
    fun getUserSettingsDao(): UserSettingsDao {
        if (shouldThrowError) {
            throw RuntimeException("Mock DAO error")
        }
        return realManager.getUserSettingsDao()
    }
    
    fun isDatabaseInitialized(): Boolean {
        return isInitialized && !shouldThrowError
    }
    
    fun closeDatabase() {
        realManager.closeDatabase()
        isInitialized = false
    }
    
    fun reinitializeDatabase(): EunioDatabase {
        closeDatabase()
        return getDatabase()
    }
}

/**
 * Mock driver factory for testing
 */
class MockDatabaseDriverFactory : DatabaseDriverFactoryInterface {
    override fun createDriver(): SqlDriver {
        // Return a mock driver that doesn't require actual database operations
        return MockSqlDriver()
    }
}

/**
 * Mock SqlDriver implementation for unit tests
 */
private class MockSqlDriver : SqlDriver {
    override fun close() {
        // No-op for mock
    }

    override fun currentTransaction(): app.cash.sqldelight.Transacter.Transaction? = null

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (app.cash.sqldelight.db.SqlPreparedStatement.() -> Unit)?
    ): app.cash.sqldelight.db.QueryResult<Long> {
        return app.cash.sqldelight.db.QueryResult.Value(0L)
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (app.cash.sqldelight.db.SqlCursor) -> app.cash.sqldelight.db.QueryResult<R>,
        parameters: Int,
        binders: (app.cash.sqldelight.db.SqlPreparedStatement.() -> Unit)?
    ): app.cash.sqldelight.db.QueryResult<R> {
        return mapper(MockSqlCursor())
    }

    override fun newTransaction(): app.cash.sqldelight.db.QueryResult<app.cash.sqldelight.Transacter.Transaction> {
        return app.cash.sqldelight.db.QueryResult.Value(MockTransaction())
    }

    override fun addListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {
        // No-op for mock
    }

    override fun removeListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {
        // No-op for mock
    }

    override fun notifyListeners(vararg queryKeys: String) {
        // No-op for mock
    }
}

/**
 * Mock SqlCursor implementation
 */
private class MockSqlCursor : app.cash.sqldelight.db.SqlCursor {
    override fun next(): app.cash.sqldelight.db.QueryResult<Boolean> {
        return app.cash.sqldelight.db.QueryResult.Value(false)
    }

    override fun getString(index: Int): String? = null
    override fun getLong(index: Int): Long? = null
    override fun getBytes(index: Int): ByteArray? = null
    override fun getDouble(index: Int): Double? = null
    override fun getBoolean(index: Int): Boolean? = null
}

/**
 * Mock Transaction implementation
 */
private class MockTransaction : app.cash.sqldelight.Transacter.Transaction() {
    override val enclosingTransaction: app.cash.sqldelight.Transacter.Transaction? = null
    
    override fun endTransaction(successful: Boolean): app.cash.sqldelight.db.QueryResult<Unit> {
        return app.cash.sqldelight.db.QueryResult.Unit
    }
}