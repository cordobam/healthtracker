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
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.*

class FoodActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotalCalories: TextView
    private lateinit var tvCaloriesBar: ProgressBar

    private lateinit var tvSelectedDate: TextView
    private lateinit var btnDateToday: Button
    private lateinit var btnDateYesterday: Button
    private lateinit var btnDatePicker: ImageButton

    private val dbSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displaySdf = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "AR"))
    private var selectedDate: String = dbSdf.format(Date())

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

        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnDateToday = findViewById(R.id.btnDateToday)
        btnDateYesterday = findViewById(R.id.btnDateYesterday)
        btnDatePicker = findViewById(R.id.btnDatePicker)
        setupDateButtons()

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
        val records = db.getFoodByDate(selectedDate)
        val total = db.getTotalCaloriesByDate(selectedDate)

        tvTotalCalories.text = "$total / $DAILY_GOAL kcal"
        tvCaloriesBar.max = DAILY_GOAL
        tvCaloriesBar.progress = minOf(total, DAILY_GOAL)

        val dateForDisplay = dbSdf.parse(selectedDate)!!
        tvSelectedDate.text = displaySdf.format(dateForDisplay)
            .replaceFirstChar { it.uppercase() }

        if (records.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.adapter = FoodAdapter(records) { id -> confirmDelete(id) }
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

                db.insertFood(name, calories, meal, selectedDate)
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
                R.id.nav_habits -> { startActivity(Intent(this, HabitsActivity::class.java)); false }
                else -> false
            }
        }
    }

    private fun setupDateButtons() {
        btnDateToday.setOnClickListener {
            selectedDate = dbSdf.format(Date())
            loadRecords()
        }
        btnDateYesterday.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            selectedDate = dbSdf.format(cal.time)
            loadRecords()
        }
        btnDatePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            val parts = selectedDate.split("-")
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                loadRecords()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
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
