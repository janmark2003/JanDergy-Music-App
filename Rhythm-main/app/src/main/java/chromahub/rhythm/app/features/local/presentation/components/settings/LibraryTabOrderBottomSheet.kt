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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
fun LibraryTabOrderBottomSheet(
    onDismiss: () -> Unit,
    appSettings: AppSettings,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val context = LocalContext.current
    val tabOrder by appSettings.libraryTabOrder.collectAsState()
    val hiddenTabs by appSettings.hiddenLibraryTabs.collectAsState()
    var reorderableList by remember { mutableStateOf(tabOrder.toList()) }
    var hiddenTabsSet by remember { mutableStateOf(hiddenTabs) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // Helper function to get display name and icon for tab
    fun getTabInfo(tabId: String): Pair<String, ImageVector> {
        return when (tabId) {
            "SONGS" -> Pair("Songs", RhythmIcons.Relax)
            "PLAYLISTS" -> Pair("Playlists", RhythmIcons.PlaylistFilled)
            "ALBUMS" -> Pair("Albums", RhythmIcons.Music.Album)
            "ARTISTS" -> Pair("Artists", RhythmIcons.Artist)
            "EXPLORER" -> Pair("Explorer", Icons.Default.Folder)
            else -> Pair(tabId, RhythmIcons.Music.Song)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
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
                            text = context.getString(R.string.library_tab_order_title),
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
                                text = context.getString(R.string.library_tab_order_desc),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Reorderable list
            itemsIndexed(
                items = reorderableList,
                key = { _, item -> item }
            ) { index, tabId ->
                val (tabName, tabIcon) = getTabInfo(tabId)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .animateItem(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = groupedBottomSheetItemShape(index, reorderableList.size)
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
                            
                            // Tab icon
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            // Tab name
                            Text(
                                text = tabName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Visibility and reorder buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Visibility toggle button
                            val isHidden = hiddenTabsSet.contains(tabId)
                            val visibleTabsCount = reorderableList.count { !hiddenTabsSet.contains(it) }
                            
                            IconButton(
                                onClick = {
                                    // Prevent hiding the last visible tab
                                    if (!isHidden && visibleTabsCount <= 1) {
                                        Toast.makeText(context, "At least one tab must be visible", Toast.LENGTH_SHORT).show()
                                        return@IconButton
                                    }
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    hiddenTabsSet = if (isHidden) {
                                        hiddenTabsSet - tabId
                                    } else {
                                        hiddenTabsSet + tabId
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isHidden) "Show tab" else "Hide tab",
                                    tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
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
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                ExpressiveButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonGroupStyle.Tonal
                ) {
                    // Reset button
                    ExpressiveGroupButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            appSettings.resetLibraryTabOrder()
                            appSettings.setHiddenLibraryTabs(emptySet())
                            reorderableList = listOf("SONGS", "PLAYLISTS", "ALBUMS", "ARTISTS", "EXPLORER")
                            hiddenTabsSet = emptySet()
                            Toast.makeText(context, "Tab order and visibility reset to default", Toast.LENGTH_SHORT).show()
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
                            appSettings.setLibraryTabOrder(reorderableList)
                            appSettings.setHiddenLibraryTabs(hiddenTabsSet)
                            Toast.makeText(context, "Tab order and visibility saved", Toast.LENGTH_SHORT).show()
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
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
