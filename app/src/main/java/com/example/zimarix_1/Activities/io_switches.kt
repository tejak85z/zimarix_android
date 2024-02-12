package com.example.zimarix_1.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
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
import com.example.zimarix_1.default_port_type
import com.example.zimarix_1.port_1_8
import com.example.zimarix_1.sw_params
import com.example.zimarix_1.update_params
import com.example.zimarix_1.zimarix_global.Companion.devices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class io_switches : AppCompatActivity() , update_params {
    private lateinit var recyclerView: RecyclerView
    lateinit var ioAdapter: IoSwitchAdapter

    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_io_switches)

        val b = intent.extras
        var value = -1 // or other values
        if (b != null) value = b.getInt("key")

        progressBar = findViewById(R.id.ioprogressBar)
        progressBar.visibility = View.GONE

        val ports = GSwitches.filter {it.did == devices[value].id && it.id.toInt() < 32}

        recyclerView = findViewById(R.id.iorecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        ioAdapter = IoSwitchAdapter(ports, this)
        recyclerView.adapter = ioAdapter

        CoroutineScope(Dispatchers.IO).launch {
            //config_updater(value, this@io_switches)
            ioAdapter.update()
        }

        val discovery: Button = findViewById(R.id.discovery)
        discovery.setOnClickListener(){
            val cmd = "EC,DISCOVERY,"
            Send_cmd(value, cmd, this).execute()
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

class IoSwitchAdapter(private var switchList: List<sw_params>, private val activity: io_switches)
    : RecyclerView.Adapter<IoSwitchAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val port: TextView = itemView.findViewById(R.id.port)
        val name: EditText = itemView.findViewById(R.id.name)
        val update: Button = itemView.findViewById(R.id.update)
        val ps: Switch = itemView.findViewById(R.id.ps)
        val type:Spinner = itemView.findViewById(R.id.type)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_io_config, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, index : Int) {
        holder.name.hint = switchList[index].name
        holder.update.setOnClickListener {
            if (holder.name.text.length > 0 && holder.name.text.length < 32) {
                val cmd =
                    "CFG,IO,SET,PORT_NAME," + switchList[index].id + "," + holder.name.text + ","
                Send_cmd(switchList[index].idx, cmd, activity).execute()
            }else{
                Toast.makeText(activity, "INVALID NAME", Toast.LENGTH_SHORT).show()
            }
        }
        holder.ps.isChecked = switchList[index].powersave == "1"
        holder.ps.setOnClickListener {
            var cmd = ""
            if (holder.ps.isChecked == true) {
                cmd = "CFG,IO,SET,POWERSAVE,"+ switchList[index].id + ",1,"
            } else {
                cmd = "CFG,IO,SET,POWERSAVE,"+ switchList[index].id + ",0,"
            }
            Send_cmd(switchList[index].idx ,cmd, activity).execute()
        }
        holder.port.text = "PORT " + switchList[index].id

        var adapter: ArrayAdapter<String>? = null
        if (index == 0 || index == 7) {
            adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, port_1_8)
        }else{
            adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, default_port_type)
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.type.adapter = adapter
        holder.type.onItemSelectedListener = null
        holder.type.setSelection(switchList[index].type - 1)
        holder.type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if ((switchList[index].type - 1) != position) {
                    val type = position + 1
                    val cmd =
                        "CFG,IO,SET,PORT_TYPE," + switchList[index].id + "," + type.toString() + ","
                    Send_cmd(switchList[index].idx, cmd, activity).execute()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }
    override fun getItemCount(): Int {
        return switchList.size
    }

    fun update() {
        notifyDataSetChanged()
    }
}