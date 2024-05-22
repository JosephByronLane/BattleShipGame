package com.example.battleshipgame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var gameManager: GameManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        gameManager = GameManager()

        val email = "user@exa12mple.com"
        val password = "securePassword123"
        val username = "exampleUser"

        // Register User
//        gameManager.registerUser(email, password, username) { success, message ->
//            if (success) {
//                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//            } else
//            {
//                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//            }
//        }




        //login user
        gameManager.loginUser(email, password) { success, message ->
            if (success) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else
            {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}