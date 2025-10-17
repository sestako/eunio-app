import { PatternAnalyzer } from '../insights/patternAnalyzer';
import { EarlyWarningDetector } from '../insights/earlyWarningDetector';
import { InsightType } from '../types';
import { 
  REGULAR_CYCLES, 
  IRREGULAR_CYCLES, 
  createDailyLogArray, 
  SYMPTOM_PATTERN, 
  MOOD_PATTERN, 
  CONCERNING_PATTERN, 
  COMPREHENSIVE_PATTERN 
} from './testDataFactory';

describe('Integration Tests', () => {
  const userId = 'test-user-123';

  describe('Pattern Analysis Integration', () => {
    it('should analyze patterns and generate insights', async () => {
      const patternAnalyzer = new PatternAnalyzer();
      const dailyLogs = createDailyLogArray(30, (i) => ({ ...SYMPTOM_PATTERN(i), ...MOOD_PATTERN(i) }));
      const insights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, REGULAR_CYCLES);
      
      expect(insights).toBeInstanceOf(Array);
      expect(insights.length).toBeGreaterThan(0);
      
      // Check that insights have required properties
      insights.forEach(insight => {
        expect(insight).toHaveProperty('userId', userId);
        expect(insight).toHaveProperty('insightText');
        expect(insight).toHaveProperty('type');
        expect(insight).toHaveProperty('confidence');
        expect(typeof insight.confidence).toBe('number');
        expect(insight.confidence).toBeGreaterThan(0);
        expect(insight.confidence).toBeLessThanOrEqual(1);
      });
    });
  });

  describe('Early Warning Detection Integration', () => {
    it('should detect warnings and generate insights', async () => {
      const earlyWarningDetector = new EarlyWarningDetector();
      const dailyLogs = createDailyLogArray(30, CONCERNING_PATTERN);
      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, IRREGULAR_CYCLES);
      
      expect(insights).toBeInstanceOf(Array);
      
      // Check that insights have required properties
      insights.forEach(insight => {
        expect(insight).toHaveProperty('userId', userId);
        expect(insight).toHaveProperty('insightText');
        expect(insight).toHaveProperty('type', InsightType.EARLY_WARNING);
        expect(insight).toHaveProperty('confidence');
        expect(insight).toHaveProperty('actionable');
        expect(typeof insight.confidence).toBe('number');
        expect(insight.confidence).toBeGreaterThan(0);
        expect(insight.confidence).toBeLessThanOrEqual(1);
      });
    });
  });

  describe('Insight Quality Integration', () => {
    it('should generate meaningful insights with proper confidence scores', async () => {
      const patternAnalyzer = new PatternAnalyzer();
      const earlyWarningDetector = new EarlyWarningDetector();
      const dailyLogs = createDailyLogArray(50, COMPREHENSIVE_PATTERN);

      const patternInsights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, REGULAR_CYCLES);
      const warningInsights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, REGULAR_CYCLES);
      
      const allInsights = [...patternInsights, ...warningInsights];
      
      expect(allInsights.length).toBeGreaterThan(0);
      
      // Verify insight quality
      allInsights.forEach(insight => {
        // Should have meaningful text
        expect(insight.insightText.length).toBeGreaterThan(10);
        
        // Should have valid confidence score
        expect(insight.confidence).toBeGreaterThan(0);
        expect(insight.confidence).toBeLessThanOrEqual(1);
        
        // Should have valid type
        expect(Object.values(InsightType)).toContain(insight.type);
        
        // Should have boolean actionable flag
        expect(typeof insight.actionable).toBe('boolean');
        
        // Should have related log IDs array
        expect(Array.isArray(insight.relatedLogIds)).toBe(true);
      });
    });
  });
});