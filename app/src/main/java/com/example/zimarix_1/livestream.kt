package com.example.zimarix_1

import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Bundle
import android.os.StrictMode
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toIcon
import com.example.zimarix_1.zimarix_global.Companion.appkey
import com.example.zimarix_1.zimarix_global.Companion.zimarix_server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class livestream : AppCompatActivity() {
    var REQ = "STREAM"
    var stream_on = 0
    var SREQ = 0
    var QUA = 80
    var seeki = 0
    var resolution = 0
    private val serverAddress = ""
    private val serverPort = 12345 // Replace with your UDP server port
    private var audioTrack: AudioTrack? = null


    fun intToByteArray(value: Int): ByteArray {
        val buffer = ByteBuffer.allocate(4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(value)
        return buffer.array()
    }
    fun byteArrayToInt(byteArray: ByteArray): Int {
        val buffer = ByteBuffer.wrap(byteArray)
        buffer.order(ByteOrder.BIG_ENDIAN)
        return buffer.int
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_livestream)

        setupAudioTrack()
        //startAudioStreaming()

        CoroutineScope(Dispatchers.IO).launch {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            stream_thread()
        }
        //var i = 0
        val button = findViewById<Button>(R.id.call)
        REQ = "STREAM"
        stream_on = 0
        val record = findViewById<Button>(R.id.record)
        record.setOnClickListener{
            REQ="RECORD"
        }
        val end = findViewById<Button>(R.id.end)
        end.setOnClickListener{
            stream_on = 0
        }

        button.setOnClickListener {
            REQ = "STREAM"
            if (stream_on == 0) {
                stream_on = 1
                /*
                if (button.text == "end call") {
                    button.text = "call"
                    i = 0
                } else if (button.text == "call") {
                    button.text = "end call"
                    i = 1

                }

                 */
            }
        }

        val spinner: Spinner = findViewById(R.id.spinner2)
        // Define data for the dropdown (e.g., a list of strings)
        val items = listOf("640X420", "1280x720", "1280x1020", "1920x1280")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                // Handle item selection here
                val selectedItem = parentView?.getItemAtPosition(position).toString()
                Toast.makeText(this@livestream, "Selected:$position $selectedItem", Toast.LENGTH_SHORT)
                    .show()
                resolution = position + 1
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Do nothing here (optional)
            }
        }
    }

    private fun setupAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize,
            AudioTrack.MODE_STREAM
        )
        audioTrack?.play()
    }

    private fun startAudioStreaming() {
        GlobalScope.launch(Dispatchers.IO) {
            val bufferSize = 320
            val buffer = ByteArray(bufferSize)

            val socket = DatagramSocket(serverPort)

            while (true) {
                val packet = DatagramPacket(buffer, bufferSize)
                socket.receive(packet)
                Log.d("debug ", "===============  received packet $bufferSize ${buffer.size}\n")

                // Write the received audio data to the AudioTrack
                audioTrack?.write(buffer, 0, bufferSize)
                audioTrack?.play()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioTrack?.release()
    }

    fun stream_thread(){
       val mp = MediaPlayer()
       val bufferSize = 1024*50
       val buffer = ByteArray(bufferSize)
       var bytesRead: Int

       val imgViewer: ImageView = findViewById<View>(R.id.imgView) as ImageView
       val dm = DisplayMetrics()
       windowManager.defaultDisplay.getMetrics(dm)
       imgViewer.setMinimumHeight(dm.heightPixels)
       imgViewer.setMinimumWidth(dm.widthPixels)


         val pro2 = findViewById<SeekBar>(R.id.seekBar)
        pro2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                if (REQ == "RECORD") {
                    SREQ = progress
                    seeki = 1
                }
            }
            override fun onStartTrackingTouch(seek: SeekBar) {
            }
            override fun onStopTrackingTouch(seek: SeekBar) {
            }
        })

        val qua = findViewById<SeekBar>(R.id.quality)
        qua.progress = 80
        qua.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                QUA = progress
            }
            override fun onStartTrackingTouch(seek: SeekBar) {
            }
            override fun onStopTrackingTouch(seek: SeekBar) {
            }
        })

        var streq = ""

       while (true){
           if(stream_on == 0){
               Thread.sleep(500)
               continue
           }
            try {
                var client = Socket(zimarix_server, 12334)
                val inputStream: InputStream = client.getInputStream()
                bytesRead = inputStream.read(buffer)
                if (bytesRead == 16) {
                    val iv = aes_decrpt(
                        "aqswdefrgthyj456",
                        "abcdefghijklmnop",
                        buffer.copyOf(bytesRead)
                    )
                    Log.d("debug ", " -------ivvvvvvvv received $iv\n")

                    val DID = '1'
                    val req = "STREAM" + DID + ','
                    val key = appkey
                    val aid = intToByteArray(2)
                    Log.d("debug ", " ---------------sending $key $iv\n")
                    val enc_data = aes_encrpt(key, iv, req)
                    client!!.outputStream.write(aid + enc_data)
                    bytesRead = inputStream.read(buffer)
                    Log.d("debug ", " ---------------received $bytesRead\n")
                    val skeyiv = aes_decrpt(key, iv, buffer.copyOf(bytesRead))
                    val skey = skeyiv.substring(0, 16)
                    val siv = skeyiv.substring(16, 32)

                    var totalread = 0

                    while (stream_on == 1) {
                        if (seeki == 0 && resolution == 0) {
                            streq = REQ+","+QUA.toString()
                        }else{
                            if (seeki != 0) {
                                streq = "SEEK," + SREQ.toString()
                                seeki = 0
                            }else if (resolution != 0){
                                streq = "RESOLUTION," + resolution.toString()
                                resolution = 0
                            }
                        }
                        Log.d("debug ", " -----------------11 $streq\n")

                        val enc_data = aes_encrpt(skey, siv, streq)
                        client!!.outputStream.write(enc_data)
                        bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1)
                            break
                        val s = byteArrayToInt(buffer.copyOfRange(0, 4))
                        val buf = buffer.copyOfRange(4, bytesRead)
                        val rs = ByteArrayOutputStream()
                        totalread = bytesRead - 4
                        rs.write(buf, 0, totalread)
                        while (totalread < s) {
                            bytesRead = inputStream.read(buffer)
                            rs.write(buffer, 0, bytesRead)
                            totalread = totalread + bytesRead
                        }
                        val fb = rs.toByteArray()
                        val keya1 = aes_decrpt_byte(skey, siv, fb)

                        var s1 = keya1[0].toInt()

                        if (s1 == 86) {
                            var keya = keya1.copyOfRange(3, keya1.size)
                            val s2 = keya1.copyOfRange(1, 3)
                            val k = ((s2[1].toInt() and 0xFF shl 8) or (s2[0].toInt() and 0xFF)).toShort()
                            Log.d("debug ", " ----------rees  $k \n")
                            //pro2.progress = k.toInt()
                            val bm = BitmapFactory.decodeByteArray(keya, 0, keya.size)
                            this@livestream.runOnUiThread(java.lang.Runnable {
                                imgViewer.setImageBitmap(bm)
                            })
                        } else if (s1 == 65) {
                            var keya = keya1.copyOfRange(1, keya1.size)
                            audioTrack?.write(keya, 0, keya.size)

                            /*
                            var keya = keya1.copyOfRange(1, keya1.size)
                            val tempMp3: File =
                                File.createTempFile("kurchina", "raw", cacheDir)
                            tempMp3.deleteOnExit();
                            val fos = FileOutputStream(tempMp3)
                            fos.write(keya)
                            fos.close()
                            val fis = FileInputStream(tempMp3)
                            mp.reset()
                            mp.setDataSource(fis.getFD());
                            mp.prepare();
                            mp.start()
                            */

                        }
                    }
                }
            } catch (t: SocketException) {
            }
        }
    }
}