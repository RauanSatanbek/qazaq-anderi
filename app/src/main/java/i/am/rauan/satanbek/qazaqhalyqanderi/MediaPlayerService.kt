package i.am.rauan.satanbek.qazaqhalyqanderi

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.media.MediaPlayer
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.media.AudioManager
import android.os.Binder
import android.os.RemoteException
import android.util.Log
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.widget.Toast
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.MediaMetadataCompat
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.support.v4.app.NotificationCompat
import android.graphics.BitmapFactory
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import i.am.rauan.satanbek.qazaqhalyqanderi.db.SongModel
import java.lang.Exception


class MediaPlayerService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener,
    MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {

    private var LOG_TAG: String = "main"
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager

    // Binder given to clients
    private val iBinder = LocalBinder()

    // Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null
    private var sharedStorage: Storage? = null
    private var songs: ArrayList<SongModel> = ArrayList()

    val ACTION_PLAY = "com.valdioveliu.valdio.audioplayer.ACTION_PLAY"
    val ACTION_PAUSE = "com.valdioveliu.valdio.audioplayer.ACTION_PAUSE"
    val ACTION_PREVIOUS = "com.valdioveliu.valdio.audioplayer.ACTION_PREVIOUS"
    val ACTION_NEXT = "com.valdioveliu.valdio.audioplayer.ACTION_NEXT"
    val ACTION_STOP = "com.valdioveliu.valdio.audioplayer.ACTION_STOP"

    //MediaSession
    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaSession: MediaSessionCompat? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null

    //AudioPlayer notification ID
    val NOTIFICATION_ID = 101

    // Becoming noisy
    private var becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //Pause media on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia()
            buildNotification(PlaybackStatus.PAUSED)
        }
    }

    private fun registerBecomingNoisyReceiver() {
        var intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(becomingNoisyReceiver, intentFilter)
    }

    // Play new song
    private var playNewSongReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            stopMedia()
            mediaPlayer?.reset()
            initMediaPlayer()
            if (sharedStorage?.getCurrentSong()!!.path != "") playMedia()
        }
    }

    private fun registerPlayNewSongReceiver() {
        var intentFilter = IntentFilter(sharedStorage!!.playNewSongReceiverAction)
        registerReceiver(playNewSongReceiver, intentFilter)
    }


    override fun onCreate() {
        super.onCreate()

        // init storage
        sharedStorage = Storage(this)

        // load songs
        songs = sharedStorage?.loadSongs()!!

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener()

        // ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver()

        // Play new sog receiver
        registerPlayNewSongReceiver()
    }

    override fun onBind(intent: Intent): IBinder {
        return iBinder
    }

    @SuppressLint("ServiceCast")
    @Throws(RemoteException::class)
    private fun initMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mediaSessionManager != null) return  //mediaSessionManager exists

                mediaSessionManager = this.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

            // Create a new MediaSession
            mediaSession = MediaSessionCompat(applicationContext, "AudioPlayer")
            //Get MediaSessions transport controls
            transportControls = mediaSession!!.controller.transportControls
            //set MediaSession -> ready to receive media commands
            mediaSession!!.isActive = true
            //indicate that the MediaSession handles transport control commands
            // through its MediaSessionCompat.Callback.
            mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

            //Set mediaSession's MetaData
            updateMetaData()

            // Attach Callback to receive MediaSession updates
            mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
                // Implement callbacks
                override fun onPlay() {
                    super.onPlay()
                    var intent = Intent(sharedStorage?.updateUIReceiverAction)
                    intent.putExtra(sharedStorage?.updateUIPlayUpdate, true)
                    sendBroadcast(intent)

                    resumeMedia()
                    buildNotification(PlaybackStatus.PLAYING)
                }

                override fun onPause() {
                    super.onPause()

                    var intent = Intent(sharedStorage?.updateUIReceiverAction)
                    intent.putExtra(sharedStorage?.updateUIPauseUpdate, true)
                    sendBroadcast(intent)
                    pauseMedia()

                    buildNotification(PlaybackStatus.PAUSED)
                }

                override fun onSkipToNext() {
                    super.onSkipToNext()
                    skipToNext()
                    updateMetaData()
                    buildNotification(PlaybackStatus.PLAYING)
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    skipToPrevious()
                    updateMetaData()
                    buildNotification(PlaybackStatus.PLAYING)
                }

                override fun onStop() {
                    super.onStop()
                    removeNotification()
                    //Stop the service
                    stopSelf()
                }

                override fun onSeekTo(position: Long) {
                    super.onSeekTo(position)
                }
            })
        }
    }


    private fun updateMetaData() {

        // Update the current metadata
        mediaSession!!.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, sharedStorage?.getCurrentSong()!!.playlist)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, sharedStorage?.getCurrentSong()!!.name)
            .build()
    )
}
    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
        // Invoked indicating buffering status of
        // a media resource being streamed over the network.
    }

     override fun onCompletion(mp: MediaPlayer) {
        // Invoked when playback of a media source has completed.
         stopMedia()

         // Stop the service
         stopSelf()
    }

    // Handle errors
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        // Invoked when there has been an error during an asynchronous operation.
        when(what) {
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> {
            }
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
            }
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
            }
        }

        return false
    }

    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        // Invoked to communicate some info.
        return false
    }

    override fun onPrepared(mp: MediaPlayer) {
        // Invoked when the media source is ready for playback.
//        playMedia()
    }

    override fun onSeekComplete(mp: MediaPlayer) {
        // Invoked indicating the completion of a seek operation.
    }

    override fun onAudioFocusChange(focusChange: Int) {
        // Invoked when the audio focus of the system is updated.
        when(focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // resume playback
                if (!sharedStorage!!.getPause()) {
                    if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                        playMedia()

                        val local = Intent(sharedStorage?.updateUIReceiverAction)
                        local.putExtra("showPauseButton", true)
                        sendBroadcast(local)

                        mediaPlayer?.setVolume(1.0f, 1.0f)
                    }
                }

            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    val local = Intent(sharedStorage?.updateUIReceiverAction)
                    local.putExtra("showPauseButton", true)
                    sendBroadcast(local)

                    sharedStorage?.setPause(true)
                    stopMedia()
                }

                mediaPlayer?.release()
                mediaPlayer = null
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lost ficus for a short time. but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer != null && mediaPlayer!!.isPlaying) mediaPlayer?.setVolume(0.1f, 0.1f)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost ficus for a short time. but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    mediaPlayer?.pause()

                    val local = Intent(sharedStorage?.updateUIReceiverAction)
                    local.putExtra("showPlayButton", true)
                    sendBroadcast(local)
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        var result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Focus gained
            return true
        }

        // Could not gain focus
        return false
    }

    private fun removeAudioFocus(): Boolean {
        try {
            if (audioManager == null) {
                return true
            }
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this)
        } catch (e: Exception) {
            return true
        }
    }

    inner class LocalBinder : Binder(), IBinder {
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }

    private fun initMediaPlayer() {
        try {
            var song = sharedStorage?.getCurrentSong()

            if (song!!.path != "") {
                mediaPlayer = MediaPlayer.create(this, Uri.parse(song.path))
            } else {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                mediaPlayer?.setDataSource(song.url)

                mediaPlayer?.setOnPreparedListener(MediaPlayer.OnPreparedListener { mp ->
                    mediaPlayer?.seekTo(sharedStorage!!.getResumePosition())

                    playMedia()
                })
                mediaPlayer?.prepareAsync()
            }

            // Set up MediaPlayer event listener
            mediaPlayer?.setOnCompletionListener(this)
            mediaPlayer?.setOnErrorListener(this)
            mediaPlayer?.setOnBufferingUpdateListener(this)
            mediaPlayer?.setOnSeekCompleteListener(this)
            mediaPlayer?.setOnInfoListener(this)

            mediaPlayer?.setOnCompletionListener {
                when (sharedStorage?.getRefreshMode()) {
                    0 -> {
                        skipToNext()
                    }
                    1 -> {
                        playMedia()
                    }
                }

                // Update Seek bar progress
                val local = Intent(sharedStorage?.updateUIReceiverAction)
                local.putExtra("seek_bar_progress", true)
                local.putExtra("seek_bar_progress_value", 0)
                local.putExtra("tv_duration", "0.00")
                this.sendBroadcast(local)
            }

            val local = Intent(sharedStorage?.updateUIReceiverAction)
            local.putExtra("tv_duration", "0.00")
            this.sendBroadcast(local)

        } catch (e: Exception) {
            Toast.makeText(this, "Проверьте ваше подключение к интернету", Toast.LENGTH_LONG).show()
        }
    }


    fun playMedia() {

        if (mediaPlayer == null) restoreMedia()

        if (!mediaPlayer!!.isPlaying) {

            mediaPlayer?.start()
            sharedStorage?.setPause(false)

            sendDuration()

            var intent = Intent(sharedStorage?.updateUIReceiverAction)
            intent.putExtra(sharedStorage?.updateUIPlayUpdate, true)
            sendBroadcast(intent)
        }
    }

    private fun restoreMedia() {
        if (mediaPlayer != null) mediaPlayer!!.release()

        if (!requestAudioFocus()) {
            // Could not gain focus
            stopSelf()
        }

        initMediaPlayer()
    }

    fun stopMedia() {
        if (mediaPlayer == null) restoreMedia()

        if (mediaPlayer == null) return
        if (mediaPlayer!!.isPlaying) {
            sharedStorage!!.setResumePosition(getCurrentSeconds())

            mediaPlayer?.stop()
            sharedStorage?.setPause(true)
        }
    }

    fun pauseMedia() {
        if (mediaPlayer == null) restoreMedia()

        if (mediaPlayer!!.isPlaying) {
            mediaPlayer?.pause()
            sharedStorage?.setResumePosition(mediaPlayer!!.currentPosition)
            sharedStorage?.setPause(true)

            val local = Intent(sharedStorage?.updateUIReceiverAction)
            local.putExtra(sharedStorage?.updateUIPause, true)
            sendBroadcast(local)
        }
    }

    fun resumeMedia() {
        if (mediaPlayer == null) {
            restoreMedia()

            if (!mediaPlayer!!.isPlaying && sharedStorage?.getCurrentSong()!!.path != "") {
                mediaPlayer?.seekTo(sharedStorage!!.getResumePosition())
                mediaPlayer?.start()
                sharedStorage?.setPause(false)

                sendDuration()
            }

        } else {
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer?.seekTo(sharedStorage!!.getResumePosition())
                mediaPlayer?.start()
                sharedStorage?.setPause(false)

                sendDuration()
            }
        }
    }

    fun skipToPrevious() {
        stopMedia()

        songs = if (sharedStorage?.getShuffleMode() == 1) {
            sharedStorage?.loadShuffleSongs()!!
        } else {
            sharedStorage?.loadSongs()!!
        }

        var currentSongIndex = 0

        for (i in songs) {
            if (i.id == sharedStorage?.getCurrentSong()!!.id) {
                currentSongIndex = songs.indexOf(i)
            }
        }

        if (currentSongIndex == 0) {
            sharedStorage?.setCurrentSong(songs[songs.size - 1])
        } else if (currentSongIndex != 0 && currentSongIndex < songs.size) {
            sharedStorage?.setCurrentSong(songs[currentSongIndex - 1])
        }

        sharedStorage!!.setResumePosition(0)
        //reset mediaPlayer
        mediaPlayer?.reset()
        initMediaPlayer()

        if (sharedStorage?.getCurrentSong()!!.path != "") {
            playMedia()
        }
    }

    fun skipToNext() {
        stopMedia()

        var currentSongIndex = 0
        songs = if (sharedStorage?.getShuffleMode() == 1) {
            sharedStorage?.loadShuffleSongs()!!
        } else {
            sharedStorage?.loadSongs()!!
        }

        for (i in songs) {
            if (i.key == sharedStorage?.getCurrentSong()!!.key) {
                currentSongIndex = songs.indexOf(i)
            }
        }

        if (currentSongIndex == songs!!.size - 1) {
            sharedStorage?.setCurrentSong(songs[0])
        } else {
            sharedStorage?.setCurrentSong(songs[currentSongIndex + 1])
        }

        sharedStorage!!.setResumePosition(0)
        //reset mediaPlayer
        mediaPlayer?.reset()
        initMediaPlayer()

        if (sharedStorage?.getCurrentSong()!!.path != "") {
            playMedia()
        }
    }

    private fun sendDuration() {
        val local = Intent(sharedStorage?.updateUIReceiverAction)
        local.putExtra("tv_duration", mediaPlayer?.durationInMinutes)
        local.putExtra("media_playing", true)
        local.putExtra("seek_bar_progress", true)
        local.putExtra("seek_bar_progress_value", getCurrentSeconds())
        sendBroadcast(local)
    }

    fun seekTo(to: Int) {
        mediaPlayer?.seekTo(to)
    }

    fun getSeconds(): Int {
        if (mediaPlayer != null)
            return mediaPlayer!!.seconds

        return 0
    }
    fun getCurrentSeconds(): Int {
        if (mediaPlayer != null)
            return mediaPlayer!!.currentSeconds

        return 0
    }
    fun getCurrentDurationInMinutes(): String {
        if (mediaPlayer != null)
            return mediaPlayer!!.currentDurationInMinutes

        return "0.00"
    }

    fun getDurationInMinutes(): String {
        if (mediaPlayer != null)
            return mediaPlayer!!.durationInMinutes

        return "0.00"
    }

    fun getAudioSessionId(): Int {
        if (mediaPlayer != null)
            return mediaPlayer!!.audioSessionId

        return -1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Request Song focus
        if (!requestAudioFocus()) {
            // Could not gain focus
            stopSelf()
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession()
            } catch (e: RemoteException) {
                e.printStackTrace()
                stopSelf()
            }
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent)

        return super.onStartCommand(intent, flags, startId)
    }

    fun buildNotification(status: PlaybackStatus) {

        var notificationAction = R.drawable.ic_pause
        var playPauseAction: PendingIntent? = null

        when(status) {
            PlaybackStatus.PLAYING -> {
                notificationAction = R.drawable.ic_pause

                playPauseAction = playbackAction(1)
            }

            PlaybackStatus.PAUSED -> {
                notificationAction = R.drawable.ic_play

                playPauseAction = playbackAction(0)
            }
        }
        val largeIcon = BitmapFactory.decodeResource(
            resources,
            R.drawable.gani)

        var notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this)
            .setShowWhen(false)
            .setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession!!.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .setColor(resources.getColor(R.color.blue))
            .setLargeIcon(largeIcon)
            .setSmallIcon(R.drawable.ic_logo)

            .setContentText(resources.getString(R.string.app_name))
            .setContentTitle(sharedStorage?.getCurrentSong()!!.name)
            .setContentInfo("")

            .addAction(R.drawable.ic_prev, "previous", playbackAction(3))
            .addAction(notificationAction, "pause", playPauseAction)
            .addAction(R.drawable.ic_next, "next", playbackAction(2))


        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun removeNotification() {
        var notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun playbackAction(i: Int): PendingIntent? {
        var playbackAction = Intent(this, MediaPlayerService::class.java)

        when(i) {
            0 -> {
                playbackAction.action = ACTION_PLAY
                return PendingIntent.getService(this, i, playbackAction, 0)
            }
            1 -> {
                playbackAction.action = ACTION_PAUSE
                return PendingIntent.getService(this, i, playbackAction, 0)
            }
            2 -> {
                playbackAction.action = ACTION_NEXT
                return PendingIntent.getService(this, i, playbackAction, 0)
            }
            3 -> {
                playbackAction.action = ACTION_PREVIOUS
                return PendingIntent.getService(this, i, playbackAction, 0)
            }
        }

        playbackAction.action = ACTION_PLAY
        return PendingIntent.getService(this, i, playbackAction, 0)
    }

    private fun handleIncomingActions(playbackAction: Intent?) {
        if (playbackAction == null  || playbackAction.action == null) return

        var actionString = playbackAction.action

        if (actionString!! == ACTION_PLAY) {
            transportControls!!.play()
        } else if (actionString == ACTION_PAUSE) {
            transportControls!!.pause()
        } else if (actionString == ACTION_NEXT) {
            transportControls!!.skipToNext()
        } else if (actionString == ACTION_PREVIOUS) {
            transportControls!!.skipToPrevious()
        } else if (actionString == ACTION_STOP) {
            transportControls!!.stop()
        }
    }
    // Handle incoming calls
    private fun callStateListener() {
        // get the telephony manager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // start listening for PhoneState changes

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    // if at least one call or phone ringing
                    // pause the MediaPlayer
                    TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> {
                        if (mediaPlayer != null) {
                            pauseMedia()

                            ongoingCall = true
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        // Phone idle, start playing
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false
                                if (!sharedStorage?.getPause()!!) {
                                    resumeMedia()
                                }
                            }
                        }
                    }
                }
                super.onCallStateChanged(state, phoneNumber)
            }
        }

        // Register the listener with the telephone manager
        // Listen for changes to the device call state.
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mediaPlayer != null) {
            stopMedia()
            mediaPlayer?.release()
        }

        removeNotification()
        removeAudioFocus()

        if (phoneStateListener != null) {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        }

        unregisterReceiver(becomingNoisyReceiver)
        unregisterReceiver(playNewSongReceiver)
    }
}


val MediaPlayer.seconds:Int
    get() {
        return this.duration / 1000
    }

val MediaPlayer.currentSeconds:Int
    get() {
        return this.currentPosition / 1000
    }

val MediaPlayer.durationInMinutes:String
    get() {
        var seconds = this.seconds % 60
        return "${this.seconds / 60}.${if (seconds < 10) "0$seconds" else seconds }"
    }


val MediaPlayer.currentDurationInMinutes:String
    get() {
        var seconds = this.currentSeconds % 60
        return "${this.currentSeconds / 60}.${if (seconds < 10) "0$seconds" else seconds }"
    }
