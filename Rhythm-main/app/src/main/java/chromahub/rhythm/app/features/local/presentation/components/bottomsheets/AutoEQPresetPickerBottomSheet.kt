package chromahub.rhythm.app.features.local.presentation.components.bottomsheets

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.util.AutoEQManager
import chromahub.rhythm.app.shared.data.model.AutoEQProfile
import chromahub.rhythm.app.features.local.presentation.screens.settings.SettingsSearchBar
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoEQPresetPickerBottomSheet(
    initialProfileName: String? = null,
    currentProfileName: String? = null,
    onDismissRequest: () -> Unit,
    onProfileSelected: (AutoEQProfile) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var profiles by remember { mutableStateOf<List<AutoEQProfile>>(emptyList()) }
    var allBrands by remember { mutableStateOf<List<String>>(emptyList()) }
    var allTypes by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        try {
            val manager = AutoEQManager(context.applicationContext)
            manager.loadProfiles()
            profiles = manager.getAllProfiles()
            allBrands = manager.getAllBrands()
            allTypes = manager.getAllTypes()
        } catch (e: Exception) {
            profiles = emptyList()
            allBrands = emptyList()
            allTypes = emptyList()
        } finally {
            loading = false
        }
    }

    val filtered = remember(profiles, searchQuery, selectedBrand, selectedType) {
        var result = profiles

        if (searchQuery.isNotBlank()) {
            result = result.filter { p ->
                p.name.contains(searchQuery, ignoreCase = true) ||
                        (p.brand?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }

        if (selectedBrand != null) {
            result = result.filter { it.brand?.equals(selectedBrand, ignoreCase = true) ?: false }
        }

        if (selectedType != null) {
            result = result.filter { it.type?.equals(selectedType, ignoreCase = true) ?: false }
        }

        result.sortedWith(compareByDescending { it.name == currentProfileName })
    }

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        showContent = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onBackground,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Choose AutoEQ Preset",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            hint = "Search presets..."
                        )

                        FilledTonalIconButton(
                            onClick = { showFilters = !showFilters },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (selectedBrand != null || selectedType != null)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (selectedBrand != null || selectedType != null)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FilterList, // Changed icon here
                                contentDescription = "Toggle filters",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                // Swapped ElevatedCard for flat Card to remove shadow elevation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filters",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            AnimatedVisibility(
                                visible = selectedBrand != null || selectedType != null,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                // Proper button for Clear all
                                TextButton(
                                    onClick = {
                                        selectedBrand = null
                                        selectedType = null
                                    }
                                ) {
                                    Text("Clear all", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        if (allBrands.isNotEmpty()) {
                            FilterSection(
                                title = "Brand",
                                items = allBrands,
                                selectedItem = selectedBrand,
                                onItemSelected = { selectedBrand = if (selectedBrand == it) null else it },
                                onClear = { selectedBrand = null }
                            )
                        }

                        if (allTypes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FilterSection(
                                title = "Type",
                                items = allTypes,
                                selectedItem = selectedType,
                                onItemSelected = { selectedType = if (selectedType == it) null else it },
                                onClear = { selectedType = null }
                            )
                        }
                    }
                }
            }

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No presets found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = filtered,
                            key = { _, profile -> profile.name }
                        ) { index, profile ->
                            val isCurrentlyActive = profile.name == currentProfileName
                            Surface(
                                onClick = {
                                    onProfileSelected(profile)
                                    scope.launch { sheetState.hide() }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isCurrentlyActive)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    MaterialTheme.colorScheme.surface,
                                shape = groupedPresetItemShape(index, filtered.size),
                                tonalElevation = if (isCurrentlyActive) 0.dp else 1.dp
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            profile.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isCurrentlyActive) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isCurrentlyActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            profile.brand ?: "Unknown Brand",
                                            color = if (isCurrentlyActive) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    trailingContent = {
                                        if (isCurrentlyActive) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Currently active",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    title: String,
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = selectedItem != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // Upgraded to a proper TextButton
                TextButton(
                    onClick = onClear,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = selectedItem == item

                FilterChip(
                    selected = isSelected,
                    onClick = { onItemSelected(item) },
                    label = {
                        Text(
                            text = item,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    // Expressive pill shape
                    shape = CircleShape,
                    border = null // Removes the default border for a cleaner, expressive filled look
                )
            }
        }
    }
}

private fun groupedPresetItemShape(index: Int, totalCount: Int): RoundedCornerShape {
    return when {
        totalCount <= 1 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        index == totalCount - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(6.dp)
    }
}