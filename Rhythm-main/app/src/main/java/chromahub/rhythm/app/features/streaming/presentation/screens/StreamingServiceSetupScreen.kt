package chromahub.rhythm.app.features.streaming.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.features.streaming.domain.model.StreamingServiceRules
import chromahub.rhythm.app.features.streaming.presentation.model.StreamingServiceOptions
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel
import chromahub.rhythm.app.features.local.presentation.screens.settings.TunerAnimatedSwitch
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen

@Composable
fun StreamingServiceSetupScreen(
    serviceId: String,
    viewModel: StreamingMusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appSettings = remember(context) { AppSettings.getInstance(context) }
    val sessions by viewModel.serviceSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val rememberStreamingPasswords by appSettings.rememberStreamingPasswords.collectAsState()
    val isBusy = isLoading

    val option = remember(serviceId) {
        StreamingServiceOptions.defaults.firstOrNull { it.id == serviceId }
    }
    val optionName = option?.let { stringResource(id = it.nameRes) } ?: serviceId
    val optionDescription = option?.let { stringResource(id = it.descriptionRes) }
    val session = sessions[serviceId] ?: viewModel.getServiceSession(serviceId)
    val requiresServerUrl = remember(serviceId) { StreamingServiceRules.requiresServerUrl(serviceId) }

    var serverUrl by rememberSaveable(serviceId) { mutableStateOf(session.serverUrl) }
    var username by rememberSaveable(serviceId) { mutableStateOf(session.username) }
    var password by rememberSaveable(serviceId) { mutableStateOf("") }

    val canSubmit = username.isNotBlank() && password.isNotBlank() && (!requiresServerUrl || serverUrl.isNotBlank())
    val primaryActionText = when {
        isBusy && session.isConnected -> R.string.streaming_service_setup_disconnecting
        isBusy -> R.string.streaming_service_setup_connecting
        session.isConnected -> R.string.streaming_service_setup_reconnect
        else -> R.string.streaming_connect
    }
    val statusText = when {
        isBusy && session.isConnected -> stringResource(id = R.string.streaming_service_setup_disconnecting)
        isBusy -> stringResource(id = R.string.streaming_service_setup_connecting)
        session.isConnected -> stringResource(id = R.string.streaming_status_connected)
        else -> stringResource(id = R.string.streaming_status_not_connected)
    }
    val statusContainerColor = when {
        isBusy -> MaterialTheme.colorScheme.primaryContainer
        session.isConnected -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusContentColor = when {
        isBusy -> MaterialTheme.colorScheme.onPrimaryContainer
        session.isConnected -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    CollapsibleHeaderScreen(
        title = stringResource(id = R.string.streaming_service_setup_title, optionName),
        showBackButton = true,
        onBackClick = onBackClick,
        headerDisplayMode = 1
    ) { contentModifier ->
        LazyColumn(
            modifier = modifier
                .then(contentModifier)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isBusy) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = statusContainerColor,
                                contentColor = statusContentColor,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudQueue,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.streaming_status_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (session.isConnected) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (session.username.isNotBlank()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = stringResource(id = R.string.streaming_service_setup_username),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = session.username,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (session.serverUrl.isNotBlank()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = stringResource(id = R.string.streaming_service_setup_server_url),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = session.serverUrl,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(id = R.string.streaming_status_connect_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (requiresServerUrl) {
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(text = stringResource(id = R.string.streaming_service_setup_server_url)) },
                                placeholder = { Text(text = stringResource(id = R.string.streaming_service_setup_server_url_hint)) },
                                supportingText = {
                                    Text(text = stringResource(id = R.string.streaming_service_setup_server_url_supporting))
                                },
                                singleLine = true,
                                enabled = !isBusy
                            )
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = stringResource(id = R.string.streaming_service_setup_username)) },
                            placeholder = { Text(text = stringResource(id = R.string.streaming_service_setup_username_hint)) },
                            singleLine = true,
                            enabled = !isBusy
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = stringResource(id = R.string.streaming_service_setup_password)) },
                            placeholder = { Text(text = stringResource(id = R.string.streaming_service_setup_password_hint)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !isBusy
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.streaming_service_setup_remember_password),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(id = R.string.streaming_service_setup_remember_password_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TunerAnimatedSwitch(
                                checked = rememberStreamingPasswords,
                                onCheckedChange = { enabled -> appSettings.setRememberStreamingPasswords(enabled) },
                                enabled = !isBusy
                            )
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.connectService(
                                    serviceId = serviceId,
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isBusy && canSubmit
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.size(12.dp))
                            }

                            Text(text = stringResource(id = primaryActionText))
                        }

                        if (session.isConnected) {
                            OutlinedButton(
                                onClick = { viewModel.disconnectService(serviceId) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isBusy
                            ) {
                                Text(text = stringResource(id = R.string.streaming_service_setup_disconnect))
                            }
                        }

                        if (!canSubmit && !isBusy) {
                            Text(
                                text = stringResource(id = R.string.streaming_service_setup_action_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}
