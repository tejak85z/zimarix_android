package com.example.zimarix_1.Activities

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextSwitcher
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.GSwitches
import com.example.zimarix_1.R
import com.example.zimarix_1.Send_cmd
import com.example.zimarix_1.dev_action
import com.example.zimarix_1.dev_req
import com.example.zimarix_1.port_1_8
import com.example.zimarix_1.sw_params

import com.example.zimarix_1.update_params
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class scheduler : AppCompatActivity() , update_params {

    lateinit var schedulerAdapter: SchedulerSwitchAdapter
    private lateinit var recyclerView: RecyclerView
    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    private lateinit var maindialog:AlertDialog
    var schedules: List<String> = emptyList()
    var current_config = ""
    var value = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheduler)

        progressBar = findViewById(R.id.schedulerprogressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        CoroutineScope(Dispatchers.IO).launch {
            schedules_updater(value,this@scheduler)
        }

        val add_schedule = findViewById<Button>(R.id.add_schedule)
        add_schedule.setOnClickListener {
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 40, 50, 10)

            val h1layout = LinearLayout(this)
            h1layout.orientation = LinearLayout.HORIZONTAL
            h1layout.setPadding(50, 40, 50, 10)
            // Create NumberPicker for hours
            val device = Spinner(this)
            val items = GSwitches.map { it.name }
            var  devadapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
            devadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            device.adapter = devadapter
            device.onItemSelectedListener = null
            h1layout.addView(device)

            val action = Spinner(this)
            var actionadapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, dev_action)
            actionadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            action.adapter = actionadapter
            action.onItemSelectedListener = null
            h1layout.addView(action)
            layout.addView(h1layout)

            val time = TextView(this)
            time.text = "\n\n       HOURS     :     MINUTES"
            layout.addView(time)

            val hlayout = LinearLayout(this)
            hlayout.orientation = LinearLayout.HORIZONTAL
            hlayout.setPadding(50, 40, 50, 10)
            // Create NumberPicker for hours
            val hoursPicker = NumberPicker(this)
            hoursPicker.minValue = 0
            hoursPicker.maxValue = 23
            hlayout.addView(hoursPicker)

            // Create NumberPicker for minutes
            val minutesPicker = NumberPicker(this)
            minutesPicker.minValue = 0
            minutesPicker.maxValue = 59
            hlayout.addView(minutesPicker)

            layout.addView(hlayout)

            val repeat = CheckBox(this)
            repeat.text = "REPEAT EVERYDAY"
            layout.addView(repeat)

            val builder = AlertDialog.Builder(this)
                .setTitle("ADD SCHEDULE")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { maindialog, whichButton ->
                    // so something, or not - dialog will close
                    maindialog.dismiss()
                }
                .setPositiveButton(android.R.string.ok){ maindialog, whichButton ->
                    var act = "1"
                    if (repeat.isChecked)
                        act = "2"
                    val cmd = "CFG,SCH,ADD,"+ GSwitches[device.selectedItemPosition].id +',' +
                            String.format("%02d", hoursPicker.value) +':'+
                            String.format("%02d", minutesPicker.value) +
                            ','+ action.selectedItem +','+act+','
                    Log.d("debug ", "===============  $cmd \n")
                    if (this.value >= 0)
                        Send_cmd(this.value,cmd,this).execute()
                    maindialog.dismiss()
                }
            maindialog = builder.create()
            maindialog.show()
        }


        recyclerView = findViewById(R.id.schedulerrecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        schedulerAdapter = SchedulerSwitchAdapter(this)
        recyclerView.adapter = schedulerAdapter
    }

    fun update_schedule_config(devindex:Int): Int {
        var ret = 0
        val resp = dev_req(devindex, "CFG,SCH,GET,")
        if (resp.length > 1 && resp != current_config) {
            current_config = resp
            schedules = resp.split(",").filter { it.isNotBlank() && it != "none"}
            return 1
        }
        return 0
    }
    fun schedules_updater(index: Int, scheduler: scheduler){
        var ret = 0
        while (active == true){
            if (scheduler.active == true) {
                ret = 0
                if (update_schedule_config(index) != 0) {
                    ret = 1
                }
                if (ret == 1) {
                    GlobalScope.launch(Dispatchers.Main) {
                        // Update UI here
                        schedulerAdapter.update()
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
class SchedulerSwitchAdapter(private val activity: scheduler)
    : RecyclerView.Adapter<SchedulerSwitchAdapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val time: Button = itemView.findViewById(R.id.time)
        val dev: Button = itemView.findViewById(R.id.device)
        val action: Spinner = itemView.findViewById(R.id.action)
        val repeat: CheckBox = itemView.findViewById(R.id.repeat)
        val state: Switch = itemView.findViewById(R.id.active)
        val delete: Button = itemView.findViewById(R.id.delete)
    }
    override fun getItemCount(): Int {
        return activity.schedules.size
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_scheduler_config, parent, false)
        return ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        val param = activity.schedules[index].split("_")
        if (param.size == 5){
            holder.delete.setOnClickListener {
                val cmd = "CFG,SCH,DELETE," + param[0] +','
                if (activity.value >= 0) {
                    Send_cmd(activity.value, cmd, activity).execute()
                }
            }
            holder.time.text = param[2]
            val time = param[2].split(":")
            holder.time.setOnClickListener {
                val layout = LinearLayout(activity)
                layout.orientation = LinearLayout.HORIZONTAL
                layout.setPadding(50, 40, 50, 10)

                // Create NumberPicker for hours
                val hoursPicker = NumberPicker(activity)
                hoursPicker.minValue = 0
                hoursPicker.maxValue = 23
                layout.addView(hoursPicker)

                // Create NumberPicker for minutes
                val minutesPicker = NumberPicker(activity)
                minutesPicker.minValue = 0
                minutesPicker.maxValue = 59
                layout.addView(minutesPicker)

                if (time.size == 2){
                    val hr = time[0].toInt()
                    if (hr >= 0 && hr < 24)
                        hoursPicker.value = hr
                    val min = time[1].toInt()
                    if (min >= 0 && min < 60)
                        minutesPicker.value = min
                }

                val builder = AlertDialog.Builder(activity)
                    .setTitle("HOURS : MINUTES")
                    .setView(layout)
                    .setNegativeButton(android.R.string.cancel) { maindialog, whichButton ->
                        // so something, or not - dialog will close
                        maindialog.dismiss()
                    }
                    .setPositiveButton(android.R.string.ok){ maindialog, whichButton ->
                        // so something, or not - dialog will close
                        val cmd = "CFG,SCH,UPDATE," + param[0] +','+ param[1] +',' +
                                String.format("%02d", hoursPicker.value) +':'+
                                String.format("%02d", minutesPicker.value) +
                                ','+ param[3] +','+param[4]+','
                        if (activity.value >= 0)
                            Send_cmd(activity.value,cmd,activity).execute()
                        maindialog.dismiss()
                    }
                maindialog = builder.create()
                maindialog.show()
            }

            if (param[4] == "0") {
                holder.repeat.isChecked = false
                holder.state.isChecked = false
            } else if(param[4] == "1") {
                holder.repeat.isChecked = false
                holder.state.isChecked = true
            }else if(param[4] == "2") {
                holder.repeat.isChecked = true
                holder.state.isChecked = true
            }
            holder.state.setOnClickListener(){
                var state = "-1"
                if (holder.state.isChecked == true){
                    if (holder.repeat.isChecked){
                        state = "2"
                    }else
                        state = "1"
                }else{
                    state = "0"
                    holder.repeat.isChecked = false
                }
                val cmd = "CFG,SCH,UPDATE," + param[0] +','+ param[1] +
                        ','+param[2]+','+ param[3]+','+state+','
                if (activity.value >= 0)
                    Send_cmd(activity.value,cmd,activity).execute()
            }

            holder.repeat.setOnClickListener(){
                var state = "-1"
                if (holder.repeat.isChecked){
                    state = "2"
                    holder.state.isChecked = true
                }else{
                    if (holder.state.isChecked == true){
                        state = "1"
                    }else
                        state = "0"
                }
                val cmd = "CFG,SCH,UPDATE," + param[0] +','+ param[1] +
                        ','+param[2]+','+ param[3]+','+state+','
                if (activity.value >= 0)
                    Send_cmd(activity.value,cmd,activity).execute()
            }

            val foundSwitch: sw_params? = GSwitches.find { it.id == param[1] }
            holder.dev.text = foundSwitch?.name
            holder.dev.setOnClickListener {
                val layout = LinearLayout(activity)
                layout.orientation = LinearLayout.HORIZONTAL
                layout.setPadding(50, 40, 50, 10)

                // Create NumberPicker for hours
                val dev_list = ListView(activity)
                val items = GSwitches.map { it.name }
                val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, items)
                dev_list.adapter = adapter

                dev_list.setOnItemClickListener { _, _, position, _ ->
                    val clickedSwitch = GSwitches[position]
                    val cmd = "CFG,SCH,UPDATE," + param[0] +','+ clickedSwitch.id +',' +
                            param[2] + ','+ param[3] +','+param[4]+','
                    if (activity.value >= 0)
                        Send_cmd(activity.value,cmd,activity).execute()
                    maindialog.dismiss()
                }

                layout.addView(dev_list)

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

            var adapter: ArrayAdapter<String> = ArrayAdapter(activity, android.R.layout.simple_spinner_item, dev_action)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            holder.action.adapter = adapter
            holder.action.onItemSelectedListener = null
            if (param[3] == "ON")
                holder.action.setSelection(0)
            else
                holder.action.setSelection(1)
            holder.action.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (param[3] != dev_action[position]) {
                        val cmd =
                            "CFG,SCH,UPDATE," + param[0] + ',' + param[1] + ',' +
                                    param[2] + ',' + dev_action[position] + ',' + param[4] + ','
                        if (activity.value >= 0)
                            Send_cmd(activity.value, cmd, activity).execute()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
    }
    fun update() {
        notifyDataSetChanged()
    }
}



