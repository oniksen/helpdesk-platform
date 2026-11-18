package domain.model.response.access

import domain.model.TrackTag

data class DevChannel(
    val management: Boolean,
    val tracks: List<TrackTag>
)
