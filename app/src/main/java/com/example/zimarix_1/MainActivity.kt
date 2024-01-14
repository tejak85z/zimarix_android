package com.example.zimarix_1

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.zimarix_global.Companion.app_state
import com.example.zimarix_1.zimarix_global.Companion.config_watchdog
import com.example.zimarix_1.zimarix_global.Companion.devices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlin.experimental.and

class MainActivity : AppCompatActivity() {

    var active:Int = 1
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var recyclerView: RecyclerView
    private val viewhandler = Handler()

    internal lateinit var progressBar: ProgressBar
    internal var aToast: Toast? = null
    var isTaskInProgress = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar1)
        progressBar.visibility = View.GONE

        recyclerView = findViewById(R.id.recyclerView)
        GSwitch_adapter = SwitchAdapter(GSwitches, this)
        recyclerView.adapter = GSwitch_adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        if(config_watchdog == 1) {
            config_watchdog = 0
            val prefs = getSharedPreferences(getString(R.string.dev_encryption_key), MODE_PRIVATE)

            CoroutineScope(IO).launch {
                server_handler(this@MainActivity)
            }
            CoroutineScope(IO).launch {
                device_handler(handler, this@MainActivity)
            }
        }

        val add_controller = findViewById<Button>(R.id.add_controller)
        add_controller.setOnClickListener {
            val intent = Intent(this,addnewcontroller::class.java)
            startActivity(intent)
        }
        val view_controller = findViewById<Button>(R.id.view_controller)
        view_controller.setOnClickListener {
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            devices.forEachIndexed { index, device ->
                val button = Button(this)
                button.text = "device_"+device.id.toString()
                layout.addView(button)
                button.setOnClickListener(){
                    if(device.client != null) {
                        val intent = Intent(this, DevConfig::class.java)
                        val b = Bundle()
                        b.putInt("key", index) //Your id
                        intent.putExtras(b)
                        startActivity(intent)
                    }else{
                        Toast.makeText(this@MainActivity, "Device Not Reachable", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            layout.setPadding(50, 40, 50, 10)

            val builder = AlertDialog.Builder(this)
                .setTitle("Controllers")
                .setView(layout)
            val dialog = builder.create()
            dialog.show()
        }

        val stream = findViewById<Button>(R.id.stream)
        stream.setOnClickListener {
            val sintent = Intent(this,livestream::class.java)
            startActivity(sintent)
        }
        val backgroundThread = Thread {
            var update = 0
            while (true) {
                viewhandler.post {
                    // for each switch
                    update = 0
                    GSwitches.forEach{
                        // for each physical switch
                        if (it.id.toInt() < 32) {
                            val i = it.id.toInt() - 1
                            val mask = (1 shl i).toShort()
                            val flag = (devices[it.idx].port and mask).toInt()
                            // update port if not used recently
                            if (it.active == 0) {
                                if (it.switchState != (flag != 0)) {
                                    it.switchState = flag != 0
                                    update = 1
                                }
                            } else {
                                if (flag == 0) {
                                    if (!it.switchState) {
                                        it.active = 0
                                    }
                                } else {
                                    if (it.switchState) {
                                        it.active = 0
                                    }
                                }
                                if (it.active != 0)
                                    it.active = it.active - 1
                            }
                        }
                    }
                    if(update == 1)
                        GSwitch_adapter.update()
                }
                Thread.sleep(1000)
            }
        }
        // Start the background thread
        backgroundThread.start()

        val radioGroup: RadioGroup = findViewById(R.id.radio_group)
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedRadioButton: RadioButton = findViewById(checkedId)
            val selectedOption = selectedRadioButton.text.toString()
            if (selectedOption == "Show All"){
                GSwitch_adapter.show_all()
            }else if (selectedOption == "On Switches"){
                GSwitch_adapter.active_switches()
            }else if (selectedOption == "IR Switches"){
                GSwitch_adapter.show_ir_remotes()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        app_state = 1
        active = 1
    }
    override fun onPause() {
        super.onPause()
        app_state = 0
        active = 0
    }
}
