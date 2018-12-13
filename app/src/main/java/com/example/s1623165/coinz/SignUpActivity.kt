package com.example.s1623165.coinz

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlin.math.sign

class SignUpActivity : AppCompatActivity() {

    private lateinit var mAuth : FirebaseAuth
    private lateinit var password : EditText
    private lateinit var email : EditText
    private lateinit var signup : Button
    private lateinit var already : Button
    private lateinit var db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        setSupportActionBar(toolbar)
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        setUpUIViews()

        // retrieves the user email and password and creates a new user in the database
        signup.setOnClickListener { _ ->
            if(validateDetails()) {
                //Upload data to database
                val userEmail = email.text.toString().trim()
                val userPassword = password.text.toString().trim()
                mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                        .addOnCompleteListener { task ->
                            if(task.isSuccessful) {
                                Toast.makeText(this,
                                        "Registration Successful",
                                        Toast.LENGTH_SHORT)
                                        .show()
                                super.onBackPressed()
                            } else {
                                if (task.exception is FirebaseAuthUserCollisionException) {
                                    Toast.makeText(this,
                                            "User with this email already exist.",
                                            Toast.LENGTH_SHORT)
                                            .show()
                                }
                                else {
                                    Toast.makeText(this,
                                            "Registration Failed",
                                            Toast.LENGTH_SHORT)
                                            .show()
                                }
                            }
                        }
            }
        }

        already.setOnClickListener { _ ->
            super.onBackPressed()
        }

    }

    // simple check to make sure user email and password is valid
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
        if(pass.length < 6) {
            Toast.makeText(this,
                    "Please enter a password of at least 6 characters.",
                    Toast.LENGTH_SHORT)
                    .show()
            return false
        }
        return true
    }
    // sets the views of variables
    private fun setUpUIViews() {
        password = findViewById(R.id.signup_password)
        email = findViewById(R.id.signup_email)
        signup = findViewById(R.id.signup_button)
        already = findViewById(R.id.alreadysignedup)
    }

}
