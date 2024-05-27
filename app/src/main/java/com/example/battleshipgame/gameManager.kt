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


    fun loginUser(email: String, password: String, completion: (Boolean, String, String?) -> Unit) {
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
                                completion(true, "Login successful.", userId)
                            } else {
                                completion(false, "Incorrect password.", null)
                            }
                        }

                        override fun onCancelled(userError: DatabaseError) {
                            completion(false, "Error retrieving user: ${userError.message}", null)
                        }
                    })
                } else {
                    completion(false, "Email not registered.", null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completion(false, "Error checking email: ${error.message}", null)
            }
        })
    }


    fun createMatch(userId: String?, completion: (Boolean, String, String?) -> Unit) {
        if (userId == null) {
            completion(false, "User ID is null.", null)
            return
        }

        // Step 1: Check if the userId exists in registered_users
        val userRef = database.child("registered_users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Step 2: If user exists, proceed to create the match
                    val gamesRef = database.child("active_games")
                    val newGameRef = gamesRef.push()  // This generates a unique ID for the new game.
                    val gameId = newGameRef.key ?: return completion(false, "Failed to create game.", null)

                    val gameData = mapOf(
                        "isFull" to false,
                        "user1Id" to userId,
                        "user2Id" to "",
                        "whosTurnIsIt" to 0,
                        "user1Data" to mapOf(
                            "userShipBoard" to List(64) { 0 },  // Initializing an empty board.
                            "userFogOfWarBoard" to List(64) { 0 },  // Initializing an empty board.
                            "hitPointsLeft" to 14
                        ),
                        "user2Data" to mapOf(
                            "userShipBoard" to List(64) { 0 },
                            "userFogOfWarBoard" to List(64) { 0 },  // Initializing an empty board.
                            "hitPointsLeft" to 14
                        )
                    )

                    newGameRef.setValue(gameData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            completion(true, "Match created successfully.", gameId)
                        } else {
                            completion(false, "Error creating match: ${task.exception?.message}", null)
                        }
                    }
                } else {
                    // Step 3: If user does not exist, return an error
                    completion(false, "User ID does not exist.", null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completion(false, "Error checking user ID: ${error.message}", null)
            }
        })
    }
}