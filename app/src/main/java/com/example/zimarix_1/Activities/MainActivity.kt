package com.example.zimarix_1.Activities

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clearAppData
import com.example.zimarix_1.GSwitch_adapter
import com.example.zimarix_1.GSwitches
import com.example.zimarix_1.R
import com.example.zimarix_1.SwitchAdapter
import com.example.zimarix_1.aes_encrpt
import com.example.zimarix_1.device_handler
import com.example.zimarix_1.server_handler
import com.example.zimarix_1.update_params
import com.example.zimarix_1.zimarix_global
import com.example.zimarix_1.zimarix_global.Companion.app_state
import com.example.zimarix_1.zimarix_global.Companion.config_watchdog
import com.example.zimarix_1.zimarix_global.Companion.devices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import showLogoutConfirmationDialog
import kotlin.experimental.and

class MainActivity : AppCompatActivity() , update_params{
    private val handler = Handler(Looper.getMainLooper())

    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    private lateinit var recyclerView: RecyclerView
    private val viewhandler = Handler()

    lateinit var otpdialog: AlertDialog
    lateinit var timerTextView: TextView // Declare the lateinit property for the timer TextView



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
            val intent = Intent(this, addnewcontroller::class.java)
            startActivity(intent)
        }
        val view_controller = findViewById<Button>(R.id.view_controller)
        view_controller.setOnClickListener {
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            devices.forEachIndexed { index, device ->
                val button = Button(this)
                button.text = "device_"+device.id.toString()
                if(device.client != null)
                    button.setBackgroundColor(Color.GREEN)
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
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            devices.forEachIndexed { index, device ->
                val button = Button(this)
                button.text = "device_"+device.id.toString()
                layout.addView(button)
                button.setOnClickListener(){
                    val intent = Intent(this, livestream::class.java)
                    val b = Bundle()
                    b.putInt("key", index) //Your id
                    intent.putExtras(b)
                    startActivity(intent)
                }
            }
            layout.setPadding(50, 40, 50, 10)

            val builder = AlertDialog.Builder(this)
                .setTitle("Controllers")
                .setView(layout)
            val dialog = builder.create()
            dialog.show()
        }

        val schedulerbtn = findViewById<Button>(R.id.scheduler)
        schedulerbtn.setOnClickListener {
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            devices.forEachIndexed { index, device ->
                val button = Button(this)
                button.text = "device_"+device.id.toString()
                layout.addView(button)
                button.setOnClickListener(){
                    if(device.client != null) {
                        val intent = Intent(this, scheduler::class.java)
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

        val logout = findViewById<Button>(R.id.app_delete)
        logout.setOnClickListener(){
            showLogoutConfirmationDialog("LOGOUT","Are you sure you want to logout?", this) {
                // This block will be executed when the user confirms logout
                val req = "LOGOUT"
                Send_cmd_to_server(req, this).execute()
                clearAppData(it.context)
            }
        }

        val dev_logout = findViewById<Button>(R.id.device_delete)
        dev_logout.setOnClickListener(){
            lateinit var dialog: AlertDialog
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            devices.forEachIndexed { index, device ->
                val button = Button(this)
                button.text = "device_"+device.id.toString()
                if(device.client != null)
                    button.setBackgroundColor(Color.GREEN)
                layout.addView(button)
                button.setOnClickListener(){
                    showLogoutConfirmationDialog("REMOVE DEVICE","Are you sure you want to remove Device ${device.id.toString()}?", this) {
                        // This block will be executed when the user confirms logout
                        val req = "DEVLOGOUT,${device.id.toString()},"
                        zimarix_global.serv_sync = 0
                        Send_cmd_to_server(req, this).execute()
                        dialog.dismiss()
                    }
                }
            }
            layout.setPadding(50, 40, 50, 10)

            val builder = AlertDialog.Builder(this)
                .setTitle("Select Device to Delete")
                .setView(layout)
            dialog = builder.create()
            dialog.show()
        }

        val update_password = findViewById<Button>(R.id.update_password)
        update_password.setOnClickListener() {
            lateinit var dialog: AlertDialog
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.HORIZONTAL

            val password = EditText(this)
            password.hint = "ENTER NEW PASSWORD"
            layout.addView(password)

            val button = Button(this)
            button.text = "UPDATE"
            layout.addView(button)
            button.setOnClickListener() {
                Log.d("debug ", "===============  ${password.text.length} \n")
                if(password.text.length > 0 && password.text.length < 32) {
                    val req = "UPDATE,${password.text.toString()},"
                    Log.d("debug ", "===============  $req \n")
                    zimarix_global.serv_sync = 0
                    Send_cmd_to_server(req, this).execute()
                    dialog.dismiss()
                }
            }

            layout.setPadding(50, 40, 50, 10)

            val builder = AlertDialog.Builder(this)
                .setTitle("UPDATE PASSWORD")
                .setCancelable(false)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { maindialog, whichButton ->
                    dialog.dismiss()
                }
            dialog = builder.create()
            dialog.show()
        }

        val delete_account = findViewById<Button>(R.id.user_delete)
        delete_account.setOnClickListener(){
            showLogoutConfirmationDialog("CONFIRM USER DELETE"," All Devices will be Reset and apps will be logged out\n" +
                    " Are you sure you want to DELETE ACCOUNT?", this) {
                val req = "DELETE,"
                zimarix_global.serv_sync = 0
                Send_cmd_to_server(req, this).execute()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        app_state = 1
        active = true
    }
    override fun onPause() {
        super.onPause()
        app_state = 0
        active = false
    }
}

class Send_cmd_to_server(
private val cmd: String,
private val activity: update_params
) : AsyncTask<Void, Void, String>() {
    var resp:String = "FAIL"
    override fun onPreExecute() {
        activity.progressBar.visibility = View.VISIBLE
    }
    override fun doInBackground(vararg params: Void?): String {
        if (activity.isTaskInProgress == true) {
            return "Previous Request In Progress"
        }else{
            activity.isTaskInProgress = true
            val enc_req = aes_encrpt(zimarix_global.appkey, zimarix_global.appiv, cmd)
            try {
                zimarix_global.ecsock!!.outputStream.write(enc_req)
                resp = "OK"
            }catch (t: Throwable){
                return "FAIL"
            }
            return resp
        }
        return "FAIL" // Return the result from the background task
    }
    override fun onPostExecute(result: String) {
        activity.progressBar.visibility = View.GONE
        activity.isTaskInProgress = false
        /*
        activity.aToast?.cancel()
        activity.aToast = Toast.makeText(activity as? Context, result, Toast.LENGTH_SHORT)
        activity.aToast?.show()
         */
    }
}
