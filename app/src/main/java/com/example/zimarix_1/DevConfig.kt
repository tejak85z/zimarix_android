package com.example.zimarix_1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.zimarix_global.Companion.devices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DevConfig : AppCompatActivity(), update_params{

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

        // Assuming you have a list of device configs
        recyclerView = findViewById(R.id.settingsrecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        deviceConfigAdapter = DeviceConfigAdapter(value, this)
        recyclerView.adapter = deviceConfigAdapter

        CoroutineScope(Dispatchers.IO).launch {
            config_updater(value, this@DevConfig)
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