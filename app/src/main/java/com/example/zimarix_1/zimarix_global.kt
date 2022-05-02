package com.example.zimarix_1

import android.app.Notification
import android.app.NotificationManager
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.example.zimarix_1.zimarix_global.Companion.appid
import com.example.zimarix_1.zimarix_global.Companion.appkey
import com.example.zimarix_1.zimarix_global.Companion.config_watchdog
import com.example.zimarix_1.zimarix_global.Companion.controller_devices
import com.example.zimarix_1.zimarix_global.Companion.controller_ids
import com.example.zimarix_1.zimarix_global.Companion.controller_ips
import com.example.zimarix_1.zimarix_global.Companion.controller_keys
import com.example.zimarix_1.zimarix_global.Companion.controller_names
import com.example.zimarix_1.zimarix_global.Companion.ip_conf_ver
import com.example.zimarix_1.zimarix_global.Companion.key_conf_ver
import java.lang.StringBuilder
import java.net.NetworkInterface
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun get_device_mac(): String {
    val interfacesList: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
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
    return ""
}

fun AES_encrpt(key: String, data: String):ByteArray{
    val keyBytes = key.toByteArray(charset("UTF8"))
    val skey = SecretKeySpec(keyBytes, "AES")
    val ser_IV = "abcdefghijklmnop"
    val spec = IvParameterSpec(ser_IV.toByteArray())
    val AEScipher = Cipher.getInstance("AES/CBC/NoPadding")
    AEScipher.init(Cipher.ENCRYPT_MODE, skey, spec)
    var padlen = 0
    if(data.length % 16 != 0)
        padlen = 16 - (data.length % 16)
    val padstr = data.padEnd(data.length + padlen, ',')
    val enc_data: ByteArray = AEScipher.doFinal(padstr.toByteArray(Charsets.UTF_8))
    return enc_data
}

fun AES_decrpt(key: String, data: ByteArray):String{
    if(data.size % 16 != 0)
        return ""
    val keyBytes = key.toByteArray(charset("UTF8"))
    val skey = SecretKeySpec(keyBytes, "AES")
    val ser_IV = "abcdefghijklmnop"
    val spec = IvParameterSpec(ser_IV.toByteArray())

    val AEScipher = Cipher.getInstance("AES/CBC/NoPadding")
    AEScipher.init(Cipher.DECRYPT_MODE, skey, spec)
    val dec_data = AEScipher.doFinal(data).toString(Charsets.UTF_8)
    return dec_data
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

fun load_app_config(prefs : SharedPreferences):Int{
    val devids = prefs.getString("controller_ids", "No data")
    if (devids != null && devids != "No data") {
        controller_ids = devids.split(",").toMutableList()
    }

    val devnames = prefs.getString("controller_names", "No data")
    if (devnames != null && devnames != "No data") {
        controller_names = devnames.split(",").toMutableList()
    }

    val devips= prefs.getString("controller_ips", "No data")
    if (devips != null && devips != "No data") {
        controller_ips = devips.split(",").toMutableList()
    }
    val devkeys = prefs.getString("controller_keys", "No data")
    if (devkeys != null && devkeys != "No data") {
        controller_keys = devkeys.split(",").toMutableList()
    }

    controller_ids.forEachIndexed { index, s ->
        controller_devices.add("")
    }

    ip_conf_ver = prefs.getString("ip_conf_ver", "0").toString()
    key_conf_ver = prefs.getString("key_conf_ver", "0").toString()
    return 0;
}

fun save_ctlr_config(editor : SharedPreferences.Editor){
    var devids = ""
    var i = 0
    controller_ids.forEach {
        if(i == 0)
            devids = devids + it
        else
            devids = devids + "," + it
        i = i + 1
    }
    editor.putString("controller_ids", devids)

    var devnames = ""
    i = 0
    controller_names.forEach {
        if(i == 0)
            devnames = devnames + it
        else
            devnames = devnames + "," + it
        i = i + 1
    }
    editor.putString("controller_names", devnames)

    var devips = ""
    i = 0
    controller_ips.forEach {
        if(i == 0) {
            devips = devips + it
        }else {
            devips = devips + "," + it
        }
        i = i + 1
    }
    editor.putString("controller_ips", devips)

    var devkeys = ""
    i = 0
    controller_keys.forEach {
        if(i == 0)
            devkeys = devkeys + it
        else
            devkeys = devkeys + "," + it
        i = i + 1
    }
    editor.putString("controller_keys", devkeys)

    editor.apply()
}

class zimarix_global {
    companion object {
        var config_watchdog = 1

        var app_state:Int = 0
        var dev_mac = ""
        var appid = ""
        var appkey = ""
        //var zimarix_server = "192.168.100.102"
        var zimarix_server = "ec2-54-74-225-50.eu-west-1.compute.amazonaws.com"

        var ip_conf_ver = ""
        var key_conf_ver = ""
        var controller_names: MutableList<String> = ArrayList()
        var controller_ids: MutableList<String> = ArrayList()
        var controller_ips: MutableList<String> = ArrayList()
        var controller_keys: MutableList<String> = ArrayList()
        var controller_devices: MutableList<String> = ArrayList()
        var dev_config: List<String> = ArrayList()
        var curr_device : Int = -1

        lateinit var notificationManager: NotificationManagerCompat
        val channelId = "Progress Notification" as String

    }
}