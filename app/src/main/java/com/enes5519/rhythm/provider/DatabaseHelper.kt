package com.enes5519.rhythm.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.enes5519.rhythm.model.Music

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
    companion object {
        private const val DATABASE_NAME = "Rhythm"

        private const val MUSIC_TABLE_NAME = "musics"
        private const val MUSIC_VIDEO_ID = "video_id"
        private const val MUSIC_TITLE = "title"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE IF NOT EXISTS $MUSIC_TABLE_NAME ($MUSIC_VIDEO_ID VARCHAR(11), $MUSIC_TITLE TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    fun insertMusic(music: Music): Boolean {
        val contentValues = ContentValues()
        contentValues.put(MUSIC_VIDEO_ID, music.video_id)
        contentValues.put(MUSIC_TITLE, music.title)
        val result = writableDatabase.insert(MUSIC_TABLE_NAME, null, contentValues)

        return result != -1L
    }

    fun readMusics(): ArrayList<Music> {
        val musicList = arrayListOf<Music>()
        val result = readableDatabase.rawQuery("SELECT * FROM $MUSIC_TABLE_NAME", null)
        if (result.moveToFirst()) {
            do {
                musicList.add(Music(result.getString(result.getColumnIndex(MUSIC_VIDEO_ID)), result.getString(result.getColumnIndex(MUSIC_TITLE))))
            } while (result.moveToNext())
        }
        result.close()

        return musicList
    }

    fun musicExists(videoId: String): Boolean {
        val result = readableDatabase.rawQuery("SELECT $MUSIC_VIDEO_ID FROM $MUSIC_TABLE_NAME WHERE $MUSIC_VIDEO_ID = ?", arrayOf(videoId))
        val exists = result.count > 0
        result.close()
        return exists
    }

    fun deleteMusic(videoId: String) : Boolean{
        return writableDatabase.delete(MUSIC_TABLE_NAME, "$MUSIC_VIDEO_ID=?", arrayOf(videoId)) > 0
    }
}