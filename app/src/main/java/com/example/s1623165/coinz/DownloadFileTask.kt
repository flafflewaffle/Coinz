package com.example.s1623165.coinz

import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

// From Lecture slides

interface DownloadCompleteListener {
    fun downloadComplete(result: String)
}

object DownloadCompleteRunner : DownloadCompleteListener {
    var result : String? = null
    override fun downloadComplete(result: String) {
        this.result = result
    }
}

class DownloadFileTask(private val caller : DownloadCompleteListener) :
        AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg urls: String?): String = try {
        loadFileFromNetwork(urls[0]!!)
    } catch (e : IOException) {
        "Unable to load content. Check your network connection"
    }

    private fun loadFileFromNetwork(urlString : String) : String {
        val stream : InputStream = downloadUrl(urlString)
        return stream.reader().use { it.readText() }
    }

    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000 // milliseconds
        conn.connectTimeout = 15000 // milliseconds
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect() // Starts the query
        return conn.inputStream
    }

    override fun onPostExecute(result: String) {
        Log.d("DownloadFileTask", "Download successful")
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }
}