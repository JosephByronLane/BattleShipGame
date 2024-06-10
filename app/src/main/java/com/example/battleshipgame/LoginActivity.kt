package com.example.battleshipgame

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.battleshipgame.GameManager

class LoginActivity : AppCompatActivity() {

    private lateinit var gameManager: GameManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        gameManager = GameManager()

        val emailEditText: EditText = findViewById(R.id.editTextTextEmailAddress)
        val passwordEditText: EditText = findViewById(R.id.editTextNumberPassword)
        val loginButton: Button = findViewById(R.id.loginButtonEmailPassword)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        gameManager.loginUser(email, password) { success, message, userId ->
            if (success) {
                saveUserToLocal(email)
                val resultIntent = Intent()
                resultIntent.putExtra("userId", userId)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                gameManager.registerUser(email, password, email.split("@")[0]) { regSuccess, regMessage ->
                    if (regSuccess) {
                        loginUser(email, password)
                    } else {
                        Toast.makeText(this, regMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveUserToLocal(email: String) {
        val sharedPref = getSharedPreferences("local_users", Context.MODE_PRIVATE)
        val users = sharedPref.getStringSet("users", mutableSetOf()) ?: mutableSetOf()
        users.add(email)
        with(sharedPref.edit()) {
            putStringSet("users", users)
            apply()
        }
    }
}
