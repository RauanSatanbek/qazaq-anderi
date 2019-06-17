package i.am.rauan.satanbek.qazaqhalyqanderi.db

import java.io.Serializable

data class SongModel(
    var id: String?,
    var key: String?,
    var name: String?,
    var playlist: String?,
    var file: String?,
    var text: String?,

    var path: String?,
    var url: String?,
    var isFavorite: Int?) : Serializable {

    var isLoading: Boolean = false
}

