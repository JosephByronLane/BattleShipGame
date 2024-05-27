package com.example.battleshipgame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var gameManager: GameManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var loggedInUserID: String? = "-NySSCZXIcVjij2SBZf1"


        gameManager = GameManager()

        val email = "user@exa12mple.com"
        val password = "securePassword123"
        val username = "exampleUser"

//         //Register User
//        gameManager.registerUser(email, password, username) { success, message ->
//            if (success) {
//                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//            } else
//            {
//                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//            }
//        }


        //login user
        gameManager.loginUser(email, password) { success, message, userId ->
            if (success) {
                Toast.makeText(this, "Login successful! User ID: $userId", Toast.LENGTH_LONG).show()
                loggedInUserID = userId
                // Save userId for later use or pass it to the next activity
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        //crate match
        Log.d("gameManagerLoginUserID", "$loggedInUserID")
        if(loggedInUserID != ""){
            gameManager.createMatch(loggedInUserID) { success, message, gameId ->
                if (success) {
                    Toast.makeText(this, "Match created! Game ID: $gameId", Toast.LENGTH_LONG).show()
                    // Navigate to game screen with gameId
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }

    }
}