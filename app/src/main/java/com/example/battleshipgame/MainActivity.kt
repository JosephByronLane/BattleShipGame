package com.example.battleshipgame

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.battleshipgame.GameManager

class MainActivity : AppCompatActivity() {

    private lateinit var gameManager: GameManager
    private var currentUserId: String? = null
    private var currentGameId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameManager = GameManager()

        val loginButton: Button = findViewById(R.id.login_button)
        val playButton: Button = findViewById(R.id.play_button)
        val scoreboardButton: Button = findViewById(R.id.scoreboard_button)
        val userSpinner: Spinner = findViewById(R.id.userSpinner)

        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivityForResult(intent, REQUEST_LOGIN)
        }

        playButton.setOnClickListener {
            if (currentUserId != null) {
                findOrCreateMatch()
            }
        }

        scoreboardButton.setOnClickListener {
            val intent = Intent(this, ScoresActivity::class.java)
            startActivity(intent)
        }

        loadLocalUsers(userSpinner)

        userSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedUser = parent?.getItemAtPosition(position).toString()
                // Implement user-specific logic if needed
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_LOGIN && resultCode == RESULT_OK) {
            currentUserId = data?.getStringExtra("userId")
            findViewById<Button>(R.id.play_button).isEnabled = true
            loadLocalUsers(findViewById(R.id.userSpinner))
        }
    }

    private fun loadLocalUsers(spinner: Spinner) {
        val sharedPref = getSharedPreferences("local_users", MODE_PRIVATE)
        val users = sharedPref.getStringSet("users", mutableSetOf())?.toList() ?: listOf()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, users)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun findOrCreateMatch() {
        currentUserId?.let { userId ->
            gameManager.joinMatch(userId) { success, message, gameId ->
                if (success) {
                    currentGameId = gameId
                    startGame()
                } else {
                    gameManager.createMatch(userId) { createSuccess, createMessage, createGameId ->
                        if (createSuccess) {
                            currentGameId = createGameId
                            startGame()
                        } else {
                            Toast.makeText(this, createMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun startGame() {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("userId", currentUserId)
        intent.putExtra("gameId", currentGameId)
        startActivity(intent)
    }

    companion object {
        const val REQUEST_LOGIN = 1
    }
}
