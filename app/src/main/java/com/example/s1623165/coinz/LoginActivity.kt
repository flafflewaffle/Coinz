package com.example.s1623165.coinz

import android.arch.lifecycle.Transformations.map
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.example.s1623165.coinz.R.id.toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth : FirebaseAuth
    private lateinit var email : EditText
    private lateinit var password : EditText
    private lateinit var login : Button
    private lateinit var signup : Button
    private lateinit var db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(toolbar)
        setUpUIViews()
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
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

    //sets the views for variables
    private fun setUpUIViews() {
        email = findViewById(R.id.login_email)
        password = findViewById(R.id.login_password)
        login = findViewById(R.id.login_button)
        signup = findViewById(R.id.signUp_button)
    }

    //checks if the user email and password is valid and logs in
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
                                    "Login successful!",
                                    Toast.LENGTH_SHORT)
                                    .show()
                            getGold()
                        } else {
                            Toast.makeText(this,
                                    "Login failed! Not signed up!",
                                    Toast.LENGTH_SHORT)
                                    .show()
                        }
                    }
        }
    }

    //gets the amount of gold from the bank, if this is not set, sets the bank
    private fun getGold() {
        db.collection("Users")
                .document(mAuth.currentUser!!.email!!)
                .collection("User Information")
                .document("Bank")
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if(documentSnapshot.exists()) {
                        val gold = documentSnapshot.get("Gold")
                        if(gold == null) {
                            setBank()
                        }
                        else {
                            val settings = getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                            val editor = settings.edit()
                            editor.putInt("Bank", (gold as Number).toInt())
                            editor.apply()
                            map()
                        }
                    }
                    else {
                        setBank()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this,
                            "Error accessing current amount of gold",
                            Toast.LENGTH_SHORT)
                            .show()
                    Log.d("Signup Activity", e.toString())
                }
    }

    //sets the allowance and gold values in the bank
    private fun setBank() {
        val gold = HashMap<String, Any>()
        gold["Gold"] = 0

        val allowance = HashMap<String, Any>()
        allowance["Allowance"] = 25

        val bankReference = db.collection("Users").document(mAuth.currentUser!!.email!!)
                .collection("User Information").document("Bank")
                bankReference.set(gold, SetOptions.merge())
                .addOnSuccessListener { _ ->
                    bankReference.set(allowance, SetOptions.merge())
                            .addOnSuccessListener {
                                Log.d("Login Activity", "Bank successfully setup for user")
                                map()
                            }
                            .addOnFailureListener { e ->
                                Log.d("Login Activity", e.toString())
                            }
                }
                .addOnFailureListener { e ->
                    Log.d("Login Activity", e.toString())
                }
    }

    //navigation functions
    fun map() {
        val menuIntent = Intent(this, Map::class.java)
        startActivity(menuIntent)
    }

    fun signup() {
        val signupIntent = Intent(this, SignUpActivity::class.java)
        startActivity(signupIntent)
    }

}
