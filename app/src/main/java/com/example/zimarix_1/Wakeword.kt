package com.example.zimarix_1

import android.app.AlertDialog
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.util.TypedValue
import android.widget.*
import com.example.zimarix_1.zimarix_global.Companion.curr_device
import org.w3c.dom.Text
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.net.SocketException

class Wakeword : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wakeword)

        //Log.d("debug ", " =====in WW activity ${zimarix_global.controller_ips[curr_device].length} ${zimarix_global.controller_ips[curr_device]}")

        var wake_words = arrayOf<String>()
        var wake_seq = arrayOf<String>()
        var active_devices = arrayOf<String>("SELECT DEVICE")
        var wwenable = 0
        var wwtimeout = "10"
        zimarix_global.dev_config.forEach{
            val row = it.split(",")
            Log.d("debug ", " ================================== $row")
            if (row[0] == "ww")
                wake_words = wake_words + it
            else if (row[0] == "wwseq") {
                if (row.size >= 3) {
                    var ww_ui = ww_display(row[2])
                    wake_seq = wake_seq + (row[1] + "  " + ww_ui)
                    if (row.size == 4)
                        ww_ui = ww_display(row[3])
                    wake_seq = wake_seq + (row[1] + "  " + ww_ui)
                }
            }
            else if (row[0] == "conf" && row[1] == "Offline Wake Word Config") {
                wwenable = row[2].toInt()
                wwtimeout = row[3]
            }
            else if (row[0] == "port" && row[3] == "1") {
                active_devices = active_devices + row[2]
            }
        }
        val wwenable_btn = findViewById<Switch>(R.id.switch1)
        wwenable_btn.isChecked = wwenable != 0
        Log.d("debug ", " ================================== $wwenable ${wwenable_btn.isChecked} ")
        wwenable_btn.setOnClickListener{
            if(wwenable_btn.isChecked == false)
                wwenable = 0
            else
                wwenable = 1
            val send_str = "U,conf,Offline Wake Word Config,enable,"+wwenable.toString()
            val resp = encrypt_and_send_data(send_str)
            Toast.makeText(this@Wakeword, resp, Toast.LENGTH_SHORT).show()
        }

        val ww_timeout = findViewById<EditText>(R.id.wwtimeout)
        ww_timeout.hint = wwtimeout + " Min 0 and Max 30"
        val ww_timeout_save_btn = findViewById<Button>(R.id.wwtimeoutsave)
        ww_timeout_save_btn.setOnClickListener{
            if(ww_timeout.text.length > 0) {
                val send_str = "U,conf,Offline Wake Word Config,timeout," + ww_timeout.text
                val resp = encrypt_and_send_data(send_str)
                Toast.makeText(this@Wakeword, resp, Toast.LENGTH_SHORT).show()
            }
        }

        val add_ww_btn = findViewById<Button>(R.id.addwakewordbutton)
        add_ww_btn.setOnClickListener{
            add_ww_dialog(active_devices,wake_words)
        }

        val listView = findViewById<ListView>(R.id.wwlist)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, wake_seq)
        listView.adapter = adapter

        listView.setOnItemClickListener { parent, view, position, id ->
        }
    }

    fun add_ww_dialog(active_devices: Array<String>, wake_words: Array<String>) {

        var wws = arrayOf<String>()
        wws = wws + "EMPTY"
        wake_words.forEach {
            val ww = it.split(",")[1]
            wws = wws + ww.replace("_raspberry-pi.ppn", "")
        }
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val device = active_devices
        val devtype = Spinner(this)
        devtype.adapter =
            this?.let { ArrayAdapter(it, android.R.layout.simple_spinner_dropdown_item, device) }
        //type.setSelection(port[4].toInt())
        layout.addView(devtype)

        val action = "SELECT ACTION,ON,OFF,TOGGLE".split(",")
        val type = Spinner(this)
        type.adapter =
            this?.let { ArrayAdapter(it, android.R.layout.simple_spinner_dropdown_item, action) }
        //type.setSelection(port[4].toInt())
        layout.addView(type)

        val txt = TextView(this)
        txt.setText("MAKE SEQUENCE FOR THIS DEVICE ACTION\n Empty fields will be ignored ")
        txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        txt.setTextColor(Color.BLUE)
        layout.addView(txt)

        val ww1 = wws

        var wwstype = arrayOf<Spinner>()

        for (i in 0..8){
            val wwtype = Spinner(this)
            wwtype.adapter = this?.let { ArrayAdapter(it, android.R.layout.simple_spinner_dropdown_item, ww1) }
            layout.addView(wwtype)
            wwstype = wwstype + wwtype
        }
        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(this)
            .setTitle("Add New Command Sequence")
            .setView(layout)
            .setPositiveButton("UPDATE", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
                dialog.dismiss()
            }
        val dialog = builder.create()

        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                if(devtype.selectedItem == "SELECT DEVICE"){
                    Toast.makeText(this, "Select a device", Toast.LENGTH_SHORT).show()
                }else if (type.selectedItem == "SELECT ACTION"){
                    Toast.makeText(this, "Select an action for this device", Toast.LENGTH_SHORT).show()
                } else {
                    var wwseq = "U,portww,"+devtype.selectedItem+","+type.selectedItem+","
                    wwstype.forEach {
                        if(it.selectedItem != "EMPTY"){
                            wwseq = wwseq+it.selectedItem+"_raspberry-pi.ppn+"
                        }
                    }
                    Toast.makeText(this, wwseq, Toast.LENGTH_SHORT).show()
                    encrypt_and_send_data(wwseq)
                }

              /*
                if(enable.isChecked == true && i == 0){
                    Toast.makeText(context, "Select Port type", Toast.LENGTH_SHORT).show()
                }else {
                    val send_str =
                        "U," + port[0] + "," + port[1] + "," + portname + "," + portenable + "," + i.toString() + "," + PS
                    Toast.makeText(context, "port params " + send_str, Toast.LENGTH_SHORT).show()
                    val resp = encrypt_and_send_data(send_str)
                    Toast.makeText(context, resp, Toast.LENGTH_SHORT).show()
                    if(resp == "OK")
                        dialog.dismiss()
                }

                 */
            }

        }
        dialog.show()
    }

    fun ww_display(data: String): String{
        var ret = ""

        val dver1 = data.split(":")
        if(dver1.size != 2 || !(dver1[0] == "ON" || dver1[0] == "OFF"))
            return ""

        val action = dver1[0]
        val wwlist = dver1[1].split("+")
        ret = ret + " "+action + " : "
        wwlist.forEach(){
            ret = ret + it.replace("_raspberry-pi.ppn", "") + " + "
        }
        return ret
    }

    fun encrypt_and_send_data(data : String): String {
        var resp = "FAIL"
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            if(zimarix_global.controller_ips[curr_device] == "0" || zimarix_global.controller_ips[curr_device].length < 7){
                return "INVALID CONFIG"
            }
            val client = Socket(zimarix_global.controller_ips[curr_device], 20009)
            val enc_probe = AES_encrpt(zimarix_global.controller_keys[curr_device],data)
            client!!.outputStream.write(enc_probe)
            val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
            resp = bufferReader.readLine()
            client.close()
        }catch (t: SocketException){

        }
        return resp
    }
}