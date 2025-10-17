# Insights Dashboard UI

This module contains the UI components for displaying personalized health insights to users.

## Components

### InsightsDashboardScreen
The main screen that displays all user insights in a card-based layout with generous white space following the Eunio design system.

**Features:**
- Displays unread and read insights in separate sections
- Pull-to-refresh functionality
- Loading, error, and empty states
- Medical disclaimer footer
- Follows Eunio design system colors and typography

### InsightCard
A dismissible card component that displays individual insights with appropriate styling based on type and confidence.

**Features:**
- Dismissible with slide-out animation
- Visual indicators for insight type (icons and colors)
- Confidence level badges
- Actionable insight indicators
- Different styling for read vs unread insights
- Tap to mark as read functionality

## Design System Integration

The insights UI follows the Eunio design system:
- **Colors**: Uses EunioColors palette with nature-inspired accents
- **Typography**: Consistent with EunioTypography scale
- **Shapes**: Rounded corners using EunioShapes
- **Spacing**: Generous white space with 16dp padding and spacing
- **Cards**: Elevated cards with subtle shadows

## Insight Types

The UI supports four types of insights with distinct visual styling:

1. **Pattern Recognition** - Blue info icon, trend analysis
2. **Early Warning** - Orange warning icon, health alerts  
3. **Cycle Prediction** - Green heart icon, menstrual cycle insights
4. **Fertility Window** - Purple info icon, fertility tracking

## Accessibility

- Semantic content descriptions for all interactive elements
- High contrast colors for readability
- Touch targets meet minimum size requirements
- Screen reader friendly text labels

## Medical Disclaimer

All insights screens include a prominent medical disclaimer footer emphasizing that insights are for informational purposes only and should not replace professional medical advice.