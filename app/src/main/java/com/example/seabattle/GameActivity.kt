package com.example.seabattle

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.util.Log

class GameActivity : ComponentActivity() {

    private var currentPlayer = "player1" // Nazwa obecnego gracza
    private var roomCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        roomCode = intent.getStringExtra("ROOM_CODE") ?: ""
        currentPlayer = intent.getStringExtra("PLAYER_NAME") ?: "player1"

        if (roomCode.isEmpty() || currentPlayer.isEmpty()) {
            Toast.makeText(this, "Błąd: Nieprawidłowe dane pomieszczenia lub gracza", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val playerField = findViewById<GridLayout>(R.id.playerFieldGrid)
        val opponentField = findViewById<GridLayout>(R.id.opponentFieldGrid)

        if (currentPlayer == "observer") {
            // Wywołaj renderObserverView, aby wyświetlić oba pola.
            renderObserverView(playerField, opponentField)
        } else {
            renderPlayerField(playerField)
            renderOpponentField(opponentField, enableClicks = true)
            listenForOpponentShots(playerField)
        }
    }
    private fun renderObserverView(playerField: GridLayout, opponentField: GridLayout) {
        // Wyświetl pole gracza 1 (Player 1)
        FirebaseUtils.roomsRef.child(roomCode).child("player1").child("ships")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val player1Ships = snapshot.children.mapNotNull { ship ->
                        val row = ship.child("first").getValue(Int::class.java)
                        val col = ship.child("second").getValue(Int::class.java)
                        if (row != null && col != null) Pair(row, col) else null
                    }
                    displayField(playerField, player1Ships, "player1")
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GameActivity, "Błąd ładowania pola gracza 1: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

        // Wyświetl pole gracza 2 (Player 2)
        FirebaseUtils.roomsRef.child(roomCode).child("player2").child("ships")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val player2Ships = snapshot.children.mapNotNull { ship ->
                        val row = ship.child("first").getValue(Int::class.java)
                        val col = ship.child("second").getValue(Int::class.java)
                        if (row != null && col != null) Pair(row, col) else null
                    }
                    displayField(opponentField, player2Ships, "player2")
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GameActivity, "Błąd ładowania pola gracza 2: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

        // Wysłuchaj strzałów gracza 1 do gracza 2.
        listenForShots(opponentField, "player1", "player2")

        // Wysłuchaj strzałów gracza 2 do gracza 1.
        listenForShots(playerField, "player2", "player1")
    }


    private fun listenForShots(field: GridLayout, targetPlayerKey: String, shipsKey: String) {
        FirebaseUtils.roomsRef.child(roomCode).child(targetPlayerKey).child("shots")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val shots = snapshot.children.mapNotNull { shot ->
                        val row = shot.child("row").getValue(Int::class.java)
                        val col = shot.child("col").getValue(Int::class.java)
                        if (row != null && col != null) Pair(row, col) else null
                    }

                    FirebaseUtils.roomsRef.child(roomCode).child(shipsKey).child("ships")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(shipSnapshot: DataSnapshot) {
                                val playerShips = shipSnapshot.children.mapNotNull { ship ->
                                    val row = ship.child("first").getValue(Int::class.java)
                                    val col = ship.child("second").getValue(Int::class.java)
                                    val hit = ship.child("hit").getValue(Boolean::class.java) ?: false
                                    if (row != null && col != null) Triple(row, col, hit) else null
                                }

                                // Aktualizacja pola na zasadzie strzał po strzale
                                for (shot in shots) {
                                    val cellIndex = shot.first * 10 + shot.second
                                    if (cellIndex in 0 until field.childCount) {
                                        val cell = field.getChildAt(cellIndex) as Button
                                        val targetShip = playerShips.find { it.first == shot.first && it.second == shot.second }
                                        if (targetShip != null && targetShip.third) {
                                            cell.setBackgroundColor(Color.MAGENTA) // Trafienie
                                        } else if (targetShip != null) {
                                            cell.setBackgroundColor(Color.BLUE) // Statek bez trafienia
                                        } else {
                                            cell.setBackgroundColor(Color.GRAY) // Miss
                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@GameActivity, "Błąd ładowania statków gracza: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GameActivity, "Błąd ładowania strzału: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }




    private fun displayField(grid: GridLayout, ships: List<Pair<Int, Int>>, playerKey: String) {
        for (i in 0 until 10) {
            for (j in 0 until 10) {
                val cell = Button(this)
                cell.layoutParams = GridLayout.LayoutParams().apply {
                    width = 70
                    height = 70
                    rowSpec = GridLayout.spec(i)
                    columnSpec = GridLayout.spec(j)
                }

                val position = Pair(i, j)
                if (ships.contains(position)) {
                    cell.setBackgroundColor(Color.BLUE) // Statki są wyświetlane na niebiesko
                } else {
                    cell.setBackgroundColor(Color.LTGRAY)
                }
                grid.addView(cell)
            }
        }

        // Wyświetlanie hitów i missów
        FirebaseUtils.roomsRef.child(roomCode).child(playerKey).child("shots")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val shots = snapshot.children.mapNotNull { shot ->
                        val row = shot.child("row").getValue(Int::class.java)
                        val col = shot.child("col").getValue(Int::class.java)
                        if (row != null && col != null) Pair(row, col) else null
                    }

                    for (shot in shots) {
                        val cellIndex = shot.first * 10 + shot.second
                        if (cellIndex in 0 until grid.childCount) {
                            val cell = grid.getChildAt(cellIndex) as Button
                            if (ships.contains(shot)) {
                                cell.setBackgroundColor(Color.MAGENTA) // Uderzenie - fiolet
                            } else {
                                cell.setBackgroundColor(Color.GRAY) // brak - wyszarzony
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GameActivity, "Błąd ładowania strzału: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun renderOpponentField(opponentField: GridLayout, enableClicks: Boolean) {
        for (i in 0 until 10) {
            for (j in 0 until 10) {
                val cell = Button(this)
                cell.layoutParams = GridLayout.LayoutParams().apply {
                    width = 70
                    height = 70
                    rowSpec = GridLayout.spec(i)
                    columnSpec = GridLayout.spec(j)
                }
                cell.setBackgroundColor(Color.LTGRAY)
                opponentField.addView(cell)

                if (enableClicks) {
                    cell.setOnClickListener {
                        currentPlayerTurn { isMyTurn ->
                            if (isMyTurn) {
                                handlePlayerShot(Pair(i, j), cell)
                            } else {
                                Toast.makeText(this@GameActivity, "To nie twój ruch!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderPlayerField(playerField: GridLayout) {
        FirebaseUtils.roomsRef.child(roomCode).child(currentPlayer).child("ships")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ships = snapshot.children.mapNotNull { ship ->
                        val row = ship.child("first").getValue(Int::class.java)
                        val col = ship.child("second").getValue(Int::class.java)
                        if (row != null && col != null) Pair(row, col) else null
                    }

                    // Wyświetlamy pole gracza
                    for (i in 0 until 10) {
                        for (j in 0 until 10) {
                            val cell = Button(this@GameActivity)
                            cell.layoutParams = GridLayout.LayoutParams().apply {
                                width = 70
                                height = 70
                                rowSpec = GridLayout.spec(i)
                                columnSpec = GridLayout.spec(j)
                            }
                            cell.setBackgroundColor(if (ships.contains(Pair(i, j))) Color.BLUE else Color.LTGRAY)
                            playerField.addView(cell)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GameActivity, "Błąd ładowania pola gracza: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun currentPlayerTurn(onResult: (Boolean) -> Unit) {
        val currentTurnRef = FirebaseUtils.roomsRef.child(roomCode).child("currentTurn")
        currentTurnRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentTurn = snapshot.getValue(String::class.java)
                onResult(currentTurn == currentPlayer)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GameActivity, "Błąd: ${error.message}", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        })
    }

    private fun handlePlayerShot(position: Pair<Int, Int>, cell: Button) {
        val opponentKey = if (currentPlayer == "player1") "player2" else "player1"

        FirebaseUtils.roomsRef.child(roomCode).child(opponentKey).child("ships")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val opponentShips = snapshot.children.mapNotNull { ship ->
                        val row = ship.child("first").getValue(Int::class.java)
                        val col = ship.child("second").getValue(Int::class.java)
                        val hit = ship.child("hit").getValue(Boolean::class.java) ?: false
                        if (row != null && col != null) Triple(row, col, hit) else null
                    }

                    val shotData = mapOf("row" to position.first, "col" to position.second)
                    FirebaseUtils.roomsRef.child(roomCode).child(currentPlayer).child("shots")
                        .push()
                        .setValue(shotData)

                    val targetShip = opponentShips.find { it.first == position.first && it.second == position.second }
                    if (targetShip != null && !targetShip.third) {
                        // Trafienie
                        cell.setBackgroundColor(Color.RED)
                        Toast.makeText(this@GameActivity, "Trafiony!", Toast.LENGTH_SHORT).show()

                        // Oznaczamy cel jako trafiony
                        val hitShipKey = snapshot.children.find { ship ->
                            val row = ship.child("first").getValue(Int::class.java)
                            val col = ship.child("second").getValue(Int::class.java)
                            row == position.first && col == position.second
                        }?.key

                        hitShipKey?.let {
                            FirebaseUtils.roomsRef.child(roomCode).child(opponentKey).child("ships").child(it)
                                .child("hit").setValue(true)
                        }

                        // Sprawdzamy zwycięstwo
                        if (opponentShips.count { !it.third } == 1) { // Ostatni statek
                            Toast.makeText(this@GameActivity, "Zwycięstwo!", Toast.LENGTH_LONG).show()
                            finish()
                            return
                        }
                    } else {
                        // Pudło
                        cell.setBackgroundColor(Color.GRAY)
                        Toast.makeText(this@GameActivity, "Pudło!", Toast.LENGTH_SHORT).show()
                    }

                    // Zmiana kolejki
                    FirebaseUtils.roomsRef.child(roomCode).child("currentTurn")
                        .setValue(if (currentPlayer == "player1") "player2" else "player1")
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GameActivity, "Błąd przetwarzania strzału: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun listenForOpponentShots(playerField: GridLayout) {
        val opponentKey = if (currentPlayer == "player1") "player2" else "player1"

        FirebaseUtils.roomsRef.child(roomCode).child(opponentKey).child("shots")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val shots = snapshot.children.mapNotNull { shot ->
                        val row = shot.child("row").getValue(Int::class.java)
                        val col = shot.child("col").getValue(Int::class.java)
                        if (row != null && col != null) Pair(row, col) else null
                    }

                    FirebaseUtils.roomsRef.child(roomCode).child(currentPlayer).child("ships")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(shipSnapshot: DataSnapshot) {
                                val playerShips = shipSnapshot.children.mapNotNull { ship ->
                                    val row = ship.child("first").getValue(Int::class.java)
                                    val col = ship.child("second").getValue(Int::class.java)
                                    val hit = ship.child("hit").getValue(Boolean::class.java) ?: false
                                    if (row != null && col != null) Triple(row, col, hit) else null
                                }

                                for (shot in shots) {
                                    val cellIndex = shot.first * 10 + shot.second
                                    if (cellIndex in 0 until playerField.childCount) {
                                        val cell = playerField.getChildAt(cellIndex) as Button

                                        val targetShip = playerShips.find { it.first == shot.first && it.second == shot.second }
                                        if (targetShip != null && targetShip.third) {
                                            // Trafienie w statek
                                            cell.setBackgroundColor(Color.MAGENTA)
                                        } else {
                                            // Pudło
                                            cell.setBackgroundColor(Color.GRAY)
                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@GameActivity, "Błąd ładowania statków: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GameActivity, "Błąd ładowania strzałów: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
