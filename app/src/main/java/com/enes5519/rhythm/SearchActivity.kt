package com.enes5519.rhythm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class SearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initViews()
        initAd()
    }

    private fun initViews() {
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

    private fun initAd(){
        MobileAds.initialize(this) {}

        findViewById<AdView>(R.id.adView).apply {
            val adRequest = AdRequest.Builder().build()
            loadAd(adRequest)
        }
    }
}