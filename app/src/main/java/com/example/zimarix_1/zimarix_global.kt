package com.example.zimarix_1

import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.zimarix_1.zimarix_global.Companion.appid
import com.example.zimarix_1.zimarix_global.Companion.appiv
import com.example.zimarix_1.zimarix_global.Companion.appkey
import com.example.zimarix_1.zimarix_global.Companion.ecsock
import java.io.InputStream
import java.net.Socket
import java.net.SocketException
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.Toast
import com.example.zimarix_1.zimarix_global.Companion.devices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
fun get_device_mac(context: Context): String {
    /*val interfacesList: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
    interfacesList.forEach {
        if (it.displayName == "wlan0") {
            val address = it.hardwareAddress
            val dev_mac = StringBuilder()
            for (b in address) {
                //res1.append(Integer.toHexString(b & 0xFF) + ":");
                dev_mac.append(String.format("%02X", b))
            }
            return dev_mac.toString()
        }
    }
     */
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val serialNumber = Build.SERIAL
    val model = Build.MODEL
    val manufacturer = Build.MANUFACTURER

    val combinedInfo = "$androidId$serialNumber$model$manufacturer"

    return sha256(combinedInfo)
}

fun getRandomString(length: Int) : String {
    val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { charset.random() }
        .joinToString("")
}

fun getKey(): SecretKey {
    val keystore = KeyStore.getInstance("AndroidKeyStore")
    keystore.load(null)

    val secretKeyEntry = keystore.getEntry("MyKeyAlias", null) as KeyStore.SecretKeyEntry
    return secretKeyEntry.secretKey
}

fun decryptData(ivBytes: ByteArray, data: ByteArray): String{
    val cipher = Cipher.getInstance("AES/CBC/NoPadding")
    val spec = IvParameterSpec(ivBytes)

    cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
    return cipher.doFinal(data).toString(Charsets.UTF_8).trim()
}
fun load_app_id_and_key(prefs : SharedPreferences):Int{
    val SEncKey = prefs.getString("key", "No data")
    val SEncIv = prefs.getString("iv", "No iv")
    if (SEncKey != "No data" || SEncIv != "No iv") {
        val BEncKey = Base64.decode(SEncKey, Base64.DEFAULT);
        val BEncIv = Base64.decode(SEncIv, Base64.DEFAULT);
        val idkey = decryptData(BEncIv, BEncKey)
        appid = idkey.split(",")[1]
        appkey = idkey.split(",")[0]
        Log.d("debug ", " ======  ff $appkey  $appid")
        return 0
    }
    return -1;
}

/////////////////////////////////////////////////////////////////////////





private fun monitor_socket(
    it: device,
    inputStream: InputStream,
    handler: Handler,
    mainActivity: MainActivity, ) {
    val bufferSize = 1024 * 50
    val buffer = ByteArray(bufferSize)
    var bytesRead: Int
    while (true) {
        try {
            bytesRead = inputStream.read(buffer)
            if (bytesRead > 0) {
                val msg1 = aes_decrpt(it.key, it.iv, buffer.copyOf(bytesRead))
                handler.post {
                    //Toast.makeText(this@MainActivity, msg1, Toast.LENGTH_SHORT).show()
                    showToast(msg1, mainActivity)
                }
                Log.d("debug ", " -------got  = ${zimarix_global.msg}")
            }
        }catch (t: SocketException){
            it.client!!.close()
            it.client = null
            it.iv = ""
            break
        }
    }
}
private var toast: Toast? = null
private fun showToast(message: String,context: Context) {
    // Cancel previous toast if it exists
    toast?.cancel()

    // Create a new toast
    toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)

    // Show the toast
    toast?.show()
}
//////////////////////////////////////////////////////////////////////////
suspend fun ec_req(req: String):String{
    var resp = "OK"
    val enc_req = aes_encrpt(appkey, appiv, req)
    try {
        ecsock!!.outputStream.write(enc_req)
    }catch (t: Throwable){
        return ""
    }
    return resp
}


///////////////////////////////////////////////////////////////////////////
interface update_params {
    var progressBar: ProgressBar
    var aToast: Toast?
    var isTaskInProgress:Boolean
    var active:Boolean
}

val port_1_8 = arrayOf("light","Switch","fan")
val default_port_type = arrayOf("light", "Switch",)
val ir_port_type = arrayOf("AC","TV","IR SWITCH","SMART LIGHT","SMART FAN", "MUSIC PLAYER")
data class ir_button_params(var id: Int, var name: String)
data class ir_button(var name: String, var bt:Button)
data class sw_params(var id:String, var name: String, var type:Int, var did:Int,var idx:Int,
                     var on:String, var off:String, var switchState: Boolean = false, var powersave: String = "0",
                     var visibility: Int = View.VISIBLE, var active: Int = 0,)
val GSwitches = mutableListOf<sw_params>()
lateinit var GSwitch_adapter: SwitchAdapter
lateinit var deviceConfigAdapter: DeviceConfigAdapter

data class device_config(var led_brightness: Int = 0,

                         var mic_state: String = "0",
                         var volume: Int= 0,

                         var screen: String = "0",

                         var ps_enable: String = "0",
                         var ps_auto_on: String = "0",
                         var ps_timeout: String= "0",

                         var alexa: String = "0",
                         var alexa_status: String = "Loading",

                         var monitor: String = "0")
data class device(var id: Int, //DID of device
                  var key: String,  // device appkey
                  var iv:String,    // device id
                  var ip:String,    // device ip
                  var port: Short,  // port states
                  var client:Socket? = null,
                  var inputStream: InputStream,
                  var lock:Any,
                  var deviceConfig: device_config)

class zimarix_global {
    companion object {
        var config_watchdog = 1
        var app_state:Int = 0
        var dev_mac = ""
        var appid = ""
        var msg = ""
        var appkey = ""
        var appiv = ""
        var zimarix_server = "ec2-3-111-217-243.ap-south-1.compute.amazonaws.com"
        var dev_config: List<String> = ArrayList()

        var ecsock: Socket? = null
        var ecinputStream: InputStream? = null
        val devices = ArrayList<device>()

        var ipver = "-1"
        var keyver = "-1"
        var portver = "-1"
        var monver = "-1"
    }
}