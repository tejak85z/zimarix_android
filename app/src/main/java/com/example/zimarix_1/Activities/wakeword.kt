package com.example.zimarix_1.Activities

import android.app.AlertDialog
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
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

class wakeword : AppCompatActivity() , update_params {
    lateinit var Adapter: WakewordAdapter
    private lateinit var recyclerView: RecyclerView
    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true
    var current_config = ""
    var value = -1
    var wakeword_params: List<String> = emptyList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wakeword)

        progressBar = findViewById(R.id.wakeword_progressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        CoroutineScope(Dispatchers.IO).launch {
            wakeword_updater(value)
        }

        recyclerView = findViewById(R.id.wakeword_recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        Adapter = WakewordAdapter(this)
        recyclerView.adapter = Adapter
    }

    fun wakeword_updater(index: Int){
        while (true){
            if (active == true) {
                val resp = dev_req(index, "CFG,WAKEWORD,GET,CONF,")
                if (resp.length > 1 && resp != current_config) {
                    current_config = resp
                    wakeword_params = resp.split(",").filter { it.isNotBlank() && it != "none"}
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

class WakewordAdapter(private val activity: wakeword)
    : RecyclerView.Adapter<WakewordAdapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog
    var device_id = "-1"
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alexa_enable: Switch = itemView.findViewById(R.id.alexa_wakeword_enable)
        val alexa_snsty: SeekBar = itemView.findViewById(R.id.alexa_sensitivity)
        val jarvis_enable: Switch = itemView.findViewById(R.id.jarvis_wakeword_enable)
        val jarvis_snsty: SeekBar = itemView.findViewById(R.id.jarvis_sensitivity)
        val jarvis_dev: Button = itemView.findViewById(R.id.jarvis_device)
        val jarvis_dev_action: Button = itemView.findViewById(R.id.jarvis_cmd)
        val update: Button = itemView.findViewById(R.id.update_wakeword)
    }
    override fun getItemCount(): Int {
        return 1
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_wakeword, parent, false)
        return ViewHolder(itemView)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        holder.alexa_snsty.min = 0
        holder.alexa_snsty.max = 100
        holder.jarvis_snsty.min = 0
        holder.jarvis_snsty.max = 100

        if (activity.wakeword_params.size == 6) {
            if (activity.wakeword_params[0] == "1")
                holder.alexa_enable.isChecked = true
            else
                holder.alexa_enable.isChecked = false

            val alexa_snsty_val = (activity.wakeword_params[1].toFloat() * holder.alexa_snsty.max).toInt()
            if (alexa_snsty_val >= 0 && alexa_snsty_val  <= 100)
                holder.alexa_snsty.progress = alexa_snsty_val

            if (activity.wakeword_params[2] == "1")
                holder.jarvis_enable.isChecked = true
            else
                holder.jarvis_enable.isChecked = false

            val jarvis_snsty_val = (activity.wakeword_params[3].toFloat() * holder.jarvis_snsty.max).toInt()
            if (jarvis_snsty_val >= 0 && jarvis_snsty_val  <= 100)
                holder.jarvis_snsty.progress = jarvis_snsty_val

            device_id = activity.wakeword_params[4]
            if (device_id != "-1") {
                val sw = GSwitches.firstOrNull { it.id == device_id }
                holder.jarvis_dev.text = sw?.name
            }else{
                holder.jarvis_dev.text = "Select Device"
            }

            holder.jarvis_dev_action.text = activity.wakeword_params[5]
        }
        holder.alexa_enable.setOnClickListener(){
            var cmd = ""
            if (holder.alexa_enable.isChecked == true) {
                cmd = "CFG,WAKEWORD,SET,ALEXA_WAKEWORD,1,"
            }else{
                cmd = "CFG,WAKEWORD,SET,ALEXA_WAKEWORD,0,"
            }
            Send_cmd(activity.value, cmd, activity).execute()
        }

        holder.alexa_snsty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val snsty = ((holder.alexa_snsty.progress.toFloat())/100).toString()
                Send_cmd(activity.value,"CFG,WAKEWORD,SET,ALEXA_SNSTY,"+ snsty +",", activity).execute()
            }
        })

        holder.jarvis_enable.setOnClickListener(){
            var cmd = ""
            if (holder.jarvis_enable.isChecked == true) {
                cmd = "CFG,WAKEWORD,SET,JARVIS_WAKEWORD,1,"
            }else{
                cmd = "CFG,WAKEWORD,SET,JARVIS_WAKEWORD,0,"
            }
            Send_cmd(activity.value, cmd, activity).execute()
        }

        holder.jarvis_snsty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val snsty = ((holder.jarvis_snsty.progress.toFloat())/100).toString()
                Send_cmd(activity.value,"CFG,WAKEWORD,SET,JARVIS_SNSTY,"+ snsty +",", activity).execute()
            }
        })

        holder.jarvis_dev.setOnClickListener {
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
                holder.jarvis_dev.text = clickedSwitch.name
                device_id = clickedSwitch.id
                maindialog.dismiss()
            }
            val builder = AlertDialog.Builder(activity)
                .setTitle("")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { maindialog, whichButton ->
                    // so something, or not - dialog will close
                    maindialog.dismiss()
                }
            maindialog = builder.create()
            maindialog.show()
        }

        holder.jarvis_dev_action.setOnClickListener {
            val layout = LinearLayout(activity)
            layout.orientation = LinearLayout.HORIZONTAL

            // Create NumberPicker for hours
            val dev_list = ListView(activity)
            val items = dev_action1
            val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, items)
            dev_list.adapter = adapter
            layout.addView(dev_list)
            dev_list.setOnItemClickListener { _, _, position, _ ->
                holder.jarvis_dev_action.text = dev_action1[position]
                maindialog.dismiss()
            }
            val builder = AlertDialog.Builder(activity)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { maindialog, whichButton ->
                    // so something, or not - dialog will close
                    maindialog.dismiss()
                }
            maindialog = builder.create()
            maindialog.show()
        }

        holder.update.setOnClickListener() {
            val cmd = "CFG,WAKEWORD,SET,JARVIS_COMMAND,$device_id,${holder.jarvis_dev_action.text}"
            Send_cmd(activity.value, cmd, activity).execute()
        }
    }
    fun update() {
        notifyDataSetChanged()
    }
}