package com.example.battleshipgame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlin.math.log

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
        val userNumberField = findViewById<TextView>(R.id.PlayerNumber)
        val userHighestScoreField = findViewById<TextView>(R.id.userhighestscore)
        val usercurrentScore = findViewById<TextView>(R.id.playercurrentscore)


        //listeners
        val userEventListenerUpdatesField = findViewById<TextView>(R.id.userListenerUpdate)
        var userRef = gameManager.usersRef
        var amntUserUpdates = 0;

        val fogOfWarEventListenerUpdatesField = findViewById<TextView>(R.id.fogOfWarUpdates)
        var fogOfWarRef: DatabaseReference? = null
        var amntFogOfWarUpdates = 0;

        val shipBoardEventListenerUpdatesField = findViewById<TextView>(R.id.shipBoardUpdates)
        var shipBoardRef: DatabaseReference? = null
        var amntShipBoardUpdates = 0;

        val gameEventListenerUpdatesField = findViewById<TextView>(R.id.gameListener)
        var gameRef: DatabaseReference? = null
        var amntGamerUpdates = 0;

        val hitPointsEventListenerUpdatesField = findViewById<TextView>(R.id.hitPointListener)
        var hitPpointsRef: DatabaseReference? = null
        var hitPointUpdates = 0;



        val shootfieldtextedit = findViewById<EditText>(R.id.shootCell)


        //listener functions


        //update Listeners

        userRef.addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                amntUserUpdates +=1;
                userEventListenerUpdatesField.text = amntUserUpdates.toString()

            }
            override fun onCancelled(error: DatabaseError) {
            }
        })

        fun setGameRefListener(){
            gameRef = gameManager.gameRef(currentGameID)
            gameRef?.addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    amntGamerUpdates +=1;
                    Log.d("TestActivityJoinMatch", "$amntGamerUpdates")
                    gameEventListenerUpdatesField.text = amntGamerUpdates.toString()

                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
        fun setFogOfWardRefListener(){
            fogOfWarRef = gameManager.userFogOfWarBoardRef(loggedInUserID, currentGameID)
            fogOfWarRef?.addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    amntFogOfWarUpdates +=1;
                    Log.d("TestActivityJoinMatch", "$amntFogOfWarUpdates")
                    fogOfWarEventListenerUpdatesField.text = amntFogOfWarUpdates.toString()

                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
        fun setShipBoardRefListeners(){
            shipBoardRef = gameManager.userShipBoardRef(currentGameID,loggedInUserID)
            shipBoardRef?.addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    amntShipBoardUpdates +=1;
                    Log.d("TestActivityJoinMatch", "$amntShipBoardUpdates")
                    shipBoardEventListenerUpdatesField.text = amntShipBoardUpdates.toString()

                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
        fun setHitPpointRefListener(){
            hitPpointsRef = gameManager.userHitPointsRef(currentGameID,loggedInUserID)
            hitPpointsRef?.addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    hitPointUpdates +=1;
                    Log.d("TestActivityJoinMatch", "$hitPointUpdates")
                    hitPointsEventListenerUpdatesField.text = hitPointUpdates.toString()
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }





        //testing functions

        fun updateScoreUI(){
            gameManager.getUserCurrentScore(currentGameID, loggedInUserID) {success, score ->
                if (success) {
                    usercurrentScore.text  = score.toString()
                } else {
                    Toast.makeText(this, "ERROR UPDATING USER CURRENT SCOROE", Toast.LENGTH_LONG).show()
                }
            }
            gameManager.getUserHighestScore(loggedInUserID) { success, score ->
                if (success) {
                    userHighestScoreField.text  = score.toString()
                } else {
                    Toast.makeText(this, "ERROR UPDATING USER higHEST SCORE", Toast.LENGTH_LONG).show()
                }
            }
        }


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
                        updateScoreUI()
                        // Save userId for later use or pass it to the next activity
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }


        fun getUserNumber(){
            gameManager.getPlayerNumber(currentGameID,loggedInUserID) { success, turn ->
                if (success) {
                    userNumberField.text  = turn.toString()
                } else {
                    Toast.makeText(this, "error bruh", Toast.LENGTH_LONG).show()
                }
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
                        getUserNumber()
                        setGameRefListener()
                        setHitPpointRefListener()
                        setFogOfWardRefListener()
                        setGameRefListener()
                        setShipBoardRefListeners()

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
                    getUserNumber()
                    setGameRefListener()
                    setHitPpointRefListener()
                    setFogOfWardRefListener()
                    setGameRefListener()
                    setShipBoardRefListeners()

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
                    updateScoreUI()

                } else {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }

        }
        gameManager.getAllUsersSortedByHighestScore { success, userList ->
            if (success) {
                userList?.forEach { user ->
                    Log.d("UserScores", "User ID: ${user["userId"]}, Username: ${user["username"]}, Highest Score: ${user["highestScore"]}")
                }
            } else {
                Log.d("UserScores","Failed to retrieve and sort users.")
            }
        }




    }


}