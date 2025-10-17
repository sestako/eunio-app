import { DailyLog, Cycle, PeriodFlow, Mood } from '../types';
import * as admin from 'firebase-admin';

// Helper function to create mock timestamps
const createMockTimestamp = (seconds: number): admin.firestore.Timestamp => ({
  seconds,
  nanoseconds: 0
} as admin.firestore.Timestamp);

// Pre-computed base timestamp for performance
const BASE_TIMESTAMP = createMockTimestamp(1704067200); // 2024-01-01

// Reusable mock objects to avoid recreation
const MOCK_DAILY_LOG_BASE: Omit<DailyLog, 'id' | 'date'> = {
  userId: 'test-user-123',
  createdAt: BASE_TIMESTAMP,
  updatedAt: BASE_TIMESTAMP
};

const MOCK_CYCLE_BASE: Omit<Cycle, 'id' | 'startDate'> = {
  userId: 'test-user-123',
  cycleLength: 28
};

// Fast factory functions using object spread with minimal computation
export const createFastDailyLog = (id: number, dayOffset = 0, overrides: Partial<DailyLog> = {}): DailyLog => ({
  ...MOCK_DAILY_LOG_BASE,
  id: `log-${id}`,
  date: createMockTimestamp(BASE_TIMESTAMP.seconds - (dayOffset * 86400)), // 86400 = seconds in a day
  ...overrides
});

export const createFastCycle = (id: number, overrides: Partial<Cycle> = {}): Cycle => ({
  ...MOCK_CYCLE_BASE,
  id: `cycle-${id}`,
  startDate: BASE_TIMESTAMP,
  ...overrides
});

// Pre-built test data sets for common scenarios
export const REGULAR_CYCLES = [
  createFastCycle(1, { cycleLength: 28 }),
  createFastCycle(2, { cycleLength: 29 }),
  createFastCycle(3, { cycleLength: 27 }),
  createFastCycle(4, { cycleLength: 28 })
];

export const IRREGULAR_CYCLES = [
  createFastCycle(1, { cycleLength: 18 }),
  createFastCycle(2, { cycleLength: 19 }),
  createFastCycle(3, { cycleLength: 20 })
];

// Fast array generation for daily logs
export const createDailyLogArray = (count: number, pattern: (i: number) => Partial<DailyLog> = () => ({})): DailyLog[] => {
  const logs: DailyLog[] = new Array(count);
  for (let i = 0; i < count; i++) {
    logs[i] = createFastDailyLog(i + 1, i, pattern(i));
  }
  return logs;
};

// Common patterns
export const SYMPTOM_PATTERN = (i: number) => ({
  symptoms: i % 3 === 0 ? ['CRAMPS'] : []
});

export const MOOD_PATTERN = (i: number) => ({
  mood: i % 2 === 0 ? Mood.HAPPY : Mood.CALM
});

export const CONCERNING_PATTERN = (i: number) => ({
  symptoms: i % 2 === 0 ? ['SEVERE_CRAMPS', 'HEAVY_BLEEDING'] : [],
  mood: i % 3 === 0 ? Mood.SAD : Mood.ANXIOUS
});

export const COMPREHENSIVE_PATTERN = (i: number) => ({
  symptoms: i % 4 === 0 ? ['CRAMPS', 'BLOATING'] : [],
  mood: i % 3 === 0 ? Mood.HAPPY : Mood.NEUTRAL,
  bbt: 98.0 + ((i % 10) * 0.05),
  periodFlow: i % 28 < 5 ? PeriodFlow.MEDIUM : undefined
});