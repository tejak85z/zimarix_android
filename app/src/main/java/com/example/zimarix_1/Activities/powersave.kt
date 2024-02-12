package com.example.zimarix_1.Activities

import android.app.AlertDialog
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.GSwitches
import com.example.zimarix_1.R
import com.example.zimarix_1.Send_cmd
import com.example.zimarix_1.dev_action1
import com.example.zimarix_1.dev_req
import com.example.zimarix_1.update_params
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class powersave : AppCompatActivity() , update_params {
    lateinit var Adapter: PowersaveAdapter
    private lateinit var recyclerView: RecyclerView
    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true
    var current_config = ""
    var value = -1
    var powersave_params: List<String> = emptyList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_powersave)

        progressBar = findViewById(R.id.powersave_progressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        CoroutineScope(Dispatchers.IO).launch {
            powersave_updater(value)
        }

        recyclerView = findViewById(R.id.powersave_recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        Adapter = PowersaveAdapter(this)
        recyclerView.adapter = Adapter
    }
    fun powersave_updater(index: Int){
        while (true){
            if (active == true) {
                val resp = dev_req(index, "CFG,PS,GET,CONF,")
                if (resp.length > 1 && resp != current_config) {
                    current_config = resp
                    powersave_params = resp.split(",").filter { it.isNotBlank() && it != "none"}
                    GlobalScope.launch(Dispatchers.Main) {
                        // Update UI here
                        Adapter.update()
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

class PowersaveAdapter(private val activity: powersave)
    : RecyclerView.Adapter<PowersaveAdapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog
    var device_id = "-1"
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val powersave_enable: Switch = itemView.findViewById(R.id.powersave_enable)
        val autoon: Switch = itemView.findViewById(R.id.auto_on_enable)
        val timeout: EditText = itemView.findViewById(R.id.powersave_timeout)
        val update: Button = itemView.findViewById(R.id.powersave_update)
    }
    override fun getItemCount(): Int {
        return 1
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_powersave, parent, false)
        return ViewHolder(itemView)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        if (activity.powersave_params.size == 3) {
            if (activity.powersave_params[0] == "1")
                holder.powersave_enable.isChecked = true
            else
                holder.powersave_enable.isChecked = false

            if (activity.powersave_params[1] == "1")
                holder.autoon.isChecked = true
            else
                holder.autoon.isChecked = false

            holder.timeout.hint = activity.powersave_params[2] + " Sec"
        }
        holder.powersave_enable.setOnClickListener(){
            var cmd = ""
            if (holder.powersave_enable.isChecked == true) {
                cmd = "CFG,PS,SET,ENABLE,1,"
            }else{
                cmd = "CFG,PS,SET,ENABLE,0,"
            }
            Send_cmd(activity.value, cmd, activity).execute()
        }

        holder.autoon.setOnClickListener(){
            var cmd = ""
            if (holder.autoon.isChecked == true) {
                cmd = "CFG,PS,SET,AUTO_ON,1,"
            }else{
                cmd = "CFG,PS,SET,AUTO_ON,0,"
            }
            Send_cmd(activity.value, cmd, activity).execute()
        }

        holder.update.setOnClickListener() {
            val ptimeout = holder.timeout.text.toString().toInt()
            if (ptimeout > 0 && ptimeout < 3600) {
                val cmd = "CFG,PS,SET,TRIGGER_TIME,${holder.timeout.text.toString()},"
                Send_cmd(activity.value, cmd, activity).execute()
            }
        }
    }
    fun update() {
        notifyDataSetChanged()
    }
}