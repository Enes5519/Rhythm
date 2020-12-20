package com.enes5519.rhythm

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.enes5519.rhythm.adapter.SearchSuggestAdapter
import com.enes5519.rhythm.provider.getSuggestions
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList


class SuggestFragment : Fragment() {
    private val suggestAdapter: SearchSuggestAdapter = SearchSuggestAdapter(data, this::forwardToResults)
    private val editText : EditText by lazy{ activity?.findViewById(R.id.search_et)!! }
    private var timer = Timer()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_suggest_list, container, false)

        if(view is RecyclerView){
            with(view){
                layoutManager = LinearLayoutManager(context)
                adapter = suggestAdapter
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editText.apply {
            doAfterTextChanged {
                timer.cancel()
                timer = Timer()
                timer.schedule(object: TimerTask() {
                    override fun run() {
                        if(it !== null) createSuggestions(it.toString())
                    }
                }, 1000)
            }
            setOnEditorActionListener { _, actionId, _ ->
                if(actionId == EditorInfo.IME_ACTION_SEARCH){
                    forwardToResults(text.toString())
                }
                false
            }
        }
    }

    private fun forwardToResults(t: String){
        if(t.isEmpty()){
            return
        }

        val imm: InputMethodManager = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)

        activity?.findViewById<EditText>(R.id.search_et)!!.apply { setText(t) }
        findNavController().navigate(R.id.action_suggest_to_result)
    }

    private fun createSuggestions(keyword: String){
        if(job !== null){
            if(!job!!.isCancelled){
                job!!.cancel()
            }
            job = null
        }
        if(keyword.isEmpty()){
            data.clear()
            suggestAdapter.notifyDataSetChanged()
            return
        }

        val kw = URLEncoder.encode(keyword, "UTF-8")
        job = CoroutineScope(Dispatchers.IO).launch {
            try{
                val client = HttpClient(Android)
                val res = client.getSuggestions(kw)

                withContext(Dispatchers.Main){
                    data.clear()
                    for (search in res) data.add(search.asString)
                    suggestAdapter.notifyDataSetChanged()
                }

                client.close()
            }catch (e: Throwable){
                Log.e("SuggestFragment", "Error on fetching: $e")
            }
        }
    }

    companion object{
        val data : ArrayList<String> = arrayListOf()
        var job : Job? = null
    }
}