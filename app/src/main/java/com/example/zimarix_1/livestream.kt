package com.example.zimarix_1

//import android.R

//import android.R
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.StrictMode
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.net.SocketException


class livestream : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_livestream)

        val b = intent.extras
        var value = -1 // or other values
        if (b != null) value = b.getInt("key")


        val imgViewer: ImageView = findViewById<View>(R.id.imgView) as ImageView
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        imgViewer.setMinimumHeight(dm.heightPixels)
        imgViewer.setMinimumWidth(dm.widthPixels)

        CoroutineScope(Dispatchers.IO).launch {
            var keya = "".toByteArray()
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            try {
                val client = Socket("192.168.1.10", 20222)
                val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
                val i = 1
                while (i == 1) {
                    val resp = bufferReader.readLine()
                    keya = Base64.decode(resp, Base64.NO_PADDING)
                    val bm = BitmapFactory.decodeByteArray(keya, 0, keya.size)
                    this@livestream.runOnUiThread(java.lang.Runnable {
                            imgViewer.setImageBitmap(bm)
                    })
                    client!!.outputStream.write("Naaaaaaa".toByteArray())
                }
                client.close()
            }catch (t: SocketException){

            }
        }
    }
}