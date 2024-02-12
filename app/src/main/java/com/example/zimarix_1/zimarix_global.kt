package com.example.zimarix_1
import java.io.InputStream
import java.net.Socket
import java.util.*
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
interface update_params {
    var progressBar: ProgressBar
    var aToast: Toast?
    var isTaskInProgress:Boolean
    var active:Boolean
}

val dev_action = arrayOf("ON","OFF")
val dev_action1 = arrayOf("ON","OFF","FLIP")
val port_1_8 = arrayOf("light","Switch","fan")
val default_port_type = arrayOf("light", "Switch",)
val ir_port_type = arrayOf("AC","TV","IR SWITCH","SMART LIGHT","SMART FAN", "MUSIC PLAYER")
data class ir_button_params(var id: Int, var name: String)
data class ir_button(var name: String, var bt:Button)
data class sw_params(var id:String, var name: String, var type:Int, var did:Int,var idx:Int,
                     var on:String, var off:String, var switchState: Boolean = false, var powersave: String = "0",
                     var visibility: Int = View.VISIBLE, var active: Int = 0,var params: List<String> = emptyList())
val GSwitches = mutableListOf<sw_params>()
lateinit var GSwitch_adapter: SwitchAdapter
data class device_config(var led_brightness: Int = 0,
                         var auto_brightness: String = "1",
                         var mic_state: String = "0",
                         var volume: Int= 0,
                         var screen: String = "0")
data class device(var id: Int, //DID of device
                  var key: String,  // device appkey
                  var iv:String,    // device iv
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
        var appkey = ""
        var appiv = ""
        var zimarix_server = "ec2-3-111-217-243.ap-south-1.compute.amazonaws.com"

        var ecsock: Socket? = null
        var ecinputStream: InputStream? = null
        var serv_sync:Int = 1
        val devices = ArrayList<device>()

        var ipver = "-1"
        var keyver = "-1"
        var portver = "-1"
        var monver = "-1"
    }
}