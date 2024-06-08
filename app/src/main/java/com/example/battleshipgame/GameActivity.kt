package com.example.battleshipgame

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    private val gridSize = 8
    private val battleshipGrid = Array(gridSize) { IntArray(gridSize) }
    private var isPlacingShips = true
    private var currentShipSize = 5 // Default to the largest ship size
    private var isVertical = false // Default orientation is horizontal

    // Track placed ships
    private val placedShips = mutableMapOf(5 to 0, 4 to 0, 3 to 0, 2 to 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val modeToggle: ToggleButton = findViewById(R.id.modeToggle)
        val shipSizeSpinner: Spinner = findViewById(R.id.shipSizeSpinner)
        val orientationToggle: ToggleButton = findViewById(R.id.orientationToggle)

        modeToggle.setOnCheckedChangeListener { _, isChecked ->
            isPlacingShips = !isChecked
        }

        shipSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentShipSize = parent?.getItemAtPosition(position).toString().toInt()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        orientationToggle.setOnCheckedChangeListener { _, isChecked ->
            isVertical = isChecked
        }

        initializeGrid()
        populateGridLayout()
    }

    private fun initializeGrid() {
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                battleshipGrid[i][j] = 0
            }
        }
    }

    private fun populateGridLayout() {
        val gridLayout: GridLayout = findViewById(R.id.gridLayout)
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
                            markHitOrMiss(i, j)
                        }
                    }
                }
                gridLayout.addView(button)
            }
        }
    }

    private fun placeShip(row: Int, col: Int) {
        if (placedShips[currentShipSize]!! >= getMaxAllowedShips(currentShipSize)) {
            Toast.makeText(this, "All $currentShipSize-cell ships are already placed", Toast.LENGTH_SHORT).show()
            return
        }

        if (isVertical) {
            if (row + currentShipSize <= gridSize && canPlaceShip(row, col, isVertical)) {
                for (i in 0 until currentShipSize) {
                    battleshipGrid[row + i][col] = 1
                    updateButtonColor(row + i, col, Color.GRAY)
                }
                placedShips[currentShipSize] = placedShips[currentShipSize]!! + 1
            }
        } else {
            if (col + currentShipSize <= gridSize && canPlaceShip(row, col, isVertical)) {
                for (i in 0 until currentShipSize) {
                    battleshipGrid[row][col + i] = 1
                    updateButtonColor(row, col + i, Color.GRAY)
                }
                placedShips[currentShipSize] = placedShips[currentShipSize]!! + 1
            }
        }
    }

    private fun getMaxAllowedShips(size: Int): Int {
        return when (size) {
            5 -> 1
            4 -> 1
            3 -> 2
            2 -> 1
            else -> 0
        }
    }

    private fun canPlaceShip(row: Int, col: Int, vertical: Boolean): Boolean {
        return if (vertical) {
            (0 until currentShipSize).all { battleshipGrid[row + it][col] == 0 }
        } else {
            (0 until currentShipSize).all { battleshipGrid[row][col + it] == 0 }
        }
    }

    private fun markHitOrMiss(row: Int, col: Int) {
        if (battleshipGrid[row][col] == 1) {
            battleshipGrid[row][col] = 2
            updateButtonColor(row, col, Color.RED) // Hit
        } else {
            updateButtonColor(row, col, Color.WHITE) // Miss
        }
    }

    private fun updateButtonColor(row: Int, col: Int, color: Int) {
        val gridLayout: GridLayout = findViewById(R.id.gridLayout)
        val button = gridLayout.getChildAt(row * gridSize + col) as Button
        button.setBackgroundColor(color)
    }

    private fun resetGrid() {
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                battleshipGrid[i][j] = 0
                updateButtonColor(i, j, Color.BLUE)
            }
        }
        placedShips.keys.forEach { placedShips[it] = 0 }
    }
}
