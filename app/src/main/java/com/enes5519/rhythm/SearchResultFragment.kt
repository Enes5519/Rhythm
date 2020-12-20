package com.enes5519.rhythm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.enes5519.rhythm.adapter.SearchResultAdapter
import com.enes5519.rhythm.model.YoutubeVideo
import com.enes5519.rhythm.provider.DatabaseHelper
import com.enes5519.rhythm.utils.PermissionManager
import com.enes5519.rhythm.utils.WebAPI
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchResultFragment : Fragment() {
    private val notificationChannelId = "download_music"
    private val searchKeyword: String by lazy{
        requireActivity().findViewById<EditText>(R.id.search_et).text.toString()
    }
    private val notificationManager: NotificationManager by lazy{
        requireActivity().getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val resultAdapter : SearchResultAdapter by lazy {
        SearchResultAdapter(requireContext(), DatabaseHelper(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        initNotification()

        val view = inflater.inflate(R.layout.fragment_search_result_list, container, false)
        if(view is RecyclerView){
            with(view){
                layoutManager = LinearLayoutManager(context)
                adapter = resultAdapter
            }
        }

        return view
    }

    private fun initNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    notificationChannelId,
                    "Download music",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadList()
    }

    private fun loadList(){
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val client = HttpClient(Android){
                    install(JsonFeature) {
                        serializer = GsonSerializer()
                    }
                }

                val res : List<YoutubeVideo> = client.get(WebAPI.createListURL(searchKeyword))
                withContext(Dispatchers.Main){
                    resultAdapter.setAll(res)
                }

                client.close()
            }catch (e: Throwable){
                Log.e("SearchActivity", "Error on fetching: $e")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val context = requireContext()
        PermissionManager.onRequestPermissionsResult(grantResults, context, context.packageName)
    }
}