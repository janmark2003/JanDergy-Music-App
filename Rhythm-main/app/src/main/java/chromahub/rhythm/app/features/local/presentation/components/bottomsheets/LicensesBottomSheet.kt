package chromahub.rhythm.app.features.local.presentation.components.bottomsheets

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.util.HapticUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesBottomSheet(
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        dragHandle = { 
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with improved layout matching particle intensity
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = context.getString(R.string.licenses_title),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            text = context.getString(R.string.licenses_desc),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Libraries Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                LicenseSheetItem(
                    name = "AutoEQ",
                    description = "Automatic headphone equalization from frequency responses by Jaakko Pasanen",
                    license = "MIT License",
                    url = "https://github.com/jaakkopasanen/AutoEq",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Jetpack Compose",
                    description = "Android's modern toolkit for building native UI",
                    license = "Apache License 2.0",
                    url = "https://developer.android.com/jetpack/compose",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Material 3 Components",
                    description = "Material Design 3 components for Android",
                    license = "Apache License 2.0",
                    url = "https://m3.material.io/",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Media3 ExoPlayer",
                    description = "Modern media playback library for Android",
                    license = "Apache License 2.0",
                    url = "https://github.com/androidx/media",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Kotlin Coroutines",
                    description = "Asynchronous programming framework for Kotlin",
                    license = "Apache License 2.0",
                    url = "https://github.com/Kotlin/kotlinx.coroutines",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Coil",
                    description = "Image loading library for Android backed by Kotlin Coroutines",
                    license = "Apache License 2.0",
                    url = "https://coil-kt.github.io/coil/",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Retrofit",
                    description = "Type-safe HTTP client for Android and Java",
                    license = "Apache License 2.0",
                    url = "https://square.github.io/retrofit/",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "OkHttp",
                    description = "HTTP client for Android, Kotlin, and Java",
                    license = "Apache License 2.0",
                    url = "https://square.github.io/okhttp/",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Gson",
                    description = "Java serialization/deserialization library for JSON",
                    license = "Apache License 2.0",
                    url = "https://github.com/google/gson",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "AndroidX Navigation",
                    description = "Navigation components for Android apps",
                    license = "Apache License 2.0",
                    url = "https://developer.android.com/guide/navigation",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Accompanist Permissions",
                    description = "Compose utilities for permissions handling",
                    license = "Apache License 2.0",
                    url = "https://google.github.io/accompanist/permissions/",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "AndroidX Palette",
                    description = "Library to extract prominent colors from images",
                    license = "Apache License 2.0",
                    url = "https://developer.android.com/jetpack/androidx/releases/palette",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "JAudioTagger",
                    description = "Audio metadata editing library for Java",
                    license = "LGPL v2.1",
                    url = "https://github.com/Borewit/jaudiotagger-android",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "AndroidX Fragment",
                    description = "Modular UI components for Android",
                    license = "Apache License 2.0",
                    url = "https://developer.android.com/jetpack/androidx/releases/fragment",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "AndroidX MediaRouter",
                    description = "Media routing support for Android",
                    license = "Apache License 2.0",
                    url = "https://developer.android.com/jetpack/androidx/releases/mediarouter",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Glance AppWidget",
                    description = "Modern reactive widgets framework with Material 3",
                    license = "Apache License 2.0",
                    url = "https://developer.android.com/jetpack/androidx/releases/glance",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "WorkManager",
                    description = "Deferrable, asynchronous task management library",
                    license = "Apache License 2.0",
                    url = "https://developer.android.com/jetpack/androidx/releases/work",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Material Icons Extended",
                    description = "Extended set of Material Design icons",
                    license = "Apache License 2.0",
                    url = "https://developer.android.com/jetpack/compose/resources/material-icons",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Ktor",
                    description = "Asynchronous framework for creating microservices and web applications",
                    license = "Apache License 2.0",
                    url = "https://ktor.io/",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "LeakCanary",
                    description = "Memory leak detection library for Android (debug builds only)",
                    license = "Apache License 2.0",
                    url = "https://square.github.io/leakcanary/",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Desugar JDK Libs",
                    description = "Allows using newer Java language APIs on older Android versions",
                    license = "Apache License 2.0",
                    url = "https://github.com/google/desugar_jdk_libs",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Media3 FFmpeg Decoder",
                    description = "FFmpeg-based decoder for Media3, enabling additional audio/video formats",
                    license = "Apache License 2.0",
                    url = "https://github.com/androidx/media",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Room",
                    description = "SQLite object mapping library that provides local data persistence",
                    license = "Apache License 2.0",
                    url = "https://developer.android.com/jetpack/androidx/releases/room",
                    context = context,
                    haptic = haptic
                )
                
                LicenseSheetItem(
                    name = "Geom Font",
                    description = "Modern, clean sans-serif typeface from Google Fonts",
                    license = "SIL Open Font License 1.1",
                    url = "https://fonts.google.com/specimen/Geom",
                    context = context,
                    haptic = haptic
                )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // License info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Actions.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = context.getString(R.string.licenses_apache),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = context.getString(R.string.licenses_attribution),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LicenseSheetItem(
    name: String,
    description: String,
    license: String,
    url: String,
    context: android.content.Context,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    Card(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = license,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Icon(
                imageVector = RhythmIcons.Forward,
                contentDescription = "View License",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
