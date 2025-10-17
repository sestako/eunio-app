package com.eunio.healthapp.data.remote.firebase

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.NSObject

/**
 * Kotlin interface that mirrors the Objective-C FirebaseIOSBridge protocol.
 * This allows type-safe interaction with the Swift FirebaseIOSBridge class
 * without requiring compile-time cinterop configuration.
 * 
 * The actual Swift class is defined in the iOS app target and passed to
 * the shared framework at runtime through dependency injection.
 */
@OptIn(ExperimentalForeignApi::class)
interface FirebaseIOSBridgeProtocol {
    fun saveDailyLogWithUserId(
        userId: String,
        logId: String,
        data: NSDictionary,
        completion: (NSError?) -> Unit
    )
    
    fun getDailyLogWithUserId(
        userId: String,
        logId: String,
        completion: (NSDictionary?, NSError?) -> Unit
    )
    
    fun getDailyLogByDateWithUserId(
        userId: String,
        epochDays: Long,
        completion: (NSDictionary?, NSError?) -> Unit
    )
    
    fun getLogsInRangeWithUserId(
        userId: String,
        startEpochDays: Long,
        endEpochDays: Long,
        completion: (NSArray?, NSError?) -> Unit
    )
    
    fun deleteDailyLogWithUserId(
        userId: String,
        logId: String,
        completion: (NSError?) -> Unit
    )
    
    fun batchSaveDailyLogsWithUserId(
        userId: String,
        logsData: NSArray,
        completion: (NSError?) -> Unit
    )
}

/**
 * Wrapper class that adapts any NSObject with the correct selectors
 * to the FirebaseIOSBridgeProtocol interface.
 * 
 * This uses Objective-C runtime message sending to call methods on the
 * Swift bridge object in a type-safe manner.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class FirebaseIOSBridgeAdapter(private val bridge: NSObject) : FirebaseIOSBridgeProtocol {
    
    override fun saveDailyLogWithUserId(
        userId: String,
        logId: String,
        data: NSDictionary,
        completion: (NSError?) -> Unit
    ) {
        val selector = NSSelectorFromString("saveDailyLogWithUserId:logId:data:completion:")
        if (bridge.respondsToSelector(selector)) {
            // Use performSelector with proper argument marshalling
            bridge.performSelector(
                selector,
                withObject = userId as NSString,
                withObject = logId as NSString,
                withObject = data,
                withObject = completion as Any
            )
        } else {
            completion(NSError.errorWithDomain(
                domain = "com.eunio.healthapp.firebase",
                code = -1,
                userInfo = mapOf(
                    NSLocalizedDescriptionKey to "Bridge does not respond to saveDailyLogWithUserId:logId:data:completion:"
                )
            ))
        }
    }
    
    override fun getDailyLogWithUserId(
        userId: String,
        logId: String,
        completion: (NSDictionary?, NSError?) -> Unit
    ) {
        val selector = NSSelectorFromString("getDailyLogWithUserId:logId:completion:")
        if (bridge.respondsToSelector(selector)) {
            bridge.performSelector(
                selector,
                withObject = userId as NSString,
                withObject = logId as NSString,
                withObject = completion as Any
            )
        } else {
            completion(null, NSError.errorWithDomain(
                domain = "com.eunio.healthapp.firebase",
                code = -1,
                userInfo = mapOf(
                    NSLocalizedDescriptionKey to "Bridge does not respond to getDailyLogWithUserId:logId:completion:"
                )
            ))
        }
    }
    
    override fun getDailyLogByDateWithUserId(
        userId: String,
        epochDays: Long,
        completion: (NSDictionary?, NSError?) -> Unit
    ) {
        val selector = NSSelectorFromString("getDailyLogByDateWithUserId:epochDays:completion:")
        if (bridge.respondsToSelector(selector)) {
            bridge.performSelector(
                selector,
                withObject = userId as NSString,
                withObject = NSNumber(longLong = epochDays),
                withObject = completion as Any
            )
        } else {
            completion(null, NSError.errorWithDomain(
                domain = "com.eunio.healthapp.firebase",
                code = -1,
                userInfo = mapOf(
                    NSLocalizedDescriptionKey to "Bridge does not respond to getDailyLogByDateWithUserId:epochDays:completion:"
                )
            ))
        }
    }
    
    override fun getLogsInRangeWithUserId(
        userId: String,
        startEpochDays: Long,
        endEpochDays: Long,
        completion: (NSArray?, NSError?) -> Unit
    ) {
        val selector = NSSelectorFromString("getLogsInRangeWithUserId:startEpochDays:endEpochDays:completion:")
        if (bridge.respondsToSelector(selector)) {
            bridge.performSelector(
                selector,
                withObject = userId as NSString,
                withObject = NSNumber(longLong = startEpochDays),
                withObject = NSNumber(longLong = endEpochDays),
                withObject = completion as Any
            )
        } else {
            completion(null, NSError.errorWithDomain(
                domain = "com.eunio.healthapp.firebase",
                code = -1,
                userInfo = mapOf(
                    NSLocalizedDescriptionKey to "Bridge does not respond to getLogsInRangeWithUserId:startEpochDays:endEpochDays:completion:"
                )
            ))
        }
    }
    
    override fun deleteDailyLogWithUserId(
        userId: String,
        logId: String,
        completion: (NSError?) -> Unit
    ) {
        val selector = NSSelectorFromString("deleteDailyLogWithUserId:logId:completion:")
        if (bridge.respondsToSelector(selector)) {
            bridge.performSelector(
                selector,
                withObject = userId as NSString,
                withObject = logId as NSString,
                withObject = completion as Any
            )
        } else {
            completion(NSError.errorWithDomain(
                domain = "com.eunio.healthapp.firebase",
                code = -1,
                userInfo = mapOf(
                    NSLocalizedDescriptionKey to "Bridge does not respond to deleteDailyLogWithUserId:logId:completion:"
                )
            ))
        }
    }
    
    override fun batchSaveDailyLogsWithUserId(
        userId: String,
        logsData: NSArray,
        completion: (NSError?) -> Unit
    ) {
        val selector = NSSelectorFromString("batchSaveDailyLogsWithUserId:logsData:completion:")
        if (bridge.respondsToSelector(selector)) {
            bridge.performSelector(
                selector,
                withObject = userId as NSString,
                withObject = logsData,
                withObject = completion as Any
            )
        } else {
            completion(NSError.errorWithDomain(
                domain = "com.eunio.healthapp.firebase",
                code = -1,
                userInfo = mapOf(
                    NSLocalizedDescriptionKey to "Bridge does not respond to batchSaveDailyLogsWithUserId:logsData:completion:"
                )
            ))
        }
    }
}
