package com.example.battleshipgame

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class ScoresActivity : AppCompatActivity() {

    lateinit var gameManager: GameManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scores)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        gameManager = GameManager()

        // Initialize Firebase
        val database = FirebaseDatabase.getInstance().reference

        // Fetch and display users
        gameManager.getAllUsersSortedByHighestScore() { success, userList ->
            if (success && userList != null) {
                val users = userList.map {
                    User(it["userId"] as String, it["username"] as String,
                        (it["highestScore"] as Long).toString()
                    )
                }
                setupRecyclerView(users)
            } else {
                // Handle the error
                Toast.makeText(this, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupRecyclerView(userList: List<User>) {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = UserAdapter(userList)
    }
}
