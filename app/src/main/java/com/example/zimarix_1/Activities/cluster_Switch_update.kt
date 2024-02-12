package com.example.zimarix_1.Activities

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.GSwitches
import com.example.zimarix_1.R
import com.example.zimarix_1.Send_cmd
import com.example.zimarix_1.sw_params
import com.example.zimarix_1.update_params

class cluster_Switch_update : AppCompatActivity() , update_params {
    private lateinit var recyclerView: RecyclerView
    lateinit var clsupdateAdapter: ClsSwitchUpdateAdapter

    var sw: sw_params? = null
    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    var portlist = listOf<String>()
    var index = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cluster_switch_update)

        val b = intent.extras
        if (b != null) {
            val sw_id= b.getString("sw_id")
            index = b.getInt("idx")
            if (sw_id != "-1")
                sw = GSwitches.firstOrNull {it.id == sw_id}
        }
        progressBar = findViewById(R.id.cswprogressBar)
        progressBar.visibility = View.GONE

        val title = findViewById<TextView>(R.id.cls_title)
        val commit = findViewById<Button>(R.id.cls_commit)
        val name = findViewById<EditText>(R.id.cls_get_name)
        val add_port = findViewById<Button>(R.id.add_cls_port)

        if (sw == null) {
            title.text = "ADD CLUSTER SWITCH"
            commit.text = "ADD"
        }else{
            title.text = "UPDATE : " +sw?.name
            commit.text = "UPDATE"
            name.hint = sw?.name
            portlist = sw!!.params
        }
        commit.setOnClickListener(){
            if (sw == null){
                if (name.text.length > 0) {
                    var req = "CFG,CLS,ADD,${name.text},"
                    portlist.forEach(){
                        req = req + it + '_'
                    }
                    Log.d("debug ", "===============  $req \n")
                    Send_cmd(index, req,this).execute()

                }else{
                    Toast.makeText(this, "INVALID NAME", Toast.LENGTH_SHORT).show()
                }
            }else{
                var req = ""
                if (name.text.length > 0) {
                    req = "CFG,CLS,UPDATE,${sw?.id},${name.text},"
                    portlist.forEach(){
                        req = req + it + '_'
                    }
                }else{
                    req = "CFG,CLS,UPDATE,${sw?.id},${sw?.name},"
                    portlist.forEach() {
                        req = req + it + '_'
                    }
                }
                Log.d("debug ", "===============  $req $index\n")
                Send_cmd(index, req,this).execute()
            }
        }

        add_port.setOnClickListener(){
            lateinit var maindialog: AlertDialog
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.HORIZONTAL

            // Create NumberPicker for hours
            val dev_list = ListView(this)
            val items = GSwitches.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
            dev_list.adapter = adapter
            layout.addView(dev_list)

            dev_list.setOnItemClickListener { _, _, position, _ ->
                val clickedSwitch = GSwitches[position]
                if (sw?.id != clickedSwitch.id){
                    if (portlist?.contains(clickedSwitch.id) == true){
                        Toast.makeText(this, "PORT Exists", Toast.LENGTH_SHORT).show()
                    }else{
                        portlist = portlist + clickedSwitch.id
                        clsupdateAdapter.update()
                    }
                }
                maindialog.dismiss()
            }
            val builder = AlertDialog.Builder(this)
                .setTitle("ADD CLUSTER SWITCH")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { maindialog, whichButton ->
                    // so something, or not - dialog will close
                    maindialog.dismiss()
                }
            maindialog = builder.create()
            maindialog.show()
        }

        recyclerView = findViewById(R.id.clsupdaterecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        clsupdateAdapter = ClsSwitchUpdateAdapter(this)
        recyclerView.adapter = clsupdateAdapter
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

class ClsSwitchUpdateAdapter(private val activity: cluster_Switch_update)
    : RecyclerView.Adapter<ClsSwitchUpdateAdapter.ViewHolder>() {

    private lateinit var maindialog: AlertDialog

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val port: Button = itemView.findViewById(R.id.cls_port_edit)
        val del: Button = itemView.findViewById(R.id.cls_port_delete)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_cluster_switch_ports, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, index : Int) {
        val sw = GSwitches.firstOrNull {it.id == activity.portlist.get(index)}
        holder.port.text = sw?.name
        holder.port.setOnClickListener {
            val layout = LinearLayout(activity)
            layout.orientation = LinearLayout.HORIZONTAL

            // Create NumberPicker for hours
            val dev_list = ListView(activity)
            val items = GSwitches.map { it.name }
            val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, items)
            dev_list.adapter = adapter
            layout.addView(dev_list)

            dev_list.setOnItemClickListener { _, _, position, _ ->
                val clickedSwitch = GSwitches[position]
                if (sw?.id != clickedSwitch.id){
                    if (activity.portlist.contains(clickedSwitch.id) == true){
                        Toast.makeText(activity, "PORT Exists", Toast.LENGTH_SHORT).show()
                    }else{
                        activity.portlist = activity.portlist.map { if (it == sw?.id) clickedSwitch.id else it }
                        notifyDataSetChanged()
                    }
                }
                maindialog.dismiss()
            }
            val builder = AlertDialog.Builder(activity)
                .setTitle("HOURS : MINUTES")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { maindialog, whichButton ->
                    // so something, or not - dialog will close
                    maindialog.dismiss()
                }
            maindialog = builder.create()
            maindialog.show()
        }

        holder.del.setOnClickListener {
            activity.portlist = activity.portlist.filterNot { it == sw?.id }
            notifyDataSetChanged()
        }
    }
    override fun getItemCount(): Int {
        return activity.portlist.size
    }
    fun update() {
        notifyDataSetChanged()
    }
}