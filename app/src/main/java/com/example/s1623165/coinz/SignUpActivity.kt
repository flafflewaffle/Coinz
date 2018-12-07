package com.example.s1623165.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlin.math.sign

class SignUpActivity : AppCompatActivity() {

    private lateinit var mAuth : FirebaseAuth
    private lateinit var password : EditText
    private lateinit var email : EditText
    private lateinit var signup : Button
    private lateinit var already : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        setSupportActionBar(toolbar)
        mAuth = FirebaseAuth.getInstance()
        setUpUIViews()

        signup.setOnClickListener { _ ->
            if(validateDetails()) {
                //Upload data to database
                val user_email = email.text.toString().trim()
                val user_password = password.text.toString().trim()

                mAuth.createUserWithEmailAndPassword(user_email, user_password)
                        .addOnCompleteListener { task ->
                            if(task.isSuccessful) {
                                Toast.makeText(this,
                                        "Registration Successful",
                                        Toast.LENGTH_SHORT)
                                        .show()
                                super.onBackPressed()
                            } else {
                                Toast.makeText(this,
                                        "Registration Failed",
                                        Toast.LENGTH_SHORT)
                                        .show()
                            }
                        }
            }
        }

        already.setOnClickListener { _ ->
            super.onBackPressed()
        }

    }

    private fun validateDetails() : Boolean {
        val pass = password.text.toString()
        val email = email.text.toString()
        if(pass.isEmpty() || email.isEmpty()) {
            Toast.makeText(this,
                    "Please enter valid credentials.",
                    Toast.LENGTH_SHORT)
                    .show()
            return false
        }
        return true
    }

    private fun setUpUIViews() {
        password = findViewById(R.id.signup_password)
        email = findViewById(R.id.signup_email)
        signup = findViewById(R.id.signup_button)
        already = findViewById(R.id.alreadysignedup)
    }

}
