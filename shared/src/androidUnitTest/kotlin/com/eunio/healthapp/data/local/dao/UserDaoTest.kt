package com.eunio.healthapp.data.local.dao

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.eunio.healthapp.database.EunioDatabase
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

class UserDaoTest {
    
    private lateinit var driver: SqlDriver
    private lateinit var database: EunioDatabase
    private lateinit var userDao: UserDao
    
    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EunioDatabase.Schema.create(driver)
        database = EunioDatabase(driver)
        userDao = UserDao(database)
    }
    
    @AfterTest
    fun tearDown() {
        driver.close()
    }
    
    @Test
    fun insertUser_shouldSaveUserToDatabase() = runTest {
        // Given
        val user = createTestUser()
        
        // When
        userDao.insertUser(user)
        
        // Then
        val retrievedUser = userDao.getUserById(user.id)
        assertNotNull(retrievedUser)
        assertEquals(user.id, retrievedUser.id)
        assertEquals(user.email, retrievedUser.email)
        assertEquals(user.name, retrievedUser.name)
        assertEquals(user.onboardingComplete, retrievedUser.onboardingComplete)
        assertEquals(user.primaryGoal, retrievedUser.primaryGoal)
    }
    
    @Test
    fun updateUser_shouldModifyExistingUser() = runTest {
        // Given
        val user = createTestUser()
        userDao.insertUser(user)
        
        val updatedUser = user.copy(
            name = "Updated Name",
            onboardingComplete = true,
            updatedAt = Clock.System.now()
        )
        
        // When
        userDao.updateUser(updatedUser)
        
        // Then
        val retrievedUser = userDao.getUserById(user.id)
        assertNotNull(retrievedUser)
        assertEquals("Updated Name", retrievedUser.name)
        assertTrue(retrievedUser.onboardingComplete)
    }
    
    @Test
    fun getUserById_withNonExistentId_shouldReturnNull() = runTest {
        // When
        val result = userDao.getUserById("non-existent-id")
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun getAllUsers_shouldReturnAllUsers() = runTest {
        // Given
        val user1 = createTestUser(id = "user1")
        val user2 = createTestUser(id = "user2", email = "user2@test.com")
        
        userDao.insertUser(user1)
        userDao.insertUser(user2)
        
        // When
        val allUsers = userDao.getAllUsers()
        
        // Then
        assertEquals(2, allUsers.size)
        assertTrue(allUsers.any { it.id == "user1" })
        assertTrue(allUsers.any { it.id == "user2" })
    }
    
    @Test
    fun getPendingSyncUsers_shouldReturnOnlyPendingUsers() = runTest {
        // Given
        val user = createTestUser()
        userDao.insertUser(user)
        
        // When
        val pendingUsers = userDao.getPendingSyncUsers()
        
        // Then
        assertEquals(1, pendingUsers.size)
        assertEquals(user.id, pendingUsers.first().id)
    }
    
    @Test
    fun updateSyncStatus_shouldChangeUserSyncStatus() = runTest {
        // Given
        val user = createTestUser()
        userDao.insertUser(user)
        
        // When
        userDao.updateSyncStatus(user.id, "SYNCED")
        
        // Then
        val pendingUsers = userDao.getPendingSyncUsers()
        assertTrue(pendingUsers.isEmpty())
    }
    
    @Test
    fun deleteUser_shouldRemoveUserFromDatabase() = runTest {
        // Given
        val user = createTestUser()
        userDao.insertUser(user)
        
        // When
        userDao.deleteUser(user.id)
        
        // Then
        val retrievedUser = userDao.getUserById(user.id)
        assertNull(retrievedUser)
    }
    
    private fun createTestUser(
        id: String = "test-user-id",
        email: String = "test@example.com"
    ): User {
        val now = Clock.System.now()
        return User(
            id = id,
            email = email,
            name = "Test User",
            onboardingComplete = false,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            createdAt = now,
            updatedAt = now
        )
    }
}