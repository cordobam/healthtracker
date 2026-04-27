package com.example.healthtracker

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class HabitsActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var rvCalendar: RecyclerView
    private lateinit var rvHabits: RecyclerView
    private lateinit var tvMonthYear: TextView
    private lateinit var tvSelectedDay: TextView
    private lateinit var tvNoHabits: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton

    private var currentCalendar = Calendar.getInstance()
    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habits)

        db = DatabaseHelper(this)

        tvMonthYear = findViewById(R.id.tvMonthYear)
        tvSelectedDay = findViewById(R.id.tvSelectedDay)
        tvNoHabits = findViewById(R.id.tvNoHabits)
        rvCalendar = findViewById(R.id.rvCalendar)
        rvHabits = findViewById(R.id.rvHabits)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)

        rvCalendar.layoutManager = GridLayoutManager(this, 7)
        rvHabits.layoutManager = LinearLayoutManager(this)

        btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            refreshCalendar()
        }
        btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            refreshCalendar()
        }

        findViewById<FloatingActionButton>(R.id.fabManageHabits).setOnClickListener {
            showManageHabitsDialog()
        }

        setupNavigation()
        refreshCalendar()
        loadHabitsForDay(selectedDate)
    }

    override fun onResume() {
        super.onResume()
        refreshCalendar()
        loadHabitsForDay(selectedDate)
    }

    // ── Calendar ──────────────────────────────────────────────────────────────

    private fun refreshCalendar() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "AR"))
        tvMonthYear.text = sdf.format(currentCalendar.time).replaceFirstChar { it.uppercase() }

        val days = buildCalendarDays()
        val completionMap = db.getMonthCompletionMap(
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH) + 1
        )
        val totalHabits = db.getActiveHabits().size

        val adapter = CalendarAdapter(days, selectedDate, completionMap, totalHabits) { date ->
            selectedDate = date
            val dayNum = date.split("-")[2].toInt()
            val sdfDay = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "AR"))
            val cal = currentCalendar.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, dayNum)
            tvSelectedDay.text = sdfDay.format(cal.time).replaceFirstChar { it.uppercase() }
            loadHabitsForDay(date)
            refreshCalendar()
        }
        rvCalendar.adapter = adapter
    }

    private fun buildCalendarDays(): List<String?> {
        val days = mutableListOf<String?>()
        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        // Offset: Monday = 0
        var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2
        if (dayOfWeek < 0) dayOfWeek = 6
        repeat(dayOfWeek) { days.add(null) }

        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH) + 1
        val maxDay = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (d in 1..maxDay) {
            days.add("%04d-%02d-%02d".format(year, month, d))
        }
        return days
    }

    // ── Habit list for selected day ───────────────────────────────────────────

    private fun loadHabitsForDay(date: String) {
        val habits = db.getActiveHabits()
        if (habits.isEmpty()) {
            tvNoHabits.visibility = View.VISIBLE
            rvHabits.visibility = View.GONE
            return
        }
        tvNoHabits.visibility = View.GONE
        rvHabits.visibility = View.VISIBLE

        val logs = db.getHabitLogsForDate(date)
        val completedSet = logs.filter { it.completed }.map { it.habitId }.toSet()

        val adapter = HabitDayAdapter(habits, completedSet) { habitId, completed ->
            db.setHabitLog(habitId, date, completed)
            refreshCalendar()
        }
        rvHabits.adapter = adapter
    }

    // ── Manage habits dialog ──────────────────────────────────────────────────

    private fun showManageHabitsDialog() {
        val habits = db.getActiveHabits()
        val options = arrayOf("➕  Agregar hábito") + habits.map { "✏️  ${it.name}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Mis hábitos (${habits.size}/6)")
            .setItems(options) { _, which ->
                if (which == 0) showAddHabitDialog()
                else showEditHabitDialog(habits[which - 1])
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showAddHabitDialog() {
        if (db.getActiveHabits().size >= 6) {
            Toast.makeText(this, "Máximo 6 hábitos. Eliminá uno primero.", Toast.LENGTH_LONG).show()
            return
        }
        val view = layoutInflater.inflate(R.layout.dialog_add_habit, null)
        val etName = view.findViewById<TextInputEditText>(R.id.etHabitName)

        AlertDialog.Builder(this)
            .setTitle("Nuevo hábito")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Escribí un nombre", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db.insertHabit(name)
                Toast.makeText(this, "✓ Hábito agregado", Toast.LENGTH_SHORT).show()
                refreshCalendar()
                loadHabitsForDay(selectedDate)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditHabitDialog(habit: HabitRecord) {
        val options = arrayOf("✏️  Renombrar", "🗑️  Eliminar")
        AlertDialog.Builder(this)
            .setTitle(habit.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(habit)
                    1 -> confirmDeleteHabit(habit)
                }
            }
            .show()
    }

    private fun showRenameDialog(habit: HabitRecord) {
        val view = layoutInflater.inflate(R.layout.dialog_add_habit, null)
        val etName = view.findViewById<TextInputEditText>(R.id.etHabitName)
        etName.setText(habit.name)

        AlertDialog.Builder(this)
            .setTitle("Renombrar hábito")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    db.renameHabit(habit.id, name)
                    refreshCalendar()
                    loadHabitsForDay(selectedDate)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDeleteHabit(habit: HabitRecord) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar hábito")
            .setMessage("¿Eliminar \"${habit.name}\" y todos sus registros?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.deleteHabit(habit.id)
                refreshCalendar()
                loadHabitsForDay(selectedDate)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_habits
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); false }
                R.id.nav_bp -> { startActivity(Intent(this, BloodPressureActivity::class.java)); false }
                R.id.nav_weight -> { startActivity(Intent(this, WeightActivity::class.java)); false }
                R.id.nav_food -> { startActivity(Intent(this, FoodActivity::class.java)); false }
                R.id.nav_habits -> true
                else -> false
            }
        }
    }
}

// ── Calendar Adapter ──────────────────────────────────────────────────────────

class CalendarAdapter(
    private val days: List<String?>,
    private val selectedDate: String,
    private val completionMap: Map<String, Pair<Int, Int>>, // date -> (done, total)
    private val totalHabits: Int,
    private val onDayClick: (String) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    // Header labels
    private val headers = listOf("L", "M", "M", "J", "V", "S", "D")

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvCalendarDay)
        val tvCount: TextView = view.findViewById(R.id.tvCalendarCount)
        val container: FrameLayout = view.findViewById(R.id.calendarCell)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // First 7 positions are headers
        if (position < 7) {
            holder.tvDay.text = headers[position]
            holder.tvDay.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
            holder.tvDay.textSize = 11f
            holder.tvCount.visibility = View.GONE
            holder.container.setBackgroundResource(0)
            holder.container.isClickable = false
            return
        }

        val date = days[position - 7]
        if (date == null) {
            holder.tvDay.text = ""
            holder.tvCount.visibility = View.GONE
            holder.container.setBackgroundResource(0)
            holder.container.isClickable = false
            return
        }

        val dayNum = date.split("-")[2].trimStart('0')
        holder.tvDay.text = dayNum
        holder.tvDay.textSize = 13f
        holder.container.isClickable = true

        val completion = completionMap[date]
        val done = completion?.first ?: 0
        val total = if (totalHabits > 0) totalHabits else 1

        // Color circle based on completion
        val bgRes = when {
            date == selectedDate -> R.drawable.circle_selected
            totalHabits == 0 -> R.drawable.circle_empty
            done == 0 -> R.drawable.circle_empty
            done >= total -> R.drawable.circle_green
            done >= total / 2 -> R.drawable.circle_yellow
            else -> R.drawable.circle_red
        }
        holder.container.setBackgroundResource(bgRes)

        // Number label
        if (totalHabits > 0) {
            holder.tvCount.visibility = View.VISIBLE
            holder.tvCount.text = "$done/$total"
            val textColor = when {
                date == selectedDate -> android.R.color.white
                done == 0 -> R.color.text_secondary
                else -> android.R.color.white
            }
            holder.tvCount.setTextColor(holder.itemView.context.getColor(textColor))
            holder.tvDay.setTextColor(
                if (date == selectedDate || done > 0)
                    holder.itemView.context.getColor(android.R.color.white)
                else
                    holder.itemView.context.getColor(R.color.primary)
            )
        } else {
            holder.tvCount.visibility = View.GONE
            holder.tvDay.setTextColor(holder.itemView.context.getColor(R.color.primary))
        }

        holder.container.setOnClickListener { onDayClick(date) }
    }

    override fun getItemCount() = days.size + 7 // +7 for headers
}

// ── Habit Day Adapter ─────────────────────────────────────────────────────────

class HabitDayAdapter(
    private val habits: List<HabitRecord>,
    private val completedIds: Set<Int>,
    private val onToggle: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<HabitDayAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvHabitName)
        val checkbox: CheckBox = view.findViewById(R.id.checkboxHabit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val habit = habits[position]
        holder.tvName.text = habit.name
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = completedIds.contains(habit.id)
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            onToggle(habit.id, isChecked)
        }
        holder.itemView.setOnClickListener {
            holder.checkbox.toggle()
        }
    }

    override fun getItemCount() = habits.size
}