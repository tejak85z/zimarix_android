package com.example.zimarix_1.ir_remotes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.example.zimarix_1.R
import com.example.zimarix_1.Activities.create_ir_switch
import com.example.zimarix_1.ir_button
import com.example.zimarix_1.ir_button_params
import com.example.zimarix_1.Activities.ir_updater
import com.example.zimarix_1.update_params
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Ir_Smart_Light_Remote : AppCompatActivity() , update_params {
    private var dev_idx = -1
    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true
    val ir_strings = mutableListOf<ir_button_params>()
    val ir_buttons = mutableListOf<ir_button>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ir_smart_light_remote)

        val b = intent.extras
        if (b != null) dev_idx = b.getInt("key")

        progressBar = findViewById(R.id.liprogressBar)
        progressBar.visibility = View.GONE

        ir_strings.add(ir_button_params(R.id.li_on, "ON"))
        ir_strings.add(ir_button_params(R.id.li_off, "OFF"))
        ir_strings.add(ir_button_params(R.id.li_blue, "blue"))
        ir_strings.add(ir_button_params(R.id.li_green, "green"))
        ir_strings.add(ir_button_params(R.id.li_orange, "orange"))
        ir_strings.add(ir_button_params(R.id.li_pink, "pink"))
        ir_strings.add(ir_button_params(R.id.li_purple, "purple"))
        ir_strings.add(ir_button_params(R.id.li_red, "red"))
        ir_strings.add(ir_button_params(R.id.li_skyblue, "skyblue"))
        ir_strings.add(ir_button_params(R.id.li_violet, "violet"))
        ir_strings.add(ir_button_params(R.id.li_white, "white"))
        ir_strings.add(ir_button_params(R.id.li_yellow, "yellow"))
        ir_strings.add(ir_button_params(R.id.li_brightnessdown, "BDOWN"))
        ir_strings.add(ir_button_params(R.id.li_brightnessup, "BUP"))

        ir_strings.forEach {
            val irbtn = ir_button(
                it.name,
                create_ir_switch(findViewById<Button>(it.id),it.name, dev_idx, this,this)
            )
            ir_buttons.add(irbtn)
        }

        CoroutineScope(Dispatchers.IO).launch {
            ir_updater(dev_idx, ir_buttons, this@Ir_Smart_Light_Remote)
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