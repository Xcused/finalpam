package com.example.Listen.loader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.MediaStore;

import com.example.Listen.database.SongCursorWrapper;
import com.example.Listen.database.SongDbHelper;
import com.example.Listen.models.SongModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


//Mendeklarasikan Variable
public class SongDataLab {

    private static SongDataLab sSongDataLab;
    private Context mContext;
    private SQLiteDatabase mDatabase;
    private List<SongModel> songs;

    private SongDataLab(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new SongDbHelper(mContext).getWritableDatabase();
        songs = querySongs();
    }


    //mendapatkan data lagu
    public static SongDataLab get(Context context) {
        if (sSongDataLab == null) {
            sSongDataLab = new SongDataLab(context);
        }
        return sSongDataLab;
    }

    public SongModel getSong(long id) {
        SongCursorWrapper cursorWrapper = querySong("_id=" + id, null);
        try {
            if (cursorWrapper.getCount() != 0) {
                cursorWrapper.moveToFirst();
                return cursorWrapper.getSong();
            }
            return SongModel.EMPTY();
        } finally {
            cursorWrapper.close();
        }
    }


    public SongModel getRandomSong() {
        Random r = new Random();
        return songs.get(r.nextInt(songs.size() - 1));
    }

    public SongModel getNextSong(SongModel currentSong) {
        try {
            return songs.get(songs.indexOf(currentSong) + 1);
        } catch (Exception e) {
            return getRandomSong();
        }
    }


    public SongModel getPreviousSong(SongModel currentSong) {
        try {
            return songs.get(songs.indexOf(currentSong) - 1);
        } catch (Exception e) {
            return getRandomSong();
        }
    }

    public List<SongModel> getSongs() {
        return songs;
    }

    //menampilkan songmodel pada tampilan play song
    public List<SongModel> querySongs() {
        List<SongModel> songs = new ArrayList();
        SongCursorWrapper cursor = querySong(null, null);
        try {
            cursor.moveToFirst();
            do {
                SongModel song = cursor.getSong();
                song = cursor.getSong();
                song.setAlbumArt(getAlbumUri(song.getAlbumId()).toString());
                songs.add(song);
            } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }
        return songs;
    }

    //Mengurutkan data music
    private SongCursorWrapper querySong(String whereClause, String[] whereArgs) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection;
        if (whereClause != null) {
            selection = MediaStore.Audio.Media.IS_MUSIC + "!=0 AND " + whereClause;
        } else {
            selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
        }
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = mContext.getContentResolver().query(
                uri,
                null,
                selection,
                whereArgs,
                sortOrder);
        return new SongCursorWrapper(cursor);
    }

    //mendapatkan album/cover
    private Uri getAlbumUri(int albumId) {
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        return ContentUris.withAppendedId(albumArtUri, albumId);
    }
}
