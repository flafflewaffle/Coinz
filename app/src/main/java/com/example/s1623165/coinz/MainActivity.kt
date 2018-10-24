package com.example.s1623165.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun menu(view: View) {
        val menuIntent = Intent(this, MenuActivity::class.java)
        startActivity(menuIntent)
    }
}
