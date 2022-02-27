package com.example.zimarix_1.ui.main

import android.R
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.zimarix_1.*
import com.example.zimarix_1.databinding.FragmentMainBinding
import com.example.zimarix_1.zimarix_global.Companion.curr_device
import com.example.zimarix_1.zimarix_global.Companion.dev_config
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.net.SocketException

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
                    if (port.size == 7 && port[0]=="port") {
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
                pioneers = pioneers + "Bluetooth Speaker ,enable remote support , upgrade ".split(",")
                listView.setOnItemClickListener(AdapterView.OnItemClickListener { arg0, arg1, position, arg3 ->
                    if(pioneers[position] == "Offline Wake Word Config"){
                        val intent = Intent(getActivity(), Wakeword::class.java)
                        startActivity(intent)
                        //process_wakeword_settings(configs[position])
                    }
                })
            }
            else if (it == "3") {
                pioneers = zimarix_global.controller_ips.toTypedArray()
                listView.setOnItemClickListener(AdapterView.OnItemClickListener { arg0, arg1, position, arg3 ->
                })
            }
            val adapter = activity?.let {
                ArrayAdapter<String>(
                    it,
                    R.layout.simple_list_item_1,
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

    fun process_wakeword_settings(s: String) {
        val layout = LinearLayout(context)
        val param = s.split("_")
        layout.orientation = LinearLayout.VERTICAL
        Log.d("debug ", " ================= $param")
        if(param[2] == "0") {
            val enable = Switch(context)
            enable.text = "ENABLE Offline Wake Word"
            enable.textSize = 16F
            layout.addView(enable)

            layout.setPadding(50, 40, 50, 10)

            val builder = AlertDialog.Builder(context)
                .setTitle("Offline Wake Word")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                    // so something, or not - dialog will close
                }
            val dialog = builder.create()
            enable.setOnClickListener {
                set_ww_params(s)
                dialog.dismiss()
            }
            dialog.show()
        }else{
            set_ww_params(s)
        }
    }

    fun set_ww_params(s: String){
        val layout = LinearLayout(context)
        val param = s.split("_")

        layout.orientation = LinearLayout.VERTICAL
        val timeout = EditText(context)
        timeout.setSingleLine()
        timeout.hint =" Timeout to discard wakeword sequence.\n Default 5 Seconds\n Current "+ param[3] + "Seconds"
        layout.addView(timeout)

        layout.setPadding(50, 40, 50, 10)

        val builder = AlertDialog.Builder(context)
            .setTitle("Offline Wake Word Settings")
            .setView(layout)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
            }
            .setPositiveButton("OK"){ dialog, whichButton ->
                // so something, or not - dialog will close
            }
        val dialog = builder.create()
        dialog.show()
    }
}