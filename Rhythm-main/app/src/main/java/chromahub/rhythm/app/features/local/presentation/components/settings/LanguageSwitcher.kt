package chromahub.rhythm.app.features.local.presentation.components.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import java.util.Locale

data class LanguageOption(
    val code: String,
    val displayName: String,
    val nativeName: String
)

object LanguageHelper {
    val supportedLanguages = listOf(
        LanguageOption("en", "English", "English"),
        LanguageOption("ar", "Arabic", "العربية"),
        LanguageOption("de", "German", "Deutsch"),
        LanguageOption("es", "Spanish", "Español"),
        LanguageOption("fr", "French", "Français"),
        LanguageOption("hi", "Hindi", "हिन्दी"),
        LanguageOption("it", "Italian", "Italiano"),
        LanguageOption("ja", "Japanese", "日本語"),
        LanguageOption("ko", "Korean", "한국어"),
        LanguageOption("nl", "Dutch", "Nederlands"),
        LanguageOption("pl", "Polish", "Polski"),
        LanguageOption("pt", "Portuguese", "Português"),
        LanguageOption("ru", "Russian", "Русский"),
        LanguageOption("tr", "Turkish", "Türkçe"),
        LanguageOption("vi", "Vietnamese", "Tiếng Việt"),
        LanguageOption("zh", "Chinese", "中文")
    )
    
    fun getCurrentLanguage(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
            localeManager?.applicationLocales?.get(0)?.language ?: Locale.getDefault().language
        } else {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) {
                Locale.getDefault().language
            } else {
                locales.get(0)?.language ?: Locale.getDefault().language
            }
        }
    }
    
    fun setLanguage(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
            localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
        } else {
            val locale = Locale.forLanguageTag(languageCode)
            val localeList = LocaleListCompat.create(locale)
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSwitcherDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(LanguageHelper.getCurrentLanguage(context)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Language",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Select Language",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(LanguageHelper.supportedLanguages, key = { "lang_${it.code}" }) { language ->
                    LanguageItem(
                        language = language,
                        isSelected = currentLanguage == language.code,
                        onClick = {
                            currentLanguage = language.code
                            LanguageHelper.setLanguage(context, language.code)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun LanguageItem(
    language: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}
