package chromahub.rhythm.app.infrastructure.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Preview components for Legacy RemoteViews widgets
 * 
 * This file provides previews for all legacy widget layouts to help visualize
 * what each widget size will look like on the home screen. These correspond
 * to the XML layout files in res/layout.
 */

@Composable
private fun LegacyWidgetPreviewCard(
    title: String,
    size: String,
    layoutFile: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = size,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = layoutFile,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun LegacyWidgetMockup(
    widthDp: Int,
    heightDp: Int,
    layoutName: String,
    showAlbumArtLarge: Boolean = false,
    isVertical: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        if (isVertical) {
            // Vertical layout (1x2)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Album Art
                Box(
                    modifier = Modifier
                        .size((widthDp * 0.6).dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                
                // Title
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                )
                
                // Artist
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                )
                
                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ControlButton(32.dp, false)
                    ControlButton(40.dp, true)
                    ControlButton(32.dp, false)
                }
                
                // Layout label
                Text(
                    text = layoutName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 9.sp
                )
            }
        } else if (showAlbumArtLarge) {
            // Large widget with prominent album art
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Album Art
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Song info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ControlButton(48.dp, false)
                    ControlButton(60.dp, true)
                    ControlButton(48.dp, false)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Layout label
                Text(
                    text = layoutName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 9.sp
                )
            }
        } else {
            // Horizontal layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                Box(
                    modifier = Modifier
                        .size((heightDp * 0.6).dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Info and controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    )
                    
                    // Artist
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    )
                    
                    // Album (if space)
                    if (heightDp > 100) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ControlButton(36.dp, false)
                        ControlButton(44.dp, true)
                        ControlButton(36.dp, false)
                    }
                    
                    // Layout label
                    Text(
                        text = layoutName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlButton(size: Dp, isPrimary: Boolean) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (isPrimary) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.5f)
                .background(
                    if (isPrimary) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onTertiary,
                    shape = CircleShape
                )
        )
    }
}

@Preview(name = "Legacy Widget Previews - All Sizes", showBackground = true, heightDp = 3000)
@Composable
fun LegacyWidgetPreviewsScreen() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "Rhythm Legacy Widget Previews",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "RemoteViews-based widgets for maximum compatibility",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Extra Small Widget
                LegacyWidgetPreviewCard(
                    title = "1×1 Extra Small Widget",
                    size = "70 × 70 dp",
                    layoutFile = "widget_music_extra_small.xml"
                ) {
                    LegacyWidgetMockup(110, 110, "ExtraSmall")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Small Widget
                LegacyWidgetPreviewCard(
                    title = "2×1 Small Horizontal Widget",
                    size = "180 × 90 dp",
                    layoutFile = "widget_music_small.xml"
                ) {
                    LegacyWidgetMockup(220, 90, "Small")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Vertical Widget
                LegacyWidgetPreviewCard(
                    title = "1×2 Vertical Widget",
                    size = "110 × 220 dp",
                    layoutFile = "widget_music_vertical.xml"
                ) {
                    LegacyWidgetMockup(110, 220, "Vertical", isVertical = true)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Medium Widget
                LegacyWidgetPreviewCard(
                    title = "3×2 Medium Widget",
                    size = "250 × 140 dp",
                    layoutFile = "widget_music_medium.xml"
                ) {
                    LegacyWidgetMockup(250, 140, "Medium")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Wide Widget
                LegacyWidgetPreviewCard(
                    title = "4×1 Wide Widget",
                    size = "300 × 90 dp",
                    layoutFile = "widget_music_wide.xml"
                ) {
                    LegacyWidgetMockup(300, 90, "Wide")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Large Widget
                LegacyWidgetPreviewCard(
                    title = "3×3 Large Widget",
                    size = "280 × 280 dp",
                    layoutFile = "widget_music_large.xml"
                ) {
                    LegacyWidgetMockup(280, 280, "Large", showAlbumArtLarge = true)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Extra Large Widget
                LegacyWidgetPreviewCard(
                    title = "4×3 Extra Large Widget",
                    size = "320 × 240 dp",
                    layoutFile = "widget_music_extra_large.xml"
                ) {
                    LegacyWidgetMockup(320, 240, "ExtraLarge", showAlbumArtLarge = true)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 5x5 Widget
                LegacyWidgetPreviewCard(
                    title = "5×5 Premium Widget",
                    size = "400 × 400 dp",
                    layoutFile = "widget_music_5x5.xml"
                ) {
                    LegacyWidgetMockup(360, 360, "5×5", showAlbumArtLarge = true)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Comparison Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Legacy Widget Features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        LegacyFeatureItem("📱 RemoteViews technology for broad compatibility")
                        LegacyFeatureItem("🎨 Material Design with system theme support")
                        LegacyFeatureItem("🔋 Battery-efficient background updates")
                        LegacyFeatureItem("📏 7 different layout sizes (1×1 to 5×5)")
                        LegacyFeatureItem("🖼️ Album art with rounded corners")
                        LegacyFeatureItem("🎵 Full playback controls")
                        LegacyFeatureItem("⚡ Compatible with Android 8.0+")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Migration Note
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Migration to Glance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Consider using Glance widgets for better Material 3 theming, dynamic colors, and more customization options.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LegacyFeatureItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(name = "Medium Legacy Widget Detail", showBackground = true)
@Composable
fun MediumLegacyWidgetDetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LegacyWidgetMockup(250, 140, "3×2 Medium Widget")
            }
        }
    }
}

@Preview(name = "Large Legacy Widget Detail", showBackground = true)
@Composable
fun LargeLegacyWidgetDetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LegacyWidgetMockup(280, 280, "3×3 Large Widget", showAlbumArtLarge = true)
            }
        }
    }
}

