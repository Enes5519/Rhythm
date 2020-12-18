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
import com.enes5519.rhythm.SearchResultFragment
import com.enes5519.rhythm.model.DownloadResult
import com.enes5519.rhythm.model.Music
import com.enes5519.rhythm.model.YoutubeVideo
import com.enes5519.rhythm.provider.DatabaseHelper
import com.enes5519.rhythm.provider.downloadFile
import com.enes5519.rhythm.utils.PermissionManager
import com.enes5519.rhythm.utils.WebAPI
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.io.File

class SearchResultAdapter(
    private val fragment: SearchResultFragment,
    private val databaseHelper: DatabaseHelper
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {
    private val values: ArrayList<YoutubeVideo> = arrayListOf()
    private val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    private val notificationChannelId = "download_music"

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val downloadView: View = itemView.findViewById(R.id.download)

        fun bind(video: YoutubeVideo){
            Glide.with(itemView).load(video.thumbnail).into(itemView.findViewById(R.id.thumbnail))
            itemView.findViewById<TextView>(R.id.title).apply { text = video.title }
            itemView.findViewById<TextView>(R.id.author).apply { text = video.author }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(values[position])
        val isDownloaded = databaseHelper.musicExists(values[position].id)
        holder.downloadView.findViewById<ImageView>(R.id.imageView).apply {
            setImageResource(if (isDownloaded) R.drawable.ic_done else R.drawable.ic_download)
            setOnClickListener {
                if(!isDownloaded) handleDownload(values[position], position, holder.downloadView)
            }
        }
    }

    override fun getItemCount(): Int = values.size

    fun setAll(newVideos: List<YoutubeVideo>){
        values.clear()
        values.addAll(newVideos)
        notifyDataSetChanged()
    }

    private fun handleDownload(video: YoutubeVideo, notificationId: Int, view: View){
        if(!PermissionManager.checkAndRequestPermission(fragment.requireActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            return
        }

        if(!directory.exists()){
            directory.mkdirs()
        }

        val builder = NotificationCompat.Builder(view.context.applicationContext, notificationChannelId).apply{
            setContentTitle(video.title)
            setContentText(view.context.getString(R.string.creating_download_link))
            setSmallIcon(R.drawable.ic_download)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setOngoing(true)
        }

        view.findViewById<ImageView>(R.id.imageView).apply { visibility = View.GONE }
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar).apply { visibility = View.VISIBLE }

        CoroutineScope(Dispatchers.IO).launch {
            NotificationManagerCompat.from(fragment.requireContext()).apply {
                withContext(Dispatchers.Main){
                    notify(notificationId, builder.build())
                }

                val client = HttpClient(Android)
                val downloadURL : String = client.get(WebAPI.createDownloadURL(video.id))

                withContext(Dispatchers.Main){
                    builder.setContentText(view.context.getString(R.string.downloading_music)).setProgress(0,0,true)
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

                                delay(1000) // hack
                                builder.setProgress(0, 0, false).setContentText(view.context.getString(R.string.music_downloaded)).setOngoing(false).setCategory(
                                    NotificationCompat.CATEGORY_MESSAGE)
                                notify(notificationId, builder.build())
                            }

                            is DownloadResult.Error -> {
                                builder.setProgress(0, 0, false).setContentText(view.context.getString(R.string.error_download)).setOngoing(false).setCategory(
                                    NotificationCompat.CATEGORY_MESSAGE)
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