# Firebase Alerts Setup Guide

**Date:** 2025-03-10  
**Project:** Eunio Health App  
**Time Required:** 15-20 minutes

---

## 🎯 Overview

Firebase Alerts notify you via email when critical issues occur in your app:
- Crash rate spikes
- Performance degradation
- Authentication failures
- Error rate increases

---

## 📧 Alert Types to Configure

### 1. Crashlytics Alerts (HIGH PRIORITY)
Get notified when crashes spike or new issues appear.

### 2. Performance Alerts (MEDIUM PRIORITY)
Get notified when app performance degrades.

### 3. Analytics Alerts (LOW PRIORITY)
Get notified about user behavior anomalies.

---

## 🚀 Setup Instructions

### Step 1: Access Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select project: **eunio-c4dde**
3. You should see the project dashboard

---

## 🔥 Crashlytics Alerts

### Setup Crash Alerts

1. **Navigate to Crashlytics**
   - Click **Crashlytics** in left sidebar
   - You should see your crash data

2. **Configure Alerts**
   - Click the **⚙️ Settings** icon (top right)
   - Or go to: Project Settings → Integrations → Crashlytics

3. **Set Up Email Notifications**
   - Click **"Set up email notifications"**
   - Add your email address
   - Choose notification preferences:
     - ✅ **New issues** - Get notified of new crash types
     - ✅ **Regressed issues** - Get notified when fixed crashes return
     - ✅ **Velocity alerts** - Get notified when crash rate spikes

4. **Configure Thresholds**
   - **New issue threshold:** Notify after **1 occurrence** (catch issues early)
   - **Velocity threshold:** Notify when crash rate increases by **50%**

5. **Save Settings**

### Recommended Crashlytics Alert Settings

```
✅ New issues: Enabled (notify after 1 occurrence)
✅ Regressed issues: Enabled
✅ Velocity alerts: Enabled (50% increase)
✅ Daily digest: Enabled (summary of all crashes)
```

---

## ⚡ Performance Monitoring Alerts

### Setup Performance Alerts

1. **Navigate to Performance**
   - Click **Performance** in left sidebar
   - You should see your performance data (app start time: 1.76s)

2. **Create Custom Alerts**
   - Click on a metric (e.g., "App start time")
   - Click **"Create alert"** button

3. **Configure App Start Time Alert**
   ```
   Alert name: Slow App Startup
   Metric: App start time
   Condition: Greater than 3 seconds
   Duration: 5 minutes
   Notification: Email
   ```

4. **Configure Network Request Alert**
   ```
   Alert name: Slow Network Requests
   Metric: Network request duration
   Condition: Greater than 5 seconds
   Duration: 5 minutes
   Notification: Email
   ```

5. **Configure Screen Rendering Alert**
   ```
   Alert name: Poor Frame Rate
   Metric: Slow rendering
   Condition: Greater than 10% slow frames
   Duration: 5 minutes
   Notification: Email
   ```

### Recommended Performance Alerts

| Alert | Metric | Threshold | Priority |
|-------|--------|-----------|----------|
| Slow App Startup | App start time | > 3s | HIGH |
| Slow Network | Network duration | > 5s | MEDIUM |
| Poor Rendering | Slow frames | > 10% | MEDIUM |
| Frozen Frames | Frozen frames | > 1% | HIGH |

---

## 🔐 Authentication Alerts (via Cloud Functions)

**Note:** This requires Cloud Functions setup (optional, more advanced)

### Option 1: Monitor via Analytics

1. **Navigate to Analytics**
   - Click **Analytics** in left sidebar
   - Go to **Events** tab

2. **Create Custom Alert**
   - Look for authentication events (sign_in, sign_up)
   - Monitor failure rates

### Option 2: Set Up Cloud Function (Advanced)

This would require:
- Setting up Cloud Functions
- Writing a function to monitor auth failures
- Sending alerts via email/SMS

**Recommendation:** Skip for now, monitor manually via Analytics dashboard

---

## 📊 Analytics Alerts

### Setup Analytics Alerts

1. **Navigate to Analytics**
   - Click **Analytics** in left sidebar
   - Go to **Events** tab

2. **Create Custom Alerts**
   - Click **"Create custom alert"**
   - Configure based on your needs

### Recommended Analytics Alerts

```
Alert: User Retention Drop
Condition: Daily active users drops by 20%
Notification: Email

Alert: Crash-Free Users Drop
Condition: Crash-free users drops below 95%
Notification: Email
```

---

## 🔔 Notification Channels

### Email Notifications (Default)

1. **Add Team Members**
   - Go to: Project Settings → Users and permissions
   - Click **"Add member"**
   - Enter email addresses
   - Assign roles (Viewer, Editor, Owner)

2. **Configure Email Preferences**
   - Each team member can configure their own preferences
   - Go to: Account Settings → Email preferences

### Slack Integration (Optional)

1. **Navigate to Integrations**
   - Go to: Project Settings → Integrations
   - Find **Slack**

2. **Connect Slack**
   - Click **"Connect"**
   - Authorize Firebase to access Slack
   - Choose channel for notifications

3. **Configure Slack Alerts**
   - Choose which alerts to send to Slack
   - Recommended: Crashlytics alerts only

### PagerDuty Integration (Optional - Production)

For critical production apps:
1. Go to: Project Settings → Integrations
2. Find **PagerDuty**
3. Connect your PagerDuty account
4. Configure on-call rotations

---

## ✅ Quick Setup Checklist

### Essential Alerts (15 minutes)

- [ ] **Crashlytics Alerts**
  - [ ] Enable new issue notifications
  - [ ] Enable velocity alerts
  - [ ] Set threshold to 50% increase
  - [ ] Add your email

- [ ] **Performance Alerts**
  - [ ] Create "Slow App Startup" alert (> 3s)
  - [ ] Create "Slow Network" alert (> 5s)
  - [ ] Add your email

- [ ] **Team Notifications**
  - [ ] Add team members to project
  - [ ] Configure email preferences

### Optional Alerts (5 minutes)

- [ ] **Analytics Alerts**
  - [ ] User retention drop alert
  - [ ] Crash-free users alert

- [ ] **Integrations**
  - [ ] Slack integration (if using Slack)
  - [ ] PagerDuty (for production)

---

## 📝 Alert Configuration Examples

### Example 1: Critical Crash Alert

```
Name: Critical Crash Spike
Service: Crashlytics
Condition: Crash rate increases by 100%
Duration: 5 minutes
Severity: Critical
Notification: Email + Slack
Recipients: dev-team@example.com
```

### Example 2: Performance Degradation

```
Name: App Performance Degraded
Service: Performance Monitoring
Condition: App start time > 3 seconds
Duration: 10 minutes
Severity: Warning
Notification: Email
Recipients: dev-team@example.com
```

### Example 3: Authentication Issues

```
Name: Auth Failure Spike
Service: Analytics
Condition: sign_in_failed events > 10 per hour
Duration: 1 hour
Severity: Warning
Notification: Email
Recipients: dev-team@example.com
```

---

## 🎯 Recommended Alert Strategy

### Development Phase (Current)
- ✅ **Crashlytics:** All new issues (immediate notification)
- ✅ **Performance:** Degradation > 50% (daily digest)
- ⏸️ **Analytics:** Monitor manually (no alerts yet)

### Beta Testing Phase
- ✅ **Crashlytics:** New issues + velocity alerts
- ✅ **Performance:** All degradation alerts
- ✅ **Analytics:** User retention alerts

### Production Phase
- ✅ **Crashlytics:** All alerts + PagerDuty integration
- ✅ **Performance:** All alerts + Slack integration
- ✅ **Analytics:** All alerts + custom dashboards

---

## 🔍 Testing Your Alerts

### Test Crashlytics Alert

1. **Trigger a test crash** (already done in your app)
2. **Wait 5-10 minutes**
3. **Check email** for crash notification
4. **Verify alert received**

### Test Performance Alert

1. **Create a slow operation** in your app
2. **Run the app** and trigger the slow operation
3. **Wait 10-15 minutes**
4. **Check email** for performance alert

---

## 📊 Monitoring Dashboard

### Create Custom Dashboard

1. **Navigate to Analytics**
   - Click **Analytics** in left sidebar
   - Go to **Dashboard** tab

2. **Add Widgets**
   - Click **"Add widget"**
   - Choose metrics to display:
     - Active users
     - Crash-free users
     - App start time
     - Network request duration

3. **Save Dashboard**
   - Name it "Health Monitoring"
   - Share with team

---

## 🚨 Alert Response Plan

### When You Receive a Crash Alert

1. **Check Crashlytics Dashboard**
   - View crash details
   - Check affected users
   - Review stack trace

2. **Assess Severity**
   - How many users affected?
   - Is it blocking critical functionality?
   - Can users work around it?

3. **Take Action**
   - **Critical:** Fix immediately, release hotfix
   - **High:** Fix in next release (within 24 hours)
   - **Medium:** Fix in next sprint
   - **Low:** Add to backlog

### When You Receive a Performance Alert

1. **Check Performance Dashboard**
   - View affected metrics
   - Check trends over time
   - Identify affected versions

2. **Investigate**
   - Review recent code changes
   - Check for new dependencies
   - Test on different devices

3. **Optimize**
   - Profile the slow operation
   - Implement performance improvements
   - Test and verify fix

---

## 📧 Email Notification Examples

### Crashlytics Alert Email

```
Subject: [Firebase] New crash in Eunio Health (iOS)

A new crash has been detected in your app:

Crash: NSInvalidArgumentException
Affected users: 5
First occurred: 2 hours ago
Version: 1.0.0 (1)

View details: [Link to Firebase Console]
```

### Performance Alert Email

```
Subject: [Firebase] Performance alert: Slow app startup

App start time has exceeded threshold:

Metric: App start time
Current value: 3.5 seconds
Threshold: 3.0 seconds
Duration: 15 minutes

View details: [Link to Firebase Console]
```

---

## ✅ Completion Checklist

### Setup Complete When:

- [x] Crashlytics alerts configured
- [x] Performance alerts configured
- [x] Email notifications enabled
- [x] Team members added
- [x] Test alerts verified
- [x] Response plan documented

---

## 📚 Additional Resources

### Firebase Documentation
- [Crashlytics Alerts](https://firebase.google.com/docs/crashlytics/customize-crash-reports)
- [Performance Monitoring Alerts](https://firebase.google.com/docs/perf-mon/get-started-ios)
- [Analytics Custom Events](https://firebase.google.com/docs/analytics/events)

### Best Practices
- Start with conservative thresholds
- Adjust based on actual app behavior
- Don't over-alert (alert fatigue)
- Review alerts weekly and adjust

---

## 🎯 Next Steps

After setting up alerts:

1. **Monitor for 1 week**
   - Check if alerts are too noisy
   - Adjust thresholds as needed

2. **Document Response Procedures**
   - Create runbook for common issues
   - Define escalation paths

3. **Set Up Dashboards**
   - Create team dashboard
   - Share with stakeholders

4. **Review Monthly**
   - Check alert effectiveness
   - Update thresholds
   - Add new alerts as needed

---

**Time to Complete:** 15-20 minutes  
**Difficulty:** Easy (all done in Firebase Console)  
**Priority:** Medium (helpful but not critical)

**Ready to start?** Go to [Firebase Console](https://console.firebase.google.com/project/eunio-c4dde) and follow the steps above!
