package com.example.seabattle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.seabattle.ui.theme.SeaBattleTheme
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var roomCodeEditText: EditText
    private lateinit var createRoomButton: Button
    private lateinit var joinRoomButton: Button
    private lateinit var startPlacementButton: Button
    private lateinit var observerButton: Button

    private var playerShips: List<Pair<Int, Int>>? = null // Lista statków

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createRoomButton = findViewById(R.id.createRoomButton)
        joinRoomButton = findViewById(R.id.joinRoomButton)
        startPlacementButton = findViewById(R.id.startPlacementButton)
        roomCodeEditText = findViewById(R.id.roomCodeEditText)
        observerButton = findViewById(R.id.observerButton)

        // Wyłączamy przyciski "Utwórz Pokój" i "Dołącz do Pokoju", dopóki statki nie zostaną rozmieszczone
        createRoomButton.isEnabled = false
        joinRoomButton.isEnabled = false


        observerButton.setOnClickListener {
            val roomCode = roomCodeEditText.text.toString()
            Log.d("Observer", "Łączenie się z pomieszczeniem jako obserwator: $roomCode")

            if (roomCode.isEmpty()) {
                Toast.makeText(this, "Wprowadź kod pokoju", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseUtils.roomsRef.child(roomCode).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d("Observer", "Pokój znaleziony. Łączenie jako obserwator")
                        val intent = Intent(this@MainActivity, GameActivity::class.java)
                        intent.putExtra("ROOM_CODE", roomCode)
                        intent.putExtra("PLAYER_NAME", "observer") // Określenie roli obserwatora
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "Nie znaleziono pokoju!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Observer", "Błąd połączenia z pokojem: ${error.message}")
                }
            })
        }
        // Logika tworzenia pokoju
        createRoomButton.setOnClickListener {
            val roomCode = (1000..9999).random().toString()
            Log.d("CreateRoom", "Tworzenie pokoju z kodem $roomCode")

            val roomData = mapOf(
                "currentTurn" to "player1", // Pierwszy ruch dla gracza 1
                "player1" to mapOf(
                    "ships" to (playerShips?.map { pair ->
                        mapOf("first" to pair.first, "second" to pair.second)
                    } ?: emptyList()),
                    "shots" to emptyList<Map<String, Int>>()  // Na razie brak strzałów
                ),
                "player2" to mapOf(
                    "ships" to emptyList<Map<String, Int>>(), // Gracz 2 jeszcze nie dołączył
                    "shots" to emptyList<Map<String, Int>>()
                )
            )

            FirebaseUtils.roomsRef.child(roomCode).setValue(roomData)
                .addOnSuccessListener {
                    Log.d("CreateRoom", "Pokój $roomCode został pomyślnie utworzony")
                    val intent = Intent(this, GameActivity::class.java)
                    intent.putExtra("ROOM_CODE", roomCode)
                    intent.putExtra("PLAYER_NAME", "player1")
                    startActivity(intent)
                }
                .addOnFailureListener { error ->
                    Log.e("CreateRoom", "Błąd podczas tworzenia pokoju: ${error.message}")
                }
        }

        // Logika dołączania do pokoju
        joinRoomButton.setOnClickListener {
            val roomCode = roomCodeEditText.text.toString()
            Log.d("JoinRoom", "Próba dołączenia do pokoju z kodem: $roomCode")

            if (roomCode.isEmpty()) {
                Toast.makeText(this, "Wprowadź kod pokoju", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseUtils.roomsRef.child(roomCode).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val roomData = snapshot.value as? Map<*, *>
                        if (roomData?.containsKey("player2") == true && (roomData["player2"] as Map<*, *>)["ships"] != null) {
                            Toast.makeText(this@MainActivity, "Pokój jest już pełny!", Toast.LENGTH_SHORT).show()
                        } else {
                            // Sprawdzamy, czy gracz rozmieszczał swoje statki
                            if (playerShips == null || playerShips!!.isEmpty()) {
                                Toast.makeText(this@MainActivity, "Najpierw rozmieść statki!", Toast.LENGTH_SHORT).show()
                                return
                            }

                            // Dodajemy gracza 2
                            val player2Data = mapOf(
                                "ships" to playerShips!!.map { pair ->
                                    mapOf("first" to pair.first, "second" to pair.second)
                                },
                                "shots" to emptyList<Map<String, Int>>()
                            )

                            FirebaseUtils.roomsRef.child(roomCode).child("player2")
                                .setValue(player2Data)
                                .addOnSuccessListener {
                                    Log.d("JoinRoom", "Dołączono do pokoju $roomCode jako gracz 2")
                                    val intent = Intent(this@MainActivity, GameActivity::class.java)
                                    intent.putExtra("ROOM_CODE", roomCode)
                                    intent.putExtra("PLAYER_NAME", "player2")
                                    startActivity(intent)
                                }
                                .addOnFailureListener { error ->
                                    Log.e("JoinRoom", "Błąd podczas dodawania gracza 2: ${error.message}")
                                }
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Pokój nie został znaleziony!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("JoinRoom", "Błąd podczas łączenia z pokojem: ${error.message}")
                }
            })
        }

        startPlacementButton.setOnClickListener {
            val intent = Intent(this, ShipPlacementActivity::class.java)
            startActivityForResult(intent, 1) // Oczekujemy wyniku
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Pobieramy listę statków z ShipPlacementActivity
            val shipsArray = data?.getSerializableExtra("PLAYER_SHIPS") as? Array<Pair<Int, Int>>
            if (shipsArray != null) {
                playerShips = shipsArray.toList() // Zapisujemy listę statków
                createRoomButton.isEnabled = true // Aktywujemy przycisk "Utwórz Pokój"
                joinRoomButton.isEnabled = true // Aktywujemy przycisk "Dołącz do Pokoju"
                Toast.makeText(this, "Statki zostały pomyślnie rozmieszczone!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Witaj $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SeaBattleTheme {
        Greeting("Android")
    }
}
