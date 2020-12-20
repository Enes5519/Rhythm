package com.enes5519.rhythm.provider

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import kotlin.math.roundToInt

suspend fun HttpClient.downloadFile(file: File, url: String) : Flow<DownloadResult> {
    return flow{
        try{
            get<HttpStatement>(url).execute{ response ->
                val channel = response.receive<ByteReadChannel>()
                val data = ByteArray(response.contentLength()!!.toInt())
                var offset = 0
                do{
                    val currentRead = channel.readAvailable(data, offset, data.size / 1)
                    offset += currentRead
                    val progress = (offset * 100f / data.size).roundToInt()
                    emit(DownloadResult.Progress(progress))
                }while (currentRead > 0)

                if(response.status.isSuccess()){
                    file.writeBytes(data)
                    emit(DownloadResult.Success)
                }else{
                    emit(DownloadResult.Error("File not downloaded"))
                }
            }
        } catch (e: TimeoutCancellationException) {
            emit(DownloadResult.Error("Connection timed out", e))
        } catch (t: Throwable) {
            emit(DownloadResult.Error("Failed to connect"))
        }
    }
}

suspend fun HttpClient.getSuggestions(keyword: String) : JsonArray {
    val response : String = get("https://google.com/suggest?client=firefox&ds=yt&format=rich&q=$keyword")
    return JsonParser.parseString(response).asJsonArray.get(1).asJsonArray
}

sealed class DownloadResult{
    object Success: DownloadResult()
    data class Error(val message: String, val cause: Exception? = null) : DownloadResult()
    data class Progress(val progress: Int): DownloadResult()
}