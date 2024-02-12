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

class Ir_Ac_Remote : AppCompatActivity() , update_params {
    private var dev_idx = -1
    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    val ir_strings = mutableListOf<ir_button_params>()
    val ir_buttons = mutableListOf<ir_button>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ir_ac_remote)

        val b = intent.extras
        if (b != null) dev_idx = b.getInt("key")

        progressBar = findViewById(R.id.acprogressBar)
        progressBar.visibility = View.GONE

        ir_strings.add(ir_button_params(R.id.ac_on, "ON"))
        ir_strings.add(ir_button_params(R.id.ac_off, "OFF"))
        ir_strings.add(ir_button_params(R.id.ac_16, "16"))
        ir_strings.add(ir_button_params(R.id.ac_17, "17"))
        ir_strings.add(ir_button_params(R.id.ac_18, "18"))
        ir_strings.add(ir_button_params(R.id.ac_19, "19"))
        ir_strings.add(ir_button_params(R.id.ac_20, "20"))
        ir_strings.add(ir_button_params(R.id.ac_21, "21"))
        ir_strings.add(ir_button_params(R.id.ac_22, "22"))
        ir_strings.add(ir_button_params(R.id.ac_23, "23"))
        ir_strings.add(ir_button_params(R.id.ac_24, "24"))
        ir_strings.add(ir_button_params(R.id.ac_25, "25"))
        ir_strings.add(ir_button_params(R.id.ac_26, "26"))
        ir_strings.add(ir_button_params(R.id.ac_27, "27"))
        ir_strings.add(ir_button_params(R.id.ac_28, "28"))
        ir_strings.add(ir_button_params(R.id.ac_29, "29"))
        ir_strings.add(ir_button_params(R.id.ac_30, "30"))
        ir_strings.add(ir_button_params(R.id.ac_31, "31"))
        ir_strings.add(ir_button_params(R.id.ac_32, "32"))
        ir_strings.add(ir_button_params(R.id.ac_auto, "AUTO"))
        ir_strings.add(ir_button_params(R.id.ac_cool, "COOL"))
        ir_strings.add(ir_button_params(R.id.ac_eco, "ECO"))
        ir_strings.add(ir_button_params(R.id.ac_heat, "HEAT"))


        ir_strings.forEach {
            val irbtn = ir_button(
                it.name,
                create_ir_switch(findViewById<Button>(it.id),it.name, dev_idx, this,this)
            )
            ir_buttons.add(irbtn)
        }

        CoroutineScope(Dispatchers.IO).launch {
            ir_updater(dev_idx, ir_buttons, this@Ir_Ac_Remote)
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