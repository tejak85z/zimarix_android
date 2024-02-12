package com.example.zimarix_1.Activities
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.GSwitches
import com.example.zimarix_1.R
import com.example.zimarix_1.Send_cmd
import com.example.zimarix_1.dev_action
import com.example.zimarix_1.dev_req
import com.example.zimarix_1.sw_params
import com.example.zimarix_1.update_params
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class btspeaker : AppCompatActivity() , update_params {
    lateinit var btAdapter: btAdapter
    lateinit var btScanAdapter: btScanAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerScanView: RecyclerView

    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true
    var value = -1
    var prev_config = ""
    var prev_scan_config = ""
    var bt_power = "no"
    var bt_scan_on = 0
    var bt_status = ""

    var btlist: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_btspeaker)

        progressBar = findViewById(R.id.btprogressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        CoroutineScope(Dispatchers.IO).launch {
            bt_updater(value,this@btspeaker)
        }

        recyclerView = findViewById(R.id.btrecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        btAdapter = btAdapter(this)
        recyclerView.adapter = btAdapter

        recyclerScanView = findViewById(R.id.btscanView)
        recyclerScanView.layoutManager = LinearLayoutManager(this)
        btScanAdapter = btScanAdapter(this)
        recyclerScanView.adapter = btScanAdapter
    }

    fun update_bt_config(devindex:Int): Int {
        var ret = 0
        val resp = dev_req(devindex, "BT,GET_POWER,")
        if (resp.length > 1 && resp != prev_config) {
            prev_config = resp
            val param = resp.split(",")
            if (param[0] == "yes" || param[0] == "no")
                bt_power = param[0]
            return 1
        }
        return 0
    }
    fun bt_updater(index: Int, btspeaker: btspeaker){
        var ret = 0
        while (active == true){
            ret = 0
            if (update_bt_config(index) != 0) {
                ret = 1
            }
            if (ret == 1) {
                GlobalScope.launch(Dispatchers.Main) {
                    // Update UI here
                    btAdapter.update()
                }
            }
            if (bt_scan_on == 1){
                val resp = dev_req(index, "BT,SCAN_RESULTS,")
                if (resp.length > 1 && resp != prev_scan_config) {
                    prev_config = resp
                    btlist = resp.lines()
                        .filter { it.contains("Device") }
                        .map { it.replace("Device", "").trim() }
                    GlobalScope.launch(Dispatchers.Main) {
                        // Update UI here
                        btScanAdapter.update()
                    }
                }
            }
            val resp = dev_req(index, "BT,STATUS,")
            if (resp.length > 1 && resp != bt_status) {
                bt_status = resp
                GlobalScope.launch(Dispatchers.Main) {
                    // Update UI here
                    btAdapter.update()
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

class btAdapter(private val activity: btspeaker)
    : RecyclerView.Adapter<btAdapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bt_enable: Switch = itemView.findViewById(R.id.bt_enable)
        val bt_scan: Button = itemView.findViewById(R.id.bt_scan)
        val bt_clear: Button = itemView.findViewById(R.id.bt_clear_scan)
        val bt_status: TextView = itemView.findViewById(R.id.btstatus)
        val bt_disconnect: Button = itemView.findViewById(R.id.bt_disconnect)
    }
    override fun getItemCount(): Int {
        return 1
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_bt_config, parent, false)
        return ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        if (activity.bt_power == "yes")
            holder.bt_enable.isChecked = true
        else
            holder.bt_enable.isChecked = false
        holder.bt_enable.setOnClickListener(){
            var cmd = ""
            if (holder.bt_enable.isChecked == true)
                cmd = "BT,POWER,on,"
            else
                cmd = "BT,POWER,off,"
            Send_cmd(activity.value, cmd, activity).execute()
        }

        holder.bt_clear.setOnClickListener(){
            var cmd = "BT,CLEAR,"
            Send_cmd(activity.value, cmd, activity).execute()
        }

        holder.bt_scan.setOnClickListener(){
            activity.bt_scan_on = 1
            var cmd = "BT,SCAN,"
            Send_cmd(activity.value, cmd, activity).execute()
        }
        holder.bt_status.text = "CONNECTION STATUS : " +
                activity.bt_status
                    .replace("Device", "Connected to Device")
                    .replace(Regex(","), "")
        holder.bt_disconnect.setOnClickListener(){
            var mac = if (activity.bt_status.contains("Device")) {
                val startIndex = activity.bt_status.indexOf("Device") + "Device".length
                val endIndex = activity.bt_status.indexOf("\n")
                activity.bt_status.substring(startIndex, endIndex).trim()
            } else {
                ""
            }
            if (mac.length == 17){
                val cmd = "BT,DISCONNECT,$mac,"
                Send_cmd(activity.value, cmd, activity).execute()
            }
        }
    }
    fun update() {
        notifyDataSetChanged()
    }
}
class btScanAdapter(private val activity: btspeaker)
    : RecyclerView.Adapter<btScanAdapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bt: Button = itemView.findViewById(R.id.ButtonBox)
    }
    override fun getItemCount(): Int {
        return activity.btlist.size
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.button_item, parent, false)
        return ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        holder.bt.text = activity.btlist[index]
        holder.bt.setOnClickListener(){
            val mac = activity.btlist[index].split(" ")[0]
            var cmd = "BT,CONNECT,$mac,"
            Send_cmd(activity.value, cmd, activity).execute()
        }
    }
    fun update() {
        notifyDataSetChanged()
    }
}