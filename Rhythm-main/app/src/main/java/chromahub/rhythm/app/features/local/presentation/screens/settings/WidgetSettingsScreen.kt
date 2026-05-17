package chromahub.rhythm.app.features.local.presentation.screens.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.infrastructure.widget.MusicWidgetProvider
import chromahub.rhythm.app.infrastructure.widget.glance.RhythmWidgetReceiver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    onBackClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    
    // Collect widget settings
    val showAlbumArt by appSettings.widgetShowAlbumArt.collectAsState()
    val showArtist by appSettings.widgetShowArtist.collectAsState()
    val showAlbum by appSettings.widgetShowAlbum.collectAsState()
    val showFavoriteButton by appSettings.widgetShowFavoriteButton.collectAsState()
    val cornerRadius by appSettings.widgetCornerRadius.collectAsState()
    val autoUpdate by appSettings.widgetAutoUpdate.collectAsState()
    
    var showCornerRadiusSheet by remember { mutableStateOf(false) }

    fun buildToggleSettingsItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        description: String,
        checked: Boolean,
        onToggle: (Boolean) -> Unit
    ): Material3SettingsItem {
        return Material3SettingsItem(
            icon = icon,
            title = { Text(title) },
            description = { Text(description) },
            trailingContent = {
                TunerAnimatedSwitch(
                    checked = checked,
                    onCheckedChange = {
                        HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.TextHandleMove)
                        onToggle(it)
                    }
                )
            },
            onClick = {
                HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.TextHandleMove)
                onToggle(!checked)
            }
        )
    }
    
    CollapsibleHeaderScreen(
        title = "Widget Settings",
        showBackButton = true,
        onBackClick = {
            HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.LongPress)
            onBackClick()
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            // Widget Preview Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Widget Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Changes will be applied to all active widgets on your home screen. Long-press the widget to resize it and see different layouts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.LongPress)
                                updateAllWidgets(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refresh All Widgets")
                        }
                    }
                }
            }
            
            // Display Options
            item {
                Spacer(modifier = Modifier.height(24.dp))
                val displayItems = listOf(
                    buildToggleSettingsItem(
                        icon = Icons.Default.Image,
                        title = "Show Album Art",
                        description = "Display album artwork in widget",
                        checked = showAlbumArt,
                        onToggle = {
                            appSettings.setWidgetShowAlbumArt(it)
                            updateAllWidgets(context)
                        }
                    ),
                    buildToggleSettingsItem(
                        icon = Icons.Default.Person,
                        title = "Show Artist Name",
                        description = "Display artist information",
                        checked = showArtist,
                        onToggle = {
                            appSettings.setWidgetShowArtist(it)
                            updateAllWidgets(context)
                        }
                    ),
                    buildToggleSettingsItem(
                        icon = Icons.Default.Album,
                        title = "Show Album Name",
                        description = "Display album information",
                        checked = showAlbum,
                        onToggle = {
                            appSettings.setWidgetShowAlbum(it)
                            updateAllWidgets(context)
                        }
                    ),
                    buildToggleSettingsItem(
                        icon = Icons.Default.Favorite,
                        title = "Show Favorite Button",
                        description = "Display favorite toggle on large widgets",
                        checked = showFavoriteButton,
                        onToggle = {
                            appSettings.setWidgetShowFavoriteButton(it)
                            updateAllWidgets(context)
                        }
                    )
                )

                Material3SettingsGroup(
                    title = "Display Options",
                    items = displayItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
            
            // Appearance Options
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Material3SettingsGroup(
                    title = "Appearance",
                    items = listOf(
                        Material3SettingsItem(
                            icon = Icons.Default.RoundedCorner,
                            title = { Text("Corner Radius") },
                            description = { Text("${cornerRadius}dp (Glance widgets only)") },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.TextHandleMove)
                                showCornerRadiusSheet = true
                            }
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
            
            // Behavior Options
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Material3SettingsGroup(
                    title = "Behavior",
                    items = listOf(
                        buildToggleSettingsItem(
                            icon = Icons.Default.AutoMode,
                            title = "Auto Update",
                            description = "Automatically update widget when song changes",
                            checked = autoUpdate,
                            onToggle = { appSettings.setWidgetAutoUpdate(it) }
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
            
            // Tips Card
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Widget Tips",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        WidgetTipItem(
                            icon = Icons.Default.Apps,
                            text = "Rhythm uses Material 3 Expressive design with dynamic colors"
                        )
                        WidgetTipItem(
                            icon = Icons.Default.Widgets,
                            text = "Widget adapts from 1x1 play button to 5x5+ full layout"
                        )
                        WidgetTipItem(
                            icon = Icons.Default.TouchApp,
                            text = "Tap the widget to open the app, buttons control playback"
                        )
                        WidgetTipItem(
                            icon = Icons.Default.GridOn,
                            text = "Long-press and resize for different expressive layouts"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Corner Radius Slider Sheet
        if (showCornerRadiusSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var tempRadius by remember { mutableIntStateOf(cornerRadius) }
            
            ModalBottomSheet(
                onDismissRequest = { showCornerRadiusSheet = false },
                sheetState = sheetState,
                dragHandle = { 
                    BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
                },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Corner Radius",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = CircleShape
                                    )
                            ) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    text = "${tempRadius}dp",
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Slider(
                        value = tempRadius.toFloat(),
                        onValueChange = { tempRadius = it.toInt() },
                        onValueChangeFinished = {
                            HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.LongPress)
                            appSettings.setWidgetCornerRadius(tempRadius)
                            updateAllWidgets(context)
                        },
                        valueRange = 0f..60f,
                        steps = 59,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Info card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Applies to Glance widgets only",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun WidgetTipItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun updateAllWidgets(context: Context) {
    // Update legacy RemoteViews widgets
    MusicWidgetProvider.updateWidgets(context)
    
    // Update Glance widgets
    val glanceIntent = android.content.Intent(context, RhythmWidgetReceiver::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    }
    val glanceIds = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, RhythmWidgetReceiver::class.java))
    glanceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, glanceIds)
    context.sendBroadcast(glanceIntent)
}
