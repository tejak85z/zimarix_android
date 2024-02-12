package com.example.zimarix_1.Activities

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

class wifi : AppCompatActivity() , update_params {
    lateinit var wifiAdapter: wifiAdapter
    lateinit var wifiScanAdapter: wifiScanAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerScanView: RecyclerView

    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true
    var value = -1
    var prev_config = ""
    var prev_scan_config = ""
    var wifi_scan_on = 0
    var wifi_status = ""

    var wifilist: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi)

        progressBar = findViewById(R.id.wifi_progressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        CoroutineScope(Dispatchers.IO).launch {
            wifi_updater(value)
        }

        recyclerView = findViewById(R.id.wifi_recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        wifiAdapter = wifiAdapter(this)
        recyclerView.adapter = wifiAdapter

        recyclerScanView = findViewById(R.id.wifi_scan_View)
        recyclerScanView.layoutManager = LinearLayoutManager(this)
        wifiScanAdapter = wifiScanAdapter(this)
        recyclerScanView.adapter = wifiScanAdapter
    }
    fun wifi_updater(index: Int){
        var ret = 0
        while (active == true){
            ret = 0
            if (wifi_scan_on == 1){
                val resp = dev_req(index, "WIFI,SCAN_RESULTS,")
                if (resp.length > 1 && resp != prev_scan_config) {
                    prev_config = resp
                    wifilist = resp.lines()
                        .filter { it.contains("Device") }
                        .map { it.replace("Device", "").trim() }
                    GlobalScope.launch(Dispatchers.Main) {
                        // Update UI here
                        wifiScanAdapter.update()
                    }
                }
            }
            val resp = dev_req(index, "WIFI,STATUS,")
            if (resp.length > 1 && resp != wifi_status) {
                wifi_status = resp
                GlobalScope.launch(Dispatchers.Main) {
                    // Update UI here
                    wifiAdapter.update()
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

class wifiAdapter(private val activity: wifi)
    : RecyclerView.Adapter<wifiAdapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val wifi_scan: Button = itemView.findViewById(R.id.wifi_scan)
        val wifi_clear: Button = itemView.findViewById(R.id.wifi_clear_scan)
        val wifi_status: TextView = itemView.findViewById(R.id.wifistatus)
        val wifi_disconnect: Button = itemView.findViewById(R.id.wifi_disconnect)
    }
    override fun getItemCount(): Int {
        return 1
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_wifi_config, parent, false)
        return ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        holder.wifi_clear.setOnClickListener(){
            var cmd = "WIFI,CLEAR,"
            Send_cmd(activity.value, cmd, activity).execute()
        }

        holder.wifi_scan.setOnClickListener(){
            activity.wifi_scan_on = 1
            var cmd = "WIFI,SCAN,"
            Send_cmd(activity.value, cmd, activity).execute()
        }
        holder.wifi_status.text = "CONNECTION STATUS : " +
                activity.wifi_status
                    .replace("Device", "Connected to Device")
                    .replace(Regex(","), "")
        holder.wifi_disconnect.setOnClickListener(){
            var mac = if (activity.wifi_status.contains("Device")) {
                val startIndex = activity.wifi_status.indexOf("Device") + "Device".length
                val endIndex = activity.wifi_status.indexOf("\n")
                activity.wifi_status.substring(startIndex, endIndex).trim()
            } else {
                ""
            }
            if (mac.length == 17){
                val cmd = "WIFI,DISCONNECT,$mac,"
                Send_cmd(activity.value, cmd, activity).execute()
            }
        }
    }
    fun update() {
        notifyDataSetChanged()
    }
}
class wifiScanAdapter(private val activity: wifi)
    : RecyclerView.Adapter<wifiScanAdapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val wifi: Button = itemView.findViewById(R.id.ButtonBox)
    }

    override fun getItemCount(): Int {
        return activity.wifilist.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.button_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        holder.wifi.text = activity.wifilist[index]
        holder.wifi.setOnClickListener() {
            val mac = activity.wifilist[index].split(" ")[0]
            var cmd = "wifi,CONNECT,$mac,"
            Send_cmd(activity.value, cmd, activity).execute()
        }
    }

    fun update() {
        notifyDataSetChanged()
    }
}
