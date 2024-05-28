package com.example.battleshipgame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class TestActivity : AppCompatActivity() {
    private lateinit var gameManager: GameManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        var loggedInUserID: String? = ""
        var currentGameID:String? = ""

        gameManager = GameManager()

        // Register UI elements
        val registerUsername = findViewById<EditText>(R.id.registerUsername)
        val registerEmail = findViewById<EditText>(R.id.registerEmail)
        val registerPassword = findViewById<EditText>(R.id.registerPassword)
        val registerButton = findViewById<Button>(R.id.registerButton)

        // Login UI elements
        val loginEmail = findViewById<EditText>(R.id.loginEmail)
        val loginPassword = findViewById<EditText>(R.id.loginPassword)
        val loginButton = findViewById<Button>(R.id.loginButton)

        // Login UI elements2
        val loginEmail2 = findViewById<EditText>(R.id.loginEmail2)
        val loginPassword2 = findViewById<EditText>(R.id.loginPassword2)
        val loginButton2 = findViewById<Button>(R.id.loginButton2)


        //other shit
        val idTextField = findViewById<TextView>(R.id.idTextField)
        val gameIdField = findViewById<TextView>(R.id.idGameField)
        val hitPointsLeftField = findViewById<TextView>(R.id.userHitPoints)
        val userTurnField =  findViewById<TextView>(R.id.userTurn)

        val shootfieldtextedit = findViewById<EditText>(R.id.shootCell)



        //testing functions
        fun updateHitPoints(currentGameID: String?, loggedInUserID:String? ){
            gameManager.getHitPoints(currentGameID, loggedInUserID) { success, message, hitPoints ->
                if (success) {
                    hitPointsLeftField.text = hitPoints.toString()
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }

        fun userLogin(loginHandler:Int){
            var email = ""
            var password = ""
            if(loginHandler ==1){
                 email = loginEmail.text.toString().trim()
                 password = loginPassword.text.toString().trim()
            }
            else{
                 email = loginEmail2.text.toString().trim()
                 password = loginPassword2.text.toString().trim()
            }

            if (email.isNotEmpty() && password.isNotEmpty()) {
                gameManager.loginUser(email, password) { success, message, userId ->
                    if (success) {
                        loggedInUserID = userId
                        Log.d("gameManagertestingregister", "UserID: $loggedInUserID")
                        Toast.makeText(
                            this,
                            "Login successful! User ID: $userId",
                            Toast.LENGTH_LONG
                        ).show()
                        idTextField.text = loggedInUserID

                        // Save userId for later use or pass it to the next activity
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }


        fun getUserTurn(){
            gameManager.getWhosTurnIsIt(currentGameID) { success, message, turn ->
                if (success) {
                    userTurnField.text  = turn.toString()
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }

        }

        // Register Button Click Listener
        registerButton.setOnClickListener {
            Log.d("gameManagertestingregister", "AAAAAAAAAAAAAAAAAAAAAAAAAAAA")
            val username = registerUsername.text.toString()
            val email = registerEmail.text.toString()
            val password = registerPassword.text.toString()
            Log.d("gameManagertestingregister", "$username")
            Log.d("gameManagertestingregister", "$email")
            Log.d("gameManagertestingregister", "$password")

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                gameManager.registerUser(email, password, username) { success, message ->
                    if (success) {
                        Log.d("gameManagertestingregister", "yes")

                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("gameManagertestingregister", "no")

                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Login Button Click Listener
        loginButton.setOnClickListener {
            userLogin(1)
        }

        // Login2 Button Click Listener
        loginButton2.setOnClickListener {
            userLogin(2)
        }

        //crate match
        findViewById<Button>(R.id.makematch).setOnClickListener {
            Log.d("gameManagerLoginUserID", "$loggedInUserID")
            if (loggedInUserID != "") {
                gameManager.createMatch(loggedInUserID) { success, message, gameId ->
                    if (success) {
                        currentGameID = gameId
                        Toast.makeText(this, "Match created! Game ID: $gameId", Toast.LENGTH_LONG)
                            .show()
                        // Navigate to game screen with gameId
                        gameIdField.text = currentGameID
                        updateHitPoints(currentGameID, loggedInUserID)
                        getUserTurn()



                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }

            }
        }

        //join match
        findViewById<Button>(R.id.joinmatch).setOnClickListener {
            gameManager.joinMatch(loggedInUserID) { success, message, gameId ->
                if (success) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    currentGameID = gameId
                    gameIdField.text = currentGameID
                    updateHitPoints(currentGameID, loggedInUserID)
                    getUserTurn()


                } else {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }



        //fake gameboard
        var fakeShipBoard: List<Int> = List(64) { 1 }

        findViewById<Button>(R.id.shipboard).setOnClickListener {
            gameManager.updateShipBoard(currentGameID, loggedInUserID, fakeShipBoard) { success, message ->
                if (success) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    // Proceed to the game stage or wait for the other player to be ready
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }


        //shoot
        findViewById<Button>(R.id.shoot).setOnClickListener {
            var cellIndex = shootfieldtextedit.text.toString().toInt()
            gameManager.shoot(currentGameID, loggedInUserID, cellIndex) { success, message ->
                if (success) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    getUserTurn()
                    // Update UI based on the result of the shot.
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }

        }



    }
}