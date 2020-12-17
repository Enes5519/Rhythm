package com.enes5519.rhythm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.findNavController

class SearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val et = findViewById<EditText>(R.id.search_et).apply {
            doAfterTextChanged {
                val nav = findNavController(R.id.nav_host_fragment)
                if (it !== null && nav.currentDestination?.id != R.id.suggest_fragment) {
                    nav.navigate(R.id.action_global_suggest_fragment)
                }
            }
        }

        findViewById<ImageView>(R.id.clear_button).setOnClickListener {
            et.setText("")
            et.requestFocus()
        }

        findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }
    }
}