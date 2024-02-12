package com.example.zimarix_1.Activities

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.R
import com.example.zimarix_1.Send_cmd
import com.example.zimarix_1.databinding.ActivityAvsregistrationBinding
import com.example.zimarix_1.dev_req
import com.example.zimarix_1.update_params
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Alexa : AppCompatActivity() , update_params {
    lateinit var alexaAdapter: alexaAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerScanView: RecyclerView

    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    var value = -1
    var prev_config = ""
    var alexa_enabled = "0"
    var alexa_status = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alexa)

        progressBar = findViewById(R.id.alexaprogressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        CoroutineScope(Dispatchers.IO).launch {
            alexa_updater(value,this@Alexa)
        }

        val t = findViewById<Button>(R.id.alexa_t)
        t.setOnClickListener(){
            val cmd = "AVS,t,"
            Send_cmd(value, cmd, this).execute()
        }
        val s = findViewById<Button>(R.id.alexa_s)
        s.setOnClickListener(){
            val cmd = "AVS,s,"
            Send_cmd(value, cmd, this).execute()
        }
        val p1 = findViewById<Button>(R.id.alexa_1)
        p1.setOnClickListener(){
            val cmd = "AVS,1,"
            Send_cmd(value, cmd, this).execute()
        }
        val p2 = findViewById<Button>(R.id.alexa_2)
        p2.setOnClickListener(){
            val cmd = "AVS,2,"
            Send_cmd(value, cmd, this).execute()
        }
        val p3 = findViewById<Button>(R.id.alexa_3)
        p3.setOnClickListener(){
            val cmd = "AVS,3,"
            Send_cmd(value, cmd, this).execute()
        }
        val p4 = findViewById<Button>(R.id.alexa_4)
        p4.setOnClickListener(){
            val cmd = "AVS,4,"
            Send_cmd(value, cmd, this).execute()
        }
        val pz = findViewById<Button>(R.id.alexa_z)
        pz.setOnClickListener(){
            val cmd = "AVS,z,"
            Send_cmd(value, cmd, this).execute()
        }


        recyclerView = findViewById(R.id.alexarecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        alexaAdapter = alexaAdapter(this)
        recyclerView.adapter = alexaAdapter
    }
    fun update_alexa_config(devindex:Int): Int {
        var ret = 0
        val resp = dev_req(devindex, "CFG,AVS,GET,ALEXA,")
        if (resp.length >= 1 && resp != prev_config) {
            prev_config = resp
            val param = resp.split(",")
            if (param[0] == "1" || param[0] == "0")
                alexa_enabled = param[0]
            return 1
        }
        return 0
    }
    fun alexa_updater(index: Int, activity: Alexa){
        var ret = 0
        while (active == true){
            ret = 0
            if (update_alexa_config(index) != 0) {
                ret = 1
            }
            if (ret == 1) {
                GlobalScope.launch(Dispatchers.Main) {
                    // Update UI here
                    alexaAdapter.update()
                }
            }

            val resp = dev_req(index, "AVSMGR,STATUS,")
            if (resp.length > 1 && resp != alexa_status) {
                alexa_status = resp
                GlobalScope.launch(Dispatchers.Main) {
                    // Update UI here
                    alexaAdapter.update()
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

class alexaAdapter(private val activity: Alexa)
    : RecyclerView.Adapter<alexaAdapter.ViewHolder>() {
    private lateinit var maindialog: AlertDialog
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alexa_enable: Switch = itemView.findViewById(R.id.alexa_enable)
        val alexa_status: TextView = itemView.findViewById(R.id.alexastatus)
        val alexa_activate: TextView = itemView.findViewById(R.id.avs_activate)

    }
    override fun getItemCount(): Int {
        return 1
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_alexa, parent, false)
        return ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        if (activity.alexa_enabled == "1")
            holder.alexa_enable.isChecked = true
        else
            holder.alexa_enable.isChecked = false
        holder.alexa_enable.setOnClickListener(){
            var cmd = ""
            if (holder.alexa_enable.isChecked == true)
                cmd = "CFG,AVS,SET,ALEXA,1"
            else
                cmd = "CFG,AVS,SET,ALEXA,0"
            Send_cmd(activity.value, cmd, activity).execute()
        }

        holder.alexa_status.text = "ALEXA STATUS : "+ activity.alexa_status.replace(",","")

        if (!activity.alexa_status.contains("Login to ", ignoreCase = true)) {
            holder.alexa_activate.visibility = View.GONE
        }else{
            holder.alexa_activate.visibility = View.VISIBLE
        }
        holder.alexa_activate.setOnClickListener(){
            val startIndex = activity.alexa_status.indexOf("Login to ") + "Login to ".length
            val endIndex = activity.alexa_status.indexOf(" and use code ")
            val url = activity.alexa_status.substring(startIndex, endIndex).trim()

            val codeStartIndex = endIndex + " and use code ".length
            val codeEndIndex = activity.alexa_status.indexOf( "to activate")
            val code = activity.alexa_status.substring(codeStartIndex, codeEndIndex).trim()

            // Construct the complete URL
            val completeUrl = "$url?code=$code"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                //showNotification(activity, code)
                showNotification1(activity, "ALEXA ACTIVATION CODE", code)
            }
            // Open the URL in a web browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(completeUrl))
            activity.startActivity(intent)
        }
    }

    private fun showNotification(context: Context, code: String) {
        val channelId = "notification_channel_id"
        val notificationId = 1001

        // Create notification channel
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notification Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Create notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.avs_code)
            .setContentTitle("Alexa Activation Code")
            .setContentText("Registration Code: $code")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Show notification
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    fun showNotification1(context: Context, title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "default_channel_id"
        val channelName = "Default Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_email) // Replace with your desired icon

        // Show notification
        notificationManager.notify(1, notificationBuilder.build())
    }
    fun update() {
        notifyDataSetChanged()
    }
}