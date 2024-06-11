package com.example.battleshipgame

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.battleshipgame.GameManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import kotlin.concurrent.thread

class GameActivity : AppCompatActivity() {

    private lateinit var gameManager: GameManager
    private var currentUserId: String? = null
    private var currentGameId: String? = null
    private var rivalUserId: String? = null
    private val gridSize = 8
    private val playerGrid = Array(gridSize) { IntArray(gridSize) }
    private val gridButtons = Array(gridSize) { arrayOfNulls<Button>(gridSize) }
    private var isPlacingShips = true
    private var currentShipSize = 5 // Default to the largest ship size
    private var isVertical = false // Default orientation is horizontal
    private val shipTypes = mapOf(5 to 1, 4 to 1, 3 to 2, 2 to 1)
    private val placedShips = mutableMapOf(5 to 0, 4 to 0, 3 to 0, 2 to 0)
    private var shipsPlaced = 0
    private var isMyTurn = false
    private var user1Id: String? = null
    private var user2Id: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameManager = GameManager()
        currentUserId = intent.getStringExtra("userId")
        currentGameId = intent.getStringExtra("gameId")

        if (currentUserId == null || currentGameId == null) {
            Toast.makeText(this, "Invalid game setup.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        fetchGameDetails()
    }

    private fun initializeViews() {
        val orientationToggle: Button = findViewById(R.id.orientationToggle)
        val shipSizeSpinner: Spinner = findViewById(R.id.shipSizeSpinner)
        val gridLayout: GridLayout = findViewById(R.id.gridLayout)

        orientationToggle.setOnClickListener {
            isVertical = !isVertical
            orientationToggle.text = if (isVertical) "Vertical" else "Horizontal"
        }

        shipSizeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, shipTypes.keys.toList()).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        shipSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentShipSize = parent?.getItemAtPosition(position) as Int
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val button = Button(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 100
                        height = 100
                        rowSpec = GridLayout.spec(i)
                        columnSpec = GridLayout.spec(j)
                        setMargins(4, 4, 4, 4)
                    }
                    setBackgroundColor(Color.BLUE)
                    setOnClickListener {
                        if (isPlacingShips) {
                            placeShip(i, j)
                        } else {
                            shoot(i, j)
                        }
                    }
                }
                gridButtons[i][j] = button
                gridLayout.addView(button)
            }
        }
    }

    private fun fetchGameDetails() {
        gameManager.gameRef(currentGameId)?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    user1Id = snapshot.child("user1Id").value as? String
                    user2Id = snapshot.child("user2Id").value as? String

                    rivalUserId = if (currentUserId == user1Id) user2Id else user1Id
                    val gameInfoTextView: TextView = findViewById(R.id.gameInfoTextView)
                    gameInfoTextView.text = "Game ID: $currentGameId\nYour ID: $currentUserId\nRival ID: $rivalUserId"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GameActivity, "Failed to fetch game details: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun placeShip(row: Int, col: Int) {
        if (placedShips[currentShipSize]!! < shipTypes[currentShipSize]!!) {
            if (canPlaceShip(row, col, isVertical)) {
                for (i in 0 until currentShipSize) {
                    if (isVertical) {
                        playerGrid[row + i][col] = 1
                        gridButtons[row + i][col]?.setBackgroundColor(Color.GRAY)
                    } else {
                        playerGrid[row][col + i] = 1
                        gridButtons[row][col + i]?.setBackgroundColor(Color.GRAY)
                    }
                }
                placedShips[currentShipSize] = placedShips[currentShipSize]!! + 1
                shipsPlaced++
                if (shipsPlaced == shipTypes.values.sum()) {
                    isPlacingShips = false
                    notifyReady()
                }
            } else {
                Toast.makeText(this, "Cannot place ship here.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "All $currentShipSize-cell ships are already placed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun canPlaceShip(row: Int, col: Int, vertical: Boolean): Boolean {
        return if (vertical) {
            (0 until currentShipSize).all { row + it < gridSize && playerGrid[row + it][col] == 0 }
        } else {
            (0 until currentShipSize).all { col + it < gridSize && playerGrid[row][col + it] == 0 }
        }
    }

    private fun notifyReady() {
        val flattenedBoard = playerGrid.flatMap { it.toList() }
        gameManager.updateShipBoard(currentGameId, currentUserId, flattenedBoard) { success, message ->
            if (success) {
                Toast.makeText(this, "Waiting for opponent to place ships...", Toast.LENGTH_SHORT).show()
                waitForOpponent()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun waitForOpponent() {
        val statusTextView: TextView = findViewById(R.id.statusTextView)
        statusTextView.text = "Waiting for opponent..."
        var opponentReady = false

        thread {
            while (!opponentReady) {
                gameManager.getWhosTurnIsIt(currentGameId) { success, message, turn ->
                    if (success && turn != null) {
                        runOnUiThread {
                            if (turn?.toLong() == 1L || turn?.toLong() == 2L) {
                                startGame()
                                opponentReady = true
                            }
                        }
                    }
                }
                Thread.sleep(2000)
            }
        }
    }

    private fun startGame() {
        val statusTextView: TextView = findViewById(R.id.statusTextView)
        statusTextView.text = "Game in progress..."
        gameManager.getWhosTurnIsIt(currentGameId) { success, _, turn ->
            if (success) {
                isMyTurn = (turn?.toLong() == 1L && currentUserId == user1Id) || (turn?.toLong() == 2L && currentUserId == user2Id)
                updateTurnStatus()
            }
        }
        resetGridForGameplay()
    }

    private fun resetGridForGameplay() {
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                gridButtons[i][j]?.setBackgroundColor(Color.BLUE)
                gridButtons[i][j]?.setOnClickListener {
                    shoot(i, j)
                }
            }
        }
    }

    private fun shoot(row: Int, col: Int) {
        if (isMyTurn) {
            gameManager.shoot(currentGameId, currentUserId, row * gridSize + col) { success, message ->
                if (success) {
                    updateGridAfterShot(row, col)
                    checkGameState()
                    isMyTurn = false
                    updateTurnStatus()
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "It's not your turn!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateGridAfterShot(row: Int, col: Int) {
        gameManager.userFogOfWarBoardRef(currentGameId, currentUserId)?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fogOfWarBoard = snapshot.getValue(object : GenericTypeIndicator<List<Int>>() {})
                if (fogOfWarBoard != null) {
                    val hit = fogOfWarBoard[row * gridSize + col] == 2
                    runOnUiThread {
                        gridButtons[row][col]?.setBackgroundColor(if (hit) Color.RED else Color.WHITE)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GameActivity, "Error fetching fog of war data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkGameState() {
        gameManager.getHitPoints(currentGameId, currentUserId) { success, _, hitPoints ->
            if (success) {
                if (hitPoints == 0) {
                    endGame(false)
                } else {
                    gameManager.getHitPoints(currentGameId, rivalUserId) { opponentSuccess, _, opponentHitPoints ->
                        if (opponentSuccess) {
                            if (opponentHitPoints == 0) {
                                endGame(true)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun endGame(won: Boolean) {
        val statusTextView: TextView = findViewById(R.id.statusTextView)
        statusTextView.text = if (won) "You won!" else "You lost!"
        thread {
            Thread.sleep(3000)
            runOnUiThread {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun updateTurnStatus() {
        val statusTextView: TextView = findViewById(R.id.statusTextView)
        statusTextView.text = if (isMyTurn) "Your turn" else "Opponent's turn"

        if (!isMyTurn) {
            waitForOpponentTurn()
        }
    }

    private fun waitForOpponentTurn() {
        var turnChanged = false

        thread {
            while (!turnChanged) {
                gameManager.getWhosTurnIsIt(currentGameId) { success, _, turn ->
                    if (success && ((turn?.toLong() == 1L && currentUserId == user1Id) || (turn?.toLong() == 2L && currentUserId == user2Id))) {
                        runOnUiThread {
                            turnChanged = true
                            isMyTurn = true
                            updateTurnStatus()
                        }
                    }
                }
                Thread.sleep(2000)
            }
        }
    }
}
