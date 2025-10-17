# Eunio Health App - Firebase Cloud Functions

This directory contains Firebase Cloud Functions for the Eunio Health App that provide intelligent insight generation and pattern analysis for women's health data.

## Overview

The functions analyze user health data to generate personalized insights including:
- **Pattern Recognition**: Identifies personal health patterns and trends
- **Early Warning Detection**: Flags concerning symptoms or irregularities
- **Fertility Window Analysis**: Provides fertility-related insights
- **Cycle Predictions**: Analyzes menstrual cycle patterns

## Functions

### `dailyInsightGeneration`
- **Type**: Scheduled function (runs daily at 2 AM UTC)
- **Purpose**: Processes all users' data to generate new insights
- **Trigger**: Cloud Pub/Sub scheduler

### `generateUserInsights`
- **Type**: Callable HTTPS function
- **Purpose**: Manually trigger insight generation for a specific user
- **Authentication**: Required
- **Usage**: For testing and on-demand insight generation

## Architecture

```
functions/
├── src/
│   ├── index.ts                    # Main function exports
│   ├── insights/
│   │   ├── insightGenerator.ts     # Core insight generation logic
│   │   ├── patternAnalyzer.ts      # Pattern recognition algorithms
│   │   └── earlyWarningDetector.ts # Early warning detection
│   ├── types/
│   │   └── index.ts                # TypeScript type definitions
│   └── test/
│       ├── setup.ts                # Test configuration
│       ├── insightGenerator.test.ts
│       ├── patternAnalyzer.test.ts
│       └── earlyWarningDetector.test.ts
├── package.json
├── tsconfig.json
└── jest.config.js
```

## Development

### Prerequisites
- Node.js 18+
- Firebase CLI
- TypeScript

### Setup
```bash
cd functions
npm install
```

### Build
```bash
npm run build
```

### Test
```bash
npm test
npm run test:watch  # Watch mode
```

### Local Development
```bash
npm run serve  # Start Firebase emulators
```

### Deploy
```bash
npm run deploy
```

## Insight Generation Logic

### Pattern Analysis
The `PatternAnalyzer` examines:
- **Cycle Length Patterns**: Regularity and variations
- **Symptom Patterns**: Frequency and timing
- **Mood Patterns**: Emotional trends and cycle correlations
- **BBT Patterns**: Temperature shifts and ovulation detection
- **Fertility Patterns**: Cervical mucus and OPK correlations

### Early Warning Detection
The `EarlyWarningDetector` monitors:
- **Irregular Bleeding**: Prolonged or frequent episodes
- **Concerning Symptoms**: High frequency of severe symptoms
- **Cycle Irregularities**: Very short/long or highly variable cycles
- **Temperature Anomalies**: Consistently high temps or lack of variation
- **Mood Concerns**: Persistent negative moods or frequent swings

### Confidence Scoring
Each insight includes a confidence score (0.0-1.0) based on:
- Data quality and quantity
- Statistical significance of patterns
- Consistency across multiple cycles
- Correlation strength between indicators

## Data Requirements

### Minimum Data Thresholds
- **Pattern Analysis**: 30+ daily logs, 3+ cycles
- **Cycle Analysis**: 3+ complete cycles
- **BBT Analysis**: 20+ temperature readings
- **Mood Analysis**: 20+ mood entries

### Data Sources
- Daily logs from Firestore `/users/{userId}/dailyLogs`
- Cycle data from `/users/{userId}/cycles`
- User preferences from `/users/{userId}`

## Security

### Authentication
- All functions require Firebase Authentication
- Users can only access their own data
- Cloud Functions have elevated permissions for insight writing

### Data Privacy
- No personal data is logged in function outputs
- Insights are stored securely in user-specific collections
- All data processing follows HIPAA compliance guidelines

## Monitoring

### Logging
Functions use structured logging with:
- User ID (for debugging, not in production logs)
- Processing statistics
- Error details and stack traces
- Performance metrics

### Metrics
Key metrics tracked:
- Daily insight generation success rate
- Processing time per user
- Insight confidence score distributions
- Error rates by function

## Testing

### Unit Tests
- Pattern analysis algorithms
- Early warning detection logic
- Data validation and error handling
- Confidence scoring accuracy

### Integration Tests
- Firestore data access
- Batch operations
- Authentication flows
- End-to-end insight generation

### Test Data
Uses realistic but anonymized health data patterns for comprehensive testing scenarios.

## Deployment

### Environment Configuration
- Development: `eunio-health-app-dev`
- Production: `eunio-health-app-prod`

### CI/CD
Functions are deployed automatically on:
- Merge to `main` branch (production)
- Merge to `develop` branch (staging)

### Rollback
Use Firebase CLI for quick rollbacks:
```bash
firebase functions:log  # Check for errors
firebase deploy --only functions  # Redeploy previous version
```

## Performance Considerations

### Batch Processing
- Users processed in batches of 10 to avoid timeouts
- Parallel processing within batches
- Graceful handling of individual user failures

### Data Optimization
- Queries limited to last 6 months of data
- Efficient Firestore indexes for date-range queries
- Minimal data transfer with targeted field selection

### Scaling
- Functions auto-scale based on demand
- Cold start optimization with minimal dependencies
- Memory allocation tuned for typical data volumes

## Medical Disclaimer

All insights generated include appropriate medical disclaimers and encourage users to consult healthcare providers for concerning patterns. The system provides observational insights, not medical diagnoses.