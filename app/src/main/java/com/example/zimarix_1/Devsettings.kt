package com.example.zimarix_1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.viewpager.widget.ViewPager
import com.example.zimarix_1.databinding.ActivityDevsettingsBinding
import com.example.zimarix_1.ui.main.SettingsPagerAdapter
import com.example.zimarix_1.zimarix_global.Companion.controller_ips
import com.google.android.material.tabs.TabLayout
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.net.SocketException
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Base64
import com.example.zimarix_1.zimarix_global.Companion.controller_keys
import com.example.zimarix_1.zimarix_global.Companion.curr_device
import com.example.zimarix_1.zimarix_global.Companion.dev_config


class Devsettings : AppCompatActivity() {
    private lateinit var binding1: ActivityDevsettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devsettings)

        binding1 = ActivityDevsettingsBinding.inflate(layoutInflater)
        setContentView(binding1.root)

        val b = intent.extras
        var value = -1 // or other values
        if (b != null) value = b.getInt("key")
        Log.d("debug ", " =====in new activity ${zimarix_global.controller_ips[value].length} ${zimarix_global.controller_ips[value]}")
        curr_device = value
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            if(controller_ips[value] == "0" || controller_ips[value].length < 7){
                Toast.makeText(this@Devsettings, "Invalid Device IP. Refresh Page", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            val client = Socket(controller_ips[value], 20009)

            val probe = "W"+ getRandomString(15)
            val enc_probe = AES_encrpt(controller_keys[value],probe)
            client!!.outputStream.write(enc_probe)

            val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
            val line = bufferReader.readLine()
            if(line.length <= 0)
                finish()

            val decode_line = Base64.decode(line, Base64.NO_PADDING)
            if(decode_line.size % 16 > 0) {
                Log.d("debug ", " ===================================1 activity ${line.length}")
                Toast.makeText(this@Devsettings, line, Toast.LENGTH_SHORT).show()
                finish()
            }
            val rcvdata = AES_decrpt(controller_keys[value],decode_line)
            dev_config = rcvdata.split("\n")
            client.close()
            Log.d("debug ", " =====in WW activity $dev_config")

        }catch (t: SocketException){
            Toast.makeText(this@Devsettings, "This Device is unrechable over Local network", Toast.LENGTH_SHORT).show()
            finish()
        }

        val sectionsPagerAdapter1 = SettingsPagerAdapter(this, supportFragmentManager)
        val viewPager1: ViewPager = binding1.viewPager
        viewPager1.adapter = sectionsPagerAdapter1
        val tabs1: TabLayout = binding1.tabs
        tabs1.setupWithViewPager(viewPager1)

    }


}