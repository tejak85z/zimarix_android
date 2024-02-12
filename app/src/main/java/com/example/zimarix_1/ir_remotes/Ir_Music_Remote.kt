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

class Ir_Music_Remote : AppCompatActivity() , update_params {
    private var dev_idx = -1
    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true
    val ir_strings = mutableListOf<ir_button_params>()
    val ir_buttons = mutableListOf<ir_button>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ir_music_remote)

        val b = intent.extras
        if (b != null) dev_idx = b.getInt("key")

        progressBar = findViewById(R.id.muprogressBar)
        progressBar.visibility = View.GONE

        ir_strings.add(ir_button_params(R.id.mu_on, "ON"))
        ir_strings.add(ir_button_params(R.id.mu_off, "OFF"))
        ir_strings.add(ir_button_params(R.id.mu_up, "UP"))
        ir_strings.add(ir_button_params(R.id.mu_down, "DOWN"))
        ir_strings.add(ir_button_params(R.id.mu_right, "RIGHT"))
        ir_strings.add(ir_button_params(R.id.mu_left, "LEFT"))
        ir_strings.add(ir_button_params(R.id.mu_ok, "SELECT"))
        ir_strings.add(ir_button_params(R.id.mu_mute, "MUTE"))
        ir_strings.add(ir_button_params(R.id.mu_vol_down, "VOLDOWN"))
        ir_strings.add(ir_button_params(R.id.mu_vol_up, "VOLUP"))
        ir_strings.add(ir_button_params(R.id.mu_pause, "Pause"))
        ir_strings.add(ir_button_params(R.id.mu_play, "Play"))
        ir_strings.add(ir_button_params(R.id.mu_fastfarword, "FastForward"))
        ir_strings.add(ir_button_params(R.id.mu_rewind, "Rewind"))
        ir_strings.add(ir_button_params(R.id.mu_previous, "Previous"))
        ir_strings.add(ir_button_params(R.id.mu_startover, "StartOver"))
        ir_strings.add(ir_button_params(R.id.mu_stop, "Stop"))
        ir_strings.add(ir_button_params(R.id.mu_next, "Next"))
        ir_strings.add(ir_button_params(R.id.mu_pageright, "PAGE_RIGHT"))
        ir_strings.add(ir_button_params(R.id.mu_pageleft, "PAGE_LEFT"))
        ir_strings.add(ir_button_params(R.id.mu_pageup, "PAGE_UP"))
        ir_strings.add(ir_button_params(R.id.mu_pagedown, "PAGE_DOWN"))
        ir_strings.add(ir_button_params(R.id.mu_info, "INFO"))
        ir_strings.add(ir_button_params(R.id.mu_more, "MORE"))

        ir_strings.forEach {
            val irbtn = ir_button(
                it.name,
                create_ir_switch(findViewById<Button>(it.id),it.name, dev_idx, this,this)
            )
            ir_buttons.add(irbtn)
        }

        CoroutineScope(Dispatchers.IO).launch {
            ir_updater(dev_idx, ir_buttons, this@Ir_Music_Remote)
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