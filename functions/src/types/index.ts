import * as admin from 'firebase-admin';

export enum InsightType {
  PATTERN_RECOGNITION = 'PATTERN_RECOGNITION',
  EARLY_WARNING = 'EARLY_WARNING',
  CYCLE_PREDICTION = 'CYCLE_PREDICTION',
  FERTILITY_WINDOW = 'FERTILITY_WINDOW'
}

export enum HealthGoal {
  CONCEPTION = 'CONCEPTION',
  CONTRACEPTION = 'CONTRACEPTION',
  CYCLE_TRACKING = 'CYCLE_TRACKING',
  GENERAL_HEALTH = 'GENERAL_HEALTH'
}

export enum PeriodFlow {
  NONE = 'NONE',
  LIGHT = 'LIGHT',
  MEDIUM = 'MEDIUM',
  HEAVY = 'HEAVY',
  SPOTTING = 'SPOTTING'
}

export enum Mood {
  HAPPY = 'HAPPY',
  SAD = 'SAD',
  ANXIOUS = 'ANXIOUS',
  CALM = 'CALM',
  IRRITABLE = 'IRRITABLE',
  ENERGETIC = 'ENERGETIC',
  TIRED = 'TIRED',
  NEUTRAL = 'NEUTRAL',
  DEPRESSED = 'DEPRESSED'
}

export enum CervicalMucus {
  DRY = 'DRY',
  STICKY = 'STICKY',
  CREAMY = 'CREAMY',
  EGG_WHITE = 'EGG_WHITE',
  WATERY = 'WATERY'
}

export enum OPKResult {
  NEGATIVE = 'NEGATIVE',
  POSITIVE = 'POSITIVE',
  INVALID = 'INVALID'
}

export interface User {
  id: string;
  email: string;
  name: string;
  onboardingComplete: boolean;
  primaryGoal: HealthGoal;
  createdAt: admin.firestore.Timestamp;
  updatedAt: admin.firestore.Timestamp;
}

export interface Cycle {
  id: string;
  userId: string;
  startDate: admin.firestore.Timestamp;
  endDate?: admin.firestore.Timestamp;
  predictedOvulationDate?: admin.firestore.Timestamp;
  confirmedOvulationDate?: admin.firestore.Timestamp;
  cycleLength?: number;
  lutealPhaseLength?: number;
}

export interface DailyLog {
  id: string;
  userId: string;
  date: admin.firestore.Timestamp;
  periodFlow?: PeriodFlow;
  symptoms?: string[];
  mood?: Mood;
  sexualActivity?: {
    occurred: boolean;
    protection: string[];
  };
  bbt?: number;
  cervicalMucus?: CervicalMucus;
  opkResult?: OPKResult;
  notes?: string;
  createdAt: admin.firestore.Timestamp;
  updatedAt: admin.firestore.Timestamp;
}

export interface Insight {
  id: string;
  userId: string;
  generatedDate: Date | admin.firestore.Timestamp;
  insightText: string;
  type: InsightType;
  isRead: boolean;
  relatedLogIds: string[];
  confidence: number;
  actionable: boolean;
}

export interface HealthReport {
  id: string;
  userId: string;
  generatedDate: admin.firestore.Timestamp;
  reportType: string;
  dateRange: {
    start: admin.firestore.Timestamp;
    end: admin.firestore.Timestamp;
  };
  data: {
    cycleSummary: Record<string, unknown>;
    symptomAnalysis: Record<string, unknown>;
    insights: Insight[];
  };
  pdfUrl?: string;
}

// Common symptom types
export const SYMPTOM_TYPES = [
  'CRAMPS',
  'BLOATING',
  'BREAST_TENDERNESS',
  'HEADACHE',
  'NAUSEA',
  'FATIGUE',
  'ACNE',
  'BACK_PAIN',
  'MOOD_SWINGS',
  'FOOD_CRAVINGS',
  'INSOMNIA',
  'HOT_FLASHES',
  'DIZZINESS',
  'CONSTIPATION',
  'DIARRHEA',
  'SEVERE_CRAMPS',
  'HEAVY_BLEEDING',
  'BREAST_PAIN'
] as const;

export type SymptomType = typeof SYMPTOM_TYPES[number];