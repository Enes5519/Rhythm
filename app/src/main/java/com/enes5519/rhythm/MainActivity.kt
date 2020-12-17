package com.enes5519.rhythm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.enes5519.rhythm.adapter.MusicListAdapter
import com.enes5519.rhythm.provider.DatabaseHelper
import com.enes5519.rhythm.utils.PermissionManager

class MainActivity : AppCompatActivity() {
    private val databaseHelper : DatabaseHelper by lazy{
        DatabaseHelper(this)
    }
    private val adapter : MusicListAdapter by lazy{
        MusicListAdapter(this, arrayListOf(), databaseHelper)
    }
    private val swipeRefreshLayout : SwipeRefreshLayout by lazy{
        findViewById(R.id.swipeRefresh)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<RecyclerView>(R.id.music_list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        swipeRefreshLayout.setOnRefreshListener { refresh() }
    }

    override fun onResume() {
        swipeRefreshLayout.isRefreshing = true
        refresh()
        super.onResume()
    }

    fun openSearch(view: View){
        startActivity(Intent(this, SearchActivity::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.onRequestPermissionsResult(grantResults, this, packageName)
    }

    private fun refresh(){
        adapter.setAll(databaseHelper.readMusics())
        swipeRefreshLayout.isRefreshing = false
    }
}