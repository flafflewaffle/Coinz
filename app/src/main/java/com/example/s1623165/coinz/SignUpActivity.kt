package com.example.s1623165.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.sign

class SignUpActivity : AppCompatActivity() {

    private lateinit var mAuth : FirebaseAuth
    private lateinit var username : EditText
    private lateinit var password : EditText
    private lateinit var email : EditText
    private lateinit var signup : Button
    private lateinit var already : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        mAuth = FirebaseAuth.getInstance()
        setUpUIViews()

        signup.setOnClickListener { v ->
            if(validateDetails()) {
                //Upload data to database
                val user_email = email.text.toString().trim()
                val user_password = password.text.toString().trim()
                val user_name = username.text.toString().trim()

                mAuth.createUserWithEmailAndPassword(user_email, user_password)
                        .addOnCompleteListener { task ->
                            if(task.isSuccessful) {
                                Toast.makeText(this,
                                        "Registration Successful",
                                        Toast.LENGTH_SHORT)
                                        .show()
                                login()
                            } else {
                                Toast.makeText(this,
                                        "Registration Failed",
                                        Toast.LENGTH_SHORT)
                                        .show()
                            }
                        }
            }
        }

        already.setOnClickListener { v ->
            login()
        }

    }

    private fun validateDetails() : Boolean {
        val name = username.text.toString()
        val pass = password.text.toString()
        val email = email.text.toString()
        if(name.isEmpty() || pass.isEmpty() || email.isEmpty()) {
            Toast.makeText(this,
                    "Please enter valid credentials.",
                    Toast.LENGTH_SHORT)
                    .show()
            return false
        }
        return true
    }

    fun login() {
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
    }

    private fun setUpUIViews() {
        username = findViewById(R.id.signup_username)
        password = findViewById(R.id.signup_password)
        email = findViewById(R.id.signup_email)
        signup = findViewById(R.id.signup_button)
        already = findViewById(R.id.alreadysignedup)
    }

}
