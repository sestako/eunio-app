import { EarlyWarningDetector } from '../insights/earlyWarningDetector';
import { DailyLog, Cycle, InsightType, PeriodFlow, Mood } from '../types';
import * as admin from 'firebase-admin';

describe('EarlyWarningDetector', () => {
  let earlyWarningDetector: EarlyWarningDetector;
  const userId = 'test-user-123';

  beforeEach(() => {
    earlyWarningDetector = new EarlyWarningDetector();
  });

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

  describe('detectIrregularBleeding', () => {
    it('should detect prolonged bleeding episodes', async () => {
      const dailyLogs: DailyLog[] = [];
      
      // Create 10 consecutive days of bleeding - create in chronological order
      for (let i = 0; i < 15; i++) {
        const date = new Date(Date.now() - (15 - i) * 24 * 60 * 60 * 1000);
        dailyLogs.push(createMockDailyLog({
          id: `log-${i}`,
          date: createMockTimestamp(date),
          periodFlow: PeriodFlow.MEDIUM
        }));
      }

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const prolongedBleedingInsight = insights.find(insight => 
        insight.insightText.includes('consecutive days of bleeding') &&
        insight.type === InsightType.EARLY_WARNING
      );

      expect(prolongedBleedingInsight).toBeDefined();
      expect(prolongedBleedingInsight?.actionable).toBe(true);
      expect(prolongedBleedingInsight?.confidence).toBeGreaterThan(0.7);
    });

    it('should detect frequent bleeding episodes', async () => {
      const dailyLogs: DailyLog[] = [];
      
      // Create multiple short bleeding episodes with short intervals - create in chronological order
      const episodes = [
        { start: 0, duration: 3 },
        { start: 15, duration: 3 },
        { start: 30, duration: 3 },
        { start: 45, duration: 3 }
      ];

      episodes.forEach((episode, episodeIndex) => {
        for (let i = 0; i < episode.duration; i++) {
          const date = new Date(Date.now() - (60 - (episode.start + i)) * 24 * 60 * 60 * 1000);
          dailyLogs.push(createMockDailyLog({
            id: `log-${episodeIndex}-${i}`,
            date: createMockTimestamp(date),
            periodFlow: PeriodFlow.LIGHT
          }));
        }
      });

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const frequentBleedingInsight = insights.find(insight => 
        insight.insightText.includes('less than 21 days between them')
      );

      expect(frequentBleedingInsight).toBeDefined();
      expect(frequentBleedingInsight?.actionable).toBe(true);
    });

    it('should handle normal bleeding patterns without warnings', async () => {
      const dailyLogs: DailyLog[] = [];
      
      // Create normal 5-day periods with 28-day intervals
      for (let cycle = 0; cycle < 3; cycle++) {
        for (let day = 0; day < 5; day++) {
          const date = new Date(Date.now() - (cycle * 28 + day) * 24 * 60 * 60 * 1000);
          dailyLogs.push(createMockDailyLog({
            date: createMockTimestamp(date),
            periodFlow: PeriodFlow.MEDIUM
          }));
        }
      }

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const bleedingWarnings = insights.filter(insight => 
        insight.insightText.includes('bleeding') ||
        insight.insightText.includes('consecutive days')
      );

      expect(bleedingWarnings).toHaveLength(0);
    });
  });

  describe('detectConcerningSymptoms', () => {
    it('should detect frequent severe symptoms', async () => {
      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          symptoms: i % 3 === 0 ? ['SEVERE_CRAMPS'] : [],
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const severeSymptomInsight = insights.find(insight => 
        insight.insightText.includes('severe cramping') &&
        insight.type === InsightType.EARLY_WARNING
      );

      expect(severeSymptomInsight).toBeDefined();
      expect(severeSymptomInsight?.actionable).toBe(true);
    });

    it('should detect multiple symptoms occurring together', async () => {
      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          symptoms: i % 2 === 0 ? ['CRAMPS', 'BLOATING', 'HEADACHE', 'NAUSEA'] : [],
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const multiSymptomInsight = insights.find(insight => 
        insight.insightText.includes('multiple symptoms together')
      );

      expect(multiSymptomInsight).toBeDefined();
      expect(multiSymptomInsight?.actionable).toBe(true);
    });

    it('should not trigger warnings for normal symptom levels', async () => {
      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          symptoms: i % 10 === 0 ? ['CRAMPS'] : [],
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const symptomWarnings = insights.filter(insight => 
        insight.insightText.includes('cramping') ||
        insight.insightText.includes('symptoms')
      );

      expect(symptomWarnings).toHaveLength(0);
    });
  });

  describe('detectCycleIrregularities', () => {
    it('should detect very short cycles', async () => {
      const cycles = [
        createMockCycle({ cycleLength: 18 }),
        createMockCycle({ cycleLength: 19 }),
        createMockCycle({ cycleLength: 20 }),
        createMockCycle({ cycleLength: 28 })
      ];

      const insights = await earlyWarningDetector.detectWarnings(userId, [], cycles);
      
      const shortCycleInsight = insights.find(insight => 
        insight.insightText.includes('shorter than 21 days')
      );

      expect(shortCycleInsight).toBeDefined();
      expect(shortCycleInsight?.actionable).toBe(true);
    });

    it('should detect very long cycles', async () => {
      const cycles = [
        createMockCycle({ cycleLength: 38 }),
        createMockCycle({ cycleLength: 42 }),
        createMockCycle({ cycleLength: 28 }),
        createMockCycle({ cycleLength: 40 })
      ];

      const insights = await earlyWarningDetector.detectWarnings(userId, [], cycles);
      
      const longCycleInsight = insights.find(insight => 
        insight.insightText.includes('longer than 35 days')
      );

      expect(longCycleInsight).toBeDefined();
      expect(longCycleInsight?.actionable).toBe(true);
    });

    it('should detect highly irregular cycles', async () => {
      const cycles = [
        createMockCycle({ cycleLength: 21 }),
        createMockCycle({ cycleLength: 35 }),
        createMockCycle({ cycleLength: 25 }),
        createMockCycle({ cycleLength: 40 }),
        createMockCycle({ cycleLength: 22 })
      ];

      const insights = await earlyWarningDetector.detectWarnings(userId, [], cycles);
      
      const irregularCycleInsight = insights.find(insight => 
        insight.insightText.includes('vary significantly')
      );

      expect(irregularCycleInsight).toBeDefined();
      expect(irregularCycleInsight?.actionable).toBe(true);
    });
  });

  describe('detectTemperatureAnomalies', () => {
    it('should detect consistently high temperatures', async () => {
      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          bbt: i < 5 ? 99.5 : 98.2, // 5 days of high temp
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const highTempInsight = insights.find(insight => 
        insight.insightText.includes('unusually high temperatures')
      );

      expect(highTempInsight).toBeDefined();
      expect(highTempInsight?.actionable).toBe(true);
    });

    it('should detect lack of temperature variation', async () => {
      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          bbt: 98.2, // Constant temperature
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const noVariationInsight = insights.find(insight => 
        insight.insightText.includes('minimal variation')
      );

      expect(noVariationInsight).toBeDefined();
      expect(noVariationInsight?.actionable).toBe(true);
    });

    it('should handle insufficient temperature data', async () => {
      const dailyLogs = Array.from({ length: 5 }, (_, i) => 
        createMockDailyLog({
          bbt: 98.2,
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const tempInsights = insights.filter(insight => 
        insight.insightText.includes('temperature')
      );

      expect(tempInsights).toHaveLength(0);
    });
  });

  describe('detectMoodConcerns', () => {
    it('should detect high percentage of negative moods', async () => {
      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          mood: i % 2 === 0 ? Mood.SAD : Mood.ANXIOUS,
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const negativeMoodInsight = insights.find(insight => 
        insight.insightText.includes('negative moods')
      );

      expect(negativeMoodInsight).toBeDefined();
      expect(negativeMoodInsight?.actionable).toBe(true);
    });

    it('should detect frequent mood swings', async () => {
      const moods = [Mood.HAPPY, Mood.SAD, Mood.ENERGETIC, Mood.DEPRESSED, Mood.CALM, Mood.ANXIOUS];
      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          mood: moods[i % moods.length],
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const moodSwingInsight = insights.find(insight => 
        insight.insightText.includes('frequent mood changes')
      );

      expect(moodSwingInsight).toBeDefined();
      expect(moodSwingInsight?.actionable).toBe(true);
    });

    it('should handle stable positive moods without warnings', async () => {
      const dailyLogs = Array.from({ length: 30 }, (_, i) => 
        createMockDailyLog({
          mood: i % 2 === 0 ? Mood.HAPPY : Mood.CALM,
          date: createMockTimestamp(new Date(Date.now() - i * 24 * 60 * 60 * 1000))
        })
      );

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, []);
      
      const moodWarnings = insights.filter(insight => 
        insight.insightText.includes('mood')
      );

      expect(moodWarnings).toHaveLength(0);
    });
  });

  describe('error handling', () => {
    it('should handle empty data gracefully', async () => {
      const insights = await earlyWarningDetector.detectWarnings(userId, [], []);
      expect(insights).toEqual([]);
    });

    it('should handle insufficient data gracefully', async () => {
      const dailyLogs = [createMockDailyLog()];
      const cycles = [createMockCycle()];

      const insights = await earlyWarningDetector.detectWarnings(userId, dailyLogs, cycles);
      expect(insights).toBeInstanceOf(Array);
    });
  });
});