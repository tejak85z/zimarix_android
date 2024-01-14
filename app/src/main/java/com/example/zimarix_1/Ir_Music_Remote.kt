package com.example.zimarix_1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Ir_Music_Remote : AppCompatActivity() , update_params{
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