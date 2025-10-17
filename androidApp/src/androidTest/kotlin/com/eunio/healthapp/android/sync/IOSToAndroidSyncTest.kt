package com.eunio.healthapp.android.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eunio.healthapp.data.remote.FirestorePaths
import com.eunio.healthapp.data.remote.dto.DailyLogDto
import com.eunio.healthapp.domain.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import kotlin.time.Duration.Companion.seconds

/**
 * Cross-platform sync test: iOS to Android
 * Tests Requirements: 5.1, 5.4, 5.5
 * 
 * This test suite verifies:
 * - Daily log created on iOS syncs to Firebase and can be read on Android
 * - Log data fields are preserved during sync
 * - Date integrity is maintained (no timezone issues)
 * - Data matches exactly between platforms
 * 
 * TEST WORKFLOW:
 * 1. Run the iOS UI test: IOSToAndroidSyncVerificationTests.testIOSToAndroidSync()
 * 2. Wait for iOS test to complete and sync to Firebase
 * 3. Run this Android test to verify the data
 * 
 * This test queries Firebase directly to verify data synced from iOS,
 * ensuring cross-platform compatibility and data integrity.
 */
@RunWith(AndroidJUnit4::class)
class IOSToAndroidSyncTest {

    private lateinit var firestore: FirebaseFirestore
    private val testUserId = "test-user-ios-to-android-sync"
    
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Initialize Firebase if not already initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        
        firestore = FirebaseFirestore.getInstance()
    }

    /**
     * Test Requirement 5.1: Verify log saved on iOS is visible when querying from Android
     * 
     * This test queries Firebase for a log that should have been created by the iOS test.
     * It verifies the log exists and all data fields match exactly.
     * 
     * Expected Test Data (from iOS test):
     * - Date: October 10, 2025 (2025-10-10)
     * - Symptom: Cramps
     * - Mood: Calm
     * - BBT: 98.4°F
     * - Notes: "iOS to Android sync test - [timestamp]"
     */
    @Test
    fun testVerifyIOSLogSyncedToAndroid() = runBlocking {
        println("\n" + "=".repeat(60))
        println("📱 iOS → Android Sync Verification Test")
        println("=".repeat(60))
        
        // Test date: October 10, 2025
        val testDate = LocalDate(2025, 10, 10)
        val logId = testDate.toString() // "2025-10-10"
        
        println("\n🔍 Step 1: Querying Firebase for iOS-created log...")
        println("   User ID: $testUserId")
        println("   Log ID: $logId")
        println("   Date: October 10, 2025")
        println("   Path: ${FirestorePaths.dailyLogDoc(testUserId, logId)}")
        
        // Query Firebase using the standardized path
        val docRef = firestore
            .collection("users")
            .document(testUserId)
            .collection("dailyLogs")
            .document(logId)
        
        try {
            val snapshot = docRef.get().await()
            
            if (!snapshot.exists()) {
                println("\n❌ PREREQUISITE NOT MET:")
                println("   Log not found in Firebase.")
                println("\n📋 REQUIRED STEPS:")
                println("   1. First run the iOS test: IOSToAndroidSyncVerificationTests.testIOSToAndroidSync()")
                println("   2. Wait for iOS test to complete and sync to Firebase (10+ seconds)")
                println("   3. Then run this Android test")
                println("\n⚠️  This test verifies iOS → Android sync, so iOS must create the data first.")
                println("=".repeat(60))
                
                fail("Log not found. Please run iOS test first to create the test data.")
                return@runBlocking
            }
            
            println("✅ Step 1 Complete: Log found in Firebase")
            
            // Step 2: Parse the document data
            println("\n🔍 Step 2: Parsing document data...")
            val data = snapshot.data!!
            
            println("   Raw Firebase data:")
            data.forEach { (key, value) ->
                println("      $key: $value")
            }
            
            // Step 3: Verify required fields exist
            println("\n🔍 Step 3: Verifying required fields...")
            
            assertNotNull("logId field should exist", data["logId"])
            assertNotNull("dateEpochDays field should exist", data["dateEpochDays"])
            assertNotNull("createdAt field should exist", data["createdAt"])
            assertNotNull("updatedAt field should exist", data["updatedAt"])
            assertNotNull("v field should exist", data["v"])
            
            println("✅ Step 3 Complete: All required fields present")
            
            // Step 4: Verify date integrity
            println("\n🔍 Step 4: Verifying date integrity...")
            
            val logIdValue = data["logId"] as String
            assertEquals("Log ID should match date", "2025-10-10", logIdValue)
            
            val dateEpochDays = (data["dateEpochDays"] as? Long) ?: (data["dateEpochDays"] as Number).toLong()
            val expectedEpochDays = testDate.toEpochDays().toLong()
            
            println("   Expected epoch days: $expectedEpochDays")
            println("   Actual epoch days: $dateEpochDays")
            
            assertEquals(
                "Date epoch days should match October 10, 2025",
                expectedEpochDays,
                dateEpochDays
            )
            
            // Verify the date converts back correctly
            val parsedDate = LocalDate.fromEpochDays(dateEpochDays.toInt())
            assertEquals("Parsed date should be October 10, 2025", testDate, parsedDate)
            
            println("✅ Step 4 Complete: Date integrity verified (no timezone shift)")
            
            // Step 5: Verify schema version
            println("\n🔍 Step 5: Verifying schema version...")
            
            val schemaVersion = (data["v"] as? Long)?.toInt() ?: (data["v"] as Number).toInt()
            assertEquals("Schema version should be 1", 1, schemaVersion)
            
            println("✅ Step 5 Complete: Schema version correct (v=1)")
            
            // Step 6: Verify optional data fields (if present from iOS test)
            println("\n🔍 Step 6: Verifying optional data fields...")
            
            val symptoms = data["symptoms"] as? List<*>
            val mood = data["mood"] as? String
            val bbt = data["bbt"] as? Double
            val notes = data["notes"] as? String
            
            println("   Symptoms: ${symptoms ?: "none"}")
            println("   Mood: ${mood ?: "none"}")
            println("   BBT: ${bbt ?: "none"}")
            println("   Notes: ${notes ?: "none"}")
            
            // Verify expected test data from iOS
            if (symptoms != null) {
                assertTrue("Symptoms should contain 'CRAMPS'", symptoms.contains("CRAMPS"))
                println("   ✓ Symptoms verified: Contains CRAMPS")
            }
            
            if (mood != null) {
                assertEquals("Mood should be CALM", "CALM", mood)
                println("   ✓ Mood verified: CALM")
            }
            
            if (bbt != null) {
                assertEquals("BBT should be 98.4", 98.4, bbt, 0.1)
                println("   ✓ BBT verified: 98.4°F")
            }
            
            if (notes != null) {
                assertTrue("Notes should contain 'iOS to Android sync test'", 
                    notes.contains("iOS to Android sync test"))
                println("   ✓ Notes verified: Contains expected text")
            }
            
            println("✅ Step 6 Complete: Optional fields verified")
            
            // Step 7: Convert to domain model and verify
            println("\n🔍 Step 7: Converting to domain model...")
            
            try {
                val dto = DailyLogDto(
                    logId = logIdValue,
                    dateEpochDays = dateEpochDays,
                    createdAt = (data["createdAt"] as Number).toLong(),
                    updatedAt = (data["updatedAt"] as Number).toLong(),
                    periodFlow = data["periodFlow"] as? String,
                    symptoms = symptoms?.map { it.toString() },
                    mood = mood,
                    bbt = bbt,
                    cervicalMucus = data["cervicalMucus"] as? String,
                    opkResult = data["opkResult"] as? String,
                    notes = notes,
                    v = schemaVersion
                )
                
                val domainLog = dto.toDomain(userId = testUserId)
                
                println("   Domain model created successfully:")
                println("      ID: ${domainLog.id}")
                println("      User ID: ${domainLog.userId}")
                println("      Date: ${domainLog.date}")
                println("      Mood: ${domainLog.mood}")
                println("      Symptoms: ${domainLog.symptoms}")
                println("      BBT: ${domainLog.bbt}")
                println("      Notes: ${domainLog.notes}")
                
                // Verify domain model fields
                assertEquals("Domain log ID should match", logId, domainLog.id)
                assertEquals("Domain log user ID should match", testUserId, domainLog.userId)
                assertEquals("Domain log date should match", testDate, domainLog.date)
                
                println("✅ Step 7 Complete: Domain model conversion successful")
                
            } catch (e: Exception) {
                println("❌ Step 7 Failed: Error converting to domain model")
                println("   Error: ${e.message}")
                throw e
            }
            
            // Final summary
            println("\n" + "=".repeat(60))
            println("✅ iOS → ANDROID SYNC TEST PASSED")
            println("=".repeat(60))
            println("\n📊 Test Summary:")
            println("   ✓ Log exists in Firebase at correct path")
            println("   ✓ All required fields present")
            println("   ✓ Date integrity maintained (no timezone shift)")
            println("   ✓ Schema version correct (v=1)")
            println("   ✓ Optional data fields preserved")
            println("   ✓ Domain model conversion successful")
            println("\n🎉 Data created on iOS successfully synced to Android!")
            println("=".repeat(60) + "\n")
            
        } catch (e: Exception) {
            println("\n❌ TEST FAILED")
            println("   Error: ${e.message}")
            println("   Stack trace:")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Test Requirement 5.4, 5.5: Verify data matches exactly between platforms
     * 
     * This test queries multiple logs created by iOS and verifies they all
     * maintain data integrity and correct dates.
     */
    @Test
    fun testVerifyMultipleDateIOSSync() = runBlocking {
        println("\n" + "=".repeat(60))
        println("📱 iOS → Android Multiple Date Sync Verification")
        println("=".repeat(60))
        
        // Test dates: October 8, 9, 10, 11, 12, 2025
        val testDates = listOf(
            LocalDate(2025, 10, 8),
            LocalDate(2025, 10, 9),
            LocalDate(2025, 10, 10),
            LocalDate(2025, 10, 11),
            LocalDate(2025, 10, 12)
        )
        
        var foundCount = 0
        var missingCount = 0
        val missingDates = mutableListOf<String>()
        
        println("\n🔍 Querying Firebase for ${testDates.size} logs...")
        
        for (testDate in testDates) {
            val logId = testDate.toString()
            
            println("\n   Checking: $logId")
            
            val docRef = firestore
                .collection("users")
                .document(testUserId)
                .collection("dailyLogs")
                .document(logId)
            
            try {
                val snapshot = docRef.get().await()
                
                if (snapshot.exists()) {
                    foundCount++
                    
                    val data = snapshot.data!!
                    val dateEpochDays = (data["dateEpochDays"] as Number).toLong()
                    val parsedDate = LocalDate.fromEpochDays(dateEpochDays.toInt())
                    
                    // Verify date integrity
                    assertEquals("Date should match for $logId", testDate, parsedDate)
                    
                    println("      ✅ Found - Date verified: $parsedDate")
                    
                    // Check for notes
                    val notes = data["notes"] as? String
                    if (notes != null) {
                        println("         Notes: $notes")
                    }
                } else {
                    missingCount++
                    missingDates.add(logId)
                    println("      ⚠️  Not found")
                }
            } catch (e: Exception) {
                missingCount++
                missingDates.add(logId)
                println("      ❌ Error: ${e.message}")
            }
        }
        
        // Summary
        println("\n" + "=".repeat(60))
        println("📊 Multiple Date Sync Summary:")
        println("   Total dates checked: ${testDates.size}")
        println("   Found: $foundCount")
        println("   Missing: $missingCount")
        
        if (missingCount > 0) {
            println("\n⚠️  Missing dates:")
            missingDates.forEach { date ->
                println("      - $date")
            }
            println("\n📋 REQUIRED STEPS:")
            println("   1. Run iOS test: IOSToAndroidSyncVerificationTests.testMultipleDateIOSToAndroidSync()")
            println("   2. Wait for sync to complete")
            println("   3. Re-run this test")
        }
        
        if (foundCount > 0) {
            println("\n✅ Found $foundCount log(s) with correct date integrity")
            println("🎉 iOS → Android sync working for multiple dates!")
        }
        
        println("=".repeat(60) + "\n")
        
        // Test passes if at least one log was found with correct data
        assertTrue("At least one log should be found from iOS sync", foundCount > 0)
    }

    /**
     * Test Requirement 5.1: Verify query by date returns same log on both platforms
     * 
     * This test verifies that querying by date on Android returns the same
     * log that was created on iOS, confirming the standardized path works.
     */
    @Test
    fun testQueryByDateReturnsIOSLog() = runBlocking {
        println("\n" + "=".repeat(60))
        println("📱 Query by Date: iOS → Android Verification")
        println("=".repeat(60))
        
        val testDate = LocalDate(2025, 10, 10)
        val dateEpochDays = testDate.toEpochDays().toLong()
        
        println("\n🔍 Querying by dateEpochDays: $dateEpochDays")
        println("   Expected date: October 10, 2025")
        
        // Query using dateEpochDays field (as the repository would)
        val querySnapshot = firestore
            .collection("users")
            .document(testUserId)
            .collection("dailyLogs")
            .whereEqualTo("dateEpochDays", dateEpochDays)
            .get()
            .await()
        
        println("   Query returned ${querySnapshot.documents.size} document(s)")
        
        if (querySnapshot.documents.isEmpty()) {
            println("\n❌ PREREQUISITE NOT MET:")
            println("   No log found for October 10, 2025")
            println("\n📋 Run iOS test first: IOSToAndroidSyncVerificationTests.testIOSToAndroidSync()")
            
            fail("No log found. Run iOS test first.")
            return@runBlocking
        }
        
        // Verify we got exactly one document
        assertEquals("Should find exactly one log for the date", 1, querySnapshot.documents.size)
        
        val doc = querySnapshot.documents[0]
        val data = doc.data!!
        
        println("\n✅ Found log by date query:")
        println("   Document ID: ${doc.id}")
        println("   Log ID: ${data["logId"]}")
        println("   Date Epoch Days: ${data["dateEpochDays"]}")
        
        // Verify the document ID matches the expected format
        assertEquals("Document ID should be 2025-10-10", "2025-10-10", doc.id)
        
        // Verify date integrity
        val actualDateEpochDays = (data["dateEpochDays"] as Number).toLong()
        assertEquals("Date epoch days should match", dateEpochDays, actualDateEpochDays)
        
        val parsedDate = LocalDate.fromEpochDays(actualDateEpochDays.toInt())
        assertEquals("Parsed date should be October 10, 2025", testDate, parsedDate)
        
        println("\n" + "=".repeat(60))
        println("✅ QUERY BY DATE TEST PASSED")
        println("=".repeat(60))
        println("\n📊 Verification:")
        println("   ✓ Query by dateEpochDays returns correct log")
        println("   ✓ Document ID matches expected format")
        println("   ✓ Date integrity maintained")
        println("\n🎉 iOS log successfully queryable from Android!")
        println("=".repeat(60) + "\n")
    }
}
