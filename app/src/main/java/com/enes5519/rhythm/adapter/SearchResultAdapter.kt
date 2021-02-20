package com.enes5519.rhythm.adapter

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.collection.arrayMapOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.enes5519.rhythm.R
import com.enes5519.rhythm.model.DownloadURL
import com.enes5519.rhythm.model.Music
import com.enes5519.rhythm.model.YoutubeVideo
import com.enes5519.rhythm.provider.DatabaseHelper
import com.enes5519.rhythm.provider.DownloadResult
import com.enes5519.rhythm.provider.downloadFile
import com.enes5519.rhythm.utils.PermissionManager
import com.enes5519.rhythm.utils.WebAPI
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.request.get
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import java.io.File

class SearchResultAdapter(
    private val context: Context,
    private val databaseHelper: DatabaseHelper
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {
    private val values: ArrayList<YoutubeVideo> = arrayListOf()
    private val directory =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    private val notificationChannelId = "download_music"

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(video: YoutubeVideo, downloadStatus: DownloadResult.Progress?) {
            Glide.with(itemView).load(video.thumbnail).into(itemView.findViewById(R.id.thumbnail))
            itemView.findViewById<TextView>(R.id.title).apply { text = video.title }
            itemView.findViewById<TextView>(R.id.author).apply { text = video.author }

            val downloadView: View = itemView.findViewById(R.id.download)
            if (downloadStatus !== null) {
                downloadView.findViewById<ImageView>(R.id.imageView).visibility = View.GONE
                downloadView.findViewById<ProgressBar>(R.id.progressBar).apply {
                    visibility = View.VISIBLE
                    progress = downloadStatus.progress
                }
            } else {
                val isDownloaded = databaseHelper.musicExists(video.id)
                downloadView.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                downloadView.findViewById<ImageView>(R.id.imageView).apply {
                    visibility = View.VISIBLE
                    setImageResource(if (isDownloaded) R.drawable.ic_done else R.drawable.ic_download)
                    setOnClickListener {
                        if (!isDownloaded) handleDownload(video)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        values[position].let { holder.bind(it, downloadStatus[it.id]) }
    }

    override fun getItemCount(): Int = values.size

    fun setAll(newVideos: List<YoutubeVideo>) {
        values.clear()
        values.addAll(newVideos)
        notifyDataSetChanged()
    }

    private fun handleDownload(video: YoutubeVideo) {
        if (!PermissionManager.checkPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            return
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val builder =
            NotificationCompat.Builder(context.applicationContext, notificationChannelId).apply {
                setContentTitle(video.title)
                setContentText(context.getString(R.string.creating_download_link))
                setSmallIcon(R.drawable.ic_download)
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                setOngoing(true)
            }

        downloadStatus[video.id] = DownloadResult.Progress(0)
        values.indexOfFirst { it.id == video.id }.let { if (it != -1) notifyItemChanged(it) }

        val notificationId = notificationIdCounter++
        CoroutineScope(Dispatchers.IO).launch {
            NotificationManagerCompat.from(context).apply {
                withContext(Dispatchers.Main) {
                    notify(notificationId, builder.build())
                }

                val client = HttpClient(Android) {
                    install(JsonFeature) {
                        serializer = GsonSerializer()
                    }
                }
                val downloadURL: DownloadURL = client.get(WebAPI.createDownloadURL(video.id))
                if (downloadURL.status != 200) {
                    builder.setProgress(0, 0, false)
                        .setContentText(context.getString(R.string.error_download))
                        .setOngoing(false).setCategory(
                            NotificationCompat.CATEGORY_MESSAGE
                        )
                    notify(notificationId, builder.build())
                } else {
                    withContext(Dispatchers.Main) {
                        builder.setContentText(context.getString(R.string.downloading_music))
                            .setProgress(0, 0, true)
                        notify(notificationId, builder.build())
                    }

                    val fileName = video.title + "." + downloadURL.extension
                    val file = File(directory, fileName)
                    client.downloadFile(file, downloadURL.url).collect {
                        withContext(Dispatchers.Main) {
                            when (it) {
                                is DownloadResult.Success -> {
                                    downloadStatus.remove(video.id)
                                    databaseHelper.insertMusic(Music(video.id, fileName))
                                    values.indexOfFirst { it.id == video.id }
                                        .let { if (it != -1) notifyItemChanged(it) }

                                    MediaScannerConnection.scanFile(
                                        context,
                                        arrayOf(file.absolutePath),
                                        null,
                                        null
                                    )

                                    delay(1000) // hack
                                    builder.setProgress(0, 0, false)
                                        .setContentText(context.getString(R.string.music_downloaded))
                                        .setOngoing(false).setCategory(
                                            NotificationCompat.CATEGORY_MESSAGE
                                        )
                                    notify(notificationId, builder.build())
                                }

                                is DownloadResult.Error -> {
                                    builder.setProgress(0, 0, false)
                                        .setContentText(context.getString(R.string.error_download))
                                        .setOngoing(false).setCategory(
                                            NotificationCompat.CATEGORY_MESSAGE
                                        )
                                    notify(notificationId, builder.build())
                                }

                                is DownloadResult.Progress -> {
                                    downloadStatus[video.id] = it
                                    values.indexOfFirst { it.id == video.id }
                                        .let { if (it != -1) notifyItemChanged(it) }
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

    companion object {
        private val downloadStatus = arrayMapOf<String, DownloadResult.Progress>()
        private var notificationIdCounter = 0
    }
}