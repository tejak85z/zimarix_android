package com.example.zimarix_1

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ir_switches : AppCompatActivity(), update_params{
    private lateinit var recyclerView: RecyclerView
    lateinit var irAdapter: IrSwitchAdapter

    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    private var value = -1
    private lateinit var maindialog:AlertDialog

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ir_switches)

        val b = intent.extras
        if (b != null) value = b.getInt("key")
        progressBar = findViewById(R.id.irprogressBar)
        progressBar.visibility = View.GONE

        val add_ir = findViewById<Button>(R.id.add_ir)
        add_ir.setOnClickListener {
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL

            val ac = Button(this)
            ac.setText("CREATE AC REMOTE")
            ac.setOnClickListener {
                add_ir_dialog(11)
            }
            layout.addView(ac)

            val tv = Button(this)
            tv.setText("CREATE TV REMOTE")
            tv.setOnClickListener {
                add_ir_dialog(12)
            }
            layout.addView(tv)

            val sw = Button(this)
            sw.setText("CREATE IR REMOTE SWITCH")
            sw.setOnClickListener {
                add_ir_dialog(13)
            }
            layout.addView(sw)

            val light = Button(this)
            light.setText("CREATE IR SMART LIGHT REMOTE")
            light.setOnClickListener {
                add_ir_dialog(14)
            }
            layout.addView(light)

            val fan = Button(this)
            fan.setText("CREATE IR SMART FAN REMOTE")
            fan.setOnClickListener {
                add_ir_dialog(15)
            }
            layout.addView(fan)

            val music = Button(this)
            music.setText("CREATE MUSIC PLAYER REMOTE")
            music.setOnClickListener {
                add_ir_dialog(16)
            }
            layout.addView(music)

            layout.setPadding(50, 40, 50, 10)

            val builder = AlertDialog.Builder(this)
                .setTitle("CREATE IR REMOTE")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { maindialog, whichButton ->
                    maindialog.dismiss()
                }
            maindialog = builder.create()
            maindialog.show()
        }

        val ports = GSwitches.filter {it.did == zimarix_global.devices[value].id && it.id.toInt() >= 32}
        recyclerView = findViewById(R.id.irrecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        irAdapter = IrSwitchAdapter(ports, this)
        recyclerView.adapter = irAdapter
    }

    fun add_ir_dialog(type: Int){
        maindialog.dismiss()
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val name = EditText(this)
        layout.addView(name)
        layout.setPadding(50, 40, 50, 10)

        var title = "SELECT NAME FOR "
        if (type == 11){
            title = title + "AC"
        }else if (type == 12){
            title = title + "TV"
        }else if (type == 13){
            title = title + "IR SWITCH"
        }else if (type == 14){
            title = title + "IR SMART LIGHT"
        }else if (type == 15){
            title = title + "IR SMART FAN"
        }else if (type == 16){
            title = title + "MUSIC PLAYER"
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("OK", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                if (name.text.length in 1..63) {
                    var req = "CFG,IR,ADD," + name.text + "," + type.toString() + ","
                    Send_cmd(value,req,this).execute()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}

class IrSwitchAdapter(private var switchList: List<sw_params>, private val activity: ir_switches)
    : RecyclerView.Adapter<IrSwitchAdapter.ViewHolder>() {

    private lateinit var maindialog:AlertDialog

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dev: Button = itemView.findViewById(R.id.ButtonBox)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.button_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, index : Int) {
        holder.dev.text = switchList[index].name
        holder.dev.setOnClickListener {
            val swindex = GSwitches.indexOf(switchList[index])
            if (swindex >= 0 && swindex < GSwitches.size) {
                val b = Bundle()
                b.putInt("key", swindex) //Your id
                if (switchList[index].type == 11){
                    val intent = Intent(activity, Ir_Ac_Remote::class.java)
                    intent.putExtras(b)
                    activity.startActivity(intent)
                } else if (switchList[index].type == 12){
                    val intent = Intent(activity, Ir_Tv_Remote::class.java)
                    intent.putExtras(b)
                    activity.startActivity(intent)
                }else if (switchList[index].type == 13){
                    val intent = Intent(activity, Ir_Switch_Remote::class.java)
                    intent.putExtras(b)
                    activity.startActivity(intent)
                }else if (switchList[index].type == 14){
                    val intent = Intent(activity, Ir_Smart_Light_Remote::class.java)
                    intent.putExtras(b)
                    activity.startActivity(intent)
                }else if (switchList[index].type == 15){
                    val intent = Intent(activity, Ir_Fan_Remote::class.java)
                    intent.putExtras(b)
                    activity.startActivity(intent)
                }else if (switchList[index].type == 16){
                    val intent = Intent(activity, Ir_Music_Remote::class.java)
                    intent.putExtras(b)
                    activity.startActivity(intent)
                }
            }
        }
        holder.dev.setOnLongClickListener {
            val layout = LinearLayout(activity)
            layout.orientation = LinearLayout.VERTICAL

            val update = Button(activity)
            update.setText("RENAME")
            update.setOnClickListener {
                rename_dialog(switchList[index])
            }
            layout.addView(update)

            val del = Button(activity)
            del.setText("DELETE")
            del.setOnClickListener {
                val req = "CFG,IR,REMOVE,${switchList[index].id},"
                Send_cmd(switchList[index].idx,req,activity).execute()
            }
            layout.addView(del)

            layout.setPadding(50, 40, 50, 10)

            val builder = AlertDialog.Builder(activity)
                .setTitle(switchList[index].name)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { maindialog, whichButton ->
                    // so something, or not - dialog will close
                    maindialog.dismiss()
                }
            maindialog = builder.create()
            maindialog.show()
            return@setOnLongClickListener(true)
        }
    }

    fun rename_dialog(sw: sw_params){
        maindialog.dismiss()
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL


        val name = EditText(activity)
        name.hint = sw.name
        layout.addView(name)

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(activity)
            .setTitle("RENAME port"+ sw.id)
            .setView(layout)
            .setPositiveButton("RENAME", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                if (name.text.length in 1..63) {
                    var req = "CFG,IR,UPDATE," + sw.id + ","+name.text+","
                    Send_cmd(sw.idx,req,activity).execute()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    override fun getItemCount(): Int {
        return switchList.size
    }

    fun update() {
        notifyDataSetChanged()
    }
}

fun create_ir_switch(sw: Button, cmd: String, dev_idx: Int, activity: update_params, context: Context): Button{
    val idx = GSwitches[dev_idx].idx
    val did = GSwitches[dev_idx].did
    val name = GSwitches[dev_idx].name
    val port_id = GSwitches[dev_idx].id

    sw.setOnClickListener {
        val req = "IR,SET,PORT,$port_id,$cmd,"
        Send_cmd(idx,req,activity).execute()
    }

    sw.setOnLongClickListener {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        val append = Button(context)
        append.setText("APPEND")
        append.setOnClickListener {
            val req = "IR,RECORD,$port_id,$cmd,APPEND,"
            Send_cmd(idx,req,activity).execute()
        }
        layout.addView(append)

        val reset = Button(context)
        reset.setText("RESET")
        reset.setOnClickListener {
            val req = "IR,RECORD,$port_id,$cmd,RESET,"
            Send_cmd(idx,req,activity).execute()
        }
        layout.addView(reset)

        val clear = Button(context)
        clear.setText("CLEAR")
        clear.setOnClickListener {
            val req = "IR,RECORD,$port_id,$cmd,CLEAR,"
            Send_cmd(idx,req,activity).execute()
        }
        layout.addView(clear)

        val clear_recent = Button(context)
        clear_recent.setText("CLEAR RECENT RECORD")
        clear_recent.setOnClickListener {
            val req = "IR,RECORD,$port_id,$cmd,CLEAR_RECENT,"
            Send_cmd(idx,req,activity).execute()
        }
        layout.addView(clear_recent)

        val delay = Button(context)
        delay.setText("ADD DELAY")
        delay.setOnClickListener {
            val req = "IR,RECORD,$port_id,$cmd,ADD_DELAY,"
            Send_cmd(idx,req,activity).execute()
        }
        layout.addView(delay)

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle(name + ": " + cmd + " configure")
            .setView(layout)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
        return@setOnLongClickListener(true)
    }
    return sw
}

fun ir_updater(index: Int, btn_list: MutableList<ir_button>, devConfig: update_params){
    Log.d("debug ", " -------------------------------starting ir_updater\n")
    while (true){
        if (devConfig.active == true) {
            val resp = dev_req(GSwitches[index].idx, "IR,GET,PORT,${GSwitches[index].id},")
            val param = resp.split(",")
            Log.d("debug ", " -------------------------------$param\n")

            btn_list.forEach {
                val name = it.name
                val btn = it.bt
                val txt = param.find { it.length > 1 && it.substring(1) == name }

                txt?.let {
                    if (it.isNotEmpty() && it[0] == '1') {
                        btn.setBackgroundColor(Color.BLUE)
                    } else {
                        btn.setBackgroundColor(Color.GRAY)
                    }
                }
            }
        }
        Thread.sleep(10000)
    }
}