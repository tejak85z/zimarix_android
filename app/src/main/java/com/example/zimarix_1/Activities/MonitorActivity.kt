package com.example.zimarix_1.Activities

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.R
import com.example.zimarix_1.Send_cmd
import com.example.zimarix_1.dev_req
import com.example.zimarix_1.update_params
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MonitorActivity : AppCompatActivity() , update_params {
    private lateinit var recyclerView: RecyclerView
    lateinit var motion_event_log_Adapter: motion_event_log_Adapter

    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    var value = -1
    var prev_config = ""
    var current_config = ""
    var event_log: List<String> = emptyList()


    lateinit var monitor_sw:Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitor)

        progressBar = findViewById(R.id.monitor_progressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        CoroutineScope(Dispatchers.IO).launch {
            monitor_updater(value)
        }
        monitor_sw = findViewById(R.id.monitor)
        monitor_sw.setOnClickListener {
            if (monitor_sw.isChecked == true) {
                Send_cmd(value,"CFG,MONITOR,SET,1," , this).execute()
            } else {
                Send_cmd(value,"CFG,MONITOR,SET,0," , this).execute()
            }
        }

        recyclerView = findViewById(R.id.motion_event_log)
        recyclerView.layoutManager = LinearLayoutManager(this)
        motion_event_log_Adapter = motion_event_log_Adapter(this)
        recyclerView.adapter = motion_event_log_Adapter
    }
    fun monitor_updater(index: Int){
        while (active == true){
            val resp = dev_req(index, "CFG,MONITOR,GET,")
            if (resp.length > 1 && resp != prev_config) {
                prev_config = resp
                val param = resp.split(",")
                if (param[0] == "0" &&  monitor_sw.isChecked) {
                    GlobalScope.launch(Dispatchers.Main) {
                        monitor_sw.isChecked = false
                    }
                }else if(param[0] == "1" &&  monitor_sw.isChecked == false) {
                    GlobalScope.launch(Dispatchers.Main) {
                        monitor_sw.isChecked = true
                    }
                }
            }

            val mresp = dev_req(index, "MOTION_LOG")
            if (mresp.length > 1 && mresp != current_config) {
                current_config = mresp
                event_log = mresp.split(",").filter { it.isNotBlank() && it != "none"}
                GlobalScope.launch(Dispatchers.Main) {
                    // Update UI here
                    motion_event_log_Adapter.update()
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

class motion_event_log_Adapter(private val activity: MonitorActivity)
    : RecyclerView.Adapter<motion_event_log_Adapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val event : TextView = itemView.findViewById(R.id.TextBox)
    }
    override fun getItemCount(): Int {
        return activity.event_log.size
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.text_item, parent, false)
        return ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        holder.event.text = activity.event_log[index]
    }
    fun update() {
        notifyDataSetChanged()
    }
}