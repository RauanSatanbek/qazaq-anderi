package i.am.rauan.satanbek.qazaqhalyqanderi

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import i.am.rauan.satanbek.qazaqhalyqanderi.db.SongModel
import java.util.*
import kotlin.collections.ArrayList

class RecyclerAdapter(private var songs: ArrayList<SongModel>, private val context: Context): RecyclerView.Adapter<SongsHolder>() {

    var holders: ArrayList<SongsHolder> = ArrayList()
    private var sharedStorage: Storage = Storage(context)

    fun addItem(song: SongModel) {
        songs.add(song)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): SongsHolder {
        return SongsHolder(LayoutInflater.from(context).inflate(R.layout.song_item, p0, false), context)
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun onBindViewHolder(holder: SongsHolder, p1: Int) {
        holder.setColorMode()
        holder.song = songs[p1]
        holder.setData()

        if (holder in holders) {
            Log.d("main", "holders exits: ${holders.size}")
        } else {
            holders.add(holder)
            Log.d("main", "holders: ${holders.size}")
        }
    }

    fun setColorModeForHolders() {
        for (holder in holders) {
            holder.setColorMode()
        }
    }

    fun pause() {
        for (i in holders) {
            if (i.isPlaying) {
                i.pause()
            }
        }
    }

    fun play() {
        offAll()

        for (i in holders) {
            if (i.song.key == sharedStorage.getCurrentSong().key) {
                i.playing()
            }
        }
    }

    fun selected() {
        for (i in holders) {
            if (i.song.key == sharedStorage.getCurrentSong().key) {
                i.selected()
            } else {
                i.notSelected()
            }
        }
    }

    fun resume() {
        offAll()

        for (i in holders) {
            if (i.song.key == sharedStorage.getCurrentSong().key) {
                i.resume()
            }
        }
    }

    fun offAll() {
        for (i in holders) {
            if (i.song.key != sharedStorage.getCurrentSong().key) {
                i.off()
            }
        }
    }

    fun next() {
        for (i in holders) {
            if (i.isPlaying) {
                i.off()
            }
            if (i.song.key == sharedStorage.getCurrentSong().key) {
                i.playing()
            }
        }
    }

    fun previous() {
        for (i in holders) {
            if (i.isPlaying) {
                i.off()
            }
            if (i.song.key == sharedStorage.getCurrentSong().key) {
                i.playing()
            }
        }
    }

    fun shuffle() {
        if (sharedStorage.getShuffleMode() == 1) {
            songs.shuffle()
            sharedStorage.storeShuffleSongs(songs)

            notifyDataSetChanged()
        } else {
            songs = sharedStorage.loadSongs()
            notifyDataSetChanged()
        }
    }

    fun setSongs(list: ArrayList<SongModel>) {
        songs = list
        holders.clear()

        notifyDataSetChanged()
    }
}
