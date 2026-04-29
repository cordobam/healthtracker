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
import java.text.SimpleDateFormat
import java.util.*

class WeightActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvWeightSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight)

        db = DatabaseHelper(this)
        recyclerView = findViewById(R.id.recyclerViewWeight)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvWeightSummary = findViewById(R.id.tvWeightSummary)

        recyclerView.layoutManager = LinearLayoutManager(this)

        setupNavigation()

        findViewById<FloatingActionButton>(R.id.fabAddWeight).setOnClickListener {
            showAddDialog()
        }

        loadRecords()
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    private fun loadRecords() {
        val records = db.getWeightLastMonth()
        val change = db.getWeightChangeLastMonth()

        if (change != null) {
            val sign = if (change >= 0) "+" else ""
            tvWeightSummary.text = "Cambio este mes: ${sign}${String.format("%.1f", change)} kg"
        } else {
            tvWeightSummary.text = "Registrá al menos 2 pesadas para ver el cambio"
        }

        if (records.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            val adapter = WeightAdapter(records) { id -> confirmDelete(id) }
            recyclerView.adapter = adapter
        }
    }

    private fun showAddDialog() {
        // Warn if already weighed this week
        val records = db.getWeightLastMonth()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val recentEntry = records.firstOrNull { it.date >= weekAgo }

        val builder = AlertDialog.Builder(this)
        if (recentEntry != null && recentEntry.date != today) {
            builder.setTitle("⚠️ Ya pesaste esta semana")
                .setMessage("Última medición: ${recentEntry.weightKg} kg (${recentEntry.date})\n¿Querés agregar otra igualmente?")
                .setPositiveButton("Sí, agregar") { _, _ -> showWeightInputDialog() }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            showWeightInputDialog()
        }
    }

    private fun showWeightInputDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_weight, null)
        val etWeight = view.findViewById<TextInputEditText>(R.id.etWeight)

        AlertDialog.Builder(this)
            .setTitle("Registrar peso")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val weight = etWeight.text.toString().toDoubleOrNull()
                if (weight == null || weight < 20 || weight > 300) {
                    Toast.makeText(this, "Ingresá un peso válido (20–300 kg)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db.insertWeight(weight)
                Toast.makeText(this, "✓ Peso guardado", Toast.LENGTH_SHORT).show()
                loadRecords()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDelete(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar registro")
            .setMessage("¿Eliminar este registro de peso?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.deleteWeight(id)
                loadRecords()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_weight
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); false }
                R.id.nav_bp -> { startActivity(Intent(this, BloodPressureActivity::class.java)); false }
                R.id.nav_weight -> true
                R.id.nav_food -> { startActivity(Intent(this, FoodActivity::class.java)); false }
                R.id.nav_habits -> { startActivity(Intent(this, HabitsActivity::class.java)); false }
                else -> false
            }
        }
    }
}

class WeightAdapter(
    private val records: List<WeightRecord>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<WeightAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWeight: TextView = view.findViewById(R.id.tvWeight)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvChange: TextView = view.findViewById(R.id.tvChange)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weight, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val r = records[position]
        holder.tvWeight.text = String.format("%.1f kg", r.weightKg)
        holder.tvDate.text = r.date
        if (position > 0) {
            val diff = r.weightKg - records[position - 1].weightKg
            val sign = if (diff >= 0) "+" else ""
            holder.tvChange.text = "${sign}${String.format("%.1f", diff)} kg"
            holder.tvChange.visibility = View.VISIBLE
        } else {
            holder.tvChange.visibility = View.INVISIBLE
        }
        holder.btnDelete.setOnClickListener { onDelete(r.id) }
    }

    override fun getItemCount() = records.size
}
