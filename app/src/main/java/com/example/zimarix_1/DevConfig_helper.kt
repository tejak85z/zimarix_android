package com.example.zimarix_1

import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.zimarix_global.Companion.devices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun update_config(devindex:Int): Int {
    var ret = 0
    val resp = dev_req(devindex, "SETTINGS,GET,")
    val param = resp.split(",")
    val len = param.size
    param.forEachIndexed { index, value ->
        if (value == "LED") {
            if (index + 2 <= len) {
                if (zimarix_global.devices[devindex].deviceConfig.led_brightness != param[index + 2].toInt()) {
                    zimarix_global.devices[devindex].deviceConfig.led_brightness = param[index + 2].toInt()
                    ret = 1
                }
            }
        }else if (value == "VOLUME") {
            if (index + 2 <= len) {
                if (zimarix_global.devices[devindex].deviceConfig.volume != param[index + 2].toInt()) {
                    zimarix_global.devices[devindex].deviceConfig.volume = param[index + 2].toInt()
                    ret = 1
                }
                if (zimarix_global.devices[devindex].deviceConfig.mic_state != param[index + 1]) {
                    zimarix_global.devices[devindex].deviceConfig.mic_state = param[index + 1]
                    ret = 1
                }
            }
        }else if (value == "SCREEN") {
            if (index + 1 <= len) {
                if (zimarix_global.devices[devindex].deviceConfig.screen != param[index + 1]) {
                    zimarix_global.devices[devindex].deviceConfig.screen = param[index + 1]
                    ret = 1
                }
            }
        }else if (value == "MONITOR") {
            if (index + 1 <= len) {
                if (zimarix_global.devices[devindex].deviceConfig.monitor != param[index + 1]) {
                    zimarix_global.devices[devindex].deviceConfig.monitor = param[index + 1]
                    ret = 1
                }
            }
        }else if (value == "POWER_SAVE") {
            if (index + 3 <= len) {
                if (zimarix_global.devices[devindex].deviceConfig.ps_enable != param[index + 1]) {
                    zimarix_global.devices[devindex].deviceConfig.ps_enable = param[index + 1]
                    ret = 1
                }
                if (zimarix_global.devices[devindex].deviceConfig.ps_auto_on != param[index + 2]) {
                    zimarix_global.devices[devindex].deviceConfig.ps_auto_on = param[index + 2]
                    ret = 1
                }
                if (zimarix_global.devices[devindex].deviceConfig.ps_timeout != param[index + 3]) {
                    zimarix_global.devices[devindex].deviceConfig.ps_timeout = param[index + 3]
                    ret = 1
                }
            }
        }
    }
    return ret
}

fun config_updater(index: Int, devConfig: DevConfig){
    var ret = 0
    while (true){
        if (devConfig.active == true) {
            ret = 0
            if (update_config(index) != 0) {
                ret = 1
            }
            if (ret == 1) {
                GlobalScope.launch(Dispatchers.Main) {
                    // Update UI here
                    deviceConfigAdapter.update()
                }
            }
        }
        Thread.sleep(1000)
    }
}

class DeviceConfigAdapter(private var index: Int, private val activity: DevConfig)
    : RecyclerView.Adapter<DeviceConfigAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val led_brightness: SeekBar = itemView.findViewById(R.id.led_brightness)
        val mic: TextView = itemView.findViewById(R.id.mic_state)
        val volume: SeekBar = itemView.findViewById(R.id.volume)
        val screen: Switch = itemView.findViewById(R.id.screen)
        val monitor: Switch = itemView.findViewById(R.id.monitor)
        val powersave:Switch = itemView.findViewById(R.id.powersave)
        val autoon:Switch = itemView.findViewById(R.id.powersave_autoon)
        val timeout:EditText = itemView.findViewById(R.id.timeout)
        val ok:Button = itemView.findViewById(R.id.ok)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_config, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val config = devices[index].deviceConfig

        holder.led_brightness.progress = config.led_brightness
        holder.led_brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val brightness = holder.led_brightness.progress.toString()
                Send_cmd(index,"CFG,LED,SET,BRIGHTNESS,"+ brightness +",", activity).execute()
            }
        })

        if (config.mic_state == "1") {
            holder.mic.text = "MIC : ENABLED"
        }else{
            holder.mic.text = "MIC : DISABLED"
        }

        holder.volume.progress = config.volume
        holder.volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val volume = holder.volume.progress.toString()
                Send_cmd(index,"IO,SET,VOLUME,VAL,"+ volume +",", activity).execute()
            }
        })
        holder.screen.isChecked = config.screen == "1"
        holder.screen.setOnClickListener {
            if (holder.screen.isChecked == true) {
                Send_cmd(index,"CFG,SCREEN,SET,ENABLE,1," , activity).execute()
            } else {
                Send_cmd(index,"CFG,SCREEN,SET,ENABLE,0," , activity).execute()
            }
        }
        holder.monitor.isChecked = config.monitor == "1"
        holder.monitor.setOnClickListener {
            if (holder.monitor.isChecked == true) {
                Send_cmd(index,"CFG,MONITOR,SET,1," , activity).execute()
            } else {
                Send_cmd(index,"CFG,MONITOR,SET,0," , activity).execute()
            }
        }

        holder.powersave.isChecked = config.ps_enable == "1"
        holder.powersave.setOnClickListener {
            if (holder.powersave.isChecked == true) {
                Send_cmd(index,"CFG,PS,SET,ENABLE,1," , activity).execute()
            } else {
                Send_cmd(index,"CFG,PS,SET,ENABLE,0," , activity).execute()
            }
        }
        holder.autoon.isChecked = config.ps_auto_on == "1"
        holder.autoon.setOnClickListener {
            if (holder.autoon.isChecked == true) {
                Send_cmd(index,"CFG,PS,SET,AUTO_ON,1," , activity).execute()
            } else {
                Send_cmd(index,"CFG,PS,SET,AUTO_ON,0," , activity).execute()
            }
        }
        holder.timeout.hint = config.ps_timeout
        holder.ok.setOnClickListener {
            val cmd = "CFG,PS,SET,TRIGGER_TIME," + holder.timeout.text +","
            Send_cmd(index,cmd, activity).execute()
        }

    }

    fun update() {
        Log.d("debug ", " --------------------------------calling update\n")
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = 1
}