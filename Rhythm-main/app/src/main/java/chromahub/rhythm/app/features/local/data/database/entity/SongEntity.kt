package chromahub.rhythm.app.features.local.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String,
    val duration: Long,
    val uri: String,
    val artworkUri: String?,
    val trackNumber: Int,
    val year: Int,
    val genre: String?,
    val dateAdded: Long,
    val dateModified: Long,
    val albumArtist: String?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val channels: Int?,
    val codec: String?,
    val discNumber: Int = 1
)
