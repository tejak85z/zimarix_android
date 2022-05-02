package com.example.zimarix_1.ui.main

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.zimarix_1.*
import com.example.zimarix_1.databinding.FragmentMainBinding
import com.example.zimarix_1.zimarix_global.Companion.channelId
import com.example.zimarix_1.zimarix_global.Companion.curr_device
import com.example.zimarix_1.zimarix_global.Companion.dev_config
import com.example.zimarix_1.zimarix_global.Companion.notificationManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.net.Socket
import java.net.SocketException

//import com.example.zimarix_1.zimarix_global.Companion.builder
//import com.example.zimarix_1.zimarix_global.Companion.notificationManager

class SettingsFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private var _binding1: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding1 get() = _binding1!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding1 = FragmentMainBinding.inflate(inflater, container, false)
        val root = binding1.root

        val listView: ListView = binding1.sectionList
        var pioneers=arrayOf<String>()
        pageViewModel.text.observe(viewLifecycleOwner, Observer {
            if (it == "1") {
                var ports=arrayOf<String>()
                dev_config.forEach{
                    val port = it.split(",")
                    if (port.size == 7 && port[0]=="port" && port[1].toInt() < 32) {
                        ports = ports + it
                        pioneers = pioneers + port[2]
                    }
                }
                listView.setOnItemClickListener(AdapterView.OnItemClickListener { arg0, arg1, position, arg3 ->
                    val layout = LinearLayout(context)
                    val port = ports[position].split(",")
                    layout.orientation = LinearLayout.VERTICAL
                    if(port[3] == "0") {
                        val enable = Switch(context)
                        enable.text = "ENABLE " + port[2]
                        enable.textSize = 16F
                        layout.addView(enable)

                        layout.setPadding(50, 40, 50, 10)

                        val builder = AlertDialog.Builder(context)
                            .setTitle("Enable " + port[2])
                            .setView(layout)
                            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                                // so something, or not - dialog will close
                            }
                        val dialog = builder.create()
                        enable.setOnClickListener {
                            set_port_params(position,port)
                            dialog.dismiss()
                        }
                        dialog.show()
                    }else{
                        set_port_params(position,port)
                    }
                })
            } else if (it == "2") {
                var configs = arrayOf<String>()
                dev_config.forEach{
                    val conf = it.split(",")
                    if (conf[0]=="conf") {
                        configs = configs + it
                        pioneers = pioneers + conf[1]
                    }
                }
                pioneers = pioneers + "Bluetooth Speaker,enable remote support , upgrade ".split(",")
                listView.setOnItemClickListener(AdapterView.OnItemClickListener { arg0, arg1, position, arg3 ->
                    if(pioneers[position] == "Offline Wake Word Config"){
                        val intent = Intent(getActivity(), Wakeword::class.java)
                        startActivity(intent)
                        //process_wakeword_settings(configs[position])
                    } else if (pioneers[position] == "Power Save Settings"){
                        power_save_dialog(configs[position])
                    }else if (pioneers[position] == "SPEAKER Settings"){
                        volume_dialog(configs[position])
                    }else if (pioneers[position] == "LED Settings"){
                        led_dialog()
                    }else if (pioneers[position] == "ALEXA Settings"){
                    /*    val intent = Intent(context, afterNotification::class.java).apply{
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
                        val progressMax = 100
                        val notification =
                            NotificationCompat.Builder(context!!, channelId)
                                .setSmallIcon(R.drawable.ic_launcher_background)
                                .setContentTitle("ALEXA ACTIVATION CODE")
                                .setContentText("12345")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                                //.setOngoing(true)
                                //.setOnlyAlertOnce(true)
                                .setContentIntent(pendingIntent)
                                //.setAutoCancel(true)
                        notificationManager.notify(1, notification.build())
                      */
                        val ret = get_service_config("AVS")
                        alexa_dialog(ret)
                    }else if (pioneers[position] == "Bluetooth Speaker"){
                        //val intent = Intent(getActivity(), btspeaker::class.java)
                        //startActivity(intent)
                    }
                })
            }
            else if (it == "3") {
                var ports=arrayOf<String>()
                pioneers = pioneers + "ADD NEW REMOTE".split(",")
                dev_config.forEach{
                    val port = it.split(",")
                    if (port.size == 7 && port[0]=="port" && port[1].toInt() >= 32) {
                        ports = ports + it
                        pioneers = pioneers + port[2]
                    }
                }
                listView.setOnItemClickListener(AdapterView.OnItemClickListener { arg0, arg1, position, arg3 ->

                    if (position == 0) {
                        add_new_remote()
                    }else{
                        show_remote(ports[position-1])
                    }
                })
                listView.setOnItemLongClickListener { parent, view, position, id ->
                    val param = ports[position-1].split(",")
                     if (position != 0) {
                         val builder = AlertDialog.Builder(context)
                             .setTitle(param[2] + " control")
                             .setPositiveButton("Delete " + param[2], null)
                             .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                                 // so something, or not - dialog will close
                                 dialog.dismiss()
                             }
                         val dialog = builder.create()
                         dialog.setOnShowListener {
                             val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                             okButton.setOnClickListener {
                                 val req = "U,IR,del,"+param[1] //delete device
                                 encrypt_and_send_data(req)
                                 dialog.dismiss()
                             }
                         }
                         dialog.show()
                    }
                    return@setOnItemLongClickListener(true)
                }
            }
            val adapter = activity?.let {
                ArrayAdapter<String>(
                    it,
                    android.R.layout.simple_list_item_1,
                    pioneers
                )
            }
            listView.adapter = adapter
        })

        return root
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): SettingsFragment {
            return SettingsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding1 = null
    }

    fun alexa_dialog(avsconf: String) {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        val params = avsconf.split(",")
        val enable = params[1]
        val ww_enable = params[2]
        val snsty = params[3]
        val mute = params[4]
        val code = params[5]
        val auth = params[6]

        val avsenable = Switch(context)
        layout.addView(avsenable)
        avsenable.text = "Enable Alexa Voice Service"
        if (enable == "1") {
            avsenable.isChecked = true
            if (auth == "1" ) {
                val avsww = Switch(context)
                avsww.text = "Use keyword ALEXA to wake up device"
                if (ww_enable == "1") {
                    avsww.isChecked = true
                } else
                    avsww.isChecked = false
                avsww.textSize = 16F
                layout.addView(avsww)
                avsww.setOnClickListener() {
                    val req = "I,AVS,WW," + avsww.isChecked
                    encrypt_and_send_data(req)
                }

                val snsty_info = TextView(context)
                snsty_info.text = "ALEXA SENSITIVITY"
                layout.addView(snsty_info)

                val alexa_snsty = SeekBar(context)
                if (snsty.length > 0)
                    alexa_snsty.progress = (snsty.toFloat()*100).toInt()
                layout.addView(alexa_snsty)

                alexa_snsty.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seek: SeekBar,
                                                   progress: Int, fromUser: Boolean) {}
                    override fun onStartTrackingTouch(seek: SeekBar) {}
                    override fun onStopTrackingTouch(seek: SeekBar) {
                        val req = "U,ww,sensitivity,alexa_raspberry-pi.ppn:"+((seek.progress.toFloat())/100).toString()
                        encrypt_and_send_data(req)
                    }
                })

                val avsmute = Switch(context)
                avsmute.text = "mute"
                if (mute == "0") {
                    avsmute.isChecked = true
                } else
                    avsmute.isChecked = false

                layout.addView(avsmute)
                avsmute.setOnClickListener() {
                    val req = "U,VOL,MUTE,"+avsmute.isChecked
                    encrypt_and_send_data(req)
                }

                val avslisten = Button(context)
                avslisten.text = "listen"
                avslisten.setOnClickListener {
                    val req = "I,AVS,listen"
                    encrypt_and_send_data(req)
                }
                layout.addView(avslisten)

                val avsstop = Button(context)
                avsstop.text = "stop"
                avsstop.setOnClickListener {
                    val req = "I,AVS,stop"
                    encrypt_and_send_data(req)
                }
                layout.addView(avsstop)

                val avsplay = Button(context)
                avsplay.text = "play"
                avsplay.setOnClickListener {
                    val req = "I,AVS,play"
                    encrypt_and_send_data(req)
                }
                layout.addView(avsplay)

                val avspause = Button(context)
                avspause.text = "pause"
                avspause.setOnClickListener {
                    val req = "I,AVS,pause"
                    encrypt_and_send_data(req)
                }
                layout.addView(avspause)

                val avsnext = Button(context)
                avsnext.text = "next"
                avsnext.setOnClickListener {
                    val req = "I,AVS,next"
                    encrypt_and_send_data(req)
                }
                layout.addView(avsnext)

                val avsprev = Button(context)
                avsprev.text = "previous"
                avsprev.setOnClickListener {
                    val req = "I,AVS,prev"
                    encrypt_and_send_data(req)
                }
                layout.addView(avsprev)

                val avsreauth = Button(context)
                avsreauth.text = "Re-Authorise"
                avsreauth.setOnClickListener {
                    val req = "I,AVS,reauth"
                    encrypt_and_send_data(req)
                }
                layout.addView(avsreauth)

            }else if(auth == "0" && code.length > 1){
                val intent = Intent(getActivity(), avsregistration::class.java)
                val b = Bundle()
                b.putString("avskey",code) //Your id
                intent.putExtras(b)
                startActivity(intent)
                return
            }
        }else {
            avsenable.isChecked = false
        }
        avsenable.textSize = 16F
        avsenable.setOnClickListener(){
            val req = "I,AVS,ENABLE,"+avsenable.isChecked
            encrypt_and_send_data(req)
        }
        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle("ALEXA Settings")
            .setView(layout)
        val dialog = builder.create()
        dialog.show()
    }

    fun led_dialog() {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        val rst = Button(context)
        rst.text = "reset led"
        rst.textSize = 16F
        layout.addView(rst)
        rst.setOnClickListener(){
            val req = "I,LED,RESET,"
            encrypt_and_send_data(req)
        }

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle("LED Settings")
            .setView(layout)
        val dialog = builder.create()
        dialog.show()
    }

    fun volume_dialog(psconf: String) {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        val params = psconf.split(",")
        val enable = params[2]
        val volume = params[3]

        val vol = SeekBar(context)
        if (volume.length > 0)
            vol.progress = volume.toInt()
        layout.addView(vol)

        vol.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                val req = "I,VOL,VAL,"+seek.progress.toString()
                encrypt_and_send_data(req)
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {
                val req = "U,VOL,VAL,"+seek.progress.toString()
                encrypt_and_send_data(req)
            }
        })


        val mutebtn = Switch(context)
        mutebtn.text = "mute"
        if (enable == "0") {
            mutebtn.isChecked = true
        }else
            mutebtn.isChecked = false
        mutebtn.textSize = 16F
        layout.addView(mutebtn)
        mutebtn.setOnClickListener(){
            val req = "U,VOL,MUTE,"+mutebtn.isChecked
            encrypt_and_send_data(req)
        }

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle("Volume Settings")
            .setView(layout)
        val dialog = builder.create()
        dialog.show()
    }

    fun power_save_dialog(psconf: String) {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        val params = psconf.split(",")
        val ps_enable = params[2]
        val ps_params = params[3].split("_")
        val enable = Switch(context)
        enable.text = "Power Save Enable"
        if (ps_enable == "1") {
            enable.isChecked = true
        }else
            enable.isChecked = false
        enable.textSize = 16F
        layout.addView(enable)

        val hlayout1 = LinearLayout(context)
        hlayout1.orientation = LinearLayout.HORIZONTAL

        val timeouttxt = TextView(context)
        timeouttxt.setText("TIMEOUT Minutes: ")
        hlayout1.addView(timeouttxt)
        val timeout = EditText(context)
        timeout.setSingleLine()
        timeout.hint = ps_params[0] + " (min:1 Max:60)"
        hlayout1.addView(timeout)
        layout.addView(hlayout1)

        val auto_on = Switch(context)
        auto_on.text = "Enable Device Auto Turn ON"
        if (ps_params[1] == "1")
            auto_on.isChecked = true
        else
            auto_on.isChecked = false
        auto_on.textSize = 16F
        layout.addView(auto_on)

        val wwflag = Switch(context)
        wwflag.text = "Use Keyword Trigger"
        if (ps_params[2] == "1")
            wwflag.isChecked = true
        else
            wwflag.isChecked = false
        wwflag.textSize = 16F
        layout.addView(wwflag)

        val motion = Switch(context)
        motion.text = "Use Motion Sensor Trigger"
        if (ps_params[3] == "1")
            motion.isChecked = true
        else
            motion.isChecked = false
        motion.textSize = 16F
        layout.addView(motion)

        val camera = Switch(context)
        camera.text = "Use Camera Trigger"
        if (ps_params[4] == "1")
            camera.isChecked = true
        else
            camera.isChecked = false
        camera.textSize = 16F
        layout.addView(camera)

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle("POWER SAVE Settings")
            .setView(layout)
            .setPositiveButton("UPDATE", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
                dialog.dismiss()
            }
        val dialog = builder.create()

        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                val req = "U,PS,"+enable.isChecked+","+timeout.text.toString()+","+auto_on.isChecked+","+wwflag.isChecked+","+motion.isChecked+","+camera.isChecked
                encrypt_and_send_data(req)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun show_remote(data:String){
        //val intent = Intent(getActivity(), Remote::class.java)
        //startActivity(intent)
        val param = data.split(",")

        val ir_dev_req = "I,IR,"+param[1]+",GET"
        val ir_dev_info = encrypt_and_send_data(ir_dev_req)
        Toast.makeText(context, ir_dev_info, Toast.LENGTH_SHORT).show()

        if(param[4] == "11") {
            show_ac_remote(param[1], param[2], ir_dev_info)
        }else if(param[4] == "12") {
            show_tv_remote(param[1], param[2], ir_dev_info)
        }else if(param[4] == "13") {
            show_remote_switch(param[1], param[2], ir_dev_info)
        }else if(param[4] == "14") {
            show_remote_smart_light(param[1], param[2], ir_dev_info)
        }else if(param[4] == "15") {
            show_remote_smart_fan(param[1], param[2], ir_dev_info)
        }else if(param[4] == "16") {
            show_player_remote(param[1], param[2], ir_dev_info)
        }
    }

    fun show_player_remote(dev_id:String, dev : String, dev_info : String){
        val curr_btns = dev_info.split(",")
        if (curr_btns[0] != "keys")
            return

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL


        // ON + OFF
        val hlayout1 = LinearLayout(context)
        hlayout1.orientation = LinearLayout.HORIZONTAL

        val on = add_button("ON", dev, check_state(curr_btns,"ON"),dev_id)
        hlayout1.addView(on)
        val off = add_button("OFF",dev,check_state(curr_btns,"OFF"),dev_id)
        hlayout1.addView(off)
        layout.addView(hlayout1)

        val hlayout5 = LinearLayout(context)
        hlayout5.orientation = LinearLayout.HORIZONTAL
        val mute = add_button("mute",dev,check_state(curr_btns,"mute"),dev_id)
        hlayout5.addView(mute)
        val pause = add_button("pause",dev,check_state(curr_btns,"pause"),dev_id)
        hlayout5.addView(pause)
        val play = add_button("play",dev,check_state(curr_btns,"play"),dev_id)
        hlayout5.addView(play)
        layout.addView(hlayout5)

        val ctrl = TextView(context)
        ctrl.setText("CONTROL")
        //txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        //txt.setTextColor(Color.BLUE)
        layout.addView(ctrl)
        val up = add_button("UP",dev,check_state(curr_btns,"UP"),dev_id)
        layout.addView(up)
        val hlayout6 = LinearLayout(context)
        hlayout6.orientation = LinearLayout.HORIZONTAL
        val prev = add_button("PREV",dev,check_state(curr_btns,"PREV"),dev_id)
        hlayout6.addView(prev)
        val ok = add_button("OK",dev,check_state(curr_btns,"OK"),dev_id)
        hlayout6.addView(ok)
        val next = add_button("NEXT",dev,check_state(curr_btns,"NEXT"),dev_id)
        hlayout6.addView(next)
        layout.addView(hlayout6)
        val down = add_button("DOWN",dev,check_state(curr_btns,"DOWN"),dev_id)
        layout.addView(down)

        val hlayout7 = LinearLayout(context)
        hlayout7.orientation = LinearLayout.HORIZONTAL
        val i3 = add_button("USB",dev,check_state(curr_btns,"USB"),dev_id)
        hlayout7.addView(i3)
        val i4 = add_button("input 0",dev,check_state(curr_btns,"input 0"),dev_id)
        hlayout7.addView(i4)
        val i5 = add_button("input 1",dev,check_state(curr_btns,"input 1"),dev_id)
        hlayout7.addView(i5)
        layout.addView(hlayout7)

        //Volume up down buttens
        val vol = TextView(context)
        vol.setText("Volume")
        //txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        //txt.setTextColor(Color.BLUE)
        layout.addView(vol)
        val volup = add_button("+VOLUP",dev,check_state(curr_btns,"VOLUP"),dev_id)
        layout.addView(volup)
        val voldown = add_button("-VOLDOWN",dev,check_state(curr_btns,"VOLDOWN"),dev_id)
        layout.addView(voldown)

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle(dev + " (TV) REMOTE")
            .setView(layout)
            .setPositiveButton("DONE", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
                dialog.dismiss()
            }
        val dialog = builder.create()

        dialog.setOnShowListener {
        }
        dialog.show()
    }

    fun show_remote_smart_fan(dev_id:String, dev : String, dev_info : String){
        val curr_btns = dev_info.split(",")
        if (curr_btns[0] != "keys")
            return

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL


        // ON + OFF
        val hlayout1 = LinearLayout(context)
        hlayout1.orientation = LinearLayout.HORIZONTAL

        val on = add_button("ON", dev, check_state(curr_btns,"ON"),dev_id)
        hlayout1.addView(on)
        val off = add_button("OFF",dev,check_state(curr_btns,"OFF"),dev_id)
        hlayout1.addView(off)
        layout.addView(hlayout1)

        //Temperatures txt
        val tmpt = TextView(context)
        tmpt.setText("FAN SPEEDS")
        tmpt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        tmpt.setTextColor(Color.BLUE)
        layout.addView(tmpt)
        //Speeds between 1 and 5
        val hlayout2 = LinearLayout(context)
        hlayout2.orientation = LinearLayout.HORIZONTAL
        val t1 = add_button("1",dev,check_state(curr_btns,"1"),dev_id)
        hlayout2.addView(t1)
        val t2 = add_button("2",dev,check_state(curr_btns,"2"),dev_id)
        hlayout2.addView(t2)
        val t3 = add_button("3",dev,check_state(curr_btns,"3"),dev_id)
        hlayout2.addView(t3)
        val t4 = add_button("4",dev,check_state(curr_btns,"4"),dev_id)
        hlayout2.addView(t4)
        val t5 = add_button("5",dev,check_state(curr_btns,"5"),dev_id)
        hlayout2.addView(t5)
        layout.addView(hlayout2)


        // fan speeds up down
        val fantxt = TextView(context)
        fantxt.setText("Fan Speed")
        //txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        //txt.setTextColor(Color.BLUE)
        layout.addView(fantxt)
        val fanup = add_button("+FANUP",dev,check_state(curr_btns,"FANUP"),dev_id)
        layout.addView(fanup)
        val fandown = add_button("-FANDOWN",dev,check_state(curr_btns,"FANDOWN"),dev_id)
        layout.addView(fandown)

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle(dev + " (SMART FAN) REMOTE")
            .setView(layout)
            .setPositiveButton("DONE", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
                dialog.dismiss()
            }
        val dialog = builder.create()

        dialog.setOnShowListener {
        }
        dialog.show()
    }

    fun add_button(value : String, dev: String, state : Int, dev_id: String): Button {
        val on = Button(context)
        on.setSingleLine()
        var name = value
        if (value[0] == '+' || value[0] == '-') {
            on.setText(value[0].toString())
            name = name.drop(1)
        }else
            on.setText(name)
        if(state == 1)
            on.setBackgroundColor(Color.rgb(200, 198, 255))
        else
            on.setBackgroundColor(Color.rgb(255, 198, 255))

        on.setOnClickListener(){
            if (state == 1) {
                Toast.makeText(context, "Clicked " + name, Toast.LENGTH_SHORT).show()
                val req = "I,IR,"+dev_id+","+name+",I"
                encrypt_and_send_data(req)
            }else {
                val builder = AlertDialog.Builder(context)
                    .setTitle(dev + " "+name+" Not Recorded")
                    .setPositiveButton("RECORD", null)
                    .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                        // so something, or not - dialog will close
                        dialog.dismiss()
                    }
                val dialog = builder.create()
                dialog.setOnShowListener {
                    val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    okButton.setOnClickListener {
                        val req = "I,IR,"+dev_id+","+name+",R"
                        encrypt_and_send_data(req)
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
        }
        on.setOnLongClickListener(){
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL

            val cncl = Button(context)
            cncl.setText("Stop Recording")
            cncl.setBackgroundColor(Color.rgb(200, 198, 255))
            layout.addView(cncl)
            layout.setPadding(50, 40, 50, 10)

            val builder = AlertDialog.Builder(context)
                .setTitle(dev + " "+name+" configure")
                .setView(layout)
                .setNeutralButton("DELETE ALL",null)
                .setPositiveButton("Continue Recording", null)
                .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                    // so something, or not - dialog will close
                    dialog.dismiss()
                }
            val dialog = builder.create()

            dialog.setOnShowListener {
                val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                okButton.setOnClickListener {
                    val req = "I,IR,"+dev_id+","+name+",R"
                    encrypt_and_send_data(req)
                    dialog.dismiss()
                }
                val nutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                nutralButton.setOnClickListener {
                    val req = "I,IR,"+dev_id+","+name+",D"
                    encrypt_and_send_data(req)
                    dialog.dismiss()
                }
            }
            dialog.show()
            cncl.setOnClickListener(){
                val req = "I,IR,STOPREC"
                encrypt_and_send_data(req)
                dialog.dismiss()
            }
            return@setOnLongClickListener(true)
        }
        return on
    }

    fun check_state(curr_btns: List<String>, act:String):Int{
        if(curr_btns.contains(act)) {
            return 1
        } else {
            return 0
        }
        return -1
    }


    fun show_remote_smart_light(dev_id:String, dev : String, dev_info : String){
        val curr_btns = dev_info.split(",")
        if (curr_btns[0] != "keys")
            return

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL


        // ON + OFF
        val hlayout1 = LinearLayout(context)
        hlayout1.orientation = LinearLayout.HORIZONTAL

        val on = add_button("ON", dev, check_state(curr_btns,"ON"),dev_id)
        hlayout1.addView(on)
        val off = add_button("OFF",dev,check_state(curr_btns,"OFF"),dev_id)
        hlayout1.addView(off)
        layout.addView(hlayout1)

        //colors txt
        val tmpt = TextView(context)
        tmpt.setText("COLORS")
        tmpt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        tmpt.setTextColor(Color.BLUE)
        layout.addView(tmpt)
        //light colors
        val hlayout2 = LinearLayout(context)
        hlayout2.orientation = LinearLayout.HORIZONTAL
        val red = add_button("red",dev,check_state(curr_btns,"red"),dev_id)
        hlayout2.addView(red)
        val blue = add_button("blue",dev,check_state(curr_btns,"blue"),dev_id)
        hlayout2.addView(blue)
        val green = add_button("green",dev,check_state(curr_btns,"green"),dev_id)
        hlayout2.addView(green)
        val yellow = add_button("yellow",dev,check_state(curr_btns,"yellow"),dev_id)
        hlayout2.addView(yellow)
        val orange = add_button("orange",dev,check_state(curr_btns,"orange"),dev_id)
        hlayout2.addView(orange)
        layout.addView(hlayout2)


        //light colors
        val hlayout3 = LinearLayout(context)
        hlayout3.orientation = LinearLayout.HORIZONTAL
        val purple = add_button("purple",dev,check_state(curr_btns,"purple"),dev_id)
        hlayout3.addView(purple)
        val violet = add_button("violet",dev,check_state(curr_btns,"violet"),dev_id)
        hlayout3.addView(violet)
        val skyblue = add_button("skyblue",dev,check_state(curr_btns,"skyblue"),dev_id)
        hlayout3.addView(skyblue)
        val pink = add_button("pink",dev,check_state(curr_btns,"pink"),dev_id)
        hlayout3.addView(pink)
        val white = add_button("white",dev,check_state(curr_btns,"white"),dev_id)
        hlayout3.addView(white)
        layout.addView(hlayout3)

        val hlayout5 = LinearLayout(context)
        hlayout5.orientation = LinearLayout.HORIZONTAL
        val mode0 = add_button("mode0",dev,check_state(curr_btns,"mode0"),dev_id)
        hlayout5.addView(mode0)
        val mode1 = add_button("mode1",dev,check_state(curr_btns,"mode1"),dev_id)
        hlayout5.addView(mode1)
        val mode2 = add_button("mode2",dev,check_state(curr_btns,"mode2"),dev_id)
        hlayout5.addView(mode2)
        val mode3 = add_button("mode3",dev,check_state(curr_btns,"mode3"),dev_id)
        hlayout5.addView(mode3)
        val mode4 = add_button("mode4",dev,check_state(curr_btns,"mode4"),dev_id)
        hlayout5.addView(mode4)
        layout.addView(hlayout5)

        //brightness up down buttens
        val temptxt = TextView(context)
        temptxt.setText("BRIGHTNESS")
        //txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        //txt.setTextColor(Color.BLUE)
        layout.addView(temptxt)
        val brtup = add_button("+BRTUP",dev,check_state(curr_btns,"BRTUP"),dev_id)
        layout.addView(brtup)
        val brtdown = add_button("-BRTDOWN",dev,check_state(curr_btns,"BRTDOWN"),dev_id)
        layout.addView(brtdown)


        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle(dev + " SMART LIGHT REMOTE")
            .setView(layout)
            .setPositiveButton("DONE", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
                dialog.dismiss()
            }
        val dialog = builder.create()

        dialog.setOnShowListener {
        }
        dialog.show()
    }

    fun show_remote_switch(dev_id:String, dev : String, dev_info : String){
        val curr_btns = dev_info.split(",")
        if (curr_btns[0] != "keys")
            return

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL


        // ON + OFF
        val hlayout1 = LinearLayout(context)
        hlayout1.orientation = LinearLayout.HORIZONTAL

        val on = add_button("ON", dev, check_state(curr_btns,"ON"),dev_id)
        hlayout1.addView(on)
        val off = add_button("OFF",dev,check_state(curr_btns,"OFF"),dev_id)
        hlayout1.addView(off)
        layout.addView(hlayout1)

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle(dev + " REMOTE SWITCH")
            .setView(layout)
            .setPositiveButton("DONE", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
                dialog.dismiss()
            }
        val dialog = builder.create()

        dialog.setOnShowListener {
        }
        dialog.show()
    }

    fun show_tv_remote(dev_id:String, dev : String, dev_info : String){
        val curr_btns = dev_info.split(",")
        if (curr_btns[0] != "keys")
            return

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL


        // ON + OFF
        val hlayout1 = LinearLayout(context)
        hlayout1.orientation = LinearLayout.HORIZONTAL

        val on = add_button("ON", dev, check_state(curr_btns,"ON"),dev_id)
        hlayout1.addView(on)
        val off = add_button("OFF",dev,check_state(curr_btns,"OFF"),dev_id)
        hlayout1.addView(off)
        layout.addView(hlayout1)

        //numbers txt
        val tmpt = TextView(context)
        tmpt.setText("CHANNEL")
        tmpt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        tmpt.setTextColor(Color.BLUE)
        layout.addView(tmpt)
        //channel number 1 to 3
        val hlayout2 = LinearLayout(context)
        hlayout2.orientation = LinearLayout.HORIZONTAL
        val t1 = add_button("1",dev,check_state(curr_btns,"1"),dev_id)
        hlayout2.addView(t1)
        val t2 = add_button("2",dev,check_state(curr_btns,"2"),dev_id)
        hlayout2.addView(t2)
        val t3 = add_button("3",dev,check_state(curr_btns,"3"),dev_id)
        hlayout2.addView(t3)
        layout.addView(hlayout2)


        //channel number 4 to 6
        val hlayout3 = LinearLayout(context)
        hlayout3.orientation = LinearLayout.HORIZONTAL
        val t4 = add_button("4",dev,check_state(curr_btns,"4"),dev_id)
        hlayout3.addView(t4)
        val t5 = add_button("5",dev,check_state(curr_btns,"5"),dev_id)
        hlayout3.addView(t5)
        val t6 = add_button("6",dev,check_state(curr_btns,"6"),dev_id)
        hlayout3.addView(t6)
        layout.addView(hlayout3)

        //channel number 7 to 9, 0
        val hlayout4 = LinearLayout(context)
        hlayout4.orientation = LinearLayout.HORIZONTAL
        val t7 = add_button("7",dev,check_state(curr_btns,"7"),dev_id)
        hlayout4.addView(t7)
        val t8 = add_button("8",dev,check_state(curr_btns,"8"),dev_id)
        hlayout4.addView(t8)
        val t9 = add_button("9",dev,check_state(curr_btns,"9"),dev_id)
        hlayout4.addView(t9)
        val t0 = add_button("0",dev,check_state(curr_btns,"0"),dev_id)
        hlayout4.addView(t0)
        layout.addView(hlayout4)

        val hlayout5 = LinearLayout(context)
        hlayout5.orientation = LinearLayout.HORIZONTAL
        val mute = add_button("mute",dev,check_state(curr_btns,"mute"),dev_id)
        hlayout5.addView(mute)
        val unmute = add_button("unmute",dev,check_state(curr_btns,"unmute"),dev_id)
        hlayout5.addView(unmute)
        val pause = add_button("pause",dev,check_state(curr_btns,"pause"),dev_id)
        hlayout5.addView(pause)
        val play = add_button("play",dev,check_state(curr_btns,"play"),dev_id)
        hlayout5.addView(play)
        layout.addView(hlayout5)

        val hlayout6 = LinearLayout(context)
        hlayout6.orientation = LinearLayout.HORIZONTAL
        val home = add_button("home",dev,check_state(curr_btns,"home"),dev_id)
        hlayout6.addView(home)
        val ok = add_button("ok",dev,check_state(curr_btns,"ok"),dev_id)
        hlayout6.addView(ok)
        val info = add_button("info",dev,check_state(curr_btns,"info"),dev_id)
        hlayout6.addView(info)
        layout.addView(hlayout6)

        val hlayout7 = LinearLayout(context)
        hlayout7.orientation = LinearLayout.HORIZONTAL
        val i1 = add_button("AV",dev,check_state(curr_btns,"AV"),dev_id)
        hlayout7.addView(i1)
        val i2 = add_button("HDMI",dev,check_state(curr_btns,"HDMI"),dev_id)
        hlayout7.addView(i2)
        val i3 = add_button("USB",dev,check_state(curr_btns,"USB"),dev_id)
        hlayout7.addView(i3)
        val i4 = add_button("input 0",dev,check_state(curr_btns,"input 0"),dev_id)
        hlayout7.addView(i4)
        val i5 = add_button("input 1",dev,check_state(curr_btns,"input 1"),dev_id)
        hlayout7.addView(i5)
        layout.addView(hlayout7)

        //Volume up down buttens
        val vol = TextView(context)
        vol.setText("Volume")
        //txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        //txt.setTextColor(Color.BLUE)
        layout.addView(vol)
        val volup = add_button("+VOLUP",dev,check_state(curr_btns,"VOLUP"),dev_id)
        layout.addView(volup)
        val voldown = add_button("-VOLDOWN",dev,check_state(curr_btns,"VOLDOWN"),dev_id)
        layout.addView(voldown)

        //CHANNEL up down buttens
        val chantxt = TextView(context)
        chantxt.setText("CHANNEL")
        //txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        //txt.setTextColor(Color.BLUE)
        layout.addView(chantxt)
        val chanup = add_button("+CHANUP",dev,check_state(curr_btns,"CHANUP"),dev_id)
        layout.addView(chanup)
        val chandown = add_button("-CHANDOWN",dev,check_state(curr_btns,"CHANDOWN"),dev_id)
        layout.addView(chandown)

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle(dev + " (TV) REMOTE")
            .setView(layout)
            .setPositiveButton("DONE", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
                dialog.dismiss()
            }
        val dialog = builder.create()

        dialog.setOnShowListener {
        }
        dialog.show()
    }

    fun show_ac_remote(dev_id:String, dev : String, dev_info : String){
        val curr_btns = dev_info.split(",")
        if (curr_btns[0] != "keys")
            return

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL


        // ON + OFF
        val hlayout1 = LinearLayout(context)
        hlayout1.orientation = LinearLayout.HORIZONTAL

        val on = add_button("ON", dev, check_state(curr_btns,"ON"),dev_id)
        hlayout1.addView(on)
        val off = add_button("OFF",dev,check_state(curr_btns,"OFF"),dev_id)
        hlayout1.addView(off)
        layout.addView(hlayout1)

        //Temperatures txt
        val tmpt = TextView(context)
        tmpt.setText("Temperature")
        tmpt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        tmpt.setTextColor(Color.BLUE)
        layout.addView(tmpt)
        //Temperatures buttons 16 to 20
        val hlayout2 = LinearLayout(context)
        hlayout2.orientation = LinearLayout.HORIZONTAL
        val t16 = add_button("16",dev,check_state(curr_btns,"16"),dev_id)
        hlayout2.addView(t16)
        val t17 = add_button("17",dev,check_state(curr_btns,"17"),dev_id)
        hlayout2.addView(t17)
        val t18 = add_button("18",dev,check_state(curr_btns,"18"),dev_id)
        hlayout2.addView(t18)
        val t19 = add_button("19",dev,check_state(curr_btns,"19"),dev_id)
        hlayout2.addView(t19)
        val t20 = add_button("20",dev,check_state(curr_btns,"20"),dev_id)
        hlayout2.addView(t20)
        layout.addView(hlayout2)


        //Temperatures buttons 21 to 25
        val hlayout3 = LinearLayout(context)
        hlayout3.orientation = LinearLayout.HORIZONTAL
        val t21 = add_button("21",dev,check_state(curr_btns,"21"),dev_id)
        hlayout3.addView(t21)
        val t22 = add_button("22",dev,check_state(curr_btns,"22"),dev_id)
        hlayout3.addView(t22)
        val t23 = add_button("23",dev,check_state(curr_btns,"23"),dev_id)
        hlayout3.addView(t23)
        val t24 = add_button("24",dev,check_state(curr_btns,"24"),dev_id)
        hlayout3.addView(t24)
        val t25 = add_button("25",dev,check_state(curr_btns,"25"),dev_id)
        hlayout3.addView(t25)
        layout.addView(hlayout3)

         //Temperatures buttons 26 to 30
        val hlayout4 = LinearLayout(context)
        hlayout4.orientation = LinearLayout.HORIZONTAL
        val t26 = add_button("26",dev,check_state(curr_btns,"26"),dev_id)
        hlayout4.addView(t26)
        val t27 = add_button("27",dev,check_state(curr_btns,"27"),dev_id)
        hlayout4.addView(t27)
        val t28 = add_button("28",dev,check_state(curr_btns,"28"),dev_id)
        hlayout4.addView(t28)
        val t29 = add_button("29",dev,check_state(curr_btns,"29"),dev_id)
        hlayout4.addView(t29)
        val t30 = add_button("30",dev,check_state(curr_btns,"30"),dev_id)
        hlayout4.addView(t30)
        layout.addView(hlayout4)

        val hlayout5 = LinearLayout(context)
        hlayout5.orientation = LinearLayout.HORIZONTAL
        val swing = add_button("swing",dev,check_state(curr_btns,"swing"),dev_id)
        hlayout5.addView(swing)
        val mode1 = add_button("mode1",dev,check_state(curr_btns,"mode1"),dev_id)
        hlayout5.addView(mode1)
        val mode2 = add_button("mode2",dev,check_state(curr_btns,"mode2"),dev_id)
        hlayout5.addView(mode2)
        val mode3 = add_button("mode3",dev,check_state(curr_btns,"mode3"),dev_id)
        hlayout5.addView(mode3)
        val mode4 = add_button("mode4",dev,check_state(curr_btns,"mode4"),dev_id)
        hlayout5.addView(mode4)
        layout.addView(hlayout5)

        //Temperatures up down buttens
        val temptxt = TextView(context)
        temptxt.setText("Temperature")
        //txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        //txt.setTextColor(Color.BLUE)
        layout.addView(temptxt)
        val tmpup = add_button("+TMPUP",dev,check_state(curr_btns,"TMPUP"),dev_id)
        layout.addView(tmpup)
        val tmpdown = add_button("-TMPDOWN",dev,check_state(curr_btns,"TMPDOWN"),dev_id)
        layout.addView(tmpdown)

        //Temperatures up down buttens
        val hlayout6 = LinearLayout(context)
        hlayout6.orientation = LinearLayout.VERTICAL
        val fantxt = TextView(context)
        fantxt.setText("Fan Speed")
        //txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        //txt.setTextColor(Color.BLUE)
        hlayout6.addView(fantxt)
        val fanup = add_button("+FANUP",dev,check_state(curr_btns,"FANUP"),dev_id)
        hlayout6.addView(fanup)
        val fandown = add_button("-FANDOWN",dev,check_state(curr_btns,"FANDOWN"),dev_id)
        hlayout6.addView(fandown)
/*
        val hlayout7 = LinearLayout(context)
        hlayout7.orientation = LinearLayout.HORIZONTAL
        hlayout7.addView(hlayout5)
        hlayout7.addView(hlayout6)
*/
        layout.addView(hlayout6)



        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle(dev + " REMOTE")
            .setView(layout)
            .setPositiveButton("DONE", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
                dialog.dismiss()
            }
        val dialog = builder.create()

        dialog.setOnShowListener {
        }
        dialog.show()
    }

    fun set_port_params(idx : Int, port:List<String>){
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        val enable = Switch(context)
        enable.text = port[2] + " Enabled"
        enable.isChecked = true
        enable.textSize = 16F
        layout.addView(enable)

        val Portname = EditText(context)
        Portname.setSingleLine()
        Portname.hint = port[2] + " :(Preferred name for Port)"
        layout.addView(Portname)

        val porttypes = "PORT TYPE, LIGHT, FAN, SWITCH, SMART LIGHT".split(",")
        val type = Spinner(context)
        type.adapter =
            context?.let { ArrayAdapter(it, android.R.layout.simple_spinner_dropdown_item, porttypes) }
        type.setSelection(port[4].toInt())
        layout.addView(type)

        val powersave = Switch(context)
        powersave.text = "  ENABLE POWER SAVE"
        if(port[5] == "1"){
            powersave.isChecked = true
        }
        powersave.textSize = 16F
        layout.addView(powersave)

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle(port[2] +" Settings")
            .setView(layout)
            .setNeutralButton("Test Port",null)
            .setPositiveButton("UPDATE", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
                dialog.dismiss()
            }
        val dialog = builder.create()

        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                var update = 0
                var portenable = "0"
                var portname = ""
                if(enable.isChecked == true){
                    if(port[3] == "0"){
                       update = 1
                    }
                    portenable = "1"
                }else{
                    if(port[3] == "1")
                        update = 1
                }

                if(Portname.text.isBlank()){
                    portname = port[2]
                }else {
                    portname = Portname.text.toString()
                    if(Portname.text.toString() != port[2]){
                        update = 1
                    }
                }
                var i = 0;
                for(item in porttypes){
                    if(item == type.selectedItem)
                        break
                    i = i + 1
                }
                if (i.toString() != port[4])
                    update = 1

                var PS = "0"
                if(powersave.isChecked == true){
                    if(port[5] == "0"){
                       update = 1
                    }
                    PS = "1"
                }else{
                    if(port[5] == "1")
                        update = 1
                }

                if(enable.isChecked == true && i == 0){
                    Toast.makeText(context, "Select Port type", Toast.LENGTH_SHORT).show()
                }else {
                    val send_str =
                        "U," + port[0] + "," + port[1] + "," + portname + "," + portenable + "," + i.toString() + "," + PS
                    Toast.makeText(context, "port params " + send_str, Toast.LENGTH_SHORT).show()
                    val resp = encrypt_and_send_data(send_str)
                    Toast.makeText(context, resp, Toast.LENGTH_SHORT).show()
                    if(resp == "OK")
                        dialog.dismiss()
                }
            }
            val nutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            nutralButton.setOnClickListener {
                Toast.makeText(context, "Switching on and off Port " + idx, Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    fun add_new_remote(){
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        val remote_name = EditText(context)
        remote_name.setSingleLine()
        remote_name.hint = "Remote Name"
        layout.addView(remote_name)

        val remotetype = "SELECT REMOTE TYPE,AC, TV, REMOTE SWITCH, REMOTE SMART LIGHT, REMOTE SMART FAN, AUDIO DEVICE".split(",")
        val type = Spinner(context)
        type.adapter =
            context?.let { ArrayAdapter(it, android.R.layout.simple_spinner_dropdown_item, remotetype) }
        layout.addView(type)

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle("ADDING NEW REMOTE")
            .setView(layout)
            .setPositiveButton("ADD", null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
                dialog.dismiss()
            }
        val dialog = builder.create()

        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                var req = "U,IR,add,"
                var err = ""
                if(remote_name.text.isNotBlank()){
                    req = req + remote_name.text.toString()
                }else
                    err= "Enter Valid name for the new remote"
                var i = 10;
                for(item in remotetype){
                    if(item == type.selectedItem)
                        break
                    i = i + 1
                }
                if (i > 10)
                    req = req + "," + i.toString()
                else if (err.length <= 1)
                    err = "Select Remote Type"

                if (err.length <= 1) {
                    val resp = encrypt_and_send_data(req)
                    Toast.makeText(context, resp, Toast.LENGTH_SHORT).show()
                    if (resp == "OK")
                        dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    fun encrypt_and_send_data(data : String): String {
        var resp = "FAIL"
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            if(zimarix_global.controller_ips[curr_device] == "0" || zimarix_global.controller_ips[curr_device].length < 7){
                return "INVALID CONFIG"
            }
            val client = Socket(zimarix_global.controller_ips[curr_device], 20009)
            val enc_probe = AES_encrpt(zimarix_global.controller_keys[curr_device],data)
            client!!.outputStream.write(enc_probe)
            val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
            resp = bufferReader.readLine()
            client.close()
        }catch (t: SocketException){

        }
        return resp
    }

    fun get_service_config(data : String): String {
        var resp = "FAIL"
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            if(zimarix_global.controller_ips[curr_device] == "0" || zimarix_global.controller_ips[curr_device].length < 7){
                return "INVALID CONFIG"
            }
            val req = "G,"+data
            val client = Socket(zimarix_global.controller_ips[curr_device], 20009)
            val enc_probe = AES_encrpt(zimarix_global.controller_keys[curr_device],req)
            client!!.outputStream.write(enc_probe)
            val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
            val buff = bufferReader.readLine()
            if (buff != null && buff.length > 0 || buff != "FAIL") {
                try {
                    val decoded_buff = Base64.decode(buff, Base64.NO_PADDING)
                    val data = AES_decrpt(zimarix_global.controller_keys[curr_device], decoded_buff)
                    val param = data.split(",")
                    if(param[0] == "G") {
                        resp = data
                    }
                } catch (t: IllegalArgumentException) {
                }
            }
            client.close()
        }catch (t: SocketException){

        }
        return resp
    }
}