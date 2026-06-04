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

class BloodPressureActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private var adapter: BpAdapter? = null
    private lateinit var tvEmpty: TextView
    private lateinit var tvTodayCount: TextView

    private lateinit var tvSelectedDate: TextView

    private val dbSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displaySdf = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "AR"))
    private var selectedDate: String = dbSdf.format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blood_pressure)

        db = DatabaseHelper(this)
        recyclerView = findViewById(R.id.recyclerViewBp)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvTodayCount = findViewById(R.id.tvTodayCount)

        recyclerView.layoutManager = LinearLayoutManager(this)

        setupNavigation()

        tvSelectedDate = findViewById(R.id.tvSelectedDate)

        val btnDateToday = findViewById<Button>(R.id.btnDateToday)
        val btnDateYesterday = findViewById<Button>(R.id.btnDateYesterday)
        val btnDatePicker = findViewById<ImageButton>(R.id.btnDatePicker)

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

        findViewById<FloatingActionButton>(R.id.fabAddBp).setOnClickListener {
            showAddDialog()
        }

        loadRecords()
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    private fun loadRecords() {
        val records = db.getBloodPressureByDate(selectedDate)

        tvTodayCount.text = "${records.size}/3 mediciones"

        val dateForDisplay = dbSdf.parse(selectedDate)!!
        tvSelectedDate.text = displaySdf.format(dateForDisplay)
            .replaceFirstChar { it.uppercase() }

        if (records.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter = BpAdapter(records) { id -> confirmDelete(id) }
            recyclerView.adapter = adapter
        }
    }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_bp, null)
        val etSystolic = view.findViewById<TextInputEditText>(R.id.etSystolic)
        val etDiastolic = view.findViewById<TextInputEditText>(R.id.etDiastolic)
        val etPulse = view.findViewById<TextInputEditText>(R.id.etPulse)

        AlertDialog.Builder(this)
            .setTitle("Registrar presión arterial")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val sys = etSystolic.text.toString().toIntOrNull()
                val dia = etDiastolic.text.toString().toIntOrNull()
                val pulse = etPulse.text.toString().toIntOrNull()

                if (sys == null || dia == null) {
                    Toast.makeText(this, "Ingresá sistólica y diastólica", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val dayRecords = db.getBloodPressureByDate(selectedDate)
                if (dayRecords.size >= 3) {
                    Toast.makeText(this, "Ya registraste 3 mediciones este día", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                db.insertBloodPressure(sys, dia, pulse, selectedDate, now)
                Toast.makeText(this, "✓ Medición guardada", Toast.LENGTH_SHORT).show()
                loadRecords()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDelete(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar medición")
            .setMessage("¿Eliminar esta medición?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.deleteBloodPressure(id)
                loadRecords()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_bp
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); false }
                R.id.nav_bp -> true
                R.id.nav_weight -> { startActivity(Intent(this, WeightActivity::class.java)); false }
                R.id.nav_food -> { startActivity(Intent(this, FoodActivity::class.java)); false }
                R.id.nav_habits -> { startActivity(Intent(this, HabitsActivity::class.java)); false }
                else -> false
            }
        }
    }
}

class BpAdapter(
    private val records: List<BloodPressureRecord>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<BpAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvReading: TextView = view.findViewById(R.id.tvBpReading)
        val tvDateTime: TextView = view.findViewById(R.id.tvBpDateTime)
        val tvStatus: TextView = view.findViewById(R.id.tvBpStatus)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blood_pressure, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val r = records[position]
        holder.tvReading.text = "${r.systolic}/${r.diastolic}"
        val pulseText = if (r.pulse > 0) " · ${r.pulse} lpm" else ""
        holder.tvDateTime.text = "${r.date}  ${r.time}$pulseText"
        holder.tvStatus.text = classifyBp(r.systolic, r.diastolic)
        holder.btnDelete.setOnClickListener { onDelete(r.id) }
    }

    override fun getItemCount() = records.size

    private fun classifyBp(sys: Int, dia: Int): String = when {
        sys < 120 && dia < 80 -> "Normal"
        sys < 130 && dia < 80 -> "Elevada"
        sys < 140 || dia < 90 -> "Alta Grado 1"
        else -> "Alta Grado 2"
    }
}
