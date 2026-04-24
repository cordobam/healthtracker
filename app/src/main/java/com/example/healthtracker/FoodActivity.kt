package com.example.healthtracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class FoodActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotalCalories: TextView
    private lateinit var tvCaloriesBar: ProgressBar

    companion object {
        const val DAILY_GOAL = 2000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food)

        db = DatabaseHelper(this)
        recyclerView = findViewById(R.id.recyclerViewFood)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvTotalCalories = findViewById(R.id.tvTotalCalories)
        tvCaloriesBar = findViewById(R.id.progressCalories)

        recyclerView.layoutManager = LinearLayoutManager(this)
        setupNavigation()

        findViewById<FloatingActionButton>(R.id.fabAddFood).setOnClickListener {
            showAddDialog()
        }

        loadRecords()
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    private fun loadRecords() {
        val records = db.getFoodToday()
        val total = db.getTotalCaloriesToday()

        tvTotalCalories.text = "Hoy: $total / $DAILY_GOAL kcal"
        tvCaloriesBar.max = DAILY_GOAL
        tvCaloriesBar.progress = minOf(total, DAILY_GOAL)

        if (records.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            val adapter = FoodAdapter(records) { id -> confirmDelete(id) }
            recyclerView.adapter = adapter
        }
    }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_food, null)
        val etName = view.findViewById<TextInputEditText>(R.id.etFoodName)
        val etCalories = view.findViewById<TextInputEditText>(R.id.etCalories)
        val spinnerMeal = view.findViewById<Spinner>(R.id.spinnerMealType)

        val mealTypes = arrayOf("Desayuno", "Almuerzo", "Merienda", "Cena", "Snack")
        spinnerMeal.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mealTypes)

        AlertDialog.Builder(this)
            .setTitle("Agregar comida")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString().trim()
                val calories = etCalories.text.toString().toIntOrNull()
                val meal = spinnerMeal.selectedItem.toString()

                if (name.isEmpty() || calories == null || calories <= 0) {
                    Toast.makeText(this, "Completá el nombre y las calorías", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                db.insertFood(name, calories, meal)
                Toast.makeText(this, "✓ Comida guardada", Toast.LENGTH_SHORT).show()
                loadRecords()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDelete(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar comida")
            .setMessage("¿Eliminar este registro?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.deleteFood(id)
                loadRecords()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_food
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); false }
                R.id.nav_bp -> { startActivity(Intent(this, BloodPressureActivity::class.java)); false }
                R.id.nav_weight -> { startActivity(Intent(this, WeightActivity::class.java)); false }
                R.id.nav_food -> true
                else -> false
            }
        }
    }
}

class FoodAdapter(
    private val records: List<FoodRecord>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<FoodAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFoodName: TextView = view.findViewById(R.id.tvFoodName)
        val tvCalories: TextView = view.findViewById(R.id.tvCalories)
        val tvMealType: TextView = view.findViewById(R.id.tvMealType)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val r = records[position]
        holder.tvFoodName.text = r.name
        holder.tvCalories.text = "${r.calories} kcal"
        holder.tvMealType.text = r.mealType
        holder.btnDelete.setOnClickListener { onDelete(r.id) }
    }

    override fun getItemCount() = records.size
}
