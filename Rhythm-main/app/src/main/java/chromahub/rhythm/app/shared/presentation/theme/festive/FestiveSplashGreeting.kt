package chromahub.rhythm.app.ui.theme.festive

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Festive splash greeting overlay for Christmas and New Year
 */
@Composable
fun FestiveSplashGreeting(
    festiveType: FestiveThemeType,
    modifier: Modifier = Modifier
) {
    if (festiveType == FestiveThemeType.NONE || festiveType == FestiveThemeType.CUSTOM) {
        return
    }
    
    // Animation for fade in and scale
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale)
        ) {
            when (festiveType) {
                FestiveThemeType.CHRISTMAS -> {
                    ChristmasGreeting()
                }
                FestiveThemeType.NEW_YEAR -> {
                    NewYearGreeting()
                }
                FestiveThemeType.HALLOWEEN -> {
                    HalloweenGreeting()
                }
                FestiveThemeType.VALENTINES -> {
                    ValentinesGreeting()
                }
            }
        }
    }
}

@Composable
private fun ChristmasGreeting() {
    // Sparkle animation
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val sparkle by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle"
    )
    
    Text(
        text = "🎄",
        fontSize = 72.sp,
        modifier = Modifier.alpha(sparkle)
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = "Merry Christmas!",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFE63946),
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = "🎅 Season's Greetings 🎁",
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFF2A9D8F),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun NewYearGreeting() {
    // Sparkle animation
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val sparkle by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle"
    )
    
    Text(
        text = "🎆",
        fontSize = 72.sp,
        modifier = Modifier.alpha(sparkle)
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = "Happy New Year!",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFFD700),
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = "🎊 2026 🎉",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF4169E1),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun HalloweenGreeting() {
    Text(
        text = "🎃",
        fontSize = 72.sp
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = "Happy Halloween!",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFF6B35),
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = "👻 Spooky Season 🦇",
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFF6A4C93),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ValentinesGreeting() {
    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Text(
        text = "💕",
        fontSize = 72.sp,
        modifier = Modifier.scale(pulse)
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = "Happy Valentine's!",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFF006E),
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = "💖 Love is in the Air 💗",
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFFFF8FA3),
        textAlign = TextAlign.Center
    )
}
