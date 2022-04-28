package com.example.zimarix_1

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat.recreate
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import com.example.zimarix_1.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.example.zimarix_1.ui.main.SectionsPagerAdapter
import com.example.zimarix_1.zimarix_global.Companion.app_state
import com.example.zimarix_1.zimarix_global.Companion.appid
import com.example.zimarix_1.zimarix_global.Companion.appkey
import com.example.zimarix_1.zimarix_global.Companion.config_watchdog
import com.example.zimarix_1.zimarix_global.Companion.controller_devices
import com.example.zimarix_1.zimarix_global.Companion.controller_ids
import com.example.zimarix_1.zimarix_global.Companion.controller_ips
import com.example.zimarix_1.zimarix_global.Companion.controller_keys
import com.example.zimarix_1.zimarix_global.Companion.controller_names
import com.example.zimarix_1.zimarix_global.Companion.ip_conf_ver
import com.example.zimarix_1.zimarix_global.Companion.key_conf_ver
import com.example.zimarix_1.zimarix_global.Companion.zimarix_server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.net.Socket
import java.net.SocketException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(config_watchdog == 1) {
            config_watchdog = 0
            val prefs = getSharedPreferences(getString(R.string.dev_encryption_key), MODE_PRIVATE)
            load_app_config(prefs)

            CoroutineScope(IO).launch {
                config_updater()
            }
            CoroutineScope(IO).launch {
                device_config_updater()
            }
        }

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)

        val fab: FloatingActionButton = binding.fab
        fab.setOnClickListener {
            val popupMenu: PopupMenu = PopupMenu(this,fab)
            popupMenu.menuInflater.inflate(R.menu.add_popup_menu,popupMenu.menu)
            popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                if(item.title == "add controller"){
                    Toast.makeText(this@MainActivity, "You Clicked : " + item.title, Toast.LENGTH_SHORT).show()
                    val intent = Intent(this,addnewcontroller::class.java)
                    startActivity(intent)
                }else if(item.title == "add switch"){
                    Toast.makeText(this@MainActivity, "You Clicked : " + item.title, Toast.LENGTH_SHORT).show()
                    val intent = Intent(this,addnewdevice::class.java)
                    startActivity(intent)
                }else if(item.title == "add remote"){
                    Toast.makeText(this@MainActivity, "You Clicked : " + item.title, Toast.LENGTH_SHORT).show()
                    val intent = Intent(this,addnewremote::class.java)
                    startActivity(intent)
                }
                true
            })
            popupMenu.show()
        }
        val fab1: FloatingActionButton = binding.fab1
        fab1.setOnClickListener {
            finish()
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        app_state = 1
    }

    override fun onPause() {
        super.onPause()
        app_state = 0
    }

    fun device_config_updater(){
        val i = 1
        while (i == 1){
            if(app_state == 1) {
                controller_ips.forEachIndexed{ index, item->
                    try {
                        if(item.length > 7) {
                            var client = Socket(item, 20009)
                            val probe = "D"+ getRandomString(15)
                            Log.i("TAG", controller_keys[index])
                            val enc_probe = AES_encrpt(controller_keys[index],probe)
                            client!!.outputStream.write(enc_probe)

                            val bufferReader =
                                BufferedReader(InputStreamReader(client!!.inputStream))
                            val line = bufferReader.readLine()
                            if (line != null && line.length > 0 || line != "W") {
                                try {
                                    val decoded_key = Base64.decode(line, Base64.NO_PADDING)
                                    val data = AES_decrpt(controller_keys[index], decoded_key)
                                    val devices = data.split(",")
                                    if (devices[0] == "D") {
                                        controller_devices[index] = data
                                        val editor = getSharedPreferences(
                                            getString(R.string.dev_encryption_key),
                                            MODE_PRIVATE
                                        ).edit()
                                        save_ctlr_config(editor)
                                    }
                                } catch (t: IllegalArgumentException) {
                                }
                            }
                            client.close()
                        }
                    } catch (t: SocketException) {
                    }
                }
            }
            Thread.sleep(5000)
        }
    }

    fun config_updater(){
        val i = 1
        while (i == 1){
            if(app_state == 1) {
                try {
                    var client = Socket(zimarix_server, 11112)
                    val Sdata = "W" + ip_conf_ver +","+ key_conf_ver
                    val enc_data = AES_encrpt(appkey, Sdata)
                    client!!.outputStream.write(appid.toByteArray()+",W".toByteArray()+ enc_data)
                    val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
                    val line = bufferReader.readLine()
                    if(line != null && line.length > 0 || line != "W") {

                        try {
                            val decoded_key = Base64.decode(line, Base64.NO_PADDING)
                            val data = AES_decrpt(appkey, decoded_key)
                            //Log.d("debug ", " ======  ff $data")
                            val devices = data.split(",")
                            if (devices[0] == "W") {
                                ip_conf_ver = devices[1]
                                key_conf_ver = devices[2]
                                devices.forEach {
                                    if (it.length > 0) {
                                        val dev_params = it.split("_")
                                        val idx =
                                            controller_ids.indexOfFirst { it == dev_params[0] }

                                        if (idx < 0) {
                                            if (dev_params.size == 3) {
                                                controller_ids.add(dev_params[0])
                                                val Didx = controller_ids.size
                                                controller_names.add("device"+Didx.toString())
                                                if (dev_params[1].length >= 7) {
                                                    controller_ips.add(dev_params[1])
                                                }else{
                                                    controller_ips.add("0")
                                                }
                                                if (dev_params[2].length == 16) {
                                                    controller_keys.add(dev_params[2])
                                                }else{
                                                    controller_keys.add("0000000000000000")
                                                }
                                                controller_devices.add("")
                                            }
                                        }else{
                                            if (dev_params.size == 3){
                                                if (dev_params[1].length >= 7) {
                                                    controller_ips[idx] = dev_params[1]
                                                }
                                                if (dev_params[2].length == 16) {
                                                    controller_keys[idx]=dev_params[2]
                                                }

                                            }
                                        }
                                    }
                                }
                                val editor = getSharedPreferences(getString(R.string.dev_encryption_key), MODE_PRIVATE).edit()
                                save_ctlr_config(editor)
                            }
                        } catch (t: IllegalArgumentException) {
                        }
                    }
                    client.close()
                }catch (t: SocketException){
                }
            }
            Thread.sleep(5000)
        }
    }
}