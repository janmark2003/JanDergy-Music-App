package chromahub.rhythm.app.features.streaming.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.features.streaming.presentation.model.StreamingServiceOption
import chromahub.rhythm.app.features.streaming.presentation.model.StreamingServiceOptions
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveFilledIconButton
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.util.HapticUtils

@Composable
fun StreamingHomeScreen(
    viewModel: StreamingMusicViewModel,
    onNavigateToSettings: () -> Unit,
    onConfigureService: (String) -> Unit,
    onSwitchToLocalMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val appSettings = remember { AppSettings.getInstance(context) }

    val selectedService by appSettings.streamingService.collectAsState()
    val appMode by appSettings.appMode.collectAsState()
    val serviceSessions by viewModel.serviceSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val displaySelectedService = remember(selectedService) {
        if (selectedService == StreamingServiceOptions.SUBSONIC) "" else selectedService
    }

    CollapsibleHeaderScreen(
        title = "",
        headerDisplayMode = 1,
        actions = {
            ExpressiveFilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onNavigateToSettings()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = RhythmIcons.Settings,
                    contentDescription = stringResource(id = R.string.home_settings_cd)
                )
            }
        }
    ) { contentModifier ->
        LazyColumn(
            modifier = modifier
                .then(contentModifier)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.rhythm_splash_logo),
                            contentDescription = "Rhythm Logo",
                            modifier = Modifier.size(100.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(id = R.string.common_rhythm),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            if (appMode == "STREAMING") {
                                Text(
                                    text = "Go",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isAuthenticated) {
                            "Ready to connect"
                        } else {
                            "Authentication pending"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            onSwitchToLocalMode()
                        }
                    ) {
                        Text(text = "Leave Go Mode")
                    }
                }
            }

            items(StreamingServiceOptions.defaults, key = { it.id }) { service ->
                StreamingServiceCard(
                    option = service,
                    isSelected = displaySelectedService == service.id,
                    isConnected = serviceSessions[service.id]?.isConnected == true,
                    onConfigure = {
                        appSettings.setStreamingService(service.id)
                        onConfigureService(service.id)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun StreamingServiceCard(
    option: StreamingServiceOption,
    isSelected: Boolean,
    isConnected: Boolean,
    onConfigure: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        modifier = Modifier.fillMaxWidth()
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
                    text = stringResource(id = option.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(id = option.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.streaming_selected),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            FilledTonalButton(onClick = onConfigure) {
                Text(
                    text = if (isConnected) {
                        stringResource(id = R.string.streaming_manage)
                    } else {
                        stringResource(id = R.string.streaming_connect)
                    }
                )
            }
        }
    }
}

private fun selectedServiceLabel(selectedService: String, context: android.content.Context): String {
    if (selectedService.isBlank()) return context.getString(R.string.streaming_not_selected)
    val matching = StreamingServiceOptions.defaults.firstOrNull { it.id == selectedService }
    return matching?.let { context.getString(it.nameRes) } ?: context.getString(R.string.streaming_not_selected)
}