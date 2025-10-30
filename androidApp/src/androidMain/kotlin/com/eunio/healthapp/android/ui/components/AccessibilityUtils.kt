package com.eunio.healthapp.android.ui.components

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized accessibility utilities for consistent implementation across the app.
 */
object AccessibilityUtils {
    
    /**
     * Announces a message to screen readers with proper priority handling.
     */
    @Composable
    fun announceForAccessibility(
        message: String,
        priority: AccessibilityPriority = AccessibilityPriority.NORMAL
    ) {
        val context = LocalContext.current
        val accessibilityManager = remember {
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        }
        
        LaunchedEffect(message) {
            announceToAccessibilityManager(accessibilityManager, context, message)
        }
    }
    
    /**
     * Non-Composable function to announce messages - for use in LaunchedEffect or other non-Composable contexts.
     */
    fun announceToAccessibilityManager(
        accessibilityManager: AccessibilityManager,
        context: Context,
        message: String
    ) {
        if (accessibilityManager.isEnabled && message.isNotBlank()) {
            val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT).apply {
                text.add(message)
                className = "AccessibilityUtils"
                packageName = context.packageName
            }
            accessibilityManager.sendAccessibilityEvent(event)
        }
    }
    
    /**
     * Creates an accessible clickable modifier with proper semantics.
     */
    @Composable
    fun Modifier.accessibleClickable(
        onClick: () -> Unit,
        role: Role = Role.Button,
        contentDescription: String? = null,
        stateDescription: String? = null,
        enabled: Boolean = true,
        onClickLabel: String? = null
    ): Modifier = this.then(
        Modifier
            .clickable(
                enabled = enabled,
                role = role,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() }
            .semantics {
                contentDescription?.let { this.contentDescription = it }
                stateDescription?.let { this.stateDescription = it }
                if (!enabled) {
                    disabled()
                }
            }
    )
    
    /**
     * Ensures minimum touch target size for accessibility.
     */
    fun Modifier.minimumTouchTarget(size: Dp = 48.dp): Modifier = this.then(
        Modifier.sizeIn(minWidth = size, minHeight = size)
    )
    
    /**
     * Adds comprehensive semantics for form fields.
     */
    fun Modifier.accessibleFormField(
        label: String,
        value: String = "",
        isError: Boolean = false,
        errorMessage: String? = null,
        isRequired: Boolean = false,
        helperText: String? = null
    ): Modifier = this.then(
        Modifier.semantics {
            contentDescription = buildString {
                append(label)
                if (isRequired) append(", required")
                if (value.isNotEmpty()) append(", current value: $value")
                if (isError && errorMessage != null) append(", error: $errorMessage")
                if (helperText != null) append(", $helperText")
            }
            
            if (isError) {
                error(errorMessage ?: "Invalid input")
            }
            
            if (isRequired) {
                // Mark as required using content description
                contentDescription = "${contentDescription ?: ""} Required".trim()
            }
        }
    )
    
    /**
     * Adds semantics for selection states (radio buttons, checkboxes, etc.).
     */
    fun Modifier.accessibleSelection(
        isSelected: Boolean,
        role: Role = Role.RadioButton,
        selectionDescription: String? = null
    ): Modifier = this.then(
        Modifier.semantics {
            this.role = role
            selected = isSelected
            stateDescription = when {
                selectionDescription != null -> selectionDescription
                isSelected -> "Selected"
                else -> "Not selected"
            }
        }
    )
    
    /**
     * Adds live region semantics for dynamic content updates.
     */
    fun Modifier.liveRegion(
        mode: LiveRegionMode = LiveRegionMode.Polite
    ): Modifier = this.then(
        Modifier.semantics {
            liveRegion = mode
        }
    )
    
    /**
     * Adds heading semantics with proper level.
     */
    fun Modifier.accessibleHeading(level: Int = 1): Modifier = this.then(
        Modifier.semantics {
            heading()
            // Note: Compose doesn't support heading levels yet, but this prepares for future support
        }
    )
    
    /**
     * Creates accessible navigation semantics.
     */
    fun Modifier.accessibleNavigation(
        currentPage: String,
        totalPages: Int? = null,
        currentIndex: Int? = null
    ): Modifier = this.then(
        Modifier.semantics {
            contentDescription = buildString {
                append("Navigation: $currentPage")
                if (totalPages != null && currentIndex != null) {
                    append(", page ${currentIndex + 1} of $totalPages")
                }
            }
        }
    )
    
    /**
     * Adds progress semantics for loading states.
     */
    fun Modifier.accessibleProgress(
        progress: Float? = null,
        description: String = "Loading"
    ): Modifier = this.then(
        Modifier.semantics {
            contentDescription = if (progress != null) {
                "$description, ${(progress * 100).toInt()}% complete"
            } else {
                description
            }
            
            progress?.let {
                progressBarRangeInfo = ProgressBarRangeInfo(it, 0f..1f)
            }
        }
    )
    
    /**
     * Creates accessible list item semantics.
     */
    fun Modifier.accessibleListItem(
        position: Int,
        totalItems: Int,
        itemDescription: String
    ): Modifier = this.then(
        Modifier.semantics {
            contentDescription = "$itemDescription, item ${position + 1} of $totalItems"
            collectionItemInfo = CollectionItemInfo(
                rowIndex = position,
                rowSpan = 1,
                columnIndex = 0,
                columnSpan = 1
            )
        }
    )
    
    /**
     * Adds custom accessibility actions.
     */
    fun Modifier.accessibleActions(
        vararg actions: Pair<String, () -> Boolean>
    ): Modifier = this.then(
        Modifier.semantics {
            actions.forEach { (label, action) ->
                customActions = customActions + CustomAccessibilityAction(label, action)
            }
        }
    )
}

/**
 * Accessibility priority levels for announcements.
 */
enum class AccessibilityPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Composable that provides accessibility context and utilities.
 */
@Composable
fun AccessibilityProvider(
    content: @Composable AccessibilityScope.() -> Unit
) {
    val context = LocalContext.current
    val accessibilityManager = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }
    
    val scope = remember(accessibilityManager) {
        AccessibilityScope(
            isScreenReaderEnabled = accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled,
            textScale = context.resources.configuration.fontScale,
            accessibilityManager = accessibilityManager
        )
    }
    
    scope.content()
}

/**
 * Scope providing accessibility context and utilities.
 */
class AccessibilityScope(
    val isScreenReaderEnabled: Boolean,
    val textScale: Float,
    private val accessibilityManager: AccessibilityManager
) {
    
    fun announceImmediate(message: String) {
        if (accessibilityManager.isEnabled && message.isNotBlank()) {
            val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT).apply {
                text.add(message)
            }
            accessibilityManager.sendAccessibilityEvent(event)
        }
    }
    
    val isLargeText: Boolean
        get() = textScale > 1.3f
    
    val isExtraLargeText: Boolean
        get() = textScale > 1.8f
}