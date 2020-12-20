package com.enes5519.rhythm.adapter

import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.enes5519.rhythm.MainActivity
import com.enes5519.rhythm.R
import com.enes5519.rhythm.model.CurrentSong
import com.enes5519.rhythm.model.Music
import com.enes5519.rhythm.provider.DatabaseHelper
import com.enes5519.rhythm.utils.PermissionManager
import java.io.File
import java.lang.Exception

class MusicListAdapter(private val activity: MainActivity, private val list: ArrayList<Music>, private val db: DatabaseHelper) : RecyclerView.Adapter<MusicListAdapter.ViewHolder>() {
    private val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    private val mediaPlayer = MediaPlayer()
    private var currentSong : CurrentSong? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playView = itemView.findViewById<ImageView>(R.id.play)!!
        val deleteView = itemView.findViewById<ImageView>(R.id.delete)!!

        fun bind(music: Music) {
            itemView.findViewById<TextView>(R.id.title).apply { text = music.title }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.music_list_card, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val music = list[position]
        holder.bind(music)
        holder.deleteView.setOnClickListener {
            AlertDialog.Builder(it.context).apply {
                setTitle(it.context.getString(R.string.delete_file))
                setMessage(it.context.getString(R.string.delete_file_question))
                setPositiveButton(it.context.getString(R.string.yes)){ _, _ ->
                    if (PermissionManager.checkAndRequestPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        if (db.deleteMusic(list[position].video_id)) {
                            list.removeAt(position)
                            notifyItemRemoved(position)
                            File(directory, music.title).let { file -> if(file.exists()) file.delete() }
                        }
                    }
                }
                setNegativeButton(it.context.getString(R.string.no), null)
                show()
            }
        }
        val filePath = directory.path + File.separator + music.title
        holder.playView.setImageResource(if(currentSong?.path == filePath) R.drawable.ic_stop else R.drawable.ic_play)
        holder.playView.setOnClickListener {
            if (PermissionManager.checkAndRequestPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                if(mediaPlayer.isPlaying){
                    mediaPlayer.stop()
                    mediaPlayer.reset()
                    if (currentSong?.path == filePath){
                        holder.playView.setImageResource(R.drawable.ic_play)
                        currentSong = null
                        return@setOnClickListener
                    }else{
                        currentSong?.position?.let { it1 -> notifyItemChanged(it1) }
                        currentSong = null
                    }
                }

                try{
                    mediaPlayer.setDataSource(filePath)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                    holder.playView.setImageResource(R.drawable.ic_stop)
                    currentSong = CurrentSong(filePath, position)
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun setAll(newList: ArrayList<Music>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}