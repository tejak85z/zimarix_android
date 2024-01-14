package com.example.zimarix_1

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.Socket
import java.net.SocketException
import kotlin.experimental.or

fun dev_req(id: Int, cmd: String): String {
    val bufferSize = 1024*50
    val buffer = ByteArray(bufferSize)
    var bytesRead: Int
    var resp = "FAIL"
    if (zimarix_global.devices[id].client == null){
        return resp
    }
    val enc_data = aes_encrpt(zimarix_global.devices[id].key, zimarix_global.devices[id].iv, cmd)
    synchronized(zimarix_global.devices[id].lock) {
        try {
            zimarix_global.devices[id].client?.outputStream?.write(enc_data)
            bytesRead = zimarix_global.devices[id].inputStream.read(buffer)
            if (bytesRead % 16 == 0) {
                resp = aes_decrpt( zimarix_global.devices[id].key,
                        zimarix_global.devices[id].iv,
                        buffer.copyOf(bytesRead))
                return resp
            }
        }catch (t: SocketException){
            Log.d("debug ", " -------closing socket at 2")
            zimarix_global.devices[id].client = null
        }
    }
    return resp
}

class Send_cmd(
    private val value: Int,
    private val cmd: String,
    private val activity: update_params
) : AsyncTask<Void, Void, String>() {

    private var resp = "FAIL"
    override fun onPreExecute() {
        activity.progressBar.visibility = View.VISIBLE
    }

    override fun doInBackground(vararg params: Void?): String {
        if (activity.isTaskInProgress == true) {
            return "Previous Request In Progress"
        }else {
            activity.isTaskInProgress = true
            if (zimarix_global.devices[value].client != null) {
                resp = dev_req(value, cmd)
            } else {
                return "FAIL"
            }
        }
        return resp
    }
    override fun onPostExecute(result: String) {
        activity.progressBar.visibility = View.GONE
        // Cancel the current Toast if it exists
        activity.aToast?.cancel()
        activity.isTaskInProgress = false
        val param = result.split(",")
        // Show the new Toast
        activity.aToast = Toast.makeText(activity as? Context, param[0], Toast.LENGTH_SHORT)
        activity.aToast?.show()
    }
}

fun update_port_info(index:Int){
    val resp = dev_req(index,"IO,GET,PORT_STATE,")
    val param = resp.split(",")
    if (param[0] == "PORT") {
        var port:Short = 0
        for (i in 0..14) {
            if (param[1][i] == '1'){
                val mask = (1 shl i).toShort()
                port = port or mask
            }
        }
        zimarix_global.devices[index].port = port
    }
}

fun check_and_update_switches(device_info_index: Int, did: Int) {
    val resp = dev_req(device_info_index, "CFG,IO,GET,PORT_INFO,")
    val ports = resp.split(",")
    var update = 0

    // Collect the ids from the response
    val responseIds = ports.filter { it.split("_").size == 4 }.map { it.split("_")[0] }
    val Gswitch_len = GSwitches.size
    // Remove entries from GSwitches with common did and not in response
    GSwitches.removeAll { it.did == did && it.id.toInt() < 32 && it.id !in responseIds }
    if (Gswitch_len != GSwitches.size){
        update = 1
    }

    if (ports.size > 0) {
        ports.forEach {
            val param = it.split("_")
            if (param.size == 4) {
                val matchingSwitch: sw_params? = GSwitches.find {it.id == param[0] && it.did == did }
                if (matchingSwitch != null) {
                    if (matchingSwitch.name != param[3]) {
                        matchingSwitch.name = param[3]
                        update = 1
                    }
                    if (matchingSwitch.type != param[1].toInt()){
                        matchingSwitch.type = param[1].toInt()
                        update = 1
                    }
                    if (matchingSwitch.powersave != param[2]){
                        matchingSwitch.powersave = param[2]
                        update = 1
                    }
                } else {
                    GSwitch_adapter.addSwitch(device_info_index, did, param[0], param[1].toInt(), param[3], param[2])
                    update = 1
                }
            }
        }
    }
    if (update == 1) {
        GlobalScope.launch(Dispatchers.Main) {
            // Update UI here
            GSwitch_adapter.update()
        }
    }
}

fun check_and_update_ir(device_info_index: Int, did: Int) {
    val resp = dev_req(device_info_index, "CFG,IR,GET,")
    val ports = resp.split(",")
    var update = 0

    // Collect the ids from the response
    val responseIds = ports.filter { it.split("_").size == 3 }.map { it.split("_")[0] }
    val Gswitch_len = GSwitches.size
    // Remove entries from GSwitches with common did and not in response
    GSwitches.removeAll { it.did == did && it.id.toInt() >= 32 && it.id !in responseIds }
    if (Gswitch_len != GSwitches.size){
        update = 1
    }

    if (ports.size > 0) {
        ports.forEach {
            val param = it.split("_")
            if (param.size == 3) {
                val matchingSwitch: sw_params? = GSwitches.find {
                    it.id == param[0] && it.did == did
                }
                if (matchingSwitch != null) {
                    if (matchingSwitch.name != param[1]) {
                        matchingSwitch.name = param[1]
                        update = 1
                    }
                    if (matchingSwitch.type != param[2].toInt()){
                        matchingSwitch.type = param[2].toInt()
                        update = 1
                    }
                } else {
                    GSwitch_adapter.addSwitch(device_info_index, did, param[0], param[2].toInt(), param[1])
                    update = 1
                }
            }
        }
    }
    if (update == 1) {
        GlobalScope.launch(Dispatchers.Main) {
            // Update UI here
            GSwitch_adapter.update()
        }
    }
}

fun device_handler(handler: Handler, mainActivity: MainActivity) {
    val bufferSize = 1024*50
    val buffer = ByteArray(bufferSize)
    var bytesRead: Int
    while (true){
        //if(app_state == 1) {
        if(true){
            zimarix_global.devices.forEachIndexed { device_info_index, it ->
                if (it.client == null){
                    try {
                        val client = Socket(it.ip, 20009)
                        val inputStream: InputStream? = client.getInputStream()
                        if (inputStream != null) {
                            bytesRead = inputStream.read(buffer)
                            if (bytesRead == 16) {
                                it.iv = aes_decrpt(it.key,"abcdefghijklmnop", buffer.copyOf(bytesRead))
                                val req = "CONNECT"
                                val enc_data = aes_encrpt(it.key, it.iv, req)
                                client.outputStream.write(enc_data)
                                bytesRead = inputStream.read(buffer)
                                if (bytesRead == 16) {
                                    val resp =
                                        aes_decrpt(it.key, it.iv, buffer.copyOf(bytesRead))
                                    val param = resp.split(",")
                                    if (param[0] == "ACCEPT") {
                                        it.client = client
                                        it.inputStream = inputStream
                                        CoroutineScope(Dispatchers.IO).launch {
                                            var i = 0
                                            while (it.client != null) {
                                                if (i > 30)
                                                    i = 0
                                                if (i%10 == 0) {
                                                    if (mainActivity.active == 1) {
                                                        check_and_update_switches(
                                                            device_info_index,
                                                            it.id
                                                        )
                                                        check_and_update_ir(
                                                            device_info_index,
                                                            it.id
                                                        )
                                                    }
                                                }
                                                update_port_info(device_info_index)
                                                Thread.sleep(1000)
                                                i += 1
                                            } //monitor_socket(it, inputStream, handler, mainActivity)
                                        }
                                    }
                                }
                            }
                        }
                        if (it.client == null) {
                            Log.d("debug ", " -------closing socket at 1")
                            client.close()
                        }
                    }catch (t: SocketException){
                        Log.d("debug ", " -------closing socket at 2")
                        it.client = null
                    }
                }
            }
        }
        Thread.sleep(1000)
    }
}