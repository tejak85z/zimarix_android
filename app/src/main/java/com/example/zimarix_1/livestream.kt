package com.example.zimarix_1

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.os.StrictMode
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.zimarix_1.zimarix_global.Companion.zimarix_server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
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
        val mp = MediaPlayer()

        var i = 0
        val button = findViewById<Button>(R.id.call)
        button.setOnClickListener {
            if(button.text == "end call") {
                button.text = "call"
                i = 0
            }else if(button.text == "call") {
                button.text = "end call"
                i = 1
                CoroutineScope(Dispatchers.IO).launch {
                    var keya = "".toByteArray()
                    val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
                    StrictMode.setThreadPolicy(policy)
                    try {
                        var client = Socket(zimarix_server, 12334)
                        val Sdata = "S" + zimarix_global.controller_ids[value].toString()
                        val enc_data = AES_encrpt(zimarix_global.appkey, Sdata)
                        client!!.outputStream.write("A".toByteArray()+zimarix_global.appid.toByteArray()+",".toByteArray()+ enc_data)
                        val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
                        while (i == 1) {
                            val resp = bufferReader.readLine()
                            if(resp.length <= 0)
                                break
                            try {
                                Log.d("debug ", " ======  fsssssf ${resp[0]}")
                                val enckeya = Base64.decode(resp.drop(1), Base64.NO_PADDING)
                                client!!.outputStream.write("ok".toByteArray())
                                keya = AES_decrpt_bytes(
                                    zimarix_global.controller_keys[value],
                                    enckeya
                                )
                                if(resp[0] == 'V') {
                                    val bm = BitmapFactory.decodeByteArray(keya, 0, keya.size)
                                    this@livestream.runOnUiThread(java.lang.Runnable {
                                        imgViewer.setImageBitmap(bm)
                                    })
                                }else if (resp[0] == 'A'){
                                    val tempMp3: File = File.createTempFile("kurchina", "raw", cacheDir)
                                    tempMp3.deleteOnExit();
                                    val fos = FileOutputStream(tempMp3)
                                    fos.write(keya)
                                    fos.close()
                                    val fis = FileInputStream(tempMp3)
                                    mp.reset()
                                    mp.setDataSource(fis.getFD());
                                    mp.prepare();
                                    mp.start()
                                }
                            } catch (t: IllegalArgumentException) {
                                continue
                            }
                        }
                    } catch (t: SocketException) {
                    }
                }
            }
        }
    }
}