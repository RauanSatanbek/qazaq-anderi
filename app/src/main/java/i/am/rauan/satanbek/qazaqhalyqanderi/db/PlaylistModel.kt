package i.am.rauan.satanbek.qazaqhalyqanderi.db

import java.io.Serializable

data class PlaylistModel(
    var id: Int?,
    var key: String?,
    var name: String?,
    var image: String?,
    var count: Int?,
    var imagePath: String?) : Serializable