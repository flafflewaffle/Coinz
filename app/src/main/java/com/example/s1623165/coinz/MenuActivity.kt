package com.example.s1623165.coinz

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        displayFragment(-1)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> {
                settings()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun displayFragment(id: Int) {
        val fragment = when (id) {
            R.id.nav_map -> {
                MapFragment()
            }
            R.id.nav_wallet -> {
                WalletFragment()
            }
            R.id.nav_bank -> {
                BankFragment()
            }
            R.id.nav_send_receive -> {
                SendReceiveFragment()
            }
            else -> {
                MapFragment()
            }
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.relativeLayout, fragment)
                .commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        displayFragment(item.itemId)

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun settings() {
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }
}
