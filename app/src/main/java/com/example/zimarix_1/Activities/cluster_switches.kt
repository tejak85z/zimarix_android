package com.example.zimarix_1.Activities

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.GSwitch_adapter
import com.example.zimarix_1.GSwitches
import com.example.zimarix_1.R
import com.example.zimarix_1.Send_cmd
import com.example.zimarix_1.dev_req
import com.example.zimarix_1.sw_params
import com.example.zimarix_1.update_params
import com.example.zimarix_1.zimarix_global
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class cluster_switches : AppCompatActivity() , update_params {
    private lateinit var recyclerView: RecyclerView
    lateinit var clsAdapter: ClsSwitchAdapter

    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true
    var value = -1
    var current_config = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cluster_switches)

        val b = intent.extras
        if (b != null) value = b.getInt("key")
        progressBar = findViewById(R.id.clsprogressBar)
        progressBar.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            cluster_switch_updater(value,this@cluster_switches)
        }

        val add_cls = findViewById<Button>(R.id.add_cluster)
        add_cls.setOnClickListener {
            val b = Bundle()
            b.putInt("idx", value)
            b.putString("sw_id", "-1") //Your id
            val intent = Intent(this, cluster_Switch_update::class.java)
            intent.putExtras(b)
            this.startActivity(intent)
        }
        recyclerView = findViewById(R.id.clsrecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        clsAdapter = ClsSwitchAdapter( this)
        recyclerView.adapter = clsAdapter

    }

    fun update_cls_config(devindex:Int): Int {
        var ret = -1
        var did = zimarix_global.devices[value].id
        val resp = dev_req(devindex, "CFG,CLS,GET,")
        if (resp.length > 1 && resp != current_config) {
            current_config = resp
            val ports = resp.split(",")

            // Collect the ids from the response
            val responseIds = ports.filter { it.split("_").size > 2 }.map { it.split("_")[0] }
            val Gswitch_len = GSwitches.size
            // Remove entries from GSwitches with common did and not in response
            GSwitches.removeAll { it.did == did && it.id.toInt() >= 500 && it.id !in responseIds }
            if (Gswitch_len != GSwitches.size){
                ret = 1
            }

            if (ports.size > 0) {
                ports.forEach {
                    val param = it.split("_")
                    if (param.size > 2) {
                        val matchingSwitch: sw_params? = GSwitches.find {
                            it.id == param[0] && it.did == did
                        }
                        if (matchingSwitch != null) {
                            if (matchingSwitch.name != param[1]) {
                                matchingSwitch.name = param[1]
                                ret = 1
                            }
                            matchingSwitch.type = 13
                            matchingSwitch.params =
                                param.subList(2, param.size).filter { it.isNotEmpty() }
                            ret = 1
                        } else {
                            GSwitch_adapter.addSwitch(devindex, did, param[0], 10, param[1])
                            ret = 1
                        }
                    }
                }
            }
            return 1
        }
        return 0
    }

    fun cluster_switch_updater(index: Int, scheduler: cluster_switches){
        var ret = 0
        while (true){
            if (active == true) {
                ret = 0
                if (update_cls_config(index) != 0) {
                    ret = 1
                }
                if (ret == 1) {
                    GlobalScope.launch(Dispatchers.Main) {
                        // Update UI here
                        clsAdapter.update()
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

class ClsSwitchAdapter(private val activity: cluster_switches)
    : RecyclerView.Adapter<ClsSwitchAdapter.ViewHolder>() {
    private lateinit var maindialog:AlertDialog
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: Button = itemView.findViewById(R.id.cls_name)
        val update: Button = itemView.findViewById(R.id.cls_update)
        val delete: Button = itemView.findViewById(R.id.cls_delete)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_cluster_switch, parent, false)
        return ClsSwitchAdapter.ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, index : Int) {
        val switchList = GSwitches.filter {it.did == zimarix_global.devices[activity.value].id && it.id.toInt() >= 500}
        holder.name.text = switchList[index].name
        holder.name.setOnClickListener {
            val layout = LinearLayout(activity)
            layout.orientation = LinearLayout.HORIZONTAL

            val on = Button(activity)
            on.setText("ON")
            on.setOnClickListener {
                val req = "CLS,SET,PORT,${switchList[index].id},ON,"
                Send_cmd(switchList[index].idx,req,activity).execute()
            }
            layout.addView(on)

            val off = Button(activity)
            off.setText("OFF")
            off.setOnClickListener {
                val req = "CLS,SET,PORT,${switchList[index].id},OFF,"
                Send_cmd(switchList[index].idx,req,activity).execute()
            }
            layout.addView(off)

            layout.setPadding(50, 40, 50, 10)

            val builder = AlertDialog.Builder(activity)
                .setTitle(switchList[index].name)
                .setView(layout)
            maindialog = builder.create()
            maindialog.show()
        }

        holder.update.setOnClickListener {
            val swindex = GSwitches.indexOf(switchList[index])
            if (swindex >= 0 && swindex < GSwitches.size) {
                val b = Bundle()
                b.putInt("idx", activity.value)
                b.putString("sw_id", switchList[index].id) //Your id
                val intent = Intent(activity, cluster_Switch_update::class.java)
                intent.putExtras(b)
                activity.startActivity(intent)
            }
        }
        holder.delete.setOnClickListener {
            val req = "CFG,CLS,DELETE,${switchList[index].id},"
            Send_cmd(switchList[index].idx,req,activity).execute()
        }
    }

    override fun getItemCount(): Int {
        return GSwitches.filter {it.did == zimarix_global.devices[activity.value].id && it.id.toInt() >= 500}.size
    }

    fun update() {
        notifyDataSetChanged()
    }
}