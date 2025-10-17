import * as functions from 'firebase-functions';
import { DailyLog, Cycle, Insight, InsightType } from '../types';

export class EarlyWarningDetector {
  
  /**
   * Detect early warning signs in user's health data
   */
  async detectWarnings(userId: string, dailyLogs: DailyLog[], cycles: Cycle[]): Promise<Insight[]> {
    const insights: Insight[] = [];

    try {
      // Detect irregular bleeding patterns
      const bleedingWarnings = this.detectIrregularBleeding(userId, dailyLogs);
      insights.push(...bleedingWarnings);

      // Detect concerning symptom patterns
      const symptomWarnings = this.detectConcerningSymptoms(userId, dailyLogs);
      insights.push(...symptomWarnings);

      // Detect cycle irregularities
      const cycleWarnings = this.detectCycleIrregularities(userId, cycles);
      insights.push(...cycleWarnings);

      // Detect temperature anomalies
      const temperatureWarnings = this.detectTemperatureAnomalies(userId, dailyLogs);
      insights.push(...temperatureWarnings);

      // Detect mood concerns
      const moodWarnings = this.detectMoodConcerns(userId, dailyLogs);
      insights.push(...moodWarnings);

      return insights;
    } catch (error) {
      functions.logger.error('Error detecting warnings', { userId, error });
      return [];
    }
  }

  /**
   * Detect irregular bleeding patterns that may need attention
   */
  private detectIrregularBleeding(userId: string, dailyLogs: DailyLog[]): Insight[] {
    const insights: Insight[] = [];
    
    // Find bleeding episodes
    const bleedingLogs = dailyLogs.filter(log => 
      log.periodFlow && log.periodFlow !== 'NONE'
    ).sort((a, b) => a.date.seconds - b.date.seconds);

    if (bleedingLogs.length < 10) {return insights;}

    // Detect prolonged bleeding (>8 days continuous)
    let consecutiveBleedingDays = 0;
    let maxConsecutiveDays = 0;
    let prolongedBleedingLogs: DailyLog[] = [];

    for (let i = 0; i < bleedingLogs.length; i++) {
      if (i === 0) {
        consecutiveBleedingDays = 1;
        prolongedBleedingLogs = [bleedingLogs[i]];
      } else {
        const prevDate = new Date(bleedingLogs[i-1].date.seconds * 1000);
        const currentDate = new Date(bleedingLogs[i].date.seconds * 1000);
        const daysDiff = (currentDate.getTime() - prevDate.getTime()) / (1000 * 60 * 60 * 24);
        
        if (daysDiff <= 1) {
          consecutiveBleedingDays++;
          prolongedBleedingLogs.push(bleedingLogs[i]);
        } else {
          if (consecutiveBleedingDays > maxConsecutiveDays) {
            maxConsecutiveDays = consecutiveBleedingDays;
          }
          consecutiveBleedingDays = 1;
          prolongedBleedingLogs = [bleedingLogs[i]];
        }
      }
    }
    
    // Check final consecutive count after loop ends
    if (consecutiveBleedingDays > maxConsecutiveDays) {
      maxConsecutiveDays = consecutiveBleedingDays;
    }

    if (maxConsecutiveDays > 8) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `You've tracked ${maxConsecutiveDays} consecutive days of bleeding. Periods longer than 8 days may warrant discussion with a healthcare provider.`,
        type: InsightType.EARLY_WARNING,
        isRead: false,
        relatedLogIds: prolongedBleedingLogs.slice(0, maxConsecutiveDays).map(log => log.id),
        confidence: 0.8,
        actionable: true
      });
    }

    // Detect frequent bleeding (bleeding episodes too close together)
    const bleedingEpisodes = this.groupBleedingEpisodes(bleedingLogs);
    const shortIntervals = bleedingEpisodes.filter((episode, index) => {
      if (index === 0) {return false;}
      const prevEpisode = bleedingEpisodes[index - 1];
      const daysBetween = (episode.startDate.getTime() - prevEpisode.endDate.getTime()) / (1000 * 60 * 60 * 24);
      return daysBetween < 21;
    });

    if (shortIntervals.length >= 2) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `You've had ${shortIntervals.length} bleeding episodes with less than 21 days between them. Frequent bleeding may indicate hormonal changes worth discussing with a healthcare provider.`,
        type: InsightType.EARLY_WARNING,
        isRead: false,
        relatedLogIds: shortIntervals.flatMap(episode => episode.logIds),
        confidence: 0.75,
        actionable: true
      });
    }

    return insights;
  }

  /**
   * Detect concerning symptom patterns
   */
  private detectConcerningSymptoms(userId: string, dailyLogs: DailyLog[]): Insight[] {
    const insights: Insight[] = [];
    
    // Define concerning symptoms and their thresholds
    const concerningSymptoms = {
      'SEVERE_CRAMPS': { threshold: 5, message: 'severe cramping' },
      'HEAVY_BLEEDING': { threshold: 3, message: 'heavy bleeding' },
      'NAUSEA': { threshold: 7, message: 'frequent nausea' },
      'HEADACHE': { threshold: 10, message: 'frequent headaches' },
      'BREAST_PAIN': { threshold: 8, message: 'persistent breast pain' }
    };

    Object.entries(concerningSymptoms).forEach(([symptom, config]) => {
      const symptomLogs = dailyLogs.filter(log => 
        log.symptoms && log.symptoms.includes(symptom)
      );

      if (symptomLogs.length >= config.threshold) {
        const recentLogs = symptomLogs.slice(0, 30); // Last 30 occurrences
        const frequency = Math.round((symptomLogs.length / dailyLogs.length) * 100);

        insights.push({
          id: '',
          userId,
          generatedDate: new Date(),
          insightText: `You've experienced ${config.message} on ${symptomLogs.length} tracked days (${frequency}% frequency). Consider discussing this pattern with a healthcare provider if it's concerning you.`,
          type: InsightType.EARLY_WARNING,
          isRead: false,
          relatedLogIds: recentLogs.map(log => log.id),
          confidence: 0.7,
          actionable: true
        });
      }
    });

    // Detect multiple concerning symptoms occurring together
    const multiSymptomDays = dailyLogs.filter(log => 
      log.symptoms && log.symptoms.length >= 3
    );

    if (multiSymptomDays.length > dailyLogs.length * 0.2) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `You frequently experience multiple symptoms together (${multiSymptomDays.length} days with 3+ symptoms). This pattern might be worth discussing with a healthcare provider.`,
        type: InsightType.EARLY_WARNING,
        isRead: false,
        relatedLogIds: multiSymptomDays.slice(0, 10).map(log => log.id),
        confidence: 0.65,
        actionable: true
      });
    }

    return insights;
  }

  /**
   * Detect cycle irregularities that may need attention
   */
  private detectCycleIrregularities(userId: string, cycles: Cycle[]): Insight[] {
    const insights: Insight[] = [];
    
    if (cycles.length < 3) {return insights;}

    const cycleLengths = cycles
      .filter(cycle => cycle.cycleLength && cycle.cycleLength > 0)
      .map(cycle => cycle.cycleLength as number);

    if (cycleLengths.length < 3) {return insights;}

    // Detect very short cycles (< 21 days)
    const shortCycles = cycleLengths.filter(length => length < 21);
    if (shortCycles.length >= 2) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `You've had ${shortCycles.length} cycles shorter than 21 days. Very short cycles may indicate hormonal imbalances worth discussing with a healthcare provider.`,
        type: InsightType.EARLY_WARNING,
        isRead: false,
        relatedLogIds: [],
        confidence: 0.8,
        actionable: true
      });
    }

    // Detect very long cycles (> 35 days)
    const longCycles = cycleLengths.filter(length => length > 35);
    if (longCycles.length >= 2) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `You've had ${longCycles.length} cycles longer than 35 days. Extended cycles may indicate hormonal changes worth monitoring with a healthcare provider.`,
        type: InsightType.EARLY_WARNING,
        isRead: false,
        relatedLogIds: [],
        confidence: 0.8,
        actionable: true
      });
    }

    // Detect highly irregular cycles (standard deviation > 7 days)
    const avgLength = cycleLengths.reduce((sum, length) => sum + length, 0) / cycleLengths.length;
    const variance = cycleLengths.reduce((sum, length) => sum + Math.pow(length - avgLength, 2), 0) / cycleLengths.length;
    const stdDev = Math.sqrt(variance);

    if (stdDev > 7) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `Your cycle lengths vary significantly (${Math.round(avgLength - stdDev)}-${Math.round(avgLength + stdDev)} days). High variability may indicate stress, lifestyle factors, or hormonal changes worth exploring.`,
        type: InsightType.EARLY_WARNING,
        isRead: false,
        relatedLogIds: [],
        confidence: 0.75,
        actionable: true
      });
    }

    return insights;
  }

  /**
   * Detect temperature anomalies in BBT data
   */
  private detectTemperatureAnomalies(userId: string, dailyLogs: DailyLog[]): Insight[] {
    const insights: Insight[] = [];
    
    const bbtLogs = dailyLogs.filter(log => log.bbt && log.bbt > 0);
    if (bbtLogs.length < 20) {return insights;}

    // Calculate normal temperature range
    const temperatures = bbtLogs.map(log => log.bbt as number);
    const avgTemp = temperatures.reduce((sum, temp) => sum + temp, 0) / temperatures.length;
    const variance = temperatures.reduce((sum, temp) => sum + Math.pow(temp - avgTemp, 2), 0) / temperatures.length;
    const stdDev = Math.sqrt(variance);

    // Detect consistently high temperatures
    const highTempThreshold = avgTemp + (2 * stdDev);
    const highTempLogs = bbtLogs.filter(log => (log.bbt as number) > highTempThreshold);
    
    if (highTempLogs.length > bbtLogs.length * 0.1) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `You've recorded ${highTempLogs.length} days with unusually high temperatures (above ${highTempThreshold.toFixed(1)}°F). Persistent elevated temperatures may warrant medical attention.`,
        type: InsightType.EARLY_WARNING,
        isRead: false,
        relatedLogIds: highTempLogs.slice(0, 10).map(log => log.id),
        confidence: 0.7,
        actionable: true
      });
    }

    // Detect lack of temperature variation (possible anovulatory cycles)
    if (stdDev < 0.2) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `Your BBT shows minimal variation (${stdDev.toFixed(2)}°F standard deviation). This might indicate anovulatory cycles, which is worth discussing with a healthcare provider if you're trying to conceive.`,
        type: InsightType.EARLY_WARNING,
        isRead: false,
        relatedLogIds: bbtLogs.slice(0, 20).map(log => log.id),
        confidence: 0.65,
        actionable: true
      });
    }

    return insights;
  }

  /**
   * Detect mood-related concerns
   */
  private detectMoodConcerns(userId: string, dailyLogs: DailyLog[]): Insight[] {
    const insights: Insight[] = [];
    
    const moodLogs = dailyLogs.filter(log => log.mood);
    if (moodLogs.length < 20) {return insights;}

    // Count negative mood days
    const negativeMoods = ['SAD', 'ANXIOUS', 'IRRITABLE', 'DEPRESSED'];
    const negativeMoodLogs = moodLogs.filter(log => 
      log.mood && negativeMoods.includes(log.mood)
    );

    const negativeMoodPercentage = (negativeMoodLogs.length / moodLogs.length) * 100;

    if (negativeMoodPercentage > 40) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `You've tracked negative moods on ${Math.round(negativeMoodPercentage)}% of days. If you're feeling persistently down or anxious, consider reaching out to a healthcare provider or mental health professional.`,
        type: InsightType.EARLY_WARNING,
        isRead: false,
        relatedLogIds: negativeMoodLogs.slice(0, 15).map(log => log.id),
        confidence: 0.7,
        actionable: true
      });
    }

    // Detect mood swings (rapid changes)
    let moodSwings = 0;
    for (let i = 1; i < moodLogs.length; i++) {
      const prevMood = moodLogs[i-1].mood;
      const currentMood = moodLogs[i].mood;
      
      if (prevMood && currentMood && this.isMoodSwing(prevMood, currentMood)) {
        moodSwings++;
      }
    }

    const moodSwingPercentage = (moodSwings / (moodLogs.length - 1)) * 100;
    if (moodSwingPercentage > 30) {
      insights.push({
        id: '',
        userId,
        generatedDate: new Date(),
        insightText: `You experience frequent mood changes (${Math.round(moodSwingPercentage)}% of tracked days). If mood swings are impacting your daily life, consider discussing this with a healthcare provider.`,
        type: InsightType.EARLY_WARNING,
        isRead: false,
        relatedLogIds: [],
        confidence: 0.65,
        actionable: true
      });
    }

    return insights;
  }

  /**
   * Helper method to group bleeding episodes
   */
  private groupBleedingEpisodes(bleedingLogs: DailyLog[]): Array<{startDate: Date, endDate: Date, logIds: string[]}> {
    const episodes: Array<{startDate: Date, endDate: Date, logIds: string[]}> = [];
    
    if (bleedingLogs.length === 0) {return episodes;}

    let currentEpisode = {
      startDate: new Date(bleedingLogs[0].date.seconds * 1000),
      endDate: new Date(bleedingLogs[0].date.seconds * 1000),
      logIds: [bleedingLogs[0].id]
    };

    for (let i = 1; i < bleedingLogs.length; i++) {
      const currentDate = new Date(bleedingLogs[i].date.seconds * 1000);
      const daysDiff = (currentDate.getTime() - currentEpisode.endDate.getTime()) / (1000 * 60 * 60 * 24);
      
      if (daysDiff <= 1) {
        // Continue current episode
        currentEpisode.endDate = currentDate;
        currentEpisode.logIds.push(bleedingLogs[i].id);
      } else {
        // Start new episode
        episodes.push(currentEpisode);
        currentEpisode = {
          startDate: currentDate,
          endDate: currentDate,
          logIds: [bleedingLogs[i].id]
        };
      }
    }
    
    episodes.push(currentEpisode);
    return episodes;
  }

  /**
   * Helper method to determine if there's a mood swing between two moods
   */
  private isMoodSwing(prevMood: string, currentMood: string): boolean {
    const positiveMoods = ['HAPPY', 'ENERGETIC', 'CALM'];
    const negativeMoods = ['SAD', 'ANXIOUS', 'IRRITABLE', 'DEPRESSED'];
    
    const prevIsPositive = positiveMoods.includes(prevMood);
    const currentIsPositive = positiveMoods.includes(currentMood);
    const prevIsNegative = negativeMoods.includes(prevMood);
    const currentIsNegative = negativeMoods.includes(currentMood);
    
    return (prevIsPositive && currentIsNegative) || (prevIsNegative && currentIsPositive);
  }
}