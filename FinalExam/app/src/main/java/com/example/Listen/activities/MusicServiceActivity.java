package com.example.Listen.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.Listen.services.MusicService;

//Mendeklarasikan Variable
public abstract class MusicServiceActivity extends PermitActivity {
    public static final String TAG = MusicServiceActivity.class.getSimpleName();
    public static final String PLAY_STATE = "play_state";
    public static final String SONG_ID = "song_id";
    public static final String DURATION = "duration";
    ServiceConnection serviceConnection;
    boolean isPlaying = false;
    public static MusicService musicService;
    boolean boundService = false;
    Intent playIntent;
    MusicService.MusicBinder binder;

    //Agar Music dapat berjalan di background
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate of "+TAG);
        if (savedInstanceState != null) {
            isPlaying = savedInstanceState.getBoolean(PLAY_STATE);
        }
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                binder = (MusicService.MusicBinder) iBinder;
                musicService = binder.getService();
                MusicServiceActivity.this.onServiceConnected();

                if(isPlaying) {
                    musicService.play(savedInstanceState.getLong(SONG_ID));
                    musicService.seekTo(savedInstanceState.getInt(DURATION));
                    musicService.toBackground();
                }
                boundService = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };
    }


    abstract public void onServiceConnected();

    //Memulai callback
    @Override
    protected void onStart() {
        super.onStart();
        playIntent = new Intent(this, MusicService.class);
        playIntent.setAction("");
        bindService(playIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(playIntent);
    }

    //Memberhentikkan callback
    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStopCalled() of "+TAG);
        runServiceIfSongIsPlaying();
    }

    void runServiceIfSongIsPlaying() {
        Log.d(TAG,"runServiceIfSUngIsPlaying");
        if (boundService) {
            Log.d(TAG,"boundService");
            if (musicService.isPlaying() || isChangingConfigurations()) {
                Log.d(TAG,"toForeground");
                musicService.toForeground();
            } else {
                    stopService(playIntent);
            }
            unbindService(serviceConnection);
            binder = null;
            boundService = false;
        }
    }

    //Menutup Aktivitas/Finish
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy of "+TAG);
        runServiceIfSongIsPlaying();
    }

    //Menyimpan Informasi sebelumnya (apabila aplikasi ditutup)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PLAY_STATE, musicService.isPlaying());
        outState.putLong(SONG_ID,musicService.getCurrentSong().getId());
        outState.putInt(DURATION,musicService.getCurrentStreamPosition());
    }
}
