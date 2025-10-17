package com.eunio.healthapp.data.local

import com.eunio.healthapp.data.local.DatabaseMigrations
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseMigrationTest {
    
    @Test
    fun `database version is correctly set to 3`() {
        assertEquals(3, DatabaseMigrations.CURRENT_VERSION)
    }
    
    @Test
    fun `getAllMigrations returns both migrations`() {
        val migrations = DatabaseMigrations.getAllMigrations()
        assertEquals(2, migrations.size)
        assertEquals(DatabaseMigrations.migration1to2, migrations[0])
        assertEquals(DatabaseMigrations.migration2to3, migrations[1])
    }
    
    @Test
    fun `migration1to2 is properly configured`() {
        val migration = DatabaseMigrations.migration1to2
        // Verify the migration is configured for version 1 to 2
        assertTrue(migration.toString().contains("AfterVersion"))
    }
}