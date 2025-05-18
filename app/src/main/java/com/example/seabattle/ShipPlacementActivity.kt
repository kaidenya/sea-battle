package com.example.seabattle
import android.content.Intent // Do przejścia na inny ekran
import android.graphics.Color // Do pracy z kolorami
import android.os.Bundle // Do zarządzania cyklem życia Activity
import android.widget.Button // Do przycisków na polu gry
import android.widget.GridLayout // Do siatki 10x10
import androidx.activity.ComponentActivity // Główna klasa Activity
import android.widget.Toast // Do wyświetlania krótkich powiadomień (Toast)

class ShipPlacementActivity : ComponentActivity() {

    private val selectedCells = mutableSetOf<Pair<Int, Int>>() // Przechowywanie zajętych komórek

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ship_placement)

        val grid = findViewById<GridLayout>(R.id.shipPlacementGrid)
        val confirmButton = findViewById<Button>(R.id.confirmButton)

        // Tworzymy siatkę 10x10
        for (i in 0 until 10) {
            for (j in 0 until 10) {
                val cell = Button(this)
                cell.layoutParams = GridLayout.LayoutParams().apply {
                    width = 100
                    height = 100
                    rowSpec = GridLayout.spec(i)
                    columnSpec = GridLayout.spec(j)
                }
                cell.setBackgroundResource(R.drawable.cell_border) // Zastosowanie obramowania
                grid.addView(cell)

                // Logika zaznaczania komórek
                cell.setOnClickListener {
                    val position = Pair(i, j)
                    if (selectedCells.contains(position)) {
                        selectedCells.remove(position)
                        cell.setBackgroundResource(R.drawable.cell_border) // Przywrócenie tła
                    } else {
                        selectedCells.add(position)
                        cell.setBackgroundColor(Color.BLUE) // Oznaczenie komórki jako zajętej
                    }
                }
            }
        }

        // Logika przycisku potwierdzenia
        confirmButton.setOnClickListener {
            if (validateShips()) {
                // Przekazanie listy statków z powrotem do MainActivity
                val resultIntent = Intent()
                resultIntent.putExtra("PLAYER_SHIPS", selectedCells.toTypedArray()) // Przekazanie tablicy wybranych komórek
                setResult(RESULT_OK, resultIntent) // Ustawienie wyniku Activity
                finish() // Zakończenie bieżącej Activity
            } else {
                // Wyświetlenie komunikatu, jeśli statki są źle rozmieszczone
                Toast.makeText(this, "Sprawdź rozmieszczenie statków!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Sprawdzenie zasad rozmieszczenia statków
    private fun validateShips(): Boolean {
        // Przykład: 5 komórek i nie powinny się krzyżować
        if (selectedCells.size != 20) return false

        // TODO: W pre aplhie bedzie dodane sprawdzenie, czy komórki statków są połączone (poziomo lub pionowo)

        return true
    }
}
