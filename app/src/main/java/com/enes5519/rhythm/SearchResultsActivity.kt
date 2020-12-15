package com.enes5519.rhythm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.enes5519.rhythm.adapter.VideoListAdapter
import com.enes5519.rhythm.model.YoutubeVideo
import com.enes5519.rhythm.provider.DatabaseHelper
import com.enes5519.vaveyla.utils.PermissionManager
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class SearchResultsActivity : AppCompatActivity() {
    private val notificationChannelId = "download_music"
    private lateinit var searchKeyword: String
    private lateinit var notificationManager: NotificationManager

    private val databaseHelper : DatabaseHelper by lazy {
        DatabaseHelper(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_results)

        searchKeyword = intent.getStringExtra("search")!!

        initNotification()
        initViews()
        loadList()
    }

    private fun initNotification() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    notificationChannelId,
                    "Müzik İndir",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    private fun initViews(){
        findViewById<TextView>(R.id.search_text).apply {
            text = searchKeyword
            setOnClickListener { openSearch(searchKeyword) }
        }
        findViewById<ImageView>(R.id.clear_button).apply { setOnClickListener { openSearch("") } }
        findViewById<ImageView>(R.id.back_button).apply { setOnClickListener { onBackPressed() } }
    }

    private fun loadList(){
        val searchResults = findViewById<RecyclerView>(R.id.searchResults)
        searchResults.layoutManager = LinearLayoutManager(this)
        searchResults.adapter = VideoListAdapter(this, arrayListOf(), databaseHelper)

        val kw = URLEncoder.encode(searchKeyword, "UTF-8")
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val client = HttpClient(Android){
                    install(JsonFeature) {
                        serializer = GsonSerializer()
                    }
                }

                val res : List<YoutubeVideo> = client.get("http://192.168.1.16:5519/api/list?keyword=$kw")
                withContext(Dispatchers.Main){
                    (searchResults.adapter as VideoListAdapter).setAll(res)
                }

                client.close()
            }catch (e: Throwable){
                Log.e("SearchActivity", "Error on fetching: $e")
            }
        }
    }

    private fun openSearch(keyword: String){
        val intent = Intent(this@SearchResultsActivity, SearchActivity::class.java)
        intent.putExtra("search", keyword)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.onRequestPermissionsResult(grantResults, this, packageName)
    }
}