package com.example.zimarix_1.Activities

import android.app.AlertDialog
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.zimarix_1.R
import com.example.zimarix_1.dev_req
import com.example.zimarix_1.update_params
import java.security.KeyStore
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class stats : AppCompatActivity(){
    private lateinit var cpuChart: LineChart
    private lateinit var memChart: LineChart

    var active: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        cpuChart = findViewById(R.id.cpuChart)
        memChart = findViewById(R.id.memoryChart)

        val b = intent.extras
        var value = -1 // or other values
        if (b != null) value = b.getInt("key")
        val idx = Bundle()
        idx.putInt("key", value) //Your id

        CoroutineScope(Dispatchers.IO).launch {
            stats_updater(value)
        }

        val cpuUsageData = listOf(10f, 20f, 30f, 25f, 15f, 35f, 40f)
        updateCpuChart(cpuUsageData)

        val memUsageData = listOf(10f, 20f, 30f, 25f, 15f, 35f, 40f)
        updateMemChart(memUsageData)
    }

    private fun updateCpuChart(cpuUsageData: List<Float>) {
        // Create entries for the chart
        val entries = mutableListOf<Entry>()
        for ((index, usage) in cpuUsageData.withIndex()) {
            entries.add(Entry(index.toFloat(), usage))
        }
        // Create a dataset with the entries
        val dataSet = LineDataSet(entries, "CPU Usage")
        dataSet.color = Color.BLUE // Set line color
        dataSet.valueTextColor = Color.BLACK // Set text color
        dataSet.setDrawCircles(false) // Do not draw circles at data points
        dataSet.setDrawValues(false) // Do not draw values

        // Add dataset to the chart
        val dataSets = mutableListOf<ILineDataSet>()
        dataSets.add(dataSet)
        val lineData = LineData(dataSets)

        // Customize chart appearance
        cpuChart.data = lineData
        cpuChart.setTouchEnabled(false)
        cpuChart.description = null // Hide description
        cpuChart.xAxis.isEnabled = false // Disable x-axis
        cpuChart.axisRight.isEnabled = false // Disable right axis
        cpuChart.legend.isEnabled = false // Hide legend

        // Set y-axis range between 0 and 100
        cpuChart.axisLeft.axisMinimum = 0f
        cpuChart.axisLeft.axisMaximum = 100f

        cpuChart.invalidate() // Refresh chart
    }

    private fun updateMemChart(memUsageData: List<Float>) {
        // Create entries for the chart
        val entries = mutableListOf<Entry>()
        for ((index, usage) in memUsageData.withIndex()) {
            entries.add(Entry(index.toFloat(), usage))
        }
        // Create a dataset with the entries
        val dataSet = LineDataSet(entries, "MEM Usage")
        dataSet.color = Color.BLUE // Set line color
        dataSet.valueTextColor = Color.BLACK // Set text color
        dataSet.setDrawCircles(false) // Do not draw circles at data points
        dataSet.setDrawValues(false) // Do not draw values

        // Add dataset to the chart
        val dataSets = mutableListOf<ILineDataSet>()
        dataSets.add(dataSet)
        val lineData = LineData(dataSets)

        // Customize chart appearance
        memChart.data = lineData
        memChart.setTouchEnabled(false)
        memChart.description = null // Hide description
        memChart.xAxis.isEnabled = false // Disable x-axis
        memChart.axisRight.isEnabled = false // Disable right axis
        memChart.legend.isEnabled = false // Hide legend

        // Set y-axis range between 0 and 100
        memChart.axisLeft.axisMinimum = 0f
        memChart.axisLeft.axisMaximum = 100f

        memChart.invalidate() // Refresh chart
    }

    fun stats_updater(index: Int){
        var resp = ""
        while (true){
            if (active){
                resp = dev_req(index, "STAT,CPU,")
                if (resp.length > 1 ) {
                    val dataArray = resp.substringAfter("[").substringBeforeLast("]").split(", ")
                    val cpuUsageData = dataArray.map { it.toFloat() }
                    GlobalScope.launch(Dispatchers.Main) {
                        updateCpuChart(cpuUsageData)
                    }
                }
                resp = dev_req(index, "STAT,MEM,")
                if (resp.length > 10 ) {
                    val dataArray = resp.substringAfter("[").substringBeforeLast("]").split(", ")
                    val memUsageData = dataArray.map { it.toFloat() }
                    GlobalScope.launch(Dispatchers.Main) {
                        updateMemChart(memUsageData)
                    }
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