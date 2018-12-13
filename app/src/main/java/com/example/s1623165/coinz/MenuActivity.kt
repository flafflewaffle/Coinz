package com.example.s1623165.coinz

import android.arch.lifecycle.Transformations.map
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.s1623165.coinz.R.id.*
import kotlinx.android.synthetic.main.activity_menu.*
import kotlinx.android.synthetic.main.app_bar_menu.*

class MenuActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {

        //set listeners and default opening fragment to wallet
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        setSupportActionBar(toolbar)
        fab.setOnClickListener { _ -> super.onBackPressed()}

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        if(savedInstanceState == null) {
            displayFragment(-1)
            nav_view.setCheckedItem(R.id.nav_wallet)
        }

    }

    //close navigation drawer on back pressed
    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    //display corresponding fragment, wallet fragment is the default
    fun displayFragment(id: Int) {
        val fragment = when (id) {
            R.id.nav_wallet -> {
                WalletFragment()
            }
            R.id.nav_bank -> {
                BankFragment()
            }
            R.id.nav_send_receive -> {
                SendReceiveFragment()
            }
            R.id.nav_powerup -> {
                Powerup_Fragment()
            }
            else -> {
                WalletFragment()
            }
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.relativeLayout, fragment as Fragment)
                .commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        displayFragment(item.itemId)
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

}
