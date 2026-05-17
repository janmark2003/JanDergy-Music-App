package chromahub.rhythm.app.features.local.presentation.components.lyrics

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import chromahub.rhythm.app.shared.presentation.components.common.ButtonGroupStyle
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveButtonGroup
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveGroupButton
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.LyricsFileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditorBottomSheet(
    currentLyrics: String,
    songTitle: String,
    initialTimeOffset: Int = 0,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onRefresh: () -> Unit = {},
    onEmbedInFile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var editedLyrics by remember { mutableStateOf(currentLyrics) }
    var timeOffset by remember { mutableIntStateOf(initialTimeOffset) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    // Helper functions for lyrics document handling
    fun getDocumentDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                        val name = cursor.getString(nameIndex)?.trim()
                        if (name.isNullOrEmpty()) null else name
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsEditor", "Unable to read lyrics document name", e)
            null
        }
    }

    fun maybeRenameLyricsDocument(uri: Uri, expectedFileName: String): Uri {
        // Validate inputs
        if (expectedFileName.isBlank()) return uri
        
        val currentName = getDocumentDisplayName(uri) ?: return uri
        if (currentName.isBlank()) return uri
        
        // Check if provider appended .txt to our .lrc filename
        // E.g., we wanted "song.lrc" but got "song.lrc.txt"
        val currentNameWithoutTxt = if (currentName.endsWith(".txt", ignoreCase = true) && currentName.length > 4) {
            currentName.substring(0, currentName.length - 4)  // Safe because we checked length > 4
        } else {
            currentName
        }
        
        // Check if removing .txt from current name gives us the expected filename
        val shouldRename = currentName.endsWith(".txt", ignoreCase = true) &&
            currentNameWithoutTxt.equals(expectedFileName, ignoreCase = true)
        
        if (!shouldRename) {
            return uri
        }
        
        return try {
            DocumentsContract.renameDocument(context.contentResolver, uri, expectedFileName) ?: uri
        } catch (e: Exception) {
            Log.w("LyricsEditor", "Unable to rename saved lyrics document from '$currentName' to '$expectedFileName'", e)
            uri
        }
    }
    
    // Check if lyrics are synced (contain LRC timestamps)
    val hasSyncedLyrics = remember(editedLyrics) {
        editedLyrics.contains(Regex("""\[\d{2}:\d{2}\.\d{2,3}\]"""))
    }
    
    // Update editedLyrics when currentLyrics changes (from refresh)
    LaunchedEffect(currentLyrics) {
        if (currentLyrics != editedLyrics) {
            editedLyrics = currentLyrics
        }
    }
    
    // Update timeOffset when initialTimeOffset changes
    LaunchedEffect(initialTimeOffset) {
        timeOffset = initialTimeOffset
    }

    // Animation states
    var showContent by remember { mutableStateOf(false) }

    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentAlpha"
    )

    val contentTranslation by animateFloatAsState(
        targetValue = if (showContent) 0f else 30f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentTranslation"
    )

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // Function to adjust LRC timestamps
    fun adjustLyricsTimestamps(lyrics: String, offsetMs: Int): String {
        if (offsetMs == 0) return lyrics
        
        val lrcRegex = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)$""", RegexOption.MULTILINE)
        return lyrics.lines().joinToString("\n") { line ->
            lrcRegex.matchEntire(line)?.let { match ->
                val minutes = match.groupValues[1].toInt()
                val seconds = match.groupValues[2].toInt()
                val centiseconds = match.groupValues[3].padEnd(3, '0').take(3).toInt()
                val text = match.groupValues[4]
                
                // Convert to milliseconds
                var totalMs = (minutes * 60 * 1000) + (seconds * 1000) + centiseconds
                totalMs += offsetMs
                
                // Don't allow negative timestamps
                if (totalMs < 0) totalMs = 0
                
                // Convert back to LRC format
                val newMinutes = totalMs / 60000
                val newSeconds = (totalMs % 60000) / 1000
                val newCentiseconds = (totalMs % 1000)
                
                "[%02d:%02d.%03d]%s".format(newMinutes, newSeconds, newCentiseconds, text)
            } ?: line
        }
    }

    // File picker launcher for loading .lrc files
    val loadLyricsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        LyricsFileUtils.loadLyricsFromUri(context, selectedUri)
                    }

                    if (result.lyrics != null) {
                        editedLyrics = result.lyrics
                        Toast.makeText(context, "Lyrics loaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            result.errorMessage ?: "Error loading lyrics file",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("LyricsEditor", "Error loading lyrics file", e)
                    Toast.makeText(context, "Error loading lyrics: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val sanitizedTitle = remember(songTitle) {
        songTitle.trim()
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("_+"), "_")  // Collapse multiple underscores
            .trim('_')  // Remove leading/trailing underscores
            .takeIf { it.isNotEmpty() } ?: "lyrics"  // Fallback to "lyrics" if empty
    }
    val defaultLyricsFileName = remember(sanitizedTitle) { "$sanitizedTitle.lrc" }

    // File picker launcher for saving .lrc files
    val saveLyricsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(editedLyrics.toByteArray())
                    outputStream.flush()
                }

                maybeRenameLyricsDocument(it, defaultLyricsFileName)

                Toast.makeText(context, "Lyrics saved successfully", Toast.LENGTH_SHORT).show()
                onSave(editedLyrics, timeOffset)
                onDismiss()
            } catch (e: Exception) {
                Log.e("LyricsEditor", "Error saving lyrics file", e)
                Toast.makeText(context, "Error saving lyrics: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onBackground,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header with animation
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                LyricsEditorHeader(
                    songTitle = songTitle,
                    hasLyrics = editedLyrics.isNotBlank()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timestamp Adjustment Controls
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (hasSyncedLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = context.getString(R.string.sync_adjustment),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasSyncedLyrics) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (hasSyncedLyrics) {
                                Text(
                                    text = "${if (timeOffset >= 0) "+" else ""}${timeOffset}ms",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Reset/Refresh button
                            FilledTonalButton(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                    timeOffset = 0
                                    onRefresh()
                                },
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(context.getString(R.string.bottomsheet_reset), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    
                    // Show info message if lyrics are not synced
                    if (!hasSyncedLyrics) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = context.getString(R.string.lyrics_sync_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Earlier Button (-500ms)
                        OutlinedButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                timeOffset -= 500
                                editedLyrics = adjustLyricsTimestamps(editedLyrics, -500)
                                onSave(editedLyrics, timeOffset) // Apply changes immediately with offset
                            },
                            enabled = hasSyncedLyrics,
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "500ms",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = context.getString(R.string.bottomsheet_lyrics_earlier),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasSyncedLyrics) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                        
                        // Earlier Button (-100ms)
                        OutlinedButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                timeOffset -= 100
                                editedLyrics = adjustLyricsTimestamps(editedLyrics, -100)
                                onSave(editedLyrics, timeOffset) // Apply changes immediately with offset
                            },
                            enabled = hasSyncedLyrics,
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "100ms",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = context.getString(R.string.bottomsheet_lyrics_earlier),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasSyncedLyrics) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                        
                        // Later Button (+100ms)
                        OutlinedButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                timeOffset += 100
                                editedLyrics = adjustLyricsTimestamps(editedLyrics, 100)
                                onSave(editedLyrics, timeOffset) // Apply changes immediately with offset
                            },
                            enabled = hasSyncedLyrics,
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "100ms",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = context.getString(R.string.bottomsheet_lyrics_later),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasSyncedLyrics) 
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                        
                        // Later Button (+500ms)
                        OutlinedButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                timeOffset += 500
                                editedLyrics = adjustLyricsTimestamps(editedLyrics, 500)
                                onSave(editedLyrics, timeOffset) // Apply changes immediately with offset
                            },
                            enabled = hasSyncedLyrics,
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "500ms",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = context.getString(R.string.bottomsheet_lyrics_later),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasSyncedLyrics) 
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lyrics Text Field with animation
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    BoxWithConstraints {
                        val maxHeight = maxOf(minOf(maxHeight * 0.4f, 350.dp), 200.dp)
                        
                        OutlinedTextField(
                            value = editedLyrics,
                            onValueChange = { editedLyrics = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(maxHeight),
                            placeholder = {
                                Text(
                                    text = context.getString(R.string.lyrics_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row (sticky at bottom like LibraryTabOrderBottomSheet)
            ExpressiveButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                style = ButtonGroupStyle.Tonal
            ) {
                // Load File Button
                ExpressiveGroupButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                        loadLyricsLauncher.launch(
                            arrayOf(
                                "text/plain",
                                "text/*",
                                "text/x-lrc",
                                "application/x-lrc",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    isStart = true
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.bottomsheet_lyrics_load))
                }

                // Save File Button
                ExpressiveGroupButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                        if (editedLyrics.isNotBlank()) {
                            saveLyricsLauncher.launch(defaultLyricsFileName)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = editedLyrics.isNotBlank(),
                    isEnd = true
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.bottomsheet_lyrics_save))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Embed in File Button
            ExpressiveButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                style = ButtonGroupStyle.Tonal
            ) {
                ExpressiveGroupButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                        if (editedLyrics.isNotBlank()) {
                            onEmbedInFile(editedLyrics)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editedLyrics.isNotBlank(),
                    isStart = true,
                    isEnd = true
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.bottomsheet_lyrics_embed))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LyricsEditorHeader(
    songTitle: String,
    hasLyrics: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = context.getString(R.string.lyrics_editor_title),
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
                    text = songTitle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun getDocumentDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    val name = cursor.getString(nameIndex)?.trim()
                    if (name.isNullOrEmpty()) null else name
                } else {
                    null
                }
            } else {
                null
            }
        }
    } catch (e: Exception) {
        Log.w("LyricsEditor", "Unable to read lyrics document name", e)
        null
    }
}