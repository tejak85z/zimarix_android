package com.example.zimarix_1.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.zimarix_1.databinding.FragmentMainBinding
import com.example.zimarix_1.zimarix_global.Companion.controller_ips
import com.example.zimarix_1.zimarix_global.Companion.controller_keys
import com.example.zimarix_1.zimarix_global.Companion.controller_names

import android.widget.AdapterView.OnItemClickListener
import com.example.zimarix_1.Devsettings
import com.example.zimarix_1.zimarix_global.Companion.controller_devices


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
                            val info = controller_ips[i]+","+dev_params[0]+","+dev_params[2]+","+dev_params[3]
                            dev_info = dev_info + info
                        }
                    }
                    i = i +1
                }
                pioneers = devices
                listView.setOnItemClickListener(OnItemClickListener { arg0, arg1, position, arg3 ->
                    val layout = LinearLayout(context)
                    layout.orientation = LinearLayout.VERTICAL

                    val cncl = Switch(context)
                    layout.addView(cncl)
                    layout.setPadding(50, 40, 50, 10)
                    val builder = AlertDialog.Builder(context)
                        .setTitle(dev_info[position])
                        .setView(layout)
                    val dialog = builder.create()
                    dialog.show()
                    cncl.setOnClickListener(){
                    }
                })
            }
            else if (it == "3") {
                pioneers = controller_ips.toTypedArray()
                listView.setOnItemClickListener(OnItemClickListener { arg0, arg1, position, arg3 ->
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