package com.example.zimarix_1.Activities

import android.app.AlertDialog
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SeekBar
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
import com.example.zimarix_1.dev_req
import com.example.zimarix_1.port_1_8
import com.example.zimarix_1.sw_params
import com.example.zimarix_1.update_params
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class camsettings : AppCompatActivity() , update_params {

    lateinit var config_adapter: cam_config_Adapter
    lateinit var gesture_adapter: cam_gesture_Adapter

    private lateinit var configView: RecyclerView
    private lateinit var gestureView: RecyclerView

    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true
    var value = -1
    var prev_config = ""
    var prev_gesture_config = ""

    //cam_config_params
    var cam_enable = "0"
    var gesture_enable = "0"
    var motion_enable = "0"
    var motion_or_face = 0
    var motion_sensitivity = 0

    data class gesture_info(var id:Int, var gesture:Int, var device:String, var cmd:String)
    val gestures = mutableListOf<gesture_info>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camsettings)
        progressBar = findViewById(R.id.cam_config_progressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        gestures.add(gesture_info(1,R.mipmap.g1,"-1","null"))
        gestures.add(gesture_info(3,R.mipmap.g3,"-1","null"))
        gestures.add(gesture_info(6,R.mipmap.g6,"-1","null"))
        gestures.add(gesture_info(7,R.mipmap.g7,"-1","null"))
        gestures.add(gesture_info(8,R.mipmap.g8,"-1","null"))
        gestures.add(gesture_info(9,R.mipmap.g9,"-1","null"))
        gestures.add(gesture_info(12,R.mipmap.g12,"-1","null"))
        gestures.add(gesture_info(13,R.mipmap.g13,"-1","null"))
        gestures.add(gesture_info(14,R.mipmap.g14,"-1","null"))
        gestures.add(gesture_info(15,R.mipmap.g15,"-1","null"))

        CoroutineScope(Dispatchers.IO).launch {
            cam_config_updater(value)
        }
        configView = findViewById(R.id.cam_config_settings)
        configView.layoutManager = LinearLayoutManager(this)
        config_adapter = cam_config_Adapter(this)
        configView.adapter = config_adapter

        gestureView = findViewById(R.id.cam_gesture_settings)
        gestureView.layoutManager = LinearLayoutManager(this)
        gesture_adapter = cam_gesture_Adapter(this)
        gestureView.adapter = gesture_adapter
    }
    fun update_cam_config(devindex:Int): Int {
        val resp = dev_req(devindex, "CFG,CAM,GET,CONF,")
        if (resp.length > 1 && resp != prev_config) {
            prev_config = resp
            val param = resp.split(",")
            if (param[0] == "1" || param[0] == "0")
                cam_enable = param[0]
            if (param[1] == "1" || param[1] == "0")
                gesture_enable = param[1]
            if (param[2] == "1" || param[2] == "0")
                motion_enable = param[2]
            if (param[3] == "1")
                motion_or_face = 1
            else if(param[3] == "0")
                motion_or_face = 0
            val snsty = param[4].toInt()
            if (snsty >= 0 && snsty <= 100)
                motion_sensitivity = snsty
            return 1
        }
        return 0
    }
    fun cam_config_updater(index: Int){
        var ret = 0
        while (active == true){
            ret = 0
            if (update_cam_config(index) != 0) {
                ret = 1
            }
            if (ret == 1) {
                GlobalScope.launch(Dispatchers.Main) {
                    // Update UI here
                    config_adapter.update()
                }
            }
            val resp = dev_req(index, "CFG,CAM,GET,GESTURES,")
            if (resp.length > 1 && resp != prev_gesture_config) {
                prev_gesture_config = resp
                val dev_gestures = resp.split(",")
                dev_gestures.forEach(){
                    val params = it.split("_")
                    if (params.size == 3){
                        val gest = gestures.firstOrNull { it.id == params[0].toInt() }
                        if (gest != null) {
                            gest.device = params[1]
                            gest.cmd = params[2]
                            Log.d("debug ", "=============== $gest  \n")
                        }
                    }
                }
                GlobalScope.launch(Dispatchers.Main) {
                    // Update UI here
                    gesture_adapter.update()
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

class cam_config_Adapter(private val activity: camsettings)
    : RecyclerView.Adapter<cam_config_Adapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog
    val motion_types = arrayOf("motion","face")

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cam_enable: Switch = itemView.findViewById(R.id.cam_enable)
        val gesture_enable: Switch = itemView.findViewById(R.id.gesture_enable)
        val motion_enable: Switch = itemView.findViewById(R.id.use_motion)
        val face_or_motion: Spinner = itemView.findViewById(R.id.cam_motion_type)
        val motion_snsty: SeekBar = itemView.findViewById(R.id.motion_sensitivity)
    }
    override fun getItemCount(): Int {
        return 1
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_cam_config, parent, false)
        return ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        if (activity.cam_enable == "1")
            holder.cam_enable.isChecked = true
        else
            holder.cam_enable.isChecked = false
        holder.cam_enable.setOnClickListener(){
            var cmd = ""
            if (holder.cam_enable.isChecked == true)
                cmd = "CFG,CAM,SET,CAM_ENABLE,1,"
            else
                cmd = "CFG,CAM,SET,CAM_ENABLE,0,"
            Send_cmd(activity.value, cmd, activity).execute()
        }

        if (activity.gesture_enable == "1")
            holder.gesture_enable.isChecked = true
        else
            holder.gesture_enable.isChecked = false
        holder.gesture_enable.setOnClickListener(){
            var cmd = ""
            if (holder.gesture_enable.isChecked == true)
                cmd = "CFG,CAM,SET,GESTURE_ENABLE,1,"
            else
                cmd = "CFG,CAM,SET,GESTURE_ENABLE,0,"
            Send_cmd(activity.value, cmd, activity).execute()
        }

        if (activity.motion_enable == "1")
            holder.motion_enable.isChecked = true
        else
            holder.motion_enable.isChecked = false
        holder.motion_enable.setOnClickListener(){
            var cmd = ""
            if (holder.motion_enable.isChecked == true)
                cmd = "CFG,CAM,SET,CAM_MOTION_ALERT,1,"
            else
                cmd = "CFG,CAM,SET,CAM_MOTION_ALERT,0,"
            Send_cmd(activity.value, cmd, activity).execute()
        }

        var adapter: ArrayAdapter<String>? = null
        adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, motion_types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.face_or_motion.adapter = adapter
        holder.face_or_motion.onItemSelectedListener = null
        holder.face_or_motion.setSelection(activity.motion_or_face)
        holder.face_or_motion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != activity.motion_or_face) {
                    val cmd = "CFG,CAM,SET,MOTION_OR_FACE,${position.toString()},"
                    Send_cmd(activity.value, cmd, activity).execute()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        holder.motion_snsty.progress = activity.motion_sensitivity
        holder.motion_snsty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val snsty = holder.motion_snsty.progress.toString()
                val cmd = "CFG,CAM,SET,MOTION_SENSITIVITY,$snsty,"
                Send_cmd(activity.value, cmd, activity).execute()
            }
        })
    }
    fun update() {
        notifyDataSetChanged()
    }
}
class cam_gesture_Adapter(private val activity: camsettings)
    : RecyclerView.Adapter<cam_gesture_Adapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gesture_img: ImageView = itemView.findViewById(R.id.gesture_image)
        val gesture_device: Button = itemView.findViewById(R.id.gesture_device)
        val gesture_cmd: Button = itemView.findViewById(R.id.gesture_cmd)
    }
    override fun getItemCount(): Int {
        return activity.gestures.size
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_gesture, parent, false)
        return ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        holder.gesture_img.setImageResource(activity.gestures[index].gesture)
        holder.gesture_img.setOnClickListener {
        }
        if (activity.gestures[index].device == "0"){
            holder.gesture_device.text = "ALEXA"
        }else if (activity.gestures[index].device == "-1"){
            holder.gesture_device.text = "EMPTY"
        }else{
            val foundSwitch: sw_params? = GSwitches.find { it.id == activity.gestures[index].device }
            holder.gesture_device.text = foundSwitch?.name
        }
        holder.gesture_device.setOnClickListener(){
            val layout = LinearLayout(activity)
            layout.orientation = LinearLayout.HORIZONTAL
            layout.setPadding(50, 40, 50, 10)

            // Create NumberPicker for hours
            val dev_list = ListView(activity)
            val items = arrayOf("null", "ALEXA") + GSwitches.map { it.name }
            val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, items)
            dev_list.adapter = adapter

            dev_list.setOnItemClickListener { _, _, position, _ ->
                var cmd = ""
                if (position == 0 ) {
                    cmd = "CFG,CAM,SET,GESTURE,${activity.gestures[index].id.toString()},-1,null"
                }else if (position == 1){
                    cmd = "CFG,CAM,SET,GESTURE,${activity.gestures[index].id.toString()},0,listen"
                }else {
                    val clickedSwitch = GSwitches[position - 2]
                    cmd = "CFG,CAM,SET,GESTURE," +
                            "${activity.gestures[index].id.toString()}," +
                            "${clickedSwitch.id},${activity.gestures[index].cmd}"
                }
                Send_cmd(activity.value, cmd, activity).execute()
                maindialog.dismiss()
            }
            layout.addView(dev_list)
            val builder = AlertDialog.Builder(activity)
                .setView(layout)
            maindialog = builder.create()
            maindialog.show()
        }

        holder.gesture_cmd.text = activity.gestures[index].cmd
        holder.gesture_cmd.setOnClickListener(){
            val layout = LinearLayout(activity)
            layout.orientation = LinearLayout.HORIZONTAL
            layout.setPadding(50, 40, 50, 10)

            // Create NumberPicker for hours
            val cmd_list = ListView(activity)
            var items: Array<String>
            if (activity.gestures[index].device == "0"){
                items = arrayOf("listen","stop","play","pause","next","previous" )
            }else if (activity.gestures[index].device == "-1"){
                items = arrayOf("null")
            }else{
                val id = activity.gestures[index].device.toInt()
                if (id > 0 && id < 16){
                    items = arrayOf("FLIP", "ON", "OFF")
                }else{
                    items = arrayOf("ON", "OFF")
                }
            }
            val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, items)
            cmd_list.adapter = adapter
            cmd_list.setOnItemClickListener { _, _, position, _ ->
                val cmd = "CFG,CAM,SET,GESTURE," +
                        "${activity.gestures[index].id.toString()}," +
                        "${activity.gestures[index].device},${items[position]},"
                Send_cmd(activity.value,cmd,activity).execute()
                maindialog.dismiss()
            }
            layout.addView(cmd_list)
            val builder = AlertDialog.Builder(activity)
                .setView(layout)
            maindialog = builder.create()
            maindialog.show()
        }
    }
    fun update() {
        notifyDataSetChanged()
    }
}