package com.example.battleshipgame

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.math.log


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
                        "whosTurnIsIt" to 1,
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

    fun joinMatch(userId: String?, completion: (Boolean, String, String?) -> Unit) {
        val gamesRef = database.child("active_games")
        gamesRef.orderByChild("isFull").equalTo(false).limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Get the first available match
                        val matchSnapshot = snapshot.children.first()
                        val gameId = matchSnapshot.key ?: return completion(false, "Failed to join match.", null)

                        // Update the match to include user2Id and set isFull to true
                        val updates = mapOf(
                            "user2Id" to userId,
                            "isFull" to true
                        )

                        gamesRef.child(gameId).updateChildren(updates).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                completion(true, "Successfully joined match.", gameId)
                            } else {
                                completion(false, "Failed to join match: ${task.exception?.message}", null)
                            }
                        }
                    } else {
                        // No available matches found
                        completion(false, "No available matches to join.", null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    completion(false, "Error finding match: ${error.message}", null)
                }
            })
    }

    fun getPlayerRole(gameId: String?, userId: String?, completion: (Boolean, String?) -> Unit) {
        val gameRef = gameId?.let { database.child("active_games").child(it) }

        gameRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user1Id = snapshot.child("user1Id").value as? String
                    val user2Id = snapshot.child("user2Id").value as? String

                    if (userId == user1Id) {
                        completion(true, "user1")
                    } else if (userId == user2Id) {
                        completion(true, "user2")
                    } else {
                        completion(false, null)
                    }
                } else {
                    completion(false, null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completion(false, null)
            }
        })
    }

    fun updateShipBoard(gameId: String?, userId: String?, shipBoard: List<Int>, completion: (Boolean, String) -> Unit) {
        getPlayerRole(gameId, userId) { success, role ->
            if (success && role != null) {
                val userBoardPath = if (role == "user1") {
                    "user1Data/userShipBoard"
                } else {
                    "user2Data/userShipBoard"
                }

                val gameRef = gameId?.let { database.child("active_games").child(it).child(userBoardPath) }

                gameRef?.setValue(shipBoard)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        completion(true, "Ship board updated successfully.")
                    } else {
                        completion(false, "Failed to update ship board: ${task.exception?.message}")
                    }
                }
            } else {
                completion(false, "Failed to determine player role.")
            }
        }
    }

    private fun updateHighestScore(userId: String, currentScore: Long) {
        val userRef = database.child("registered_users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val highestScore = snapshot.child("highestScore").value as? Long ?: 0L

                    if (currentScore > highestScore) {
                        userRef.child("highestScore").setValue(currentScore)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if necessary
            }
        })
    }
    fun shoot(gameId: String?, userId: String?, cellIndex: Int, completion: (Boolean, String) -> Unit) {
        if (userId == null) {
            completion(false, "User ID is null")
            return
        }

        val gameRef = gameId?.let { database.child("active_games").child(it) }
        if (gameRef != null) {
            gameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val isFull = snapshot.child("isFull").value as? Boolean
                        if (isFull == false) {
                            completion(false, "Match is not full yet.")
                            return
                        }
                        val currentTurn = snapshot.child("whosTurnIsIt").value as? Long
                        val user1Id = snapshot.child("user1Id").value as? String
                        val user2Id = snapshot.child("user2Id").value as? String
                        val isUser1Turn = (currentTurn == 1L && userId == user1Id)
                        val isUser2Turn = (currentTurn == 2L && userId == user2Id)

                        if (isUser1Turn || isUser2Turn) {
                            val shooterBoardPath = if (isUser1Turn) "user1Data/userFogOfWarBoard" else "user2Data/userFogOfWarBoard"
                            val targetBoardPath = if (isUser1Turn) "user2Data/userShipBoard" else "user1Data/userShipBoard"
                            val targetHitPointsPath = if (isUser1Turn) "user2Data/hitPointsLeft" else "user1Data/hitPointsLeft"
                            val shooterScorePath = if (isUser1Turn) "user1Data/currentScore" else "user2Data/currentScore"
                            val shooterConsecutiveHitsPath = if (isUser1Turn) "user1Data/consecutiveHits" else "user2Data/consecutiveHits"
                            val shooterHighestScorePath = if (isUser1Turn) "user1HighestScore" else "user2HighestScore"

                            val nextTurn = if (isUser1Turn) 2L else 1L

                            val shooterFogOfWarBoard = snapshot.child(shooterBoardPath).value as? List<Int>
                            val targetShipBoard = snapshot.child(targetBoardPath).value as? List<Int>
                            val targetHitPoints = snapshot.child(targetHitPointsPath).value as Long
                            Log.d("GameManagerShoot", "HP: $targetHitPointsPath")

                            val shooterScore = snapshot.child(shooterScorePath).value as? Long ?: 0L
                            val shooterConsecutiveHits = snapshot.child(shooterConsecutiveHitsPath).value as? Long ?: 0L

                            if (shooterFogOfWarBoard != null && targetShipBoard != null) {
                                val updatedFogOfWarBoard = shooterFogOfWarBoard.toMutableList()
                                val targetCellStatus = targetShipBoard[cellIndex]
                                var updatedHitPoints = targetHitPoints

                                var newScore = shooterScore
                                var newConsecutiveHits = shooterConsecutiveHits

                                if (targetCellStatus == 1) {
                                    updatedFogOfWarBoard[cellIndex] = 2  // Hit

                                    if (updatedHitPoints!=null){
                                        updatedHitPoints -= 1
                                        Log.d("GameManagerShoot", "HP: $targetHitPointsPath")
                                    }

                                    newScore += 20 + (newConsecutiveHits * 5)
                                    newConsecutiveHits += 1


                                } else {
                                    updatedFogOfWarBoard[cellIndex] = 1  // Miss
                                    newConsecutiveHits = 0
                                    newScore -= 2
                                }

                                val updates = mapOf(
                                    "whosTurnIsIt" to nextTurn,
                                    "$shooterBoardPath" to updatedFogOfWarBoard,
                                    targetHitPointsPath to updatedHitPoints,
                                    shooterScorePath to newScore,
                                    shooterConsecutiveHitsPath to newConsecutiveHits
                                )

                                if (gameRef != null) {
                                    gameRef.updateChildren(updates).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            updateHighestScore(userId, newScore)
                                            completion(true, "Shot fired successfully.")
                                        } else {
                                            completion(false, "Failed to update shot: ${task.exception?.message}")
                                        }
                                    }
                                }
                            } else {
                                completion(false, "Failed to retrieve boards.")
                            }
                        } else {
                            completion(false, "It's not your turn.")
                        }
                    } else {
                        completion(false, "Game not found.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    completion(false, "Failed to read game data: ${error.message}")
                }
            })
        }
    }

    fun getHitPoints(gameId: String?, userId: String?, completion: (Boolean, String, Int?) -> Unit) {
        if (userId == null) {
            completion(false, "User ID is null", null)
            return
        }

        val gameRef = gameId?.let { database.child("active_games").child(it) }
        if (gameRef != null) {
            gameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user1Id = snapshot.child("user1Id").value as? String
                        val user2Id = snapshot.child("user2Id").value as? String

                        val hitPointsPath = when (userId) {
                            user1Id -> "user1Data/hitPointsLeft"
                            user2Id -> "user2Data/hitPointsLeft"
                            else -> {
                                completion(false, "User not found in this game.", null)
                                return
                            }
                        }

                        val hitPoints = snapshot.child(hitPointsPath).value as? Long
                        if (hitPoints != null) {
                            completion(true, "Hit points retrieved successfully.", hitPoints.toInt())
                        } else {
                            completion(false, "Failed to retrieve hit points.", null)
                        }
                    } else {
                        completion(false, "Game not found.", null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    completion(false, "Failed to read game data: ${error.message}", null)
                }
            })
        }
    }

    fun getWhosTurnIsIt(gameId: String?, completion: (Boolean, String, Int?) -> Unit) {
        val gameRef = gameId?.let { database.child("active_games").child(it).child("whosTurnIsIt") }

        if (gameRef != null) {
            gameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val turn = snapshot.value as? Long
                        if (turn != null) {
                            completion(true, "Turn retrieved successfully.", turn.toInt())
                        } else {
                            completion(false, "Failed to retrieve turn.", null)
                        }
                    } else {
                        completion(false, "Game not found.", null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    completion(false, "Failed to read game data: ${error.message}", null)
                }
            })
        }
    }

    fun getPlayerNumber(gameId: String?, userId: String?, completion: (Boolean, String?) -> Unit) {
        val gameRef = gameId?.let { database.child("active_games").child(it) }

        gameRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user1Id = snapshot.child("user1Id").value as? String
                    val user2Id = snapshot.child("user2Id").value as? String

                    if (userId == user1Id) {
                        completion(true, "1")
                    } else if (userId == user2Id) {
                        completion(true, "2")
                    } else {
                        completion(false, null)
                    }
                } else {
                    completion(false, null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completion(false, null)
            }
        })
    }

    fun getAllUsersSortedByHighestScore(completion: (Boolean, List<Map<String, Any>>?) -> Unit) {
        val usersRef = database.child("registered_users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userList = mutableListOf<Map<String, Any>>()

                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key
                        val userName = userSnapshot.child("username").value as? String
                        val highestScore = userSnapshot.child("highestScore").value as? Long

                        if (userId != null && userName != null && highestScore != null) {
                            val userMap = mapOf(
                                "userId" to userId,
                                "username" to userName,
                                "highestScore" to highestScore
                            )
                            userList.add(userMap)
                        }
                    }

                    // Sort the list by highestScore in descending order
                    userList.sortByDescending { it["highestScore"] as Long }

                    completion(true, userList)
                } else {
                    completion(false, null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completion(false, null)
            }
        })
    }
    fun getUserHighestScore(userId: String?, completion: (Boolean, Long?) -> Unit) {
        val userRef = userId?.let { database.child("registered_users").child(it).child("highestScore") }

        if (userRef != null) {
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val highestScore = snapshot.getValue(Long::class.java)
                    completion(true, highestScore)
                }

                override fun onCancelled(error: DatabaseError) {
                    completion(false, null)
                }
            })
        }
    }
    fun getUserCurrentScore(gameId: String?, userId: String?, completion: (Boolean, Long?) -> Unit) {
        val gameRef = gameId?.let { database.child("active_games").child(it) }

        if (gameRef != null) {
            gameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userPath = if (snapshot.child("user1Id").value == userId) "user1Data/currentScore" else "user2Data/currentScore"
                    val currentScore = snapshot.child(userPath).getValue(Long::class.java)
                    completion(true, currentScore)
                }

                override fun onCancelled(error: DatabaseError) {
                    completion(false, null)
                }
            })
        }
    }





}