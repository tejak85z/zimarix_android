package com.example.zimarix_1
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class Send_Global_Switch_Req(
    private val value: Int,
    private val cmd: String,
    private val activity: MainActivity,
    private val position: Int
) : AsyncTask<Void, Void, String>() {
    var resp:String = "FAIL"

    override fun onPreExecute() {
        activity.progressBar.visibility = View.VISIBLE
    }

    override fun doInBackground(vararg params: Void?): String {
        if (activity.isTaskInProgress == true) {
            return "Previous Request In Progress"
        }else{
            activity.isTaskInProgress = true
            if (zimarix_global.devices[value].client != null) {
                resp = dev_req(value, cmd)
            }else{
                val req = "SEND,"+ zimarix_global.devices[value].id.toString()+"," +cmd
                val enc_req = aes_encrpt(zimarix_global.appkey, zimarix_global.appiv, req)
                try {
                    zimarix_global.ecsock!!.outputStream.write(enc_req)
                }catch (t: Throwable){
                    return "FAIL"
                }
            }
            return resp
        }
        return "FAIL" // Return the result from the background task
    }

    override fun onPostExecute(result: String) {
        activity.progressBar.visibility = View.GONE
        activity.isTaskInProgress = false
        val param = result.split(",")
        activity.aToast?.cancel()
        activity.aToast = Toast.makeText(activity, result, Toast.LENGTH_SHORT)
        activity.aToast?.show()

        if (param[0] != "OK"){
            if (GSwitches[position].type == 1 || GSwitches[position].type == 2) {
                GSwitches[position].switchState = GSwitches[position].switchState != true
                GSwitch_adapter.update_index(position)
            }
        }
    }
}

class SwitchAdapter(private var switchList: MutableList<sw_params>, private val activity: MainActivity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_SWITCH = 1
        private const val TYPE_BUTTON = 2
    }

    class SwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val switch: Switch = itemView.findViewById(R.id.SwitchBox)
    }

    class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button: Button = itemView.findViewById(R.id.ButtonBox)
    }

    override fun getItemViewType(position: Int): Int {
        return when (switchList[position].type) {
            1 -> TYPE_SWITCH
            2 -> TYPE_SWITCH
            3 -> TYPE_BUTTON
            else -> TYPE_BUTTON // Default to switch if type is not recognized
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SWITCH -> {
                val itemView =
                    LayoutInflater.from(parent.context).inflate(R.layout.switch_item, parent, false)
                SwitchViewHolder(itemView)
            }
            TYPE_BUTTON -> {
                val itemView =
                    LayoutInflater.from(parent.context).inflate(R.layout.button_item, parent, false)
                ButtonViewHolder(itemView)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_SWITCH -> {
                val switchHolder = holder as SwitchViewHolder
                val currentSwitch = switchList[position]
                switchHolder.switch.text = currentSwitch.name
                switchHolder.switch.isChecked = currentSwitch.switchState
                switchHolder.switch.visibility = currentSwitch.visibility
                switchHolder.switch.setOnClickListener {
                    if (!activity.isTaskInProgress) {
                        currentSwitch.active = 10
                        setSwitchState(position, switchHolder.switch.isChecked)
                        if (switchHolder.switch.isChecked) {
                            Send_Global_Switch_Req(currentSwitch.idx, switchList[position].on, activity, position).execute()
                        } else {
                            Send_Global_Switch_Req(currentSwitch.idx, switchList[position].off, activity, position).execute()
                        }
                    }else{
                        setSwitchState(position, !switchHolder.switch.isChecked)
                    }
                }
            }
            TYPE_BUTTON -> {
                val buttonHolder = holder as ButtonViewHolder
                val currentButton = switchList[position]
                buttonHolder.button.text = currentButton.name
                buttonHolder.button.visibility = currentButton.visibility
                buttonHolder.button.setOnClickListener {
                    // Handle button click event
                    val swindex = GSwitches.indexOf(switchList[position])
                    if (swindex >= 0 && swindex < GSwitches.size) {
                        val b = Bundle()
                        b.putInt("key", swindex) //Your id
                        if (switchList[position].type == 11){
                            val intent = Intent(activity, Ir_Ac_Remote::class.java)
                            intent.putExtras(b)
                            activity.startActivity(intent)
                        } else if (switchList[position].type == 12){
                            val intent = Intent(activity, Ir_Tv_Remote::class.java)
                            intent.putExtras(b)
                            activity.startActivity(intent)
                        }else if (switchList[position].type == 13){
                            val intent = Intent(activity, Ir_Switch_Remote::class.java)
                            intent.putExtras(b)
                            activity.startActivity(intent)
                        }else if (switchList[position].type == 14){
                            val intent = Intent(activity, Ir_Smart_Light_Remote::class.java)
                            intent.putExtras(b)
                            activity.startActivity(intent)
                        }else if (switchList[position].type == 15){
                            val intent = Intent(activity, Ir_Fan_Remote::class.java)
                            intent.putExtras(b)
                            activity.startActivity(intent)
                        }else if (switchList[position].type == 16){
                            val intent = Intent(activity, Ir_Music_Remote::class.java)
                            intent.putExtras(b)
                            activity.startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return switchList.size
    }

    fun setSwitchState(position: Int, newState: Boolean) {
        // Check if the position is within the valid range
        if (position in 0 until switchList.size) {
            // Update the switch state
            switchList[position].switchState = newState
            // Notify the adapter about the data set change
            notifyItemChanged(position)
        } else {
            Log.e("SwitchAdapter", "Invalid position: $position")
        }
    }

    fun getSwitchState(position: Int) :Boolean{
        // Check if the position is within the valid range
        if (position in 0 until switchList.size) {
            // Update the switch state
            return switchList[position].switchState
        } else {
            Log.e("SwitchAdapter", "Invalid position: $position")
        }
        return false
    }

    fun sort_inactive_switches() {
        switchList.sortBy { it.switchState }
        notifyDataSetChanged()
    }
    fun sort_active_switches() {
        switchList.sortByDescending { it.switchState }
        notifyDataSetChanged()
    }

    fun active_switches() {
        switchList.sortByDescending { it.switchState }
        // Check if the position is within the valid range
        for (position in 0 until switchList.size) {
            if (switchList[position].switchState != true) {
                switchList[position].visibility = View.GONE
            }else{
                switchList[position].visibility = View.VISIBLE
            }
        }
        notifyDataSetChanged()
    }

    fun show_ir_remotes(){
        switchList.sortByDescending { it.type }
        for (position in 0 until switchList.size) {
            if (switchList[position].id.toInt() < 32)
                switchList[position].visibility = View.GONE
            else{
                switchList[position].visibility = View.VISIBLE
            }
        }
        notifyDataSetChanged()
    }

    fun show_all(){
        for (position in 0 until switchList.size) {
                switchList[position].visibility = View.VISIBLE
        }
        switchList.sortBy { it.id.toInt() }
        notifyDataSetChanged()
    }

    fun addSwitch(idx:Int, dev_id:Int, id:String, type: Int, name: String, ps:String = "0") {
        var on = ""
        var off = ""
        if (type == 1 || type == 2){
            on = "IO,SET,PORT,"+id+",ON,"
            off = "IO,SET,PORT,"+id+",OFF,"
        }
        switchList.add(sw_params(id, name, type, dev_id, idx, on, off, powersave = ps))
    }
    fun update() {
        notifyDataSetChanged()
    }
    fun update_index(position: Int) {
        notifyItemChanged(position)
    }
}