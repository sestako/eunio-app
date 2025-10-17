import Foundation

/// Protocol defining the Firebase bridge interface for Kotlin/Native interop.
/// All methods use Objective-C compatible types for seamless cinterop.
@objc public protocol FirebaseBridgeProtocol {
    
    /// Save a daily log to Firestore
    /// - Parameters:
    ///   - userId: User ID
    ///   - logId: Log document ID
    ///   - data: Log data as NSDictionary
    ///   - completion: Completion handler with optional error
    @objc func saveDailyLog(
        userId: String,
        logId: String,
        data: NSDictionary,
        completion: @escaping (NSError?) -> Void
    )
    
    /// Get a daily log from Firestore by ID
    /// - Parameters:
    ///   - userId: User ID
    ///   - logId: Log document ID
    ///   - completion: Completion handler with data dictionary and optional error
    @objc func getDailyLog(
        userId: String,
        logId: String,
        completion: @escaping (NSDictionary?, NSError?) -> Void
    )
    
    /// Get a daily log from Firestore by date (epoch days)
    /// - Parameters:
    ///   - userId: User ID
    ///   - epochDays: Date as epoch days since Unix epoch
    ///   - completion: Completion handler with data dictionary and optional error
    @objc func getDailyLogByDate(
        userId: String,
        epochDays: Int64,
        completion: @escaping (NSDictionary?, NSError?) -> Void
    )
    
    /// Get daily logs in a date range
    /// - Parameters:
    ///   - userId: User ID
    ///   - startEpochDays: Start date as epoch days
    ///   - endEpochDays: End date as epoch days
    ///   - completion: Completion handler with array of dictionaries and optional error
    @objc func getLogsInRange(
        userId: String,
        startEpochDays: Int64,
        endEpochDays: Int64,
        completion: @escaping (NSArray?, NSError?) -> Void
    )
    
    /// Delete a daily log from Firestore
    /// - Parameters:
    ///   - userId: User ID
    ///   - logId: Log document ID
    ///   - completion: Completion handler with optional error
    @objc func deleteDailyLog(
        userId: String,
        logId: String,
        completion: @escaping (NSError?) -> Void
    )
    
    /// Batch save multiple daily logs to Firestore
    /// - Parameters:
    ///   - userId: User ID
    ///   - logsData: Array of log data dictionaries
    ///   - completion: Completion handler with optional error
    @objc func batchSaveDailyLogs(
        userId: String,
        logsData: NSArray,
        completion: @escaping (NSError?) -> Void
    )
}

/// Global provider for the Firebase bridge implementation.
/// The concrete implementation is injected at runtime from the iOS app.
@objc public class FirebaseBridgeProvider: NSObject {
    
    /// Shared singleton instance holder
    @objc public static var shared: FirebaseBridgeProtocol?
    
    private override init() {
        super.init()
    }
}
