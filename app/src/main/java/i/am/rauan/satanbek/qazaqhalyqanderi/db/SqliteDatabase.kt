package i.am.rauan.satanbek.qazaqhalyqanderi.db


import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Created by Eyehunt Team on 07/06/18.
 */
class SqliteDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        val createPlaylistTable = "CREATE TABLE $playlistTable " +
                "($pId Integer PRIMARY KEY AUTOINCREMENT, $pKey TEXT, $pName TEXT, $pImage TEXT, $pCount Integer, $pImagePath TEXT, $pType INTEGER)"
        db?.execSQL(createPlaylistTable)

        var createSongsTable = "CREATE TABLE $songTable ($sId INTEGER PRIMARY KEY AUTOINCREMENT, $sKey TEXT, $sName TEXT, $sPlaylist TEXT, $sFile TEXT, $sText TEXT, $sPath TEXT, $sUrl TEXT, $sIsFavorite INTEGER DEFAULT 0)"

        db?.execSQL(createSongsTable)

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $playlistTable")
        db?.execSQL("DROP TABLE IF EXISTS $songTable")

        onCreate(db)
    }

    fun addSingerPlaylist(p: PlaylistModel): Boolean {
        if (getPlaylistBy(pKey, p.key.toString()).key == "") {
            val db = this.writableDatabase
            val values = ContentValues()

            values.put(pKey, p.key)
            values.put(pName, p.name)
            values.put(pImage, p.image)
            values.put(pCount, p.count)
            values.put(pImagePath, p.imagePath)
            values.put(pType, SINGER_PLAYLIST)

            val _success = db.insert(playlistTable, null, values)

            return (Integer.parseInt("$_success") != -1)
        }

        return false
    }

    fun addOwnPlaylist(p: PlaylistModel): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(pName, p.name)
        values.put(pImage, p.image)
        values.put(pCount, p.count)
        values.put(pImagePath, p.imagePath)
        values.put(pType, OWN_PLAYLIST)

        val success = db.insert(playlistTable, null, values)

        db.close()
        return (Integer.parseInt("$success") != -1)
    }

    fun getAllSingerPlaylist(): ArrayList<PlaylistModel> {
        var playlists = ArrayList<PlaylistModel>()

        val db = readableDatabase
        val query = "SELECT * FROM $playlistTable WHERE $pType = ?"
        val cursor = db.rawQuery(query, arrayOf(SINGER_PLAYLIST.toString()))

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    var id = cursor.getInt(cursor.getColumnIndex(pId))
                    var key = cursor.getString(cursor.getColumnIndex(pKey))
                    var name = cursor.getString(cursor.getColumnIndex(pName))
                    var image = cursor.getString(cursor.getColumnIndex(pImage))
                    var count = cursor.getInt(cursor.getColumnIndex(pCount))
                    var imagePath = cursor.getString(cursor.getColumnIndex(pImagePath))

                    playlists.add(PlaylistModel(id, key, name, image, count, imagePath))
                } while (cursor.moveToNext())
            }
        }

        cursor.close()
        db.close()
        return playlists
    }

    fun getPlaylistBy(key: String, value: String): PlaylistModel {
        val db = this.readableDatabase
        var query = "SELECT * FROM $playlistTable WHERE $key = ?"
        var cursor = db.rawQuery(query, arrayOf(value))

        if (cursor != null && cursor.moveToFirst()) {
            var id = cursor.getInt(cursor.getColumnIndex(pId))
            var key = cursor.getString(cursor.getColumnIndex(pKey))
            var name = cursor.getString(cursor.getColumnIndex(pName))
            var image = cursor.getString(cursor.getColumnIndex(pImage))
            var count = cursor.getInt(cursor.getColumnIndex(pCount))
            var imagePath = cursor.getString(cursor.getColumnIndex(pImagePath))

            return PlaylistModel(id, key, name, image, count ,imagePath)
        }

        return PlaylistModel(0,  "","", "", 0, "")
    }


    fun getAllOwnPlaylist(): ArrayList<PlaylistModel> {
        var playlists = ArrayList<PlaylistModel>()

        val db = readableDatabase
        val query = "SELECT * FROM $playlistTable WHERE $pType = ?"
        val cursor = db.rawQuery(query, arrayOf(OWN_PLAYLIST.toString()))

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    var id = cursor.getInt(cursor.getColumnIndex(pId))
                    var name = cursor.getString(cursor.getColumnIndex(pName))
                    var image = cursor.getString(cursor.getColumnIndex(pImage))
                    var count = cursor.getInt(cursor.getColumnIndex(pCount))
                    var imagePath = cursor.getString(cursor.getColumnIndex(pImagePath))

                    playlists.add(PlaylistModel(id, id.toString(), name, image, count, imagePath))
                } while (cursor.moveToNext())
            }
        }

        cursor.close()
        db.close()
        return playlists
    }

    fun addSong(s: SongModel): Boolean {
        if (getSongBy(sKey, s.key.toString()).key == "") {
            val db = this.writableDatabase
            val values = ContentValues()

            values.put(sKey, s.key)
            values.put(sName, s.name)
            values.put(sPlaylist, s.playlist)
            values.put(sFile, s.file)
            values.put(sText, s.text)
            values.put(sPath, s.path)
            values.put(sUrl, s.url)
            values.put(sIsFavorite, 0)

            var success = db.insert(songTable, null, values)

            db.close()

            return (Integer.parseInt("$success") != -1)
        }

        return false
    }


    fun updateSongPath(s: SongModel): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(sKey, s.key)
        values.put(sName, s.name)
        values.put(sText, s.text)
        values.put(sPlaylist, s.playlist)
        values.put(sFile, s.file)
        values.put(sUrl, s.url)
        values.put(sPath, s.path)

        val success = db.update(songTable, values, "$sKey=?", arrayOf(s.key.toString())).toLong()

        return Integer.parseInt("$success") != -1
    }

    fun getAllSongs(): ArrayList<SongModel> {
        var songs = ArrayList<SongModel> ()

        val db = this.readableDatabase
        val query = "SELECT * FROM $songTable"
        val cursor = db.rawQuery(query, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    var id = cursor.getInt(cursor.getColumnIndex(sId))
                    var key = cursor.getString(cursor.getColumnIndex(sKey))
                    var name = cursor.getString(cursor.getColumnIndex(sName))
                    var playlist = cursor.getString(cursor.getColumnIndex(sPlaylist))
                    var file = cursor.getString(cursor.getColumnIndex(sFile))
                    var text = cursor.getString(cursor.getColumnIndex(sText))
                    var url = cursor.getString(cursor.getColumnIndex(sUrl))
                    var path = cursor.getString(cursor.getColumnIndex(sPath))
                    var isFavorite = cursor.getInt(cursor.getColumnIndex(sIsFavorite))

                    songs.add(SongModel("$id", key, name, playlist, file, text, path, url, isFavorite))

                } while (cursor.moveToNext())
            }
        }

        cursor.close()
        db.close()

        return songs
     }

    fun getLoadedSongs(): ArrayList<SongModel> {
        var songs = ArrayList<SongModel> ()

        val db = this.readableDatabase
        val query = "SELECT * FROM $songTable WHERE $sPath != ?"
        val cursor = db.rawQuery(query, arrayOf(""))

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    var id = cursor.getInt(cursor.getColumnIndex(sId))
                    var key = cursor.getString(cursor.getColumnIndex(sKey))
                    var name = cursor.getString(cursor.getColumnIndex(sName))
                    var playlist = cursor.getString(cursor.getColumnIndex(sPlaylist))
                    var file = cursor.getString(cursor.getColumnIndex(sFile))
                    var text = cursor.getString(cursor.getColumnIndex(sText))
                    var url = cursor.getString(cursor.getColumnIndex(sUrl))
                    var path = cursor.getString(cursor.getColumnIndex(sPath))
                    var isFavorite = cursor.getInt(cursor.getColumnIndex(sIsFavorite))

                    songs.add(SongModel("${id}", key, name, playlist, file, text, path, url, isFavorite))

                } while (cursor.moveToNext())
            }
        }

        cursor.close()
        db.close()

        return songs
    }

    fun getSongsBy(key: String, value: String): ArrayList<SongModel> {
        var songs = ArrayList<SongModel> ()

        val db = this.readableDatabase
        var query = "SELECT * FROM $songTable WHERE $key = ?"
        var cursor = db.rawQuery(query, arrayOf(value))

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    var id = cursor.getInt(cursor.getColumnIndex(sId))
                    var key = cursor.getString(cursor.getColumnIndex(sKey))
                    var name = cursor.getString(cursor.getColumnIndex(sName))
                    var playlist = cursor.getString(cursor.getColumnIndex(sPlaylist))
                    var file = cursor.getString(cursor.getColumnIndex(sFile))
                    var text = cursor.getString(cursor.getColumnIndex(sText))
                    var url = cursor.getString(cursor.getColumnIndex(sUrl))
                    var path = cursor.getString(cursor.getColumnIndex(sPath))
                    var isFavorite = cursor.getInt(cursor.getColumnIndex(sIsFavorite))

                    songs.add(SongModel("${id}", key, name, playlist, file, text, path, url, isFavorite))

                } while (cursor.moveToNext())
            }
        }

        return songs
    }

    fun getSongBy(key: String, value: String): SongModel {
        val db = this.readableDatabase
        var query = "SELECT * FROM $songTable WHERE $key = ?"
        var cursor = db.rawQuery(query, arrayOf(value))

        if (cursor != null && cursor.moveToFirst()) {
            var id = cursor.getInt(cursor.getColumnIndex(sId))
            var key = cursor.getString(cursor.getColumnIndex(sKey))
            var name = cursor.getString(cursor.getColumnIndex(sName))
            var playlist = cursor.getString(cursor.getColumnIndex(sPlaylist))
            var file = cursor.getString(cursor.getColumnIndex(sFile))
            var text = cursor.getString(cursor.getColumnIndex(sText))
            var url = cursor.getString(cursor.getColumnIndex(sUrl))
            var path = cursor.getString(cursor.getColumnIndex(sPath))
            var isFavorite = cursor.getInt(cursor.getColumnIndex(sIsFavorite))

             return SongModel("${id}", key, name, playlist, file, text, path, url, isFavorite)
        }

        return SongModel("",  "","", "", "", "", "", "", 0)
    }


    fun updateFavorite(s: SongModel): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(sIsFavorite, s.isFavorite)

        val success = db.update(songTable, values, "$sKey=?", arrayOf(s.key.toString())).toLong()

        return Integer.parseInt("$success") != -1
    }

    companion object {
        private const val DB_NAME = "a7766gsrabskNewAlbumAppForGaniMatebayevBy_r1_"
        private const val DB_VERSION = 1

        private const val SINGER_PLAYLIST = 1
        private const val OWN_PLAYLIST = 2

        private const val playlistTable = "playlist"
        private const val pId = "id"
        private const val pKey = "_key"
        private const val pName = "name"
        private const val pImage = "image"
        private const val pCount = "count"
        private const val pImagePath = "image_path"
        private const val pType = "type"

        private const val songTable = "song"
        private const val sId = "id"
        private const val sKey = "_key"
        private const val sName = "name"
        private const val sPlaylist = "playlist"
        private const val sFile = "file"
        private const val sText = "_text"
        private const val sUrl = "_url"
        private const val sPath = "path"
        private const val sIsFavorite = "is_favorite"
    }
}