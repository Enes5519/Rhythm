package com.enes5519.rhythm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.enes5519.rhythm.R

class SearchSuggestAdapter(
    private val values: ArrayList<String>,
    private val onClickFunction: (String) -> Unit
) : RecyclerView.Adapter<SearchSuggestAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_suggest, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(values[position])
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(suggest: String){
            itemView.findViewById<TextView>(R.id.suggest).apply { text = suggest }
            itemView.setOnClickListener { onClickFunction(suggest) }
        }
    }
}