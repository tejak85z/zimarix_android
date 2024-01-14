package com.example.zimarix_1

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Ir_Tv_Remote : AppCompatActivity(), update_params {
    private lateinit var recyclerView: RecyclerView
    lateinit var irAdapter: IrSwitchAdapter
    private var dev_idx = -1

    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    val ir_strings = mutableListOf<ir_button_params>()
    val ir_buttons = mutableListOf<ir_button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ir_tv_remote)

        val b = intent.extras
        if (b != null) dev_idx = b.getInt("key")

        progressBar = findViewById(R.id.tvprogressBar)
        progressBar.visibility = View.GONE

        ir_strings.add(ir_button_params(R.id.tv_on, "ON"))
        ir_strings.add(ir_button_params(R.id.tv_off, "OFF"))
        ir_strings.add(ir_button_params(R.id.tv_1, "1"))
        ir_strings.add(ir_button_params(R.id.tv_2, "2"))
        ir_strings.add(ir_button_params(R.id.tv_3, "3"))
        ir_strings.add(ir_button_params(R.id.tv_4, "4"))
        ir_strings.add(ir_button_params(R.id.tv_5, "5"))
        ir_strings.add(ir_button_params(R.id.tv_6, "6"))
        ir_strings.add(ir_button_params(R.id.tv_7, "7"))
        ir_strings.add(ir_button_params(R.id.tv_8, "8"))
        ir_strings.add(ir_button_params(R.id.tv_9, "9"))
        ir_strings.add(ir_button_params(R.id.tv_0, "0"))

        ir_strings.add(ir_button_params(R.id.tv_chan_up, "CHANUP"))
        ir_strings.add(ir_button_params(R.id.tv_chan_down, "CHANDOWN"))

        ir_strings.add(ir_button_params(R.id.tv_vol_down, "VOLDOWN"))
        ir_strings.add(ir_button_params(R.id.tv_vol_up, "VOLUP"))
        ir_strings.add(ir_button_params(R.id.tv_mute, "MUTE"))

        ir_strings.add(ir_button_params(R.id.tv_up, "UP"))
        ir_strings.add(ir_button_params(R.id.tv_down, "DOWN"))
        ir_strings.add(ir_button_params(R.id.tv_right, "RIGHT"))
        ir_strings.add(ir_button_params(R.id.tv_left, "LEFT"))
        ir_strings.add(ir_button_params(R.id.tv_ok, "SELECT"))

        ir_strings.add(ir_button_params(R.id.tv_hdmi_1, "HDMI 1"))
        ir_strings.add(ir_button_params(R.id.tv_hdmi_2, "HDMI 2"))
        ir_strings.add(ir_button_params(R.id.tv_usb, "USB"))
        ir_strings.add(ir_button_params(R.id.tv_tv, "TV"))
        ir_strings.add(ir_button_params(R.id.tv_more, "MORE"))
        ir_strings.add(ir_button_params(R.id.tv_info, "INFO"))

        ir_strings.forEach {
            val irbtn = ir_button(
                it.name,
                create_ir_switch(findViewById<Button>(it.id),it.name, dev_idx, this,this)
            )
            ir_buttons.add(irbtn)
        }

        CoroutineScope(Dispatchers.IO).launch {
            ir_updater(dev_idx, ir_buttons, this@Ir_Tv_Remote)
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


