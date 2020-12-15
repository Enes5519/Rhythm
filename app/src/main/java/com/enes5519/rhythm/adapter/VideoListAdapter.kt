package com.enes5519.rhythm.adapter

import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.enes5519.rhythm.R
import com.enes5519.rhythm.SearchResultsActivity
import com.enes5519.rhythm.model.DownloadResult
import com.enes5519.rhythm.model.Music
import com.enes5519.rhythm.model.YoutubeVideo
import com.enes5519.rhythm.provider.DatabaseHelper
import com.enes5519.rhythm.provider.downloadFile
import com.enes5519.vaveyla.utils.PermissionManager
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import java.io.File
import kotlinx.coroutines.flow.collect

class VideoListAdapter(private val activity: SearchResultsActivity, private val videos: ArrayList<YoutubeVideo>, private val databaseHelper: DatabaseHelper) : RecyclerView.Adapter<VideoListAdapter.ViewHolder>() {
    private val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    private val notificationChannelId = "download_music"

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val downloadView: View = itemView.findViewById(R.id.download)

        fun bind(video: YoutubeVideo){
            Glide.with(itemView).load(video.thumbnail).into(itemView.findViewById(R.id.thumbnail))
            itemView.findViewById<TextView>(R.id.title).apply { text = video.title }
            itemView.findViewById<TextView>(R.id.author).apply { text = video.author }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_list_card, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(videos[position])
        val isDownloaded = databaseHelper.musicExists(videos[position].id)
        holder.downloadView.findViewById<ImageView>(R.id.imageView).apply {
            setImageResource(if (isDownloaded) R.drawable.ic_done else R.drawable.ic_download)
            setOnClickListener {
                if(!isDownloaded) handleDownload(videos[position], position, holder.downloadView)
            }
        }
    }

    override fun getItemCount(): Int = videos.size

    fun setAll(newVideos: List<YoutubeVideo>){
        videos.clear()
        videos.addAll(newVideos)
        notifyDataSetChanged()
    }

    private fun handleDownload(video: YoutubeVideo, notificationId: Int, view: View){
        if(!PermissionManager.checkAndRequestPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            return
        }

        if(!directory.exists()){
            directory.mkdirs()
        }

        val builder = NotificationCompat.Builder(activity.applicationContext, notificationChannelId).apply{
            setContentTitle(video.title)
            setContentText("Link oluşturuluyor...")
            setSmallIcon(R.drawable.ic_download)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setOngoing(true)
        }

        view.findViewById<ImageView>(R.id.imageView).apply { visibility = View.GONE }
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar).apply { visibility = View.VISIBLE }

        CoroutineScope(Dispatchers.IO).launch {
            NotificationManagerCompat.from(activity).apply {
                withContext(Dispatchers.Main){
                    notify(notificationId, builder.build())
                }

                val client = HttpClient(Android)
                val downloadURL : String = client.get("http://192.168.1.16:5519/api/download?video_id=${video.id}")

                withContext(Dispatchers.Main){
                    builder.setContentText("Video İndiriliyor").setProgress(0,0,true)
                    notify(notificationId, builder.build())
                }

                client.downloadFile(File(directory, "${video.title}.mp3"), downloadURL).collect{
                    withContext(Dispatchers.Main) {
                        when (it) {
                            is DownloadResult.Success -> {
                                databaseHelper.insertMusic(Music(video.id, video.title))
                                progressBar.visibility = View.GONE
                                view.findViewById<ImageView>(R.id.imageView).apply {
                                    visibility = View.VISIBLE
                                    setImageResource(R.drawable.ic_done)
                                }

                                delay(500)
                                builder.setProgress(0, 0, false).setContentText("Müzik indirildi!").setOngoing(false).setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                notify(notificationId, builder.build())
                            }

                            is DownloadResult.Error -> {
                                builder.setProgress(0, 0, false).setContentText("Müzik indirilirken hata oluştu!").setOngoing(false).setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                notify(notificationId, builder.build())
                            }

                            is DownloadResult.Progress -> {
                                progressBar.progress = it.progress
                                builder.setProgress(100, it.progress, false)
                                notify(notificationId, builder.build())
                            }
                        }
                    }
                }
            }
        }
    }
}