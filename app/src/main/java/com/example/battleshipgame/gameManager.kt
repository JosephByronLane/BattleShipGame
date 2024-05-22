package com.example.battleshipgame

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


data class User(
    var userId: String = "",
    var email: String = "",
    var password: String = "",
    var username: String = "",
    var highestScore: Int = 0
)
data class Game(
    var gameId: String = "",
    var user1Id: String = "",
    var user2Id: String? = null,
    var gameStatus: String = "waiting_for_player",  // e.g., waiting_for_player, in_progress, completed
    var whosTurnIsIt: String? = null,
    var user1Data: GameUserData? = null,
    var user2Data: GameUserData? = null
)

data class GameUserData(
    var gameBoard: List<Int> = List(64) { 0 },  // Initialize a board of 8x8 with all zeros
    var hitPointsLeft: Int = 14
)
class GameManager(){
    val database = Firebase.database.reference
    fun registerUser(email: String, password: String, username: String, completion: (Boolean, String) -> Unit) {
        val emailRef = database.child("email_to_userId").child(email.replace(".", ","))

        emailRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    completion(false, "Email already registered.")
                } else {
                    // Email not registered, proceed with registration
                    val usersRef = database.child("registered_users")
                    val userId = usersRef   .push().key ?: ""
                    val newUser = User(userId, email, password, username)

                    usersRef.child(userId).setValue(newUser).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Update email to userId mapping
                            emailRef.setValue(userId)
                            completion(true, "User registered successfully.")
                        } else {
                            completion(false, "Failed to register user: ${task.exception?.message}")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completion(false, "Error checking email: ${error.message}")
            }
        })
    }


    fun loginUser(email: String, password: String, completion: (Boolean, String) -> Unit) {
        val emailRef = database.child("email_to_userId").child(email.replace(".", ","))

        emailRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userId = snapshot.value as String
                    // Fetch user data using the retrieved user ID
                    val userRef = database.child("registered_users").child(userId)
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null && user.password == password) {
                                completion(true, "Login successful.")
                            } else {
                                completion(false, "Incorrect password.")
                            }
                        }

                        override fun onCancelled(userError: DatabaseError) {
                            completion(false, "Error retrieving user: ${userError.message}")
                        }
                    })
                } else {
                    completion(false, "Email not registered.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completion(false, "Error checking email: ${error.message}")
            }
        })
    }


    fun createMatch(userId: String, completion: (Boolean, String) -> Unit) {
        val gamesRef = database.child("active_games")
        val newGameRef = gamesRef.push()  // Automatically generate a unique ID for the game
        val gameId = newGameRef.key ?: return completion(false, "Failed to generate game ID.")

        val newUserGameData = GameUserData()  // Initialize user game data
        val newGame = Game(
            gameId = gameId,
            user1Id = userId,
            gameStatus = "waiting_for_player",
            user1Data = newUserGameData
        )

        newGameRef.setValue(newGame).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                completion(true, "Match created successfully. Waiting for another player.")
            } else {
                completion(false, "Failed to create match: ${task.exception?.message}")
            }
        }
    }


}