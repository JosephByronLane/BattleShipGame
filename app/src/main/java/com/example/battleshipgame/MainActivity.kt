package com.example.battleshipgame

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var botonJuego: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        botonJuego = findViewById(R.id.start_game_button)

        botonJuego.setOnClickListener{
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
    }
}