package com.enes5519.rhythm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.enes5519.rhythm.adapter.MusicListAdapter
import com.enes5519.rhythm.provider.DatabaseHelper

class MainActivity : AppCompatActivity() {
    private val databaseHelper : DatabaseHelper by lazy{
        DatabaseHelper(this)
    }
    private val adapter : MusicListAdapter by lazy{
        MusicListAdapter(this, arrayListOf(), databaseHelper)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<RecyclerView>(R.id.music_list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    override fun onResume() {
        adapter.setAll(databaseHelper.readMusics())
        super.onResume()
    }

    fun openSearch(view: View){
        startActivity(Intent(this, SearchActivity::class.java))
    }
}