package com.enes5519.rhythm.utils

import java.net.URLEncoder

object WebAPI {
    private const val URL = "https://ritim.herokuapp.com/"
    private const val LIST = "list?keyword="
    private const val DOWNLOAD = "download?video_id="

    fun createListURL(keyword: String): String = URL + LIST + URLEncoder.encode(keyword, "UTF-8")
    fun createDownloadURL(videoId: String): String = URL + DOWNLOAD + videoId
}