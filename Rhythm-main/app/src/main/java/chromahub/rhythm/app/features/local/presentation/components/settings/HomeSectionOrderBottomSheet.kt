@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package chromahub.rhythm.app.features.local.presentation.components.settings

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.common.ButtonGroupStyle
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveButtonGroup
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveGroupButton
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.util.HapticUtils
import kotlinx.coroutines.launch

private fun groupedBottomSheetItemShape(index: Int, totalCount: Int): RoundedCornerShape {
    if (totalCount <= 1) return RoundedCornerShape(24.dp)

    return when (index) {
        0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        totalCount - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(6.dp)
    }
}

@Composable
fun HomeSectionOrderBottomSheet(
    onDismiss: () -> Unit,
    appSettings: AppSettings
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val sectionOrder by appSettings.homeSectionOrder.collectAsState()
    
    // Collect all visibility states
    val showGreeting by appSettings.homeShowGreeting.collectAsState()
    val showRecentlyPlayed by appSettings.homeShowRecentlyPlayed.collectAsState()
    val showDiscoverCarousel by appSettings.homeShowDiscoverCarousel.collectAsState()
    val showArtists by appSettings.homeShowArtists.collectAsState()
    val showNewReleases by appSettings.homeShowNewReleases.collectAsState()
    val showRecentlyAdded by appSettings.homeShowRecentlyAdded.collectAsState()
    val showRecommended by appSettings.homeShowRecommended.collectAsState()
    val showListeningStats by appSettings.homeShowListeningStats.collectAsState()
    
    // Fixed sections: GREETING always first, DISCOVER always second (not reorderable)
    val fixedSections = setOf("GREETING", "DISCOVER", "MOOD")
    var reorderableList by remember { mutableStateOf(sectionOrder.filter { it !in fixedSections }.toList()) }
    var visibilityMap by remember {
        mutableStateOf(
            mapOf(
                "GREETING" to showGreeting,
                "RECENTLY_PLAYED" to showRecentlyPlayed,
                "DISCOVER" to showDiscoverCarousel,
                "ARTISTS" to showArtists,
                "NEW_RELEASES" to showNewReleases,
                "RECENTLY_ADDED" to showRecentlyAdded,
                "RECOMMENDED" to showRecommended,
                "STATS" to showListeningStats
            )
        )
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // Helper function to get display name and icon for section
    fun getSectionInfo(sectionId: String): Pair<String, ImageVector> {
        return when (sectionId) {
            "GREETING" -> Pair("Greeting", Icons.Default.WavingHand)
            "RECENTLY_PLAYED" -> Pair("Recently Played", Icons.Default.History)
            "DISCOVER" -> Pair("Discover Carousel", Icons.Default.Explore)
            "ARTISTS" -> Pair("Top Artists", Icons.Default.Person)
            "NEW_RELEASES" -> Pair("New Releases", Icons.Default.NewReleases)
            "RECENTLY_ADDED" -> Pair("Recently Added", RhythmIcons.Music.Album)
            "RECOMMENDED" -> Pair("Recommended", Icons.Default.Recommend)
            "STATS" -> Pair("Listening Stats", Icons.Default.InsertChart)
            else -> Pair(sectionId, RhythmIcons.Music.Song)
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { 
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        val totalSectionCards = reorderableList.size + 2

        Column(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.bottomsheet_home_section_order),
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
                                text = context.getString(R.string.bottomsheet_reorder_toggle_visibility),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Fixed Greeting section (always first, not reorderable)
            item(key = "fixed_greeting") {
                val (greetingName, greetingIcon) = getSectionInfo("GREETING")
                val isGreetingVisible = visibilityMap["GREETING"] ?: true

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    shape = groupedBottomSheetItemShape(0, totalSectionCards)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,

                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = greetingIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = greetingName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fixed position",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                visibilityMap = visibilityMap.toMutableMap().apply {
                                    this["GREETING"] = !isGreetingVisible
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isGreetingVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isGreetingVisible) "Hide greeting" else "Show greeting",
                                tint = if (isGreetingVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Fixed Discover Carousel section (always second, not reorderable)
            item(key = "fixed_discover") {
                val (discoverName, discoverIcon) = getSectionInfo("DISCOVER")
                val isDiscoverVisible = visibilityMap["DISCOVER"] ?: true
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    shape = groupedBottomSheetItemShape(1, totalSectionCards)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Fixed position indicator
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            Icon(
                                imageVector = discoverIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = discoverName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fixed position",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Visibility toggle only (no reorder buttons)
                        IconButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                visibilityMap = visibilityMap.toMutableMap().apply {
                                    this["DISCOVER"] = !isDiscoverVisible
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isDiscoverVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isDiscoverVisible) "Hide carousel" else "Show carousel",
                                tint = if (isDiscoverVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Reorderable list
            itemsIndexed(
                items = reorderableList,
                key = { _, item -> item }
            ) { index, sectionId ->
                val (sectionName, sectionIcon) = getSectionInfo(sectionId)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .animateItem(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = groupedBottomSheetItemShape(index + 2, totalSectionCards)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Position indicator
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            // Section icon
                            Icon(
                                imageVector = sectionIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            // Section name
                            Text(
                                text = sectionName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Visibility and reorder buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Visibility toggle button
                            val isVisible = visibilityMap[sectionId] ?: true
                            val visibleSectionsCount = visibilityMap.values.count { it }
                            
                            IconButton(
                                onClick = {
                                    // Prevent hiding the last visible section
                                    if (isVisible && visibleSectionsCount <= 1) {
                                        Toast.makeText(context, "At least one section must be visible", Toast.LENGTH_SHORT).show()
                                        return@IconButton
                                    }
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    visibilityMap = visibilityMap.toMutableMap().apply {
                                        this[sectionId] = !isVisible
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isVisible) "Hide section" else "Show section",
                                    tint = if (isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Move up button
                            FilledIconButton(
                                onClick = {
                                    if (index > 0) {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                        val newList = reorderableList.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index - 1, item)
                                        reorderableList = newList
                                    }
                                },
                                enabled = index > 0,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move up",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Move down button
                            FilledIconButton(
                                onClick = {
                                    if (index < reorderableList.size - 1) {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                        val newList = reorderableList.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index + 1, item)
                                        reorderableList = newList
                                    }
                                },
                                enabled = index < reorderableList.size - 1,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move down",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Sticky action buttons at bottom
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            ExpressiveButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                style = ButtonGroupStyle.Tonal
            ) {
                // Reset button
                ExpressiveGroupButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        val defaultOrder = listOf(
                            "RECENTLY_PLAYED", "ARTISTS",
                            "NEW_RELEASES", "RECENTLY_ADDED", "RECOMMENDED", "STATS"
                        )
                        reorderableList = defaultOrder
                        visibilityMap = mapOf(
                            "GREETING" to true,
                            "RECENTLY_PLAYED" to true,
                            "DISCOVER" to true,
                            "ARTISTS" to true,
                            "NEW_RELEASES" to true,
                            "RECENTLY_ADDED" to true,
                            "RECOMMENDED" to true,
                            "STATS" to true
                        )
                        Toast.makeText(context, "Section order and visibility reset to default", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    isStart = true
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.bottomsheet_reset))
                }

                // Save button
                ExpressiveGroupButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)

                        // Save section order (GREETING first, DISCOVER second, then user-ordered sections)
                        val finalOrder = listOf("GREETING", "DISCOVER") + reorderableList
                        appSettings.setHomeSectionOrder(finalOrder)

                        // Save visibility for each section.
                        appSettings.setHomeShowGreeting(visibilityMap["GREETING"] ?: true)
                        appSettings.setHomeShowRecentlyPlayed(visibilityMap["RECENTLY_PLAYED"] ?: true)
                        appSettings.setHomeShowDiscoverCarousel(visibilityMap["DISCOVER"] ?: true)
                        appSettings.setHomeShowArtists(visibilityMap["ARTISTS"] ?: true)
                        appSettings.setHomeShowNewReleases(visibilityMap["NEW_RELEASES"] ?: true)
                        appSettings.setHomeShowRecentlyAdded(visibilityMap["RECENTLY_ADDED"] ?: true)
                        appSettings.setHomeShowRecommended(visibilityMap["RECOMMENDED"] ?: true)
                        appSettings.setHomeShowListeningStats(visibilityMap["STATS"] ?: true)

                        Toast.makeText(context, "Home section order and visibility saved", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    isEnd = true
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.bottomsheet_save))
                }
            }
        }
    }
    }
}
