package com.example.zimarix_1.Activities

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.R
import com.example.zimarix_1.Send_cmd
import com.example.zimarix_1.dev_req
import com.example.zimarix_1.update_params
import com.example.zimarix_1.zimarix_global
import com.example.zimarix_1.zimarix_global.Companion.devices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DevConfig : AppCompatActivity(), update_params {
    lateinit var deviceConfigAdapter: DeviceConfigAdapter
    private lateinit var recyclerView: RecyclerView
    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dev_config)

        progressBar = findViewById(R.id.configprogressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        var value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        CoroutineScope(Dispatchers.IO).launch {
            config_updater(value,this@DevConfig)
        }

        var switches: Button = findViewById(R.id.io)
        switches.setOnClickListener {
            if(devices[value].client != null) {
                val intent = Intent(this, io_switches::class.java)
                intent.putExtras(idx)
                startActivity(intent)
            }else{
                Toast.makeText(this@DevConfig, "Device Not Reachable", Toast.LENGTH_SHORT).show()
            }
        }
        var remotes: Button = findViewById(R.id.ir)
        remotes.setOnClickListener {
            if(devices[value].client != null) {
                val intent = Intent(this, ir_switches::class.java)
                intent.putExtras(idx)
                startActivity(intent)
            }else{
                Toast.makeText(this@DevConfig, "Device Not Reachable", Toast.LENGTH_SHORT).show()
            }
        }

        var clusters: Button = findViewById(R.id.cls)
        clusters.setOnClickListener {
            if(devices[value].client != null) {
                val intent = Intent(this, cluster_switches::class.java)
                intent.putExtras(idx)
                startActivity(intent)
            }else{
                Toast.makeText(this@DevConfig, "Device Not Reachable", Toast.LENGTH_SHORT).show()
            }
        }

        var schedules: Button = findViewById(R.id.schedules)
        schedules.setOnClickListener {
            if(devices[value].client != null) {
                val intent = Intent(this, scheduler::class.java)
                intent.putExtras(idx)
                startActivity(intent)
            }else{
                Toast.makeText(this@DevConfig, "Device Not Reachable", Toast.LENGTH_SHORT).show()
            }
        }

        val powersave_btn:Button = findViewById(R.id.powersave)
        powersave_btn.setOnClickListener {
            val intent = Intent(this, powersave::class.java)
            intent.putExtras(idx)
            startActivity(intent)
        }
        val wakeword_btn:Button = findViewById(R.id.wakeword)
        wakeword_btn.setOnClickListener {
            val intent = Intent(this, wakeword::class.java)
            intent.putExtras(idx)
            startActivity(intent)
        }
        val alexa_btn:Button = findViewById(R.id.alexa)
        alexa_btn.setOnClickListener {
            val intent = Intent(this, Alexa::class.java)
            intent.putExtras(idx)
            startActivity(intent)
        }
        val bt:Button = findViewById(R.id.bluetooth)
        bt.setOnClickListener {
            val intent = Intent(this, btspeaker::class.java)
            intent.putExtras(idx)
            startActivity(intent)
        }
        val cam:Button = findViewById(R.id.camera)
        cam.setOnClickListener {
            val intent = Intent(this, camsettings::class.java)
            intent.putExtras(idx)
            startActivity(intent)
        }
        val wifi_btn:Button = findViewById(R.id.wifi)
        wifi_btn.setOnClickListener {
            val intent = Intent(this, wifi::class.java)
            intent.putExtras(idx)
            startActivity(intent)
        }

        val settings_btn:Button = findViewById(R.id.system_config)
        settings_btn.setOnClickListener {
            val intent = Intent(this, DeviceSettings::class.java)
            intent.putExtras(idx)
            startActivity(intent)
        }

        val monitor_btn:Button = findViewById(R.id.monitor_cfg)
        monitor_btn.setOnClickListener {
            val intent = Intent(this, MonitorActivity::class.java)
            intent.putExtras(idx)
            startActivity(intent)
        }
        // Assuming you have a list of device configs
        recyclerView = findViewById(R.id.settingsrecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        deviceConfigAdapter = DeviceConfigAdapter(value, this)
        recyclerView.adapter = deviceConfigAdapter

        CoroutineScope(Dispatchers.IO).launch {
            config_updater(value, this@DevConfig)
        }
    }

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
                    if (zimarix_global.devices[devindex].deviceConfig.auto_brightness != param[index + 1]) {
                        zimarix_global.devices[devindex].deviceConfig.auto_brightness = param[index + 1]
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

    override fun onResume() {
        super.onResume()
        active = true
    }
    override fun onPause() {
        super.onPause()
        active = false
    }
}



class DeviceConfigAdapter(private var index: Int, private val activity: DevConfig)
    : RecyclerView.Adapter<DeviceConfigAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val led_brightness: SeekBar = itemView.findViewById(R.id.led_brightness)
        val auto_brightness: CheckBox = itemView.findViewById(R.id.auto_brightness)
        val mic: ImageView = itemView.findViewById(R.id.mic_state)
        val volume: SeekBar = itemView.findViewById(R.id.volume)
        val screen: Switch = itemView.findViewById(R.id.screen)
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
        holder.auto_brightness.isChecked = config.auto_brightness == "1"
        holder.auto_brightness.setOnClickListener(){
            if (holder.auto_brightness.isChecked)
                Send_cmd(index,"CFG,LED,SET,LED_AUTO_ENABLE,1,", activity).execute()
            else
                Send_cmd(index,"CFG,LED,SET,LED_AUTO_ENABLE,0,", activity).execute()
        }

        if (config.mic_state == "1") {
            holder.mic.setBackgroundColor(Color.parseColor("#46B927"))
            holder.mic.setImageResource(android.R.drawable.presence_audio_online)
        }else{
            holder.mic.setBackgroundColor(Color.parseColor("#873232"))
            holder.mic.setImageResource(android.R.drawable.stat_notify_call_mute)
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
    }
    fun update() {
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int = 1
}