import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import { PatternAnalyzer } from './patternAnalyzer';
import { EarlyWarningDetector } from './earlyWarningDetector';
import { DailyLog, Cycle } from '../types';

const db = admin.firestore();

export interface InsightGenerationResult {
  totalUsers: number;
  successfulUsers: number;
  failedUsers: number;
  totalInsightsGenerated: number;
}

export interface UserInsightResult {
  userId: string;
  insightsGenerated: number;
  patterns: string[];
  warnings: string[];
}

/**
 * Generate insights for all users in the system
 */
export async function generateInsightsForAllUsers(): Promise<InsightGenerationResult> {
  const result: InsightGenerationResult = {
    totalUsers: 0,
    successfulUsers: 0,
    failedUsers: 0,
    totalInsightsGenerated: 0
  };

  try {
    // Get all users
    const usersSnapshot = await db.collection('users').get();
    result.totalUsers = usersSnapshot.size;

    functions.logger.info(`Processing insights for ${result.totalUsers} users`);

    // Process users in batches to avoid timeout
    const batchSize = 10;
    const userDocs = usersSnapshot.docs;
    
    for (let i = 0; i < userDocs.length; i += batchSize) {
      const batch = userDocs.slice(i, i + batchSize);
      const batchPromises = batch.map(async (userDoc) => {
        try {
          const userResult = await generateInsightsForUser(userDoc.id);
          result.successfulUsers++;
          result.totalInsightsGenerated += userResult.insightsGenerated;
        } catch (error) {
          functions.logger.error(`Failed to generate insights for user ${userDoc.id}`, error);
          result.failedUsers++;
        }
      });

      await Promise.all(batchPromises);
    }

    return result;
  } catch (error) {
    functions.logger.error('Failed to generate insights for all users', error);
    throw error;
  }
}

/**
 * Generate insights for a specific user
 */
export async function generateInsightsForUser(userId: string): Promise<UserInsightResult> {
  const result: UserInsightResult = {
    userId,
    insightsGenerated: 0,
    patterns: [],
    warnings: []
  };

  try {
    // Get user data
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) {
      throw new Error(`User ${userId} not found`);
    }

    // Get user's daily logs from the last 6 months
    const sixMonthsAgo = new Date();
    sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);

    const logsSnapshot = await db
      .collection('users')
      .doc(userId)
      .collection('dailyLogs')
      .where('date', '>=', admin.firestore.Timestamp.fromDate(sixMonthsAgo))
      .orderBy('date', 'desc')
      .get();

    const dailyLogs: DailyLog[] = logsSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    } as DailyLog));

    // Get user's cycles from the last 6 months
    const cyclesSnapshot = await db
      .collection('users')
      .doc(userId)
      .collection('cycles')
      .where('startDate', '>=', admin.firestore.Timestamp.fromDate(sixMonthsAgo))
      .orderBy('startDate', 'desc')
      .get();

    const cycles: Cycle[] = cyclesSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    } as Cycle));

    // Skip if insufficient data
    if (dailyLogs.length < 30) {
      functions.logger.info(`Insufficient data for user ${userId}: ${dailyLogs.length} logs`);
      return result;
    }

    // Initialize analyzers
    const patternAnalyzer = new PatternAnalyzer();
    const earlyWarningDetector = new EarlyWarningDetector();

    // Generate pattern insights
    const patternInsights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, cycles);
    result.patterns = patternInsights.map(insight => insight.insightText);

    // Generate early warning insights
    const warningInsights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, cycles);
    result.warnings = warningInsights.map(insight => insight.insightText);

    // Combine all insights
    const allInsights = [...patternInsights, ...warningInsights];

    // Save insights to Firestore
    const batch = db.batch();
    for (const insight of allInsights) {
      const insightRef = db
        .collection('users')
        .doc(userId)
        .collection('insights')
        .doc();
      
      batch.set(insightRef, {
        ...insight,
        id: insightRef.id,
        generatedDate: admin.firestore.FieldValue.serverTimestamp()
      });
    }

    await batch.commit();
    result.insightsGenerated = allInsights.length;

    functions.logger.info(`Generated ${allInsights.length} insights for user ${userId}`);
    return result;

  } catch (error) {
    functions.logger.error(`Failed to generate insights for user ${userId}`, error);
    throw error;
  }
}