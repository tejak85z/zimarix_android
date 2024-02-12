package com.example.zimarix_1.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.example.zimarix_1.R
import com.example.zimarix_1.Send_cmd
import com.example.zimarix_1.update_params
import showLogoutConfirmationDialog

class DeviceSettings : AppCompatActivity() , update_params {

    override lateinit var progressBar: ProgressBar
    override var aToast: Toast? = null
    override var isTaskInProgress: Boolean = false
    override var active: Boolean = true

    var value = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_settings)

        progressBar = findViewById(R.id.devsettings_progressBar)
        progressBar.visibility = View.GONE

        val b = intent.extras
        value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        val reboot_btn: Button = findViewById(R.id.reboot)
        reboot_btn.setOnClickListener {
            showLogoutConfirmationDialog("CONFIRM DEVICE REBOOT","Device will be Rebooted", this) {
                val cmd = "REBOOT,"
                Send_cmd(value, cmd, this).execute()
            }
        }
        val reset_btn:Button = findViewById(R.id.reset)
        reset_btn.setOnClickListener {
            showLogoutConfirmationDialog("CONFIRM DEVICE CONFIG RESET","Device config will be reset to default", this) {
                val cmd = "RESET,"
                Send_cmd(value, cmd, this).execute()
            }
        }
        val stats_btn:Button = findViewById(R.id.stats)
        stats_btn.setOnClickListener {
            val intent = Intent(this, stats::class.java)
            intent.putExtras(idx)
            startActivity(intent)
        }
        val upgrade_btn:Button = findViewById(R.id.upgrade)
        upgrade_btn.setOnClickListener {

        }
    }
}