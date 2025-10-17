import { PatternAnalyzer } from '../insights/patternAnalyzer';
import { DailyLog, Cycle, InsightType, PeriodFlow, Mood, CervicalMucus, OPKResult } from '../types';
import * as admin from 'firebase-admin';

describe('PatternAnalyzer', () => {
  let patternAnalyzer: PatternAnalyzer;
  const userId = 'test-user-123';

  beforeEach(() => {
    patternAnalyzer = new PatternAnalyzer();
  });

  const createMockTimestamp = (date: Date) => ({
    seconds: Math.floor(date.getTime() / 1000),
    nanoseconds: 0
  } as admin.firestore.Timestamp);

  const createMockDailyLog = (overrides: Partial<DailyLog> = {}): DailyLog => ({
    id: `log-${Date.now()}`,
    userId,
    date: createMockTimestamp(new Date()),
    createdAt: createMockTimestamp(new Date()),
    updatedAt: createMockTimestamp(new Date()),
    ...overrides
  });

  const createMockCycle = (overrides: Partial<Cycle> = {}): Cycle => ({
    id: `cycle-${Date.now()}`,
    userId,
    startDate: createMockTimestamp(new Date()),
    cycleLength: 28,
    ...overrides
  });

  describe('analyzeCycleLengthPatterns', () => {
    it('should detect regular cycle patterns', async () => {
      const cycles = [
        createMockCycle({ cycleLength: 28 }),
        createMockCycle({ cycleLength: 29 }),
        createMockCycle({ cycleLength: 27 }),
        createMockCycle({ cycleLength: 28 })
      ];

      const insights = await patternAnalyzer.analyzePatterns(userId, [], cycles);
      
      const regularCycleInsight = insights.find(insight => 
        insight.insightText.includes('very regular') && 
        insight.type === InsightType.PATTERN_RECOGNITION
      );

      expect(regularCycleInsight).toBeDefined();
      expect(regularCycleInsight?.confidence).toBeGreaterThan(0.8);
    });

    it('should detect irregular cycle patterns', async () => {
      const cycles = [
        createMockCycle({ cycleLength: 21 }),
        createMockCycle({ cycleLength: 35 }),
        createMockCycle({ cycleLength: 25 }),
        createMockCycle({ cycleLength: 40 }),
        createMockCycle({ cycleLength: 22 })
      ];

      const insights = await patternAnalyzer.analyzePatterns(userId, [], cycles);
      
      const irregularCycleInsight = insights.find(insight => 
        insight.insightText.includes('vary significantly') && 
        insight.type === InsightType.PATTERN_RECOGNITION
      );

      expect(irregularCycleInsight).toBeDefined();
      expect(irregularCycleInsight?.actionable).toBe(true);
    });
  });

  describe('analyzeSymptomPatterns', () => {
    it('should identify most common symptoms', async () => {
      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          symptoms: i % 3 === 0 ? ['CRAMPS'] : i % 5 === 0 ? ['BLOATING'] : ['CRAMPS'],
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, []);
      
      const symptomInsight = insights.find(insight => 
        insight.insightText.includes('cramps') && 
        insight.type === InsightType.PATTERN_RECOGNITION
      );

      expect(symptomInsight).toBeDefined();
    });

    it('should detect PMS patterns', async () => {
      const dailyLogs: DailyLog[] = [];
      
      // Create logs with symptoms before period - create in chronological order for proper detection
      for (let cycle = 0; cycle < 3; cycle++) {
        for (let day = 0; day < 28; day++) {
          const date = new Date(Date.now() - (84 - (cycle * 28 + day)) * 24 * 60 * 60 * 1000);
          
          // Add symptoms in days 25-27 (pre-menstrual), period flow on day 0 of next cycle
          const symptoms = (day >= 25 && day <= 27) ? ['MOOD_SWINGS', 'BLOATING'] : [];
          const periodFlow = day === 0 && cycle > 0 ? PeriodFlow.MEDIUM : undefined;
          
          dailyLogs.push(createMockDailyLog({
            id: `log-${cycle}-${day}`,
            date: createMockTimestamp(date),
            symptoms,
            periodFlow
          }));
        }
      }
      
      // Add period flow at the beginning of the first cycle
      dailyLogs.push(createMockDailyLog({
        id: 'log-period-start',
        date: createMockTimestamp(new Date(Date.now() - 84 * 24 * 60 * 60 * 1000)),
        periodFlow: PeriodFlow.MEDIUM
      }));

      const insights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, []);
      
      const pmsInsight = insights.find(insight => 
        insight.insightText.includes('before your period')
      );

      expect(pmsInsight).toBeDefined();
      expect(pmsInsight?.actionable).toBe(true);
    });
  });

  describe('analyzeMoodPatterns', () => {
    it('should identify dominant mood patterns', async () => {
      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          mood: i % 2 === 0 ? Mood.HAPPY : Mood.CALM,
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, []);
      
      const moodInsight = insights.find(insight => 
        insight.insightText.includes('most commonly') && 
        insight.type === InsightType.PATTERN_RECOGNITION
      );

      expect(moodInsight).toBeDefined();
    });
  });

  describe('analyzeBBTPatterns', () => {
    it('should detect temperature shifts indicating ovulation', async () => {
      const dailyLogs: DailyLog[] = [];
      
      // Create BBT pattern with temperature shift
      for (let i = 0; i < 28; i++) {
        const date = new Date(Date.now() - i * 24 * 60 * 60 * 1000);
        const bbt = i < 14 ? 97.2 + Math.random() * 0.2 : 98.0 + Math.random() * 0.2;
        
        dailyLogs.push(createMockDailyLog({
          date: createMockTimestamp(date),
          bbt
        }));
      }

      const insights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, []);
      
      const bbtInsight = insights.find(insight => 
        insight.insightText.includes('temperature shifts')
      );

      expect(bbtInsight).toBeDefined();
      expect(bbtInsight?.confidence).toBeGreaterThan(0.7);
    });

    it('should handle insufficient BBT data gracefully', async () => {
      const dailyLogs = Array.from({ length: 5 }, (_, i) => 
        createMockDailyLog({
          bbt: 98.0,
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, []);
      
      const bbtInsights = insights.filter(insight => 
        insight.insightText.includes('temperature')
      );

      expect(bbtInsights).toHaveLength(0);
    });
  });

  describe('analyzeFertilityPatterns', () => {
    it('should detect fertile cervical mucus patterns', async () => {
      const dailyLogs = Array.from({ length: 20 }, (_, i) => 
        createMockDailyLog({
          cervicalMucus: i % 3 === 0 ? CervicalMucus.EGG_WHITE : CervicalMucus.DRY,
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, []);
      
      const fertilityInsight = insights.find(insight => 
        insight.insightText.includes('fertile-quality cervical mucus') &&
        insight.type === InsightType.FERTILITY_WINDOW
      );

      expect(fertilityInsight).toBeDefined();
      expect(fertilityInsight?.actionable).toBe(true);
    });

    it('should detect positive OPK patterns', async () => {
      const dailyLogs = Array.from({ length: 15 }, (_, i) => 
        createMockDailyLog({
          opkResult: i % 5 === 0 ? OPKResult.POSITIVE : OPKResult.NEGATIVE,
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await patternAnalyzer.analyzePatterns(userId, dailyLogs, []);
      
      const opkInsight = insights.find(insight => 
        insight.insightText.includes('positive ovulation tests') &&
        insight.type === InsightType.FERTILITY_WINDOW
      );

      expect(opkInsight).toBeDefined();
      expect(opkInsight?.confidence).toBeGreaterThan(0.8);
    });
  });

  describe('error handling', () => {
    it('should handle empty data gracefully', async () => {
      const insights = await patternAnalyzer.analyzePatterns(userId, [], []);
      expect(insights).toEqual([]);
    });

    it('should handle malformed data gracefully', async () => {
      const malformedLogs = [
        createMockDailyLog({ symptoms: undefined }),
        createMockDailyLog({ mood: undefined }),
        createMockDailyLog({ bbt: undefined })
      ];

      const insights = await patternAnalyzer.analyzePatterns(userId, malformedLogs, []);
      expect(insights).toBeInstanceOf(Array);
    });
  });
});