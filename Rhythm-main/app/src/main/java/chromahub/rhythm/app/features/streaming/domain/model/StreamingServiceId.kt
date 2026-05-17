package chromahub.rhythm.app.features.streaming.domain.model

object StreamingServiceId {
    const val SUBSONIC = "SUBSONIC"
    const val JELLYFIN = "JELLYFIN"
    val all = listOf(
        SUBSONIC,
        JELLYFIN
    )
}

object StreamingServiceRules {
    fun requiresServerUrl(serviceId: String): Boolean {
        return serviceId == StreamingServiceId.SUBSONIC || serviceId == StreamingServiceId.JELLYFIN
    }
}