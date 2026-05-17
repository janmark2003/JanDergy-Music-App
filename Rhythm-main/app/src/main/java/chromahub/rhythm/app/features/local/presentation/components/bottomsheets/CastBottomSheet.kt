package chromahub.rhythm.app.features.local.presentation.components.bottomsheets

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.PlayerStateTransfer
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

/**
 * Data class representing a Cast device
 */
data class CastDevice(
    val id: String,
    val name: String,
    val deviceType: CastDeviceType,
    val isConnected: Boolean = false
)

enum class CastDeviceType {
    TV, SPEAKER, SOUNDBAR, PHONE, UNKNOWN
}

/**
 * Cast Bottom Sheet for managing cast/streaming devices
 * Uses native Android output switcher for device selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastBottomSheet(
    musicViewModel: MusicViewModel,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    // Animation states
    var showContent by remember { mutableStateOf(false) }
    
    // Get current playback device info (if available)
    val currentDevice by remember { mutableStateOf<CastDevice?>(
        CastDevice("local", context.getString(R.string.bottomsheet_this_phone), CastDeviceType.PHONE, isConnected = true)
    ) }
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentAlpha"
    )
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
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
                .padding(bottom = 32.dp)
        ) {
            // Header
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                CastHeader(
                    isConnected = currentDevice != null,
                    connectedDeviceName = currentDevice?.name
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current Device Card
                if (currentDevice != null) {
                    item {
                        CurrentDeviceCard(
                            device = currentDevice!!,
                            haptics = haptics
                        )
                    }
                }
                
                // Output Switcher Button
                item {
                    OutputSwitcherCard(
                        onOpenSwitcher = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            // Use native Android output switcher
                            musicViewModel.showOutputSwitcherDialog()
                            Toast.makeText(context, context.getString(R.string.bottomsheet_opening_switcher), Toast.LENGTH_SHORT).show()
                        },
                        haptics = haptics
                    )
                }
                
                // Cast to TV/Chromecast Card (Future Integration)
                item {
                    CastToDeviceCard(
                        isCasting = false,
                        castDeviceName = null,
                        onCastClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            Toast.makeText(context, "Cast integration coming soon", Toast.LENGTH_SHORT).show()
                        },
                        haptics = haptics
                    )
                }
                
                // Info Card
                item {
                    CastInfoCard()
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CastHeader(
    isConnected: Boolean,
    connectedDeviceName: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = context.getString(R.string.bottomsheet_cast_stream),
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
                    text = if (isConnected) context.getString(R.string.bottomsheet_connected_to_device, connectedDeviceName ?: "") else context.getString(R.string.bottomsheet_stream_to_devices),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        if (isConnected) {
            Icon(
                imageVector = Icons.Rounded.CastConnected,
                contentDescription = null,
                
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun CurrentDeviceCard(
    device: CastDevice,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "connectedPulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )
    
//    Card(
//        modifier = modifier
//            .fillMaxWidth()
//            .border(
//                width = 2.dp,
//                brush = Brush.horizontalGradient(
//                    colors = listOf(
//                        MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
//                        MaterialTheme.colorScheme.tertiary.copy(alpha = borderAlpha)
//                    )
//                ),
//                shape = RoundedCornerShape(20.dp)
//            ),
//        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Device icon
//            Box(
//                modifier = Modifier
//                    .size(56.dp)
//                    .clip(CircleShape)
//                    .background(MaterialTheme.colorScheme.primary),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = getDeviceIcon(device.deviceType),
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.onPrimary,
//                    modifier = Modifier.size(28.dp)
//                )
//            }
//
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = device.name,
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.SemiBold,
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(4.dp)
//                ) {
//                    Text(
//                        text = context.getString(R.string.bottomsheet_playing_now),
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
//
//            Icon(
//                imageVector = Icons.Default.Check,
//                contentDescription = context.getString(R.string.bottomsheet_active_device),
//                
//                modifier = Modifier.size(24.dp)
//            )
//        }
//    }
}

@Composable
private fun OutputSwitcherCard(
    onOpenSwitcher: () -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cast,
                    contentDescription = null,
                    
                    modifier = Modifier.size(28.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = context.getString(R.string.bottomsheet_switch_output),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = context.getString(R.string.bottomsheet_connect_devices),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onOpenSwitcher,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cast,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = context.getString(R.string.bottomsheet_open_switcher),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CastToDeviceCard(
    isCasting: Boolean,
    castDeviceName: String?,
    onCastClick: () -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "castPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCasting) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
//
//    Card(
//        modifier = modifier
//            .fillMaxWidth()
//            .then(if (isCasting) Modifier.graphicsLayer { scaleX = pulseScale; scaleY = pulseScale } else Modifier),
//        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = if (isCasting)
//                MaterialTheme.colorScheme.primaryContainer
//            else
//                MaterialTheme.colorScheme.surfaceContainerHigh
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                Icon(
//                    imageVector = if (isCasting) Icons.Default.CastConnected else Icons.Default.Tv,
//                    contentDescription = null,
//                    tint = if (isCasting) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.size(28.dp)
//                )
//
//                Column(modifier = Modifier.weight(1f)) {
//                    Text(
//                        text = if (isCasting) context.getString(R.string.bottomsheet_connected_to_device, castDeviceName ?: "") else context.getString(R.string.bottomsheet_cast_to_tv),
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.SemiBold,
//                        color = if (isCasting) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
//                    )
//
//                    Text(
//                        text = if (isCasting) context.getString(R.string.bottomsheet_tap_disconnect) else context.getString(R.string.bottomsheet_cast_description),
//                        style = MaterialTheme.typography.bodySmall,
//                        color = if (isCasting) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            if (isCasting) {
//                OutlinedButton(
//                    onClick = onCastClick,
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(16.dp),
//                    contentPadding = PaddingValues(16.dp)
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Close,
//                        contentDescription = null,
//                        modifier = Modifier.size(20.dp)
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = context.getString(R.string.bottomsheet_disconnect),
//                        style = MaterialTheme.typography.labelLarge,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                }
//            } else {
//                Button(
//                    onClick = onCastClick,
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(16.dp),
//                    contentPadding = PaddingValues(16.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = MaterialTheme.colorScheme.tertiary,
//                        contentColor = MaterialTheme.colorScheme.onTertiary
//                    )
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Tv,
//                        contentDescription = null,
//                        modifier = Modifier.size(20.dp)
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = context.getString(R.string.bottomsheet_find_devices),
//                        style = MaterialTheme.typography.labelLarge,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                }
//            }
//        }
//    }
}

@Composable
private fun CastInfoCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
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
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    text = context.getString(R.string.bottomsheet_about_casting),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = context.getString(R.string.bottomsheet_casting_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun getDeviceIcon(deviceType: CastDeviceType): ImageVector {
    return when (deviceType) {
        CastDeviceType.TV -> Icons.Default.Tv
        CastDeviceType.SPEAKER -> Icons.Default.Speaker
        CastDeviceType.SOUNDBAR -> Icons.Default.Speaker
        CastDeviceType.PHONE -> Icons.Default.PhoneAndroid
        CastDeviceType.UNKNOWN -> Icons.Default.Cast
    }
}

@Composable
private fun getDeviceTypeName(deviceType: CastDeviceType): String {
    val context = LocalContext.current
    return when (deviceType) {
        CastDeviceType.TV -> context.getString(R.string.device_type_television)
        CastDeviceType.SPEAKER -> context.getString(R.string.device_type_smart_speaker)
        CastDeviceType.SOUNDBAR -> context.getString(R.string.device_type_soundbar)
        CastDeviceType.PHONE -> context.getString(R.string.device_type_phone)
        CastDeviceType.UNKNOWN -> context.getString(R.string.device_type_cast_device)
    }
}
