package com.example.s1623165.coinz

import android.arch.lifecycle.Transformations.map
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth : FirebaseAuth
    private lateinit var email : EditText
    private lateinit var password : EditText
    private lateinit var login : Button
    private lateinit var signup : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(toolbar)
        setUpUIViews()
        mAuth = FirebaseAuth.getInstance()
        val currentUser = mAuth.currentUser
        if(currentUser != null) {
            finish()
            map()
        }

        login.setOnClickListener { _ ->
            validate(email.text.toString(), password.text.toString())
        }

        signup.setOnClickListener{ _ ->
            signup()
        }

    }

    private fun setUpUIViews() {
        email = findViewById(R.id.login_email)
        password = findViewById(R.id.login_password)
        login = findViewById(R.id.login_button)
        signup = findViewById(R.id.signUp_button)
    }

    private fun validate(userEmail : String, userPassword : String) {
        if(userEmail.isEmpty() || userPassword.isEmpty()) {
            Toast.makeText(this,
                    "Please enter valid credentials.",
                    Toast.LENGTH_SHORT)
                    .show()
        } else {

            mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this,
                                    "Login successful",
                                    Toast.LENGTH_SHORT)
                                    .show()
                            map()
                        } else {
                            Toast.makeText(this,
                                    "Login failed",
                                    Toast.LENGTH_SHORT)
                                    .show()
                        }
                    }
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
