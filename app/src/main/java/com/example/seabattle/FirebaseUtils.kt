package com.example.seabattle
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseUtils {
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance("https://seebattle-572c9-default-rtdb.europe-west1.firebasedatabase.app/")
    }

    val roomsRef: DatabaseReference by lazy { database.getReference("rooms") }

    // Metoda do aktualizacji aktualnego ruchu
    fun updateTurn(roomCode: String, nextPlayer: String) {
        roomsRef.child(roomCode).child("currentTurn").setValue(nextPlayer)
    }
}
