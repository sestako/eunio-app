//
//  FirebaseBridge.h
//  EunioBridgeKit
//
//  Protocol defining Firebase operations for Kotlin/Native interop
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/// Completion handler for operations that may fail
typedef void (^FirebaseCompletionHandler)(NSError * _Nullable error);

/// Completion handler for operations that return a document
typedef void (^FirebaseDocumentHandler)(NSDictionary * _Nullable document, NSError * _Nullable error);

/// Completion handler for operations that return multiple documents
typedef void (^FirebaseDocumentsHandler)(NSArray<NSDictionary *> * _Nullable documents, NSError * _Nullable error);

/// Protocol defining Firebase bridge operations
/// This protocol uses only Objective-C compatible types for Kotlin/Native cinterop
@protocol FirebaseBridge <NSObject>

@required

#pragma mark - Daily Log Operations

/// Save a daily log to Firestore
/// @param userId User ID
/// @param logId Log ID
/// @param data Log data as dictionary
/// @param completion Completion handler called with error or nil on success
- (void)saveDailyLogWithUserId:(NSString *)userId
                         logId:(NSString *)logId
                          data:(NSDictionary *)data
                    completion:(FirebaseCompletionHandler)completion;

/// Get a daily log by ID
/// @param userId User ID
/// @param logId Log ID
/// @param completion Completion handler called with document or error
- (void)getDailyLogWithUserId:(NSString *)userId
                        logId:(NSString *)logId
                   completion:(FirebaseDocumentHandler)completion;

/// Get a daily log by date (epoch days)
/// @param userId User ID
/// @param epochDays Date as epoch days
/// @param completion Completion handler called with document or error
- (void)getDailyLogByDateWithUserId:(NSString *)userId
                          epochDays:(int64_t)epochDays
                         completion:(FirebaseDocumentHandler)completion;

/// Get daily logs in a date range
/// @param userId User ID
/// @param startEpochDays Start date as epoch days
/// @param endEpochDays End date as epoch days
/// @param completion Completion handler called with documents array or error
- (void)getLogsInRangeWithUserId:(NSString *)userId
                  startEpochDays:(int64_t)startEpochDays
                    endEpochDays:(int64_t)endEpochDays
                      completion:(FirebaseDocumentsHandler)completion;

/// Delete a daily log
/// @param userId User ID
/// @param logId Log ID
/// @param completion Completion handler called with error or nil on success
- (void)deleteDailyLogWithUserId:(NSString *)userId
                          logId:(NSString *)logId
                     completion:(FirebaseCompletionHandler)completion;

/// Batch save multiple daily logs
/// @param userId User ID
/// @param logsData Array of log dictionaries
/// @param completion Completion handler called with error or nil on success
- (void)batchSaveDailyLogsWithUserId:(NSString *)userId
                            logsData:(NSArray<NSDictionary *> *)logsData
                          completion:(FirebaseCompletionHandler)completion;

@end

NS_ASSUME_NONNULL_END
