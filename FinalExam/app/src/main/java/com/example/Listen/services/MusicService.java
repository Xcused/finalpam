package com.example.Listen.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.Listen.interfaces.PlayerInterface;
import com.example.Listen.loader.SongDataLab;
import com.example.Listen.models.SongModel;
import com.example.Listen.utils.MusicPreference;
import com.example.Listen.utils.NotificationHandler;

import java.util.List;

//mendeklarasikan variable
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, PlayerInterface,
        AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = MusicService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 12302;

    private MediaPlayer player;
    private SongModel currentSong;
    private int currentSongPosition;
    Callback callback;
    private final IBinder musicBind = new MusicBinder();
    private AudioManager audioManager;
    private boolean audioFocusState = false;
    PlayerThread mPlayerThread;

    private boolean firstTime = true;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();
        mPlayerThread = new PlayerThread();
        mPlayerThread.start();
        initMusicService();
        Log.i(TAG, "onCreate Service");
    }

    //button didalam play menu (previous,next,play,pause)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        switch (intent.getAction()) {

            case "action.prev":
                playPrev();
                mNotificationManager.notify(NOTIFICATION_ID, NotificationHandler.createNotification(this, currentSong, true));
                break;
            case "action.pause":
                pause();
                mNotificationManager.notify(NOTIFICATION_ID, NotificationHandler.createNotification(this, currentSong, false));
                break;
            case "action.play":
                mNotificationManager.notify(NOTIFICATION_ID, NotificationHandler.createNotification(this, currentSong, true));
                if (player != null) {
                    start();
                } else {
                    initMusicService();
                    start();
                }
                break;
            case "action.next":
                playNext();
                mNotificationManager.notify(NOTIFICATION_ID, NotificationHandler.createNotification(this, currentSong, true));
                break;
            case "action.stop":
                stop();
                stopForeground(true);
                stopSelf();
                break;
        }
        return START_STICKY;
    }

    //mengelola audio
    public void initMusicService() {
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        restoreSong();
        if (!audioFocusState) {
            Log.i(TAG, "Focus before is: " + audioFocusState);
            requestAudioFocusManager();
            Log.i(TAG, "Focus after is: " + audioFocusState);
        }
    }

    //menampilkan lagu yang telah terputar sebelumnya
    private void restoreSong() {
        long currentSongId = MusicPreference.get(this).getLastPlayedSongId();
        if (currentSongId != 0) {
            currentSong = SongDataLab.get(this).getSong(currentSongId);
        } else {
            currentSong = SongDataLab.get(this).getRandomSong();
        }
        long currentSongDuration = MusicPreference.get(this).getLastPlayedSongDuration();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (callback != null) {
            callback.onCompletion(currentSong);
        }
    }

    //mengatur audio lagu
    //jika user sedang menggunakan app lain, misalkan videocall audio bakal mati/meredup
    @Override
    public void onAudioFocusChange(int i) {
        switch (i) {
            // audio focus
            case AudioManager.AUDIOFOCUS_GAIN:
                if (player == null) initMusicService();
                else if (!player.isPlaying()) player.start();
                player.setVolume(1.0f, 1.0f);
                break;
            // audio focus berkurang/loss
            case AudioManager.AUDIOFOCUS_LOSS:
                try {
                    if (player != null) player.stop();
                    player.release();
                    player = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                //audio kehilangan focus untuk sementara waktu, menghentikan lagu
                if (player.isPlaying()) player.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //audio kehilangan focus untuk sementara waktu, tapi tetap bisa melanjutkan
                if (player.isPlaying()) player.setVolume(0.1f, 0.1f);
                break;
        }
    }

    //audio focus request
    private void requestAudioFocusManager() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioFocusState = true;
        } else {
            audioFocusState = false;
        }

    }

    //menghilangkan audio focus
    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy Music Service");
        MusicPreference.get(this).setCurrentSongStatus(currentSong.getId(), player.getCurrentPosition());
        player.stop();
        player.release();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }

    // Mengganti user interface
    @Override
    public void start() {
        if (firstTime) {
            play(currentSong);
            firstTime = false;
        } else {
            player.start();
        }
    }


    @Override
    public void play(long songId) {
        player.reset();
        SongModel playSong = SongDataLab.get(this).getSong(songId);
        play(playSong);
    }

    @Override
    public void play(SongModel song) {
        mPlayerThread.play(song);
    }

    @Override
    public void pause() {
        if (player != null) {
            player.pause();
            callback.onPause();
        }
    }

    @Override
    public void stop() {
        if (player != null) {
            player.stop();
        }
    }

    @Override
    public boolean isPlaying() {
        if (player != null) {
            return player.isPlaying();
        }
        return false;
    }

    @Override
    public int getCurrentStreamPosition() {
        if (player != null) {
            return player.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (player != null) {
            return player.getDuration();
        } else {
            return 0;
        }
    }

    @Override
    public void seekTo(int position) {
        player.seekTo(position);
    }


    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setSong(int songIndex) {
        currentSongPosition = songIndex;
    }

    public String getCurrentSongName() {
        return currentSong.getTitle();
    }

    public SongModel getCurrentSong() {
        return currentSong;
    }

    //play next dan previous
    public void playNext() {
        play(SongDataLab.get(this).getNextSong(currentSong));
    }

    public void playPrev() {

        play(SongDataLab.get(this).getPreviousSong(currentSong));

    }



    //memasukkan lagu kedalam list
    public List<SongModel> getSongs() {
        return SongDataLab.get(this).getSongs();
    }

    //mengubah app menjadi berjalan di background
    public void toForeground() {
        startForeground(NOTIFICATION_ID, NotificationHandler.createNotification(this, currentSong, true));
        Log.d(TAG, "toForeground() called");
    }

    public void toBackground() {
        stopForeground(true);
    }


    public class PlayerThread extends Thread {
        private Handler mHandler;

        @Override
        public void run() {
            super.run();
            Looper.prepare();
            mHandler = new Handler();
            Looper.loop();
        }

        public void play(final SongModel song) {
            currentSong = song;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (player != null) {
                        player.reset();
                        try {
                            player.setDataSource(song.getData());
                            Log.i(TAG, song.getBookmark() + "");
                            player.prepareAsync();
                            MusicService.this.callback.onTrackChange(song);

                        } catch (Exception e) {
                            Log.e(TAG, "Error playing from data source", e);
                        }
                    }
                }
            });
        }

        public void prepareNext() {

        }

        public void exit() {
            mHandler.getLooper().quit();
        }
    }


}
