// Simple integration test for insight generator
import { PatternAnalyzer } from '../insights/patternAnalyzer';
import { EarlyWarningDetector } from '../insights/earlyWarningDetector';
import { DailyLog, Cycle, InsightType, Mood } from '../types';
import * as admin from 'firebase-admin';

describe('InsightGenerator Simple Tests', () => {
  const userId = 'test-user-123';

  const createMockTimestamp = (date: Date) => ({
    seconds: Math.floor(date.getTime() / 1000),
    nanoseconds: 0
  } as admin.firestore.Timestamp);

  const createMockDailyLog = (overrides: Partial<DailyLog> = {}): DailyLog => ({
    id: `log-${Date.now()}-${Math.random()}`,
    userId,
    date: createMockTimestamp(new Date()),
    createdAt: createMockTimestamp(new Date()),
    updatedAt: createMockTimestamp(new Date()),
    ...overrides
  });

  const createMockCycle = (overrides: Partial<Cycle> = {}): Cycle => ({
    id: `cycle-${Date.now()}-${Math.random()}`,
    userId,
    startDate: createMockTimestamp(new Date()),
    cycleLength: 28,
    ...overrides
  });

  describe('Pattern Analysis and Warning Detection', () => {
    it('should generate pattern insights from regular cycles', async () => {
      const patternAnalyzer = new PatternAnalyzer();
      
      const cycles = [
        createMockCycle({ cycleLength: 28 }),
        createMockCycle({ cycleLength: 29 }),
        createMockCycle({ cycleLength: 27 }),
        createMockCycle({ cycleLength: 28 })
      ];

      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          symptoms: i % 3 === 0 ? ['CRAMPS'] : [],
          mood: i % 2 === 0 ? Mood.HAPPY : Mood.CALM,
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, cycles);
      
      expect(insights).toBeInstanceOf(Array);
      expect(insights.length).toBeGreaterThan(0);
      
      // Verify insight structure
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

    it('should generate warning insights from concerning patterns', async () => {
      const earlyWarningDetector = new EarlyWarningDetector();
      
      const cycles = [
        createMockCycle({ cycleLength: 18 }), // Short cycle
        createMockCycle({ cycleLength: 19 }),
        createMockCycle({ cycleLength: 20 })
      ];

      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          symptoms: i % 2 === 0 ? ['SEVERE_CRAMPS', 'HEAVY_BLEEDING'] : [],
          mood: i % 3 === 0 ? Mood.SAD : Mood.ANXIOUS,
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, cycles);
      
      expect(insights).toBeInstanceOf(Array);
      
      // Check that warning insights have proper structure
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

    it('should handle empty data gracefully', async () => {
      const patternAnalyzer = new PatternAnalyzer();
      const earlyWarningDetector = new EarlyWarningDetector();
      
      const patternInsights = await patternAnalyzer.analyzePatterns(userId, [], []);
      const warningInsights = await earlyWarningDetector.detectWarnings(userId, [], []);
      
      expect(patternInsights).toEqual([]);
      expect(warningInsights).toEqual([]);
    });

    it('should generate insights with proper confidence scoring', async () => {
      const patternAnalyzer = new PatternAnalyzer();
      
      // Create data that should generate high-confidence insights
      const cycles = Array.from({ length: 6 }, () => 
        createMockCycle({ cycleLength: 28 }) // Very regular cycles
      );

      const dailyLogs = Array.from({ length: 50 }, (_, i) => 
        createMockDailyLog({
          symptoms: i % 4 === 0 ? ['CRAMPS'] : [], // Regular symptom pattern
          mood: Mood.HAPPY, // Consistent mood
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, cycles);
      
      // Should have at least one high-confidence insight about regular cycles
      const regularCycleInsight = insights.find(insight => 
        insight.insightText.includes('regular') && insight.confidence > 0.8
      );
      
      expect(regularCycleInsight).toBeDefined();
    });
  });

  describe('Insight Quality Validation', () => {
    it('should generate actionable insights for concerning patterns', async () => {
      const earlyWarningDetector = new EarlyWarningDetector();
      
      // Create concerning pattern data
      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          symptoms: ['SEVERE_CRAMPS', 'HEAVY_BLEEDING'], // Frequent concerning symptoms
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      // Should have actionable insights for concerning symptoms
      const actionableInsights = insights.filter(insight => insight.actionable);
      expect(actionableInsights.length).toBeGreaterThan(0);
      
      actionableInsights.forEach(insight => {
        expect(insight.insightText.length).toBeGreaterThan(20); // Meaningful text
        expect(insight.type).toBe(InsightType.EARLY_WARNING);
      });
    });

    it('should include related log IDs for relevant insights', async () => {
      const patternAnalyzer = new PatternAnalyzer();
      
      const dailyLogs = Array.from({ length: 25 }, (_, i) => 
        createMockDailyLog({
          bbt: 98.0 + (i > 12 ? 0.5 : 0), // Temperature shift pattern
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, []);
      
      // BBT insights should include related log IDs
      const bbtInsights = insights.filter(insight => 
        insight.insightText.includes('temperature')
      );
      
      bbtInsights.forEach(insight => {
        expect(Array.isArray(insight.relatedLogIds)).toBe(true);
        if (insight.relatedLogIds.length > 0) {
          insight.relatedLogIds.forEach(logId => {
            expect(typeof logId).toBe('string');
            expect(logId.length).toBeGreaterThan(0);
          });
        }
      });
    });
  });
});