package i.am.rauan.satanbek.qazaqhalyqanderi

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import i.am.rauan.satanbek.qazaqhalyqanderi.db.SongModel


class Storage(context: Context) {
    val updateUIReceiverAction = "i.am.rauan.satanbek.qazaqhalyqanderi.UPDATE_ACTIVITY_UI"
    val updateUIPlay = "update_ui_to_play"
    val updateUIPause = "update_ui_to_pause"
    val updateUIResume = "update_ui_to_resume"
    val updateUIStop = "update_ui_to_stop"
    val updateUISkipToNext = "update_ui_to_skip_to_next"
    val updateUISkipToPrevious = "update_ui_to_skip_to_previous"

    val updateUIPlayUpdate = "update_ui_to_play_update"
    val updateUIPauseUpdate = "update_ui_to_pause_update"
    val updateUIResumeUpdate = "update_ui_to_resume_update"
    val updateUIStopUpdate = "update_ui_to_stop_update"
    val updateUISkipToNextUpdate = "update_ui_to_skip_to_next_update"
    val updateUISkipToPreviousUpdate = "update_ui_to_skip_to_previous_update"

    //AudioPlayer notification ID
    val NOTIFICATION_ID = 101

    val playNewSongReceiverAction = "i.am.rauan.satanbek.qazaqhalyqanderi.PLAY_NEW_SONG"
    private val STORAGE: String = "i.am.rauan.satanbek.qazaqhalyqanderi.STORAGE"
    private var preferences: SharedPreferences? = null
    private var context: Context? = null

    init {
        this.context = context
    }

    private var shuffleMode = "SHUFFLE_MODE"
    private var refreshMode = "REFRESH_MODE"
    private var mediaPause = "PAUSE"
    private var resumePosition = "RESUME_POSITION"
    private var colorMode = "COLOR_MODE"
    var DARK_MODE = 0
    var LIGHT_MODE = 1

    fun setSuffleMode(mode: Int) {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt(shuffleMode, mode)
        editor.apply()
    }

    fun getShuffleMode(): Int {
        return getPreferences()!!.getInt(shuffleMode, 0)
    }

    fun setRefreshMode(mode: Int) {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt(refreshMode, mode)
        editor.apply()
    }

    fun getRefreshMode(): Int {
        return getPreferences()!!.getInt(refreshMode, 0)
    }

    fun setPause(mode: Boolean) {
        Log.d("main", "setPause: $mode")
        var editor: SharedPreferences.Editor = getEditor()

        editor.putBoolean(mediaPause, mode)
        editor.apply()
    }

    fun getPause(): Boolean {
        return getPreferences()!!.getBoolean(mediaPause, true)
    }

    fun setResumePosition(position: Int) {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt(resumePosition, position)
        editor.apply()
    }

    fun getResumePosition(): Int {
        return getPreferences()!!.getInt(resumePosition, 0)
    }


    fun setDarkMode() {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt(colorMode, DARK_MODE)
        editor.apply()
    }

    fun setLightMode() {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt(colorMode, LIGHT_MODE)
        editor.apply()
    }

    fun getColorMode(): Int {
        return getPreferences()!!.getInt(colorMode, DARK_MODE)
    }

    fun setInitListMode(mode: Int) {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt("InitListMode", mode)
        editor.apply()
    }

    fun getInitListMode(): Int {
        return getPreferences()!!.getInt("InitListMode", 2)
    }

    fun storeSongs(arrayList: ArrayList<SongModel>) {
        val editor = getEditor()
        val gson = Gson()
        val json = gson.toJson(arrayList)
        editor.putString("audioArrayList", json)
        editor.apply()
    }

    fun loadSongs(): ArrayList<SongModel> {
        preferences = getPreferences()
        val gson = Gson()
        val json = preferences?.getString("audioArrayList", null)

        if (json != null) {
            val type = object : TypeToken<ArrayList<SongModel>>() {}.type

            return gson.fromJson(json, type)
        }

        return ArrayList()
    }

    fun storeShuffleSongs(arrayList: ArrayList<SongModel>) {
        val editor = getEditor()
        val gson = Gson()
        val json = gson.toJson(arrayList)
        editor.putString("audioShuffleArrayList", json)
        editor.apply()
    }

    fun loadShuffleSongs(): ArrayList<SongModel> {
        preferences = getPreferences()
        val gson = Gson()
        val json = preferences?.getString("audioShuffleArrayList", null)

        if (json != null) {
            val type = object : TypeToken<ArrayList<SongModel>>() {}.type

            return gson.fromJson(json, type)
        }

        return ArrayList()
    }

    fun setCurrentSong(song: SongModel) {
        getEditor().putString("currentSong", Gson().toJson(song)).apply()
    }

    fun getCurrentSong(): SongModel {
        val type = object : TypeToken<SongModel>() {}.type
        if (getPreferences()!!.getString("currentSong", "") != "")
            return Gson().fromJson(getPreferences()!!.getString("currentSong", ""), type)

        return SongModel("", "", "", "", "", "", "", "", 0)
    }

    fun isPlayerActive(): Boolean {
        return getPreferences()!!.getBoolean("is_player_active", false)
    }

    fun playerActive() {
        getEditor().putBoolean("is_player_active", true).apply()
    }

    fun storeFileDir(fileDir: String){
        getEditor().putString("files_dir", fileDir).apply()
    }

    fun getFileDir(): String  {
        return getPreferences()!!.getString("files_dir", "")
    }

    private fun getPreferences(): SharedPreferences? {
        return context?.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
    }

    private fun getEditor(): SharedPreferences.Editor {
        preferences = getPreferences()
        return preferences!!.edit()
    }

    fun storeLoadedTime(time: Long) {
        getEditor().putLong("loaded_time", time).apply()
    }

    fun getLoadedTime(): Long {
        return getPreferences()!!.getLong("loaded_time", 0)
    }
}