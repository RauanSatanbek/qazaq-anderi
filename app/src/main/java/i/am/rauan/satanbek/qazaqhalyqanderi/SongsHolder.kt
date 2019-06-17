package i.am.rauan.satanbek.qazaqhalyqanderi

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import i.am.rauan.satanbek.qazaqhalyqanderi.db.SongModel
import i.am.rauan.satanbek.qazaqhalyqanderi.db.SqliteDatabase
import kotlinx.android.synthetic.main.song_item.view.*
import java.io.File

class SongsHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    private var itemView: View = itemView

    val songItem = itemView.songItem
    val hr = itemView.hr
    var note = itemView.imageButtonNote
    var earphone = itemView.imageButtonEarPhone
    var mGifVisualizer = itemView.mGifVisualizer
    var download = itemView.imageButtonDownload
    var progressBar = itemView.downloadProgressBar
    var play = itemView.imageButtonPlay
    var pause = itemView.imageButtonPause

    var selectedItem = itemView.selectedItemBg
    var songTitle = itemView.song_title
    private var sharedStorage: Storage = Storage(context)
    var song: SongModel =
        SongModel("", "", "", "", "", "", "", "", 0)

    var isPlaying = false

    var TAG = "main"
    init {
        play.setOnClickListener(this)
        pause.setOnClickListener(this)
        download.setOnClickListener(this)

        itemView.setOnClickListener {
            if(!isPlaying) {
                playBtnClicked()
            } else {
                pauseBtnClicked()
            }
        }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.imageButtonPlay -> {
                playBtnClicked()
            }

            R.id.imageButtonPause -> {
                pauseBtnClicked()
            }

            R.id.imageButtonDownload -> {
                setLoading(true)

                var pathname = "${sharedStorage.getFileDir()}/${song.file}"
                var storage = FirebaseStorage.getInstance(context.resources.getString(R.string.storage_url))
                var storageRef = storage!!.reference
                var audio = storageRef.child(song.file!!)

                val localFile = File(pathname)
                var a = localFile.createNewFile()

                audio.getFile(localFile).addOnSuccessListener {
                    // Local temp file has been created
                    Runtime.getRuntime().exec("chmod 777 $pathname")

                    Toast.makeText(context, "Ән сәтті сақталды", Toast.LENGTH_LONG).show()
                    song.path = pathname

                    var sqliteDatabase = SqliteDatabase(context)
                    sqliteDatabase.updateSongPath(song)

                    setLoading(false)

                    progressBar.visibility = View.GONE
                    download.visibility = View.GONE
                }.addOnFailureListener {
                    setLoading(false)
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        initLoadingUI(isLoading)

        song.isLoading = isLoading
        if (sharedStorage.getShuffleMode() == 1) {
            var songs = sharedStorage.loadShuffleSongs()

            for (i in 0 until songs.size) {
                if (songs[i].key == song.key) {
                    songs[i].isLoading = isLoading
                    songs[i].path = song.path
                }
            }

            sharedStorage.storeShuffleSongs(songs)
        } else if (sharedStorage.getShuffleMode() == 0) {
            var songs = sharedStorage.loadSongs()

            for (i in 0 until songs.size) {
                if (songs[i].key == song.key) {
                    songs[i].isLoading = isLoading
                    songs[i].path = song.path
                }
            }

            sharedStorage.storeSongs(songs)
        }
    }

    private fun initLoadingUI(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            download.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            download.visibility = View.VISIBLE
        }
    }
    private fun pauseBtnClicked() {
        if (isPlaying) {
            var intent = Intent(sharedStorage.updateUIReceiverAction)
            intent.putExtra(sharedStorage.updateUIPause, true)
            context.sendBroadcast(intent)

            off()

            isPlaying = false
            pause()
            selectedItem.visibility = View.VISIBLE
        }
    }

    private fun playBtnClicked() {
        if (!isPlaying) {
            if(sharedStorage.getCurrentSong() != null && song.key != sharedStorage.getCurrentSong().key) {
                sharedStorage.setResumePosition(0)

                sharedStorage.setCurrentSong(song)

                var intent = Intent(sharedStorage.updateUIReceiverAction)
                intent.putExtra(sharedStorage.updateUIPlay, true)
                context.sendBroadcast(intent)
            } else if (song.key == sharedStorage.getCurrentSong().key) {
                var intent = Intent(sharedStorage.updateUIReceiverAction)
                intent.putExtra(sharedStorage.updateUIResume, true)
                context.sendBroadcast(intent)
            }

            isPlaying = true
            playing()
            selectedItem.visibility = View.VISIBLE
        }
    }

    fun playing() {
        note.visibility = View.INVISIBLE
//        earphone.visibility = View.VISIBLE
        mGifVisualizer.visibility = View.VISIBLE

        play.visibility = View.GONE
        play.isEnabled = false

        pause.visibility = View.VISIBLE
        pause.isEnabled = true

        selectedItem.visibility = View.VISIBLE

        isPlaying = true

        initLoadingUI(song.isLoading)

        if (song.path != "") {
            download.visibility = View.GONE
        }
    }

    fun resume() {
        Log.d("main", "resume")
        note.visibility = View.VISIBLE
//        earphone.visibility = View.VISIBLE
        mGifVisualizer.visibility = View.INVISIBLE

        download.visibility = View.VISIBLE

        play.visibility = View.VISIBLE
        play.isEnabled = true

        pause.visibility = View.GONE
        pause.isEnabled = false

        selectedItem.visibility = View.VISIBLE

        isPlaying = false

        initLoadingUI(song.isLoading)

        if (song.path != "") {
            download.visibility = View.GONE
        }

        if (song.key == sharedStorage.getCurrentSong().key && !sharedStorage?.getPause()) {
            var intent = Intent(sharedStorage.updateUIReceiverAction)
            intent.putExtra(sharedStorage.updateUIResume, true)
            context.sendBroadcast(intent)
        }
    }

    fun setAudioSessionID(sessionID: Int) {
    }

    fun off() {
        note.visibility = View.VISIBLE
//        earphone.visibility = View.INVISIBLE
        mGifVisualizer.visibility = View.INVISIBLE

        download.visibility = View.VISIBLE

        play.visibility = View.VISIBLE
        play.isEnabled = true

        pause.visibility = View.GONE
        pause.isEnabled = false

        selectedItem.visibility = View.INVISIBLE

        isPlaying = false

        initLoadingUI(song.isLoading)

        if (song.path != "") {
            download.visibility = View.GONE
        }

    }

    fun selected() {
        selectedItem.visibility = View.VISIBLE
    }

    fun notSelected() {
        selectedItem.visibility = View.INVISIBLE
    }

    fun pause() {
        off()

        selectedItem.visibility = View.VISIBLE
    }

    fun setData() {
        off()

        songTitle.text = song.name

        if (this.song.key == sharedStorage.getCurrentSong().key) {
            Log.d("main", "${song.name}, ${sharedStorage.getPause()}")
            if (sharedStorage.getPause()) {
                resume()
            } else {
                playBtnClicked()
            }
        }
    }

    fun setColorMode() {
        when(sharedStorage.getColorMode()) {
            sharedStorage.DARK_MODE -> {
                songItem.setBackgroundColor(context.resources.getColor(R.color.dark_blue))
                selectedItem.setBackgroundColor(context.resources.getColor(R.color.white_dark_blue))
                hr.setBackgroundColor(context.resources.getColor(R.color.hr))
                songTitle.setTextColor(context.resources.getColor(R.color.white))
            }

            sharedStorage.LIGHT_MODE -> {
                songItem.setBackgroundColor(context.resources.getColor(R.color.invert_dark_blue))
                selectedItem.setBackgroundColor(context.resources.getColor(R.color.invert_white_dark_blue))
                hr.setBackgroundColor(context.resources.getColor(R.color.invert_hr))
                songTitle.setTextColor(context.resources.getColor(R.color.black))
            }
        }
    }

}