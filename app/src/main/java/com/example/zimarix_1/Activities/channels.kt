package com.example.zimarix_1.Activities

import android.app.AlertDialog
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
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

class channels : AppCompatActivity() , update_params {
    lateinit var channelAdapter: channelsSwitchAdapter
    private lateinit var recyclerView: RecyclerView
    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    private lateinit var maindialog: AlertDialog
    var schedules: List<String> = emptyList()
    var current_config = ""
    var value = -1
    var channel_list: List<String> = listOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels)

        progressBar = findViewById(R.id.channels_progressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        CoroutineScope(Dispatchers.IO).launch {
            channels_updater(value)
        }

        val add_channel: Button = findViewById(R.id.add_channels)
        add_channel.setOnClickListener(){
            lateinit var maindialog:AlertDialog
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 40, 50, 10)


            val name = EditText(this)
            name.hint = "ENTER CHANNEL NAME"
            layout.addView(name)

            val number = EditText(this)
            number.hint = "ENTER CHANNEL NUMBER"
            layout.addView(number)

            val builder = AlertDialog.Builder(this)
                .setTitle("ADD CHANNEL")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { maindialog, whichButton ->
                    // so something, or not - dialog will close
                    maindialog.dismiss()
                }
                .setPositiveButton(android.R.string.ok){ maindialog, whichButton ->
                    var act = "1"
                    if (name.text.length > 0 && name.text.length < 32 &&
                        number.text.length > 0 && number.text.length < 5) {
                        val cmd = "IR,SET,CHANNEL,ADD,${name.text},${number.text},"
                        Send_cmd(this.value, cmd, this).execute()
                    }
                    maindialog.dismiss()
                }
            maindialog = builder.create()
            maindialog.show()
        }

        recyclerView = findViewById(R.id.channelrecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        channelAdapter = channelsSwitchAdapter(this)
        recyclerView.adapter = channelAdapter
    }

    fun channels_updater(index: Int){
        var ret = 0
        while (true){
            if (active == true) {
                val resp = dev_req(index, "IR,GET,CHANNEL,")
                val validate_len = "CHANNELS:".length
                if (resp.length >= validate_len && resp != current_config) {
                    current_config = resp
                    val Presp = resp.replace("CHANNELS:","")
                    channel_list = Presp.split(",").filter { it.contains("_") }
                    GlobalScope.launch(Dispatchers.Main) {
                        // Update UI here
                        channelAdapter.update()
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

class channelsSwitchAdapter(private val activity: channels)
    : RecyclerView.Adapter<channelsSwitchAdapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.channel_name)
        val number: EditText = itemView.findViewById(R.id.channel_number)
        val update: Button = itemView.findViewById(R.id.channel_update)
        val delete: Button = itemView.findViewById(R.id.channel_delete)

    }
    override fun getItemCount(): Int {
        return activity.channel_list.size
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_channels, parent, false)
        return ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        val parts = activity.channel_list[index].split("_", limit = 2)

        val firstPart = parts.firstOrNull() ?: ""
        val secondPart = parts.getOrNull(1)?.removePrefix("_") ?: ""
        Log.d("debug ", "===============  $firstPart $secondPart \n")
        holder.name.text = parts.getOrNull(1)?.removePrefix("_") ?: ""
        holder.number.hint = parts.firstOrNull() ?: ""
        holder.number.text = Editable.Factory.getInstance().newEditable("")
        if (holder.number.hint == "-1")
            holder.name.setTextColor(Color.RED)

        holder.update.setOnClickListener(){
            if(holder.number.text.length > 0 && holder.number.text.length < 5){
                val cmd ="IR,SET,CHANNEL,UPDATE,${holder.name.text},${holder.number.text},"
                Send_cmd(activity.value, cmd, activity).execute()
            }
        }
        holder.delete.setOnClickListener(){
            val cmd ="IR,SET,CHANNEL,DELETE,${holder.name.text},"
            Send_cmd(activity.value, cmd, activity).execute()
        }

    }
    fun update() {
        notifyDataSetChanged()
    }
}