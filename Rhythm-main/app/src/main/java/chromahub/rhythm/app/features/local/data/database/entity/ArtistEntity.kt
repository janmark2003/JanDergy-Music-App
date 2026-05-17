package chromahub.rhythm.app.features.local.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artworkUri: String?,
    val numberOfAlbums: Int,
    val numberOfTracks: Int,
    val groupByAlbumArtist: Boolean
)