package chromahub.rhythm.app.features.streaming.domain.model

/**
 * Represents a browse category/genre from a streaming service.
 * Used for browsing music by category (e.g., "Rock", "Pop", "Workout", etc.).
 */
data class BrowseCategory(
    val id: String,
    val name: String,
    val iconUrl: String? = null
)
