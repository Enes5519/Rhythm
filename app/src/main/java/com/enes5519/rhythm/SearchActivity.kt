package com.enes5519.rhythm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.URLEncoder

class SearchActivity : AppCompatActivity() {
    private val list : ArrayList<String> = arrayListOf()
    private lateinit var adapter : ArrayAdapter<String>

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initViews()
    }

    private fun initViews(){
        val editText = findViewById<EditText>(R.id.search_et)
        adapter = ArrayAdapter(this@SearchActivity, R.layout.search_list_item, list)

        findViewById<ListView>(R.id.searchList).apply {
            adapter = this@SearchActivity.adapter
            setOnItemClickListener { _, _, position, _ -> openResults(list[position]) }
        }

        intent.getStringExtra("search")?.let {
            editText.setText(it)
            createSuggestions(it)
        }

        findViewById<ImageView>(R.id.back_button).setOnClickListener { onBackPressed() }
        findViewById<ImageView>(R.id.clear_button).setOnClickListener { editText.setText("") }

        editText.doAfterTextChanged {
            if(it !== null){
                createSuggestions(it.toString())
            }
        }

        editText.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                openResults(editText.text.toString())
            }
            false
        }
    }

    private fun createSuggestions(keyword: String){
        job?.cancel()
        if(keyword.isEmpty()){
            list.clear()
            adapter.notifyDataSetChanged()
            return
        }

        val kw = URLEncoder.encode(keyword, "UTF-8")

        job = CoroutineScope(Dispatchers.IO).launch {
            try{
                val client = HttpClient(Android)

                val res = client.get<String>("https://google.com/suggest?client=firefox&ds=yt&format=rich&q=$kw")

                val arr = JSONArray(res).getJSONArray(1)

                withContext(Dispatchers.Main){
                    list.clear()
                    for (i in 0 until arr.length()) {
                        list.add(arr.getString(i))
                    }
                    adapter.notifyDataSetChanged()
                }

                client.close()
            }catch (e: Throwable){
                Log.e("SearchActivity", "Error on fetching: $e")
            }
        }
    }

    private fun openResults(keyword: String){
        if(keyword.isEmpty()){
            return
        }

        val intent = Intent(this, SearchResultsActivity::class.java)
        intent.putExtra("search", keyword)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }
}