import * as functions from 'firebase-functions';
import { DailyLog, Cycle, Insight, InsightType } from '../types';

export class PatternAnalyzer {
  
  /**
   * Analyze patterns in user's health data and generate insights
   */
  async analyzePatterns(userId: string, dailyLogs: DailyLog[], cycles: Cycle[]): Promise<Insight[]> {
    const insights: Insight[] = [];

    try {
      // Analyze cycle length patterns
      const cycleLengthInsights = this.analyzeCycleLengthPatterns(userId, cycles);
      insights.push(...cycleLengthInsights);

      // Analyze symptom patterns
      const symptomInsights = this.analyzeSymptomPatterns(userId, dailyLogs);
      insights.push(...symptomInsights);

      // Analyze mood patterns
      const moodInsights = this.analyzeMoodPatterns(userId, dailyLogs);
      insights.push(...moodInsights);

      // Analyze BBT patterns
      const bbtInsights = this.analyzeBBTPatterns(userId, dailyLogs);
      insights.push(...bbtInsights);

      // Analyze fertility patterns
      const fertilityInsights = this.analyzeFertilityPatterns(userId, dailyLogs, cycles);
      insights.push(...fertilityInsights);

      return insights;
    } catch (error) {
      functions.logger.error('Error analyzing patterns', { userId, error });
      return [];
    }
  }

  /**
   * Analyze cycle length patterns
   */
  private analyzeCycleLengthPatterns(userId: string, cycles: Cycle[]): Insight[] {
    const insights: Insight[] = [];
    
    if (cycles.length < 3) {return insights;}

    const cycleLengths = cycles
      .filter(cycle => cycle.cycleLength && cycle.cycleLength > 0)
      .map(cycle => cycle.cycleLength as number);

    if (cycleLengths.length < 3) {
      return insights;
    }

    const avgLength = cycleLengths.reduce((sum, length) => sum + length, 0) / cycleLengths.length;
    const variance = cycleLengths.reduce((sum, length) => sum + Math.pow(length - avgLength, 2), 0) / cycleLengths.length;
    const stdDev = Math.sqrt(variance);

    // Regular cycle pattern
    if (stdDev <= 2) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `Your cycles are very regular with an average length of ${Math.round(avgLength)} days. This consistency suggests healthy hormonal balance.`,
        type: InsightType.PATTERN_RECOGNITION,
        isRead: false,
        relatedLogIds: [],
        confidence: 0.9,
        actionable: false
      });
    }

    // Irregular cycle pattern
    if (stdDev > 7) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `Your cycle lengths vary significantly (${Math.round(avgLength - stdDev)}-${Math.round(avgLength + stdDev)} days). Consider tracking stress, diet, and exercise as these can affect cycle regularity.`,
        type: InsightType.PATTERN_RECOGNITION,
        isRead: false,
        relatedLogIds: [],
        confidence: 0.8,
        actionable: true
      });
    }

    return insights;
  }

  /**
   * Analyze symptom patterns
   */
  private analyzeSymptomPatterns(userId: string, dailyLogs: DailyLog[]): Insight[] {
    const insights: Insight[] = [];
    
    // Group logs by cycle phase (approximate)
    const symptomFrequency: { [symptom: string]: number } = {};
    const preMenstrualSymptoms: { [symptom: string]: number } = {};
    
    dailyLogs.forEach(log => {
      if (log.symptoms && log.symptoms.length > 0) {
        log.symptoms.forEach(symptom => {
          symptomFrequency[symptom] = (symptomFrequency[symptom] || 0) + 1;
          
          // Check if this is likely pre-menstrual (rough estimation)
          if (this.isLikelyPreMenstrual(log, dailyLogs)) {
            preMenstrualSymptoms[symptom] = (preMenstrualSymptoms[symptom] || 0) + 1;
          }
        });
      }
    });

    // Most common symptoms
    const sortedSymptoms = Object.entries(symptomFrequency)
      .sort(([,a], [,b]) => b - a)
      .slice(0, 3);

    if (sortedSymptoms.length > 0) {
      const topSymptom = sortedSymptoms[0];
      const frequency = Math.round((topSymptom[1] / dailyLogs.length) * 100);
      
      if (frequency > 20) {
        insights.push({
          id: '',
          userId,
          generatedDate: new Date(),
          insightText: `You experience ${topSymptom[0].toLowerCase()} in ${frequency}% of your logged days. This is your most common symptom.`,
          type: InsightType.PATTERN_RECOGNITION,
          isRead: false,
          relatedLogIds: [],
          confidence: 0.7,
          actionable: false
        });
      }
    }

    // PMS pattern detection
    const totalPreMenstrualDays = Object.values(preMenstrualSymptoms).reduce((sum, count) => sum + count, 0);
    if (totalPreMenstrualDays > 10) {
      const topPMSSymptom = Object.entries(preMenstrualSymptoms)
        .sort(([,a], [,b]) => b - a)[0];
      
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `You frequently experience ${topPMSSymptom[0].toLowerCase()} before your period. Consider tracking this pattern to better prepare for upcoming cycles.`,
        type: InsightType.PATTERN_RECOGNITION,
        isRead: false,
        relatedLogIds: [],
        confidence: 0.75,
        actionable: true
      });
    }

    return insights;
  }

  /**
   * Analyze mood patterns
   */
  private analyzeMoodPatterns(userId: string, dailyLogs: DailyLog[]): Insight[] {
    const insights: Insight[] = [];
    
    const moodCounts: { [mood: string]: number } = {};
    const cyclePhaseMoods: { [phase: string]: string[] } = {
      menstrual: [],
      follicular: [],
      ovulatory: [],
      luteal: []
    };

    dailyLogs.forEach(log => {
      if (log.mood) {
        moodCounts[log.mood] = (moodCounts[log.mood] || 0) + 1;
        
        // Rough cycle phase estimation
        const phase = this.estimateCyclePhase(log, dailyLogs);
        if (phase) {
          cyclePhaseMoods[phase].push(log.mood);
        }
      }
    });

    // Overall mood pattern
    const totalMoodEntries = Object.values(moodCounts).reduce((sum, count) => sum + count, 0);
    if (totalMoodEntries > 20) {
      const dominantMood = Object.entries(moodCounts)
        .sort(([,a], [,b]) => b - a)[0];
      
      const percentage = Math.round((dominantMood[1] / totalMoodEntries) * 100);
      
      if (percentage > 40) {
        insights.push({
          id: '',
          userId,
          generatedDate: new Date(),
          insightText: `Your mood is most commonly ${dominantMood[0].toLowerCase()} (${percentage}% of tracked days). This gives insight into your overall emotional patterns.`,
          type: InsightType.PATTERN_RECOGNITION,
          isRead: false,
          relatedLogIds: [],
          confidence: 0.7,
          actionable: false
        });
      }
    }

    return insights;
  }

  /**
   * Analyze BBT patterns
   */
  private analyzeBBTPatterns(userId: string, dailyLogs: DailyLog[]): Insight[] {
    const insights: Insight[] = [];
    
    const bbtLogs = dailyLogs.filter(log => log.bbt && log.bbt > 0);
    if (bbtLogs.length < 20) {return insights;}

    // Calculate average BBT
    const avgBBT = bbtLogs.reduce((sum, log) => sum + (log.bbt as number), 0) / bbtLogs.length;
    
    // Look for biphasic pattern (temperature shift indicating ovulation)
    const temperatureShifts = this.detectTemperatureShifts(bbtLogs);
    
    if (temperatureShifts.length > 0) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `Your BBT shows ${temperatureShifts.length} clear temperature shifts, indicating likely ovulation. Your average BBT is ${avgBBT.toFixed(1)}°F.`,
        type: InsightType.PATTERN_RECOGNITION,
        isRead: false,
        relatedLogIds: temperatureShifts.map(shift => shift.logId),
        confidence: 0.8,
        actionable: false
      });
    }

    return insights;
  }

  /**
   * Analyze fertility patterns
   */
  private analyzeFertilityPatterns(userId: string, dailyLogs: DailyLog[], _cycles: Cycle[]): Insight[] {
    const insights: Insight[] = [];
    
    // Analyze cervical mucus patterns
    const mucusLogs = dailyLogs.filter(log => log.cervicalMucus);
    const fertileQualityDays = mucusLogs.filter(log => 
      log.cervicalMucus === 'EGG_WHITE' || log.cervicalMucus === 'CREAMY'
    );

    if (fertileQualityDays.length > 5) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `You've tracked ${fertileQualityDays.length} days with fertile-quality cervical mucus. This indicates good fertility awareness and potential fertile windows.`,
        type: InsightType.FERTILITY_WINDOW,
        isRead: false,
        relatedLogIds: fertileQualityDays.map(log => log.id),
        confidence: 0.75,
        actionable: true
      });
    }

    // Analyze OPK patterns
    const opkLogs = dailyLogs.filter(log => log.opkResult);
    const positiveOPKs = opkLogs.filter(log => log.opkResult === 'POSITIVE');

    if (positiveOPKs.length > 2) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `You've recorded ${positiveOPKs.length} positive ovulation tests. This data helps confirm your fertile windows and ovulation timing.`,
        type: InsightType.FERTILITY_WINDOW,
        isRead: false,
        relatedLogIds: positiveOPKs.map(log => log.id),
        confidence: 0.85,
        actionable: false
      });
    }

    return insights;
  }

  /**
   * Helper method to estimate if a log is likely pre-menstrual
   */
  private isLikelyPreMenstrual(log: DailyLog, allLogs: DailyLog[]): boolean {
    // Simple heuristic: look for period flow in the next 1-7 days
    const logDate = new Date(log.date.seconds * 1000);
    const nextWeekLogs = allLogs.filter(nextLog => {
      const nextDate = new Date(nextLog.date.seconds * 1000);
      const daysDiff = (nextDate.getTime() - logDate.getTime()) / (1000 * 60 * 60 * 24);
      return daysDiff > 0 && daysDiff <= 7;
    });

    return nextWeekLogs.some(nextLog => nextLog.periodFlow && nextLog.periodFlow !== 'NONE');
  }

  /**
   * Helper method to estimate cycle phase
   */
  private estimateCyclePhase(log: DailyLog, _allLogs: DailyLog[]): string | null {
    // This is a simplified estimation - in a real app you'd use more sophisticated logic
    if (log.periodFlow && log.periodFlow !== 'NONE') {
      return 'menstrual';
    }
    
    // More sophisticated phase detection would go here
    return 'follicular'; // Default assumption
  }

  /**
   * Helper method to detect temperature shifts in BBT data
   */
  private detectTemperatureShifts(bbtLogs: DailyLog[]): Array<{logId: string, shiftAmount: number}> {
    const shifts: Array<{logId: string, shiftAmount: number}> = [];
    
    for (let i = 3; i < bbtLogs.length; i++) {
      const recent = bbtLogs.slice(i-3, i);
      const current = bbtLogs[i];
      
      const recentAvg = recent.reduce((sum, log) => sum + (log.bbt as number), 0) / recent.length;
      const shift = (current.bbt as number) - recentAvg;
      
      // Detect significant temperature rise (0.2°F or more)
      if (shift >= 0.2) {
        shifts.push({
          logId: current.id,
          shiftAmount: shift
        });
      }
    }
    
    return shifts;
  }
}