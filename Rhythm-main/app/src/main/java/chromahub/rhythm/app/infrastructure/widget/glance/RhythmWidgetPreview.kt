package chromahub.rhythm.app.infrastructure.widget.glance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Preview components for Glance widgets
 * 
 * This file provides previews for all widget sizes and states to help visualize
 * the widget layouts during development. These previews show what the actual
 * Glance widgets will look like on the home screen.
 */

@Composable
private fun WidgetPreviewCard(
    title: String,
    size: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
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
            modifier = Modifier.padding(bottom = 12.dp)
        )
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
private fun WidgetMockup(
    widthDp: Int,
    heightDp: Int,
    layoutName: String
) {
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Album Art Mockup
            Box(
                modifier = Modifier
                    .size((heightDp * 0.4).dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Title Mockup
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Artist Mockup
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Controls Row Mockup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (it == 1) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.tertiary
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Layout Name
            Text(
                text = layoutName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

@Preview(name = "Glance Widget Previews - All Sizes", showBackground = true)
@Composable
fun GlanceWidgetPreviewsScreen() {
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
                    text = "Rhythm Glance Widget Previews",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // 1x1 Widget
                WidgetPreviewCard(
                    title = "1×1 Compact Widget",
                    size = "110 × 110 dp"
                ) {
                    WidgetMockup(110, 110, "OneByOne")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 1x2 Narrow Vertical Widget
                WidgetPreviewCard(
                    title = "1×2 Vertical Widget",
                    size = "110 × 220 dp"
                ) {
                    WidgetMockup(110, 220, "Gabe")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 2x1 Horizontal Strip Widget
                WidgetPreviewCard(
                    title = "2×1 Horizontal Strip",
                    size = "250 × 80 dp"
                ) {
                    WidgetMockup(250, 80, "Thin")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 2x2 Small Widget
                WidgetPreviewCard(
                    title = "2×2 Small Widget",
                    size = "180 × 180 dp"
                ) {
                    WidgetMockup(180, 180, "Small")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 3x2 Medium Widget
                WidgetPreviewCard(
                    title = "3×2 Medium Widget",
                    size = "250 × 150 dp"
                ) {
                    WidgetMockup(250, 150, "Medium")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 4x2 Large Widget
                WidgetPreviewCard(
                    title = "4×2 Large Widget",
                    size = "300 × 180 dp"
                ) {
                    WidgetMockup(300, 180, "Large")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 3x3 Extra Large Widget
                WidgetPreviewCard(
                    title = "3×3 Extra Large Widget",
                    size = "300 × 220 dp"
                ) {
                    WidgetMockup(300, 220, "ExtraLarge")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 4x3 Extra Large Plus Widget
                WidgetPreviewCard(
                    title = "4×3 Extra Large Plus Widget",
                    size = "350 × 260 dp"
                ) {
                    WidgetMockup(350, 260, "ExtraLargePlus")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 5x4 Huge Widget
                WidgetPreviewCard(
                    title = "5×4 Huge Widget",
                    size = "400 × 300 dp"
                ) {
                    WidgetMockup(400, 300, "Huge")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Widget States Section
                Text(
                    text = "Widget States",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Playing", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        WidgetMockup(180, 180, "Playing")
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Paused", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        WidgetMockup(180, 180, "Paused")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Design Notes
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Design Features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        DesignFeatureItem("🎨 Material 3 Expressive Design")
                        DesignFeatureItem("📐 SizeMode.Exact for pixel-perfect layouts")
                        DesignFeatureItem("🎵 Dynamic play/pause button corners")
                        DesignFeatureItem("🖼️ Album art with LruCache optimization")
                        DesignFeatureItem("🎯 Adaptive layouts for all sizes")
                        DesignFeatureItem("✨ Rounded corners and expressive spacing")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DesignFeatureItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(name = "Small Widget Detail", showBackground = true)
@Composable
fun SmallWidgetDetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WidgetMockup(180, 180, "2×2 Small Widget")
            }
        }
    }
}

@Preview(name = "Medium Widget Detail", showBackground = true)
@Composable
fun MediumWidgetDetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WidgetMockup(250, 150, "3×2 Medium Widget")
            }
        }
    }
}

@Preview(name = "Large Widget Detail", showBackground = true)
@Composable
fun LargeWidgetDetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WidgetMockup(300, 220, "3×3 Extra Large Widget")
            }
        }
    }
}

