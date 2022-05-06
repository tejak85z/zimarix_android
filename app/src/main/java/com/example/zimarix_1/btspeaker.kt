package com.example.zimarix_1

import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.net.SocketException

class btspeaker : AppCompatActivity() {
    private var progressBar: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_btspeaker)

        progressBar = findViewById<ProgressBar>(R.id.progress_Bar) as ProgressBar


        var ret = ""
        val button = findViewById<Button>(R.id.bt_scan)
        button.setOnClickListener{
            progressBar!!.visibility = View.VISIBLE
            Thread(Runnable {
                val req = "I,BT,SCAN"
                ret = encrypt_and_send_data(req)
                this@btspeaker.runOnUiThread(java.lang.Runnable {
                    progressBar!!.visibility = View.INVISIBLE
                    val layout = LinearLayout(this)
                    layout.orientation = LinearLayout.VERTICAL
                    val devices = ret.split(",")
                    devices.forEach {
                        val dev = Button(this)
                        val mac = it.split("_")[0]
                        dev.setText(it)
                        dev.setOnClickListener {
                            val req = "I,BT,CONNECT,"+mac
                            Toast.makeText(this, req, Toast.LENGTH_SHORT).show()
                            ret = encrypt_and_send_data(req)
                        }
                        layout.addView(dev)
                    }
                    layout.setPadding(50, 40, 50, 10)
                    val builder = AlertDialog.Builder(this)
                        .setTitle("Scan Results")
                        .setView(layout)
                    val dialog = builder.create()
                    dialog.show()
                })
            }).start()

        }
    }

    fun encrypt_and_send_data(data : String): String {
        var resp = "FAIL"
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            if(zimarix_global.controller_ips[zimarix_global.curr_device] == "0" || zimarix_global.controller_ips[zimarix_global.curr_device].length < 7){
                return "INVALID CONFIG"
            }
            val client = Socket(zimarix_global.controller_ips[zimarix_global.curr_device], 20009)
            val enc_probe = AES_encrpt(zimarix_global.controller_keys[zimarix_global.curr_device],data)
            client!!.outputStream.write(enc_probe)
            val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
            resp = bufferReader.readLine()
            client.close()
        }catch (t: SocketException){

        }
        return resp
    }
}