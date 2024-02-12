package com.example.zimarix_1.Activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.R
import com.example.zimarix_1.aes_decrpt
import com.example.zimarix_1.aes_decrpt_byte
import com.example.zimarix_1.aes_encrpt
import com.example.zimarix_1.update_params
import com.example.zimarix_1.zimarix_global
import com.example.zimarix_1.zimarix_global.Companion.appid
import com.example.zimarix_1.zimarix_global.Companion.appkey
import com.example.zimarix_1.zimarix_global.Companion.zimarix_server
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder


class livestream : AppCompatActivity() , update_params {

    private lateinit var recyclerView: RecyclerView
    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    var KEY = ""
    var DID = -1
    var AID = "".toByteArray()
    var REQ = "STREAM"
    var SREQ = 0
    var QUA = 80
    var seeki = 0

    var VIDEO = -1
    var AUDIO = -1
    var AUDSTR = "A"

    private var audioTrack: AudioTrack? = null
    var imgViewer: PhotoView? = null
    var zoomLevel = 1f // Initialize zoom level
    fun byteArrayToInt(byteArray: ByteArray): Int {
        val buffer = ByteBuffer.wrap(byteArray)
        buffer.order(ByteOrder.BIG_ENDIAN)
        return buffer.int
    }
    fun intToByteArray(value: Int): ByteArray {
        val buffer = ByteBuffer.allocate(4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(value)
        return buffer.array()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_livestream)

        progressBar = findViewById(R.id.streamprogressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        var value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        KEY = zimarix_global.devices[value].key
        DID = zimarix_global.devices[value].id
        AID = intToByteArray(appid.toInt())

        imgViewer = findViewById(R.id.imgView)

        fun toggleFullScreen() {
            val layoutParams = imgViewer!!.layoutParams
            layoutParams.width = if (layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }
            layoutParams.height = if (layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }
            imgViewer!!.layoutParams = layoutParams
        }

        imgViewer!!.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                // This will be triggered when a double tap occurs
                toggleFullScreen()
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
                // This will be triggered for events related to double tap
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                // This will be triggered when a single tap occurs
                return true
            }
        })

        setupAudioTrack()

        CoroutineScope(Dispatchers.IO).launch {
            local_video_stream_thread(this@livestream, value)
        }
        CoroutineScope(Dispatchers.IO).launch {
            remote_video_stream_thread(this@livestream, value)
        }

        CoroutineScope(Dispatchers.IO).launch {
            local_audio_stream_thread(this@livestream, value)
        }

        CoroutineScope(Dispatchers.IO).launch {
            remote_audio_stream_thread(this@livestream, value)
        }

        val button = findViewById<Button>(R.id.call)
        val mute = findViewById<CheckBox>(R.id.mute)
        button.setOnClickListener {
            REQ = "STREAM"
            if (mute.isChecked)
                AUDSTR = "P"
            else
                AUDSTR = "A"
        }
        val record = findViewById<Button>(R.id.record)
        record.setOnClickListener{
            REQ = "RECORD"
            AUDSTR = "P"
        }
        val pause = findViewById<Button>(R.id.pause)
        pause.setOnClickListener{
            REQ = "PAUSE"
        }
        mute.setOnClickListener{
            if (mute.isChecked)
                AUDSTR = "P"
            else
                AUDSTR = "A"
        }

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

    override fun onDestroy() {
        super.onDestroy()
        audioTrack?.release()
    }

    override fun onResume() {
        super.onResume()
        active = true
    }
    override fun onPause() {
        super.onPause()
        active = false
    }
    fun readBytes(inputStream: InputStream, numBytes: Int): ByteArray? {
        if (numBytes < 0)
            return null
        val buffer = ByteArray(numBytes)
        var totalBytesRead = 0

        while (totalBytesRead < numBytes) {
            val bytesRead = inputStream.read(buffer, totalBytesRead, numBytes - totalBytesRead)
            if (bytesRead == -1) {
                // End of stream reached before reading the specified number of bytes
                return null
            }
            totalBytesRead += bytesRead
        }
        Log.d("debug ", "===============  $numBytes \n")

        return buffer
    }

    fun video_stream_transmitter_thread(skey: String, siv: String, client: Socket, remote:Int) {
        var streq = ""
        while (this.active && (remote != 1 || VIDEO == 0)) {
            if (seeki == 0) {
                streq = REQ+","+QUA.toString()
            }else{
                streq = "SEEK," + SREQ.toString()
                seeki = 0
            }
            // write request
            val enc_data = aes_encrpt(skey, siv, streq)
            try {
                client.outputStream.write(enc_data)
            }catch (t: SocketException) {
                break
            }
            Thread.sleep(1000)
        }
    }
    fun start_video_streaming(client: Socket, inputStream: InputStream, skey: String, siv: String, remote: Int){
        var bytesRead: Int
        val bufferSize = 1024*10
        val buffer = ByteArray(bufferSize)
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        CoroutineScope(Dispatchers.IO).launch {
            video_stream_transmitter_thread(skey,siv,client, remote)
        }

        while (active == true && (remote != 1 || VIDEO == 0)) {
            try {
                bytesRead = inputStream.read(buffer,0,4)
            } catch (e: SocketTimeoutException) {
                continue
            }
            if (bytesRead != 4) {
                break
            }
            // Extract the size from the header
            val dataSize = byteArrayToInt(buffer)
            val encrypted_data = readBytes(inputStream, dataSize)
            if (encrypted_data != null) {
                if (encrypted_data!!.size == dataSize) {
                    val decrypt_data = aes_decrpt_byte(skey, siv, encrypted_data!!)
                    if (decrypt_data.isNotEmpty() && decrypt_data[0].toInt()
                            .toChar() == 'V'
                    ) {
                        var image = decrypt_data.copyOfRange(3, decrypt_data.size)
                        val param2 = decrypt_data.copyOfRange(1, 3)
                        val progress_bytes =
                            ((param2[1].toInt() and 0xFF shl 8) or (param2[0].toInt() and 0xFF)).toShort()
                        //pro2.progress = progress_bytes.toInt()
                        val bm = BitmapFactory.decodeByteArray(image, 0, image.size)
                        this@livestream.runOnUiThread(java.lang.Runnable {
                            imgViewer?.setImageBitmap(bm)
                        })
                    }
                }else
                    break
            }else
                break
        }
    }

    fun audio_stream_transmitter_thread(skey: String, siv: String, client: Socket, remote:Int) {
        while (this.active && (remote != 1 || AUDIO == 0)) {
            val enc_data = aes_encrpt(skey, siv, AUDSTR)
            try {
                client!!.outputStream.write(enc_data)
            }catch (t: SocketException) {
                break
            }
            Thread.sleep(1000)
        }
    }
    fun start_audio_streaming(client: Socket, inputStream: InputStream, skey: String, siv: String, remote: Int){
        var bytesRead: Int
        val bufferSize = 1024*10
        val buffer = ByteArray(bufferSize)
        CoroutineScope(Dispatchers.IO).launch {
            audio_stream_transmitter_thread(skey,siv,client, remote)
        }
        while (active == true && (remote != 1 || AUDIO == 0)) {
            try {
                bytesRead = inputStream.read(buffer,0,4)
            } catch (e: SocketTimeoutException) {
                continue
            }
            if (bytesRead != 4) {
                break
            }
            // Extract the size from the header
            val dataSize = byteArrayToInt(buffer)
            val encrypted_data = readBytes(inputStream, dataSize)

            val decrypt_data = aes_decrpt_byte(skey, siv, encrypted_data!!)
            if (decrypt_data.isNotEmpty() && decrypt_data[0].toInt().toChar() == 'A') {
                var audio = decrypt_data.copyOfRange(1, decrypt_data.size)
                audioTrack?.write(audio, 0, audio.size)
            }
        }
    }
    fun remote_video_stream_thread(livestream: livestream, value: Int){
        GlobalScope.launch(Dispatchers.Main) {
            livestream.progressBar.visibility = View.VISIBLE
        }
        val bufferSize = 1024*10
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int

        while (active == true) {
            if (VIDEO == 0) {
                try {
                    var client = Socket(zimarix_server, 12334)
                    //var client = Socket(zimarix_global.devices[value].ip, 29999)
                    val timeout = 5000
                    client.soTimeout = timeout

                    // get input stream
                    val inputStream: InputStream = client.getInputStream()
                    // receive and decrypt iv for further communication
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == 16) {
                        val iv = aes_decrpt(
                            "aqswdefrgthyj456",
                            "abcdefghijklmnop",
                            buffer.copyOf(bytesRead)
                        )

                        // Send the STREAM request
                        val req = "STREAM" + DID + ','
                        val enc_data = aes_encrpt(appkey, iv, req)
                        client!!.outputStream.write(AID + enc_data)
                        try {
                            bytesRead = inputStream.read(buffer)
                        } catch (e: SocketTimeoutException) {
                            client.close()
                            inputStream.close()
                            Thread.sleep(1000)
                            continue
                        }
                        if (bytesRead != -1) {
                            //get new key iv from server
                            val skeyiv = aes_decrpt(appkey, iv, buffer.copyOf(bytesRead))
                            val skey = skeyiv.substring(0, 16)
                            val siv = skeyiv.substring(16, 32)
                            GlobalScope.launch(Dispatchers.Main) {
                                livestream.progressBar.visibility = View.GONE
                            }
                            start_video_streaming(client, inputStream, skey, siv, 1)
                        }else{
                            client.close()
                            inputStream.close()
                            Thread.sleep(1000)
                            continue
                        }
                    }
                    client.close()
                    inputStream.close()
                } catch (t: SocketException) {

                }
            }
            Thread.sleep(1000)
       }
    }

    fun local_video_stream_thread(livestream: livestream, value: Int){
        GlobalScope.launch(Dispatchers.Main) {
            livestream.progressBar.visibility = View.VISIBLE
        }
        val bufferSize = 1024*10
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int

        val timeoutInMillis = 5000 // 5 seconds
        val socketAddress = InetSocketAddress(zimarix_global.devices[value].ip, 29999)
        var client:Socket
        while (active == true) {
            if (zimarix_global.devices[value].client != null) {
                client = Socket()
                try {
                    client.connect(socketAddress, timeoutInMillis)
                } catch (e: Exception) {
                    VIDEO = 0
                    client.close()
                    Thread.sleep(1000)
                    continue
                }
            }else{
                VIDEO = 0
                Thread.sleep(1000)
                continue
            }
            try {
                client.soTimeout = timeoutInMillis
                val inputStream: InputStream = client.getInputStream()
                bytesRead = inputStream.read(buffer)
                if (bytesRead == 16) {

                    val iv = aes_decrpt(
                        zimarix_global.devices[value].key,
                        "abcdefghijklmnop",
                        buffer.copyOf(bytesRead)
                    )
                    // Send the STREAM request
                    val req = "STREAM"
                    val enc_data = aes_encrpt(KEY, iv, req)
                    client!!.outputStream.write(enc_data)

                    try {
                        bytesRead = inputStream.read(buffer)
                    } catch (e: SocketTimeoutException) {
                        client.close()
                        inputStream.close()
                        Thread.sleep(1000)
                        continue
                    }
                    if (bytesRead != -1){
                        VIDEO = 1
                        GlobalScope.launch(Dispatchers.Main) {
                            livestream.progressBar.visibility = View.GONE
                        }
                        start_video_streaming(client, inputStream, KEY, iv, 0)
                    }else{
                        client.close()
                        inputStream.close()
                        Thread.sleep(1000)
                        continue
                    }
                }
                client.close()
                inputStream.close()
            } catch (t: SocketException) {
            }
            VIDEO = 0
            Thread.sleep(1000)
        }
    }
    fun local_audio_stream_thread(livestream: livestream, value: Int){
        val bufferSize = 1024*50
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int

        val timeoutInMillis = 5000 // 5 seconds
        val socketAddress = InetSocketAddress(zimarix_global.devices[value].ip, 29998)
        while (active == true) {
            val client = Socket()
            try {
                client.connect(socketAddress, timeoutInMillis)
            } catch (e: Exception) {
                AUDIO = 0
                client.close()
                Thread.sleep(1000)
                continue
            }
            try {
                client.soTimeout = timeoutInMillis

                val inputStream: InputStream = client.getInputStream()
                bytesRead = inputStream.read(buffer)
                if (bytesRead == 16) {
                    val iv = aes_decrpt(
                        zimarix_global.devices[value].key,
                        "abcdefghijklmnop",
                        buffer.copyOf(bytesRead)
                    )

                    val req = "STREAM"
                    var enc_data = aes_encrpt(KEY, iv, req)
                    client!!.outputStream.write(enc_data)

                    try {
                        bytesRead = inputStream.read(buffer)
                    } catch (e: SocketTimeoutException) {
                        client.close()
                        inputStream.close()
                        Thread.sleep(1000)
                        continue
                    }
                    if (bytesRead == -1) {
                        client.close()
                        inputStream.close()
                        Thread.sleep(1000)
                        continue
                    }
                    AUDIO = 1
                    start_audio_streaming(client, inputStream, KEY, iv, 0)
                }
                client.close()
                inputStream.close()
            } catch (t: SocketException) {

            }
            AUDIO = 0
            Thread.sleep(1000)
        }
    }

    fun remote_audio_stream_thread(livestream: livestream, value: Int){
        val bufferSize = 1024*50
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int

        while (active == true) {
            if (AUDIO == 0) {
                try {
                    var client = Socket(zimarix_server, 12336)
                    val timeout = 5000
                    client.soTimeout = timeout

                    val inputStream: InputStream = client.getInputStream()
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == 16) {
                        val iv = aes_decrpt(
                            "aqswdefrgthyj456",
                            "abcdefghijklmnop",
                            buffer.copyOf(bytesRead)
                        )

                        val req = "AUDIOSTREAM" + DID + ','
                        val enc_data = aes_encrpt(appkey, iv, req)
                        client!!.outputStream.write(AID + enc_data)

                        try {
                            bytesRead = inputStream.read(buffer)
                        } catch (e: SocketTimeoutException) {
                            client.close()
                            inputStream.close()
                            Thread.sleep(1000)
                            continue
                        }

                        if (bytesRead == -1) {
                            client.close()
                            inputStream.close()
                            Thread.sleep(1000)
                            continue
                        }

                        val skeyiv = aes_decrpt(appkey, iv, buffer.copyOf(bytesRead))
                        val skey = skeyiv.substring(0, 16)
                        val siv = skeyiv.substring(16, 32)
                        start_audio_streaming(client, inputStream, skey, siv, 1)
                    }
                    client.close()
                    inputStream.close()
                } catch (t: SocketException) {

                }
                Thread.sleep(1000)
            }
        }
    }
}


