package com.example.s1623165.coinz

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

class BankFragment : Fragment() {

    private var exchangeRates = HashMap<String, String>()
    private var barEntries = ArrayList<BarEntry>()
    private var currencies = ArrayList<String>()
    private var colours = ArrayList<Int>()
    private val prefsFile = "MyPrefsFile"

    private lateinit var mContext: Context
    private lateinit var barChart : BarChart

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        this.mContext = context!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setExchangeRates()
        setBarEntries()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater?.inflate(R.layout.bank_fragment, null)
        barChart = root.findViewById(R.id.exchangeRates) as BarChart

        colours.add(ContextCompat.getColor(barChart.context, R.color.colorPrimary))
        colours.add(ContextCompat.getColor(barChart.context, R.color.colorAccent))
        colours.add(ContextCompat.getColor(barChart.context, R.color.fbYellow))
        colours.add(ContextCompat.getColor(barChart.context, R.color.fbGreen))

        val barDataSet = BarDataSet(barEntries, "Exchange Rates")
        barDataSet.colors = colours
        val barData = BarData(barDataSet)
        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.xAxis.setLabelCount(4,true)
        barChart.xAxis.setValueFormatter { value, _ ->
            currencies[value.roundToInt()%currencies.size]
        }
        return root
    }

    // retrieve the current exchange rates from shared preferences
    private fun setExchangeRates() {
        val settings = activity!!.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)

        exchangeRates.put("QUID",settings.getString("QUID",""))
        exchangeRates.put("DOLR",settings.getString("DOLR",""))
        exchangeRates.put("PENY",settings.getString("PENY",""))
        exchangeRates.put("SHIL",settings.getString("SHIL",""))
    }

    private fun setBarEntries() {
        barEntries.add(BarEntry(0F, exchangeRates["QUID"]!!.toFloat()))
        barEntries.add(BarEntry(1F,exchangeRates["DOLR"]!!.toFloat()))
        barEntries.add(BarEntry(2F,exchangeRates["PENY"]!!.toFloat()))
        barEntries.add(BarEntry(3F,exchangeRates["SHIL"]!!.toFloat()))
        currencies.add("QUID")
        currencies.add("DOLR")
        currencies.add("PENY")
        currencies.add("SHIL")
    }
}