package com.example.zimarix_1

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.Socket
import java.util.ArrayList

suspend fun status_config_updater(){
    while (true){
        ec_req("SYNC,"+ zimarix_global.keyver +","+ zimarix_global.ipver +","+ zimarix_global.portver +","+ zimarix_global.monver +",")
        Thread.sleep(5000)
    }
    Log.d("debug ", " -------------- closing send socket")
    zimarix_global.ecsock!!.close()
}

fun adddevice(objectList: ArrayList<device>, newObject: device) {
    objectList.add(newObject)
}

class DummyInputStream : InputStream() {
    override fun read(): Int {
        return -1 // Indicates the end of the stream
    }
}

fun check_and_add_device(idToAdd: Int, key:String) {
    val foundObject = zimarix_global.devices.find { it.id == idToAdd }
    foundObject?.let {
        it.key = key
        it.port = 0
        it.ip = ""
    } ?:adddevice(zimarix_global.devices, device(idToAdd,key,"", "",0,lock = Any(), inputStream = DummyInputStream(),deviceConfig = device_config()))
}

fun check_and_update_ip(idToAdd: Int, ip:String) {
    val foundObject = zimarix_global.devices.find { it.id == idToAdd }
    foundObject?.let {
        it.ip = ip
    }
}

fun check_and_update_port(idToAdd: Int, port:String) {
    Log.d("debug ", " -------updating port id = $idToAdd  port = $port")
    val foundObject = zimarix_global.devices.find { it.id == idToAdd }
    foundObject?.let {
        if (it.client == null)
            it.port = port.toShort()
    }
}

fun update_list(idx:Int, data:String){
    if(idx == 0){
        val param = data.split("_")
        check_and_add_device(param[0].toInt(),param[1])
    }else if(idx == 1){
        val param = data.split("_")
        check_and_update_ip(param[0].toInt(),param[1])
    }else if(idx == 2){
        val param = data.split("_")
        check_and_update_port(param[0].toInt(),param[1])
    }else if(idx == 3){
        Log.d("debug ", " monitor info -----$data")
    }
}

fun process_server_response(context: Context, data: String){
    val param = data.split(',')
    Log.d("debug ", " ---------------------print to tost $param")

    if (param[0] == "SYNC") {
        var i = 1
        var idx = -1

        while (i < param.size) {
            if (param[i] == "SYNCKEY") {
                idx = 0
                zimarix_global.keyver = param[i + 1]
                i += 1 // Skip the next index
            } else if (param[i] == "SYNCIPS") {
                idx = 1
                zimarix_global.ipver = param[i + 1]
                i += 1 // Skip the next index
            } else if (param[i] == "SYNCPTS") {
                idx = 2
                zimarix_global.portver = param[i + 1]
                i += 1 // Skip the next index
            } else if (param[i] == "SYNCMON") {
                idx = 3
                zimarix_global.monver = param[i + 1]
                i += 1 // Skip the next index
            } else if (param[i] == "END") {
                return
            } else {
                update_list(idx, param[i])
            }
            i += 1 // Skip the next index
        }
    }else if (param[0] == "RESP"){
        GlobalScope.launch(Dispatchers.Main) {
            // Update UI here
            Toast.makeText(context, param[1], Toast.LENGTH_SHORT).show()
        }
    }
}

fun server_handler(context: Context){
    val bufferSize = 1024
    val buffer = ByteArray(bufferSize)
    var bytesRead: Int
    var client:Socket
    var inputStream: InputStream
    while (true){
        if(true) {
            try {
                client = Socket(zimarix_global.zimarix_server, 11112)
            }catch (t: Throwable){
                continue
            }
            try {
                inputStream = client.getInputStream()
                // Read and decrypt iv from server
                bytesRead = inputStream.read(buffer)
            }catch (t: Throwable){
                client.close()
                continue
            }
            if(bytesRead == 16) {
                zimarix_global.appiv =
                    aes_decrpt("aqswdefrgthyj456", "abcdefghijklmnop", buffer.copyOf(bytesRead))
                val msg = "CONNECT"
                val enc_data = aes_encrpt(zimarix_global.appkey, zimarix_global.appiv, msg)
                try {
                    client!!.outputStream.write(zimarix_global.appid.toByteArray() + ",".toByteArray() + enc_data)
                    bytesRead = inputStream.read(buffer)
                }catch (t: Throwable){
                    client.close()
                    continue
                }
                if (bytesRead > 0 && bytesRead % 16 == 0) {
                    val status = aes_decrpt(zimarix_global.appkey,
                        zimarix_global.appiv,
                        buffer.copyOf(bytesRead)
                    )
                    if (status.contains("CONNECTED")) {
                        zimarix_global.ecsock = client
                        zimarix_global.ecinputStream = inputStream
                        CoroutineScope(Dispatchers.IO).launch {
                            status_config_updater()
                        }
                        while (true) {
                            try {
                                bytesRead = inputStream.read(buffer)
                            }catch (t: Throwable){
                                break
                            }
                            if (bytesRead > 0) {
                                val bytedata = aes_decrpt(
                                    zimarix_global.appkey,
                                    zimarix_global.appiv, buffer.copyOf(bytesRead)
                                )
                                process_server_response(context, bytedata)
                            } else {
                                break
                            }
                        }
                        zimarix_global.ecsock = null
                        zimarix_global.ecinputStream = null
                    }
                }
                client.close()
            }
        }
        Thread.sleep(5000)
    }
}