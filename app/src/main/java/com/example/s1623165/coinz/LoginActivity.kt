package com.example.s1623165.coinz

import android.arch.lifecycle.Transformations.map
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth : FirebaseAuth
    private lateinit var username : EditText
    private lateinit var password : EditText
    private lateinit var login : Button
    private lateinit var signup : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance()
        setUpUIViews()

        login.setOnClickListener { v ->
            validate(username.text.toString(), password.text.toString())
        }

        signup.setOnClickListener{ v ->
            signup()
        }

    }

    private fun setUpUIViews() {
        username = findViewById(R.id.login_username)
        password = findViewById(R.id.login_password)
        login = findViewById(R.id.login_button)
        signup = findViewById(R.id.signUp_button)
    }

    override fun onStart() {
        super.onStart()
//        val currentUser = mAuth.currentUser
//        updateUI(currentUser)
    }

    private fun validate(userName : String, userPassword : String) {
        if(username.equals(userName) && password.equals(userPassword))
        {
            map()
        } else {
          Toast.makeText(this,
                  "Username or Password is invalid, Please try again.",
                  Toast.LENGTH_SHORT)
                  .show()
        }
    }

    fun map() {
        val menuIntent = Intent(this, Map::class.java)
        startActivity(menuIntent)
    }

    fun signup() {
        val signupIntent = Intent(this, SignUpActivity::class.java)
        startActivity(signupIntent)
    }


}
