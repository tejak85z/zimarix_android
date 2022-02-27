package com.example.zimarix_1

import android.app.PendingIntent.getActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import android.util.Base64
import com.example.zimarix_1.zimarix_global.Companion.appid
import com.example.zimarix_1.zimarix_global.Companion.appkey
import java.lang.StringBuilder
import java.net.NetworkInterface
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class addnewcontroller : AppCompatActivity() {

    private var m_bluetoothAdapter: BluetoothAdapter? = null
    private val devices_list : ArrayList<BluetoothDevice> = ArrayList()
    private var reciver_registered = 0

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addnewcontroller)
        val button = findViewById<Button>(R.id.bt_discover_devices_button)
        button.setOnClickListener{
            if (m_bluetoothAdapter == null) {
                initBluetoothDiscovery()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun initBluetoothDiscovery() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
         m_bluetoothAdapter = bluetoothManager.adapter
         if(m_bluetoothAdapter == null) {
             Toast.makeText(this, "this device doesn't support bluetooth", Toast.LENGTH_SHORT).show()
             return
         }
         if (m_bluetoothAdapter?.isEnabled == false) {
             val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
             startActivityForResult(enableBtIntent, 1)
             val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                 putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
             }
             startActivity(discoverableIntent)
         }

         devices_list.clear()
         discoverDevices()
    }

    private fun discoverDevices(){
        if (m_bluetoothAdapter?.isDiscovering == true) {
            Log.i("TAG", "cancel start discovery")
            m_bluetoothAdapter?.cancelDiscovery()
        }
        Log.i("TAG", "start discovery, show loading")
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(mReceiver, intentFilter)
        m_bluetoothAdapter?.startDiscovery()
        reciver_registered = 1

        val pairedDevices: Set<BluetoothDevice>? = m_bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            devices_list.add(device)
        }
    }

    private val mReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d("debug ", " onreceiver ==================== $action")
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                Log.i("TAG", "Discovery finished, hide loading")
                val listView = findViewById<ListView>(R.id.discovery_list)
                val uniq_device_list = devices_list.distinct()
                val adapter = ArrayAdapter<BluetoothDevice>(context, android.R.layout.simple_list_item_1, uniq_device_list)
                listView.adapter = adapter

                listView.setOnItemClickListener { parent, view, position, id ->
                    val device = devices_list.get(position)
                    Toast.makeText(context, "device : " +device.name + " "+ device.address, Toast.LENGTH_SHORT).show()
                    //val intent = Intent(this,addcontrollerpopup::class.java)
                    //startActivity(intent)


                    try{
                        val s=ConnectThread(device)
                        s.run()

                        val layout = LinearLayout(context)
                        layout.orientation = LinearLayout.VERTICAL
                        val ssid = EditText(context)
                        ssid.setSingleLine()
                        ssid.hint = "WIFI SSID(name)"
                        layout.addView(ssid)

                        val password = EditText(context)
                        password.setSingleLine()
                        password.hint = "Wifi Password"
                        layout.addView(password)

                        layout.setPadding(50, 40, 50, 10)

                        val builder = android.app.AlertDialog.Builder(context)
                            .setTitle("Device Wifi Setup")
                            .setMessage("Enter wifi SSID(name) and password and click OK")
                            .setView(layout)
                            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                                    // so something, or not - dialog will close
                                dialog.dismiss()
                                val retstr = register_device(s)
                                Toast.makeText(context, "$retstr", Toast.LENGTH_SHORT).show()
                            }
                            .setPositiveButton("OK", null)
                        val dialog = builder.create()

                        dialog.setOnShowListener {
                            val okButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                            okButton.setOnClickListener {
                                val strssid = ssid.text.toString()
                                val strpassword = password.text.toString()
                                s.send_data("WIFI\n".toByteArray()+strssid.toByteArray()+"\n".toByteArray()+strpassword.toByteArray())
                                val recvdata = s.recv_data()
                                Log.e(TAG, "================= $recvdata")
                                if(recvdata == "ok") {
                                    val retstr = register_device(s)
                                    Toast.makeText(context, "$retstr", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                }else
                                    Toast.makeText(context, "$recvdata", Toast.LENGTH_SHORT).show()
                            }
                        }

                        dialog.show()
                        /*

                        val alert = AlertDialog.Builder(context)
                        alert.setTitle("Device Wifi Setup")
                        alert.setMessage("Enter wifi SSID(name) and password and click OK")

                        val layout = LinearLayout(context)
                        layout.orientation = LinearLayout.VERTICAL

                        val ssid = EditText(context)
                        ssid.setSingleLine()
                        ssid.hint = "WIFI SSID(name)"
                        layout.addView(ssid)

                        val password = EditText(context)
                        password.setSingleLine()
                        password.hint = "Wifi Password"
                        layout.addView(password)

                        layout.setPadding(50, 40, 50, 10)
                        alert.setView(layout)
                        alert.setPositiveButton("Ok") { _, _ ->
                            val strssid = ssid.text.toString()
                            val strpassword = password.text.toString()
                            s.send_data("WIFI\n".toByteArray()+strssid.toByteArray()+"\n".toByteArray()+strpassword.toByteArray())
                            val recvdata = s.recv_data()
                            if(recvdata == "ok") {
                                val retstr = register_device(s)
                                Toast.makeText(context, "$retstr", Toast.LENGTH_SHORT).show()
                            }else
                                Toast.makeText(context, "$recvdata", Toast.LENGTH_SHORT).show()
                        }
                        alert.setNegativeButton("Skip") { dialog, _ ->
                            dialog.dismiss()
                            val retstr = register_device(s)
                            Toast.makeText(context, "$retstr", Toast.LENGTH_SHORT).show()
                        }
                        //alert.setCancelable(false)
                        alert.show()

                         */
                    }
                    catch (e:Exception){
                        Toast.makeText(context, "Failed to connect to this device", Toast.LENGTH_SHORT).show()
                    }

                }
            }else if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    devices_list.add(device)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to unregister the ACTION_FOUND receiver.
        if(reciver_registered == 1)
            unregisterReceiver(mReceiver)
    }

    private fun register_device(s:ConnectThread) :String{
        var recvdata = ""
        try {
            val sdata = "REGISTER".toByteArray()
            s.send_data(sdata)
            recvdata = s.recv_data()
            if (recvdata.first() != 'R') {
                return recvdata
            }
            val enc_dev_mac = AES_encrpt(appkey, recvdata)
            s.send_data(appid.toByteArray() + ",".toByteArray() + enc_dev_mac)
            recvdata = s.recv_data()
        }
        catch (e:Exception){

        }
        return recvdata
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            val uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee")
            device.createRfcommSocketToServiceRecord(uuid)
        }

        public override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            m_bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                val ret = socket.connect()
                Log.e(TAG, "Could not close the client socket====== $ret")
                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
            }
        }
        private val mmInStream: InputStream = mmSocket!!.inputStream
        private val mmOutStream: OutputStream = mmSocket!!.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        fun send_data(byteArray: ByteArray) {
            mmOutStream.write(byteArray)
        }

        fun recv_data():String{
            var ret = 0
            try {
                ret = mmInStream.read(mmBuffer)
            } catch (e: IOException) {
                Log.d(TAG, "Input stream was disconnected", e)
            }
            val rcvdata = String(mmBuffer).take(ret)
            return rcvdata
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                //Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
}