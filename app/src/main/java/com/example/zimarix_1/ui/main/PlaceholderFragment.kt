package com.example.zimarix_1.ui.main

//import android.R
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.zimarix_1.*
import com.example.zimarix_1.databinding.FragmentMainBinding
import com.example.zimarix_1.zimarix_global.Companion.controller_devices
import com.example.zimarix_1.zimarix_global.Companion.controller_ids
import com.example.zimarix_1.zimarix_global.Companion.controller_ips
import com.example.zimarix_1.zimarix_global.Companion.controller_keys
import com.example.zimarix_1.zimarix_global.Companion.controller_names
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.net.SocketException


/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val root = binding.root

        val listView: ListView = binding.sectionList
        var pioneers=arrayOf<String>()
        pageViewModel.text.observe(viewLifecycleOwner, Observer {
            if (it == "1") {
                pioneers = controller_names.toTypedArray()
                listView.setOnItemClickListener(OnItemClickListener { arg0, arg1, position, arg3 ->
                    val intent = Intent(getActivity(), Devsettings::class.java)
                    val b = Bundle()
                    b.putInt("key",position) //Your id
                    intent.putExtras(b)
                    startActivity(intent)
                })
            } else if (it == "2") {
                var devices = arrayOf<String>()
                var dev_info = arrayOf<String>()
                var i = 0
                controller_devices.forEach {
                    val data = it.split(",")
                    data.forEach{
                        val dev_params =  it.split("_")
                        if(dev_params.size > 3){
                            devices = devices + dev_params[1]
                            val info = i.toString()+","+dev_params[0]+","+dev_params[2]+","+dev_params[3]
                            dev_info = dev_info + info
                        }
                    }
                    i = i +1
                }
                pioneers = devices
                listView.setOnItemClickListener(OnItemClickListener { arg0, arg1, position, arg3 ->
                    val layout = LinearLayout(context)
                    layout.orientation = LinearLayout.VERTICAL

                    val dev = dev_info[position].split(",")
                    if(dev[2] == "1") {
                        val cncl = Button(context)
                        if(dev[3] == "1") {
                            cncl.setText("ON")
                            cncl.setBackgroundColor(Color.rgb(10, 200, 10))
                        }else{
                            cncl.setText("OFF")
                            cncl.setBackgroundColor(Color.rgb(100, 10, 10))
                        }
                        cncl.setOnClickListener() {
                            var tstr = dev[0]+","+dev[1]+","+dev[2]+","
                            var cmd = "C,"+dev[1]+","
                            if(cncl.text == "ON"){
                                cncl.setText("OFF")
                                cncl.setBackgroundColor(Color.rgb(100, 10, 10))
                                tstr = tstr+"0"
                                cmd = cmd + "OFF"
                            }else{
                                cncl.setText("ON")
                                cncl.setBackgroundColor(Color.rgb(10, 200, 10))
                                tstr = tstr+"1"
                                cmd = cmd + "ON"
                            }
                            dev_info[position] = tstr
                            send_to_device(dev[0].toInt(),cmd)
                        }
                        layout.addView(cncl)
                    }
                    //layout.setPadding(50, 40, 50, 10)
                    val builder = AlertDialog.Builder(context)
                        .setTitle(devices[position])
                        .setView(layout)
                    val dialog = builder.create()
                    dialog.show()
                })
            }
            else if (it == "3") {
                pioneers = controller_ips.toTypedArray()
                listView.setOnItemClickListener(OnItemClickListener { arg0, arg1, position, arg3 ->
                    val intent = Intent(getActivity(), livestream::class.java)
                    val b = Bundle()
                    b.putInt("key",position) //Your id
                    intent.putExtras(b)
                    startActivity(intent)
                })
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

    fun send_to_device(i:Int, data:String): String {
        var resp = "FAIL"
        resp = send_to_local_device(i,data)
        if(resp != "OK")
            resp = send_to_remote(i,data)

        return resp
    }

    fun send_to_local_device(i:Int, data:String): String {
        var resp = "FAIL"
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            if(zimarix_global.controller_ips[i] == "0" || zimarix_global.controller_ips[i].length < 7){
                return "INVALID CONFIG"
            }
            val client = Socket(controller_ips[i], 20009)
            val enc_probe = AES_encrpt(controller_keys[i],data)
            client!!.outputStream.write(enc_probe)
            val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
            resp = bufferReader.readLine()
            client.close()
        }catch (t: SocketException){

        }
        return resp
    }
    fun send_to_remote(i:Int, data:String): String {
        var resp = "FAIL"
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            var client = Socket(zimarix_global.zimarix_server, 11112)


            val Sdata = "C"+controller_ids[i]
            val enc_data = AES_encrpt(zimarix_global.appkey, Sdata)


            val enc_probe = AES_encrpt(controller_keys[i],data)
            client!!.outputStream.write(zimarix_global.appid.toByteArray()+",C".toByteArray()+ enc_data+",".toByteArray()+enc_probe)

            val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
            resp = bufferReader.readLine()
            client.close()
        }catch (t: SocketException){

        }
        return resp
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
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}