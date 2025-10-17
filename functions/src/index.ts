import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { generateInsightsForAllUsers, generateInsightsForUser } from './insights/insightGenerator';

// Initialize Firebase Admin SDK
admin.initializeApp();

/**
 * Scheduled function that runs daily at 2 AM UTC to generate insights for all users
 */
export const dailyInsightGeneration = functions.pubsub
  .schedule('0 2 * * *')
  .timeZone('UTC')
  .onRun(async (context) => {
    functions.logger.info('Starting daily insight generation', { timestamp: context.timestamp });
    
    try {
      const result = await generateInsightsForAllUsers();
      functions.logger.info('Daily insight generation completed', result);
      return result;
    } catch (error) {
      functions.logger.error('Daily insight generation failed', error);
      throw error;
    }
  });

/**
 * HTTP function to manually trigger insight generation for a specific user (for testing)
 */
export const generateUserInsights = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const userId = data.userId || context.auth.uid;
  
  try {
    functions.logger.info('Generating insights for user', { userId });
    const result = await generateInsightsForUser(userId);
    functions.logger.info('User insight generation completed', { userId, result });
    return result;
  } catch (error) {
    functions.logger.error('User insight generation failed', { userId, error });
    throw new functions.https.HttpsError('internal', 'Failed to generate insights');
  }
});