package com.example.healthtracker

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    // Dashboard views
    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView

    // Blood pressure card
    private lateinit var tvBpAvgSystolic: TextView
    private lateinit var tvBpAvgDiastolic: TextView
    private lateinit var tvBpStatus: TextView
    private lateinit var tvBpReadingsCount: TextView

    // Weight card
    private lateinit var tvWeightCurrent: TextView
    private lateinit var tvWeightChange: TextView
    private lateinit var tvWeightStatus: TextView

    // Calories card
    private lateinit var tvCaloriesToday: TextView
    private lateinit var tvCaloriesAvg: TextView
    private lateinit var tvCaloriesStatus: TextView

    private lateinit var cardBp: MaterialCardView
    private lateinit var cardWeight: MaterialCardView
    private lateinit var cardFood: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)
        initViews()
        setupNavigation()
        setupCardClicks()
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun initViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvDate = findViewById(R.id.tvDate)

        tvBpAvgSystolic = findViewById(R.id.tvBpAvgSystolic)
        tvBpAvgDiastolic = findViewById(R.id.tvBpAvgDiastolic)
        tvBpStatus = findViewById(R.id.tvBpStatus)
        tvBpReadingsCount = findViewById(R.id.tvBpReadingsCount)

        tvWeightCurrent = findViewById(R.id.tvWeightCurrent)
        tvWeightChange = findViewById(R.id.tvWeightChange)
        tvWeightStatus = findViewById(R.id.tvWeightStatus)

        tvCaloriesToday = findViewById(R.id.tvCaloriesToday)
        tvCaloriesAvg = findViewById(R.id.tvCaloriesAvg)
        tvCaloriesStatus = findViewById(R.id.tvCaloriesStatus)

        cardBp = findViewById(R.id.cardBp)
        cardWeight = findViewById(R.id.cardWeight)
        cardFood = findViewById(R.id.cardFood)
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_bp -> {
                    startActivity(Intent(this, BloodPressureActivity::class.java))
                    false
                }
                R.id.nav_weight -> {
                    startActivity(Intent(this, WeightActivity::class.java))
                    false
                }
                R.id.nav_food -> {
                    startActivity(Intent(this, FoodActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun setupCardClicks() {
        cardBp.setOnClickListener { startActivity(Intent(this, BloodPressureActivity::class.java)) }
        cardWeight.setOnClickListener { startActivity(Intent(this, WeightActivity::class.java)) }
        cardFood.setOnClickListener { startActivity(Intent(this, FoodActivity::class.java)) }
    }

    private fun loadDashboard() {
        setGreeting()
        loadBloodPressureCard()
        loadWeightCard()
        loadCaloriesCard()
    }

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when {
            hour < 12 -> "¡Buenos días! 🌅"
            hour < 18 -> "¡Buenas tardes! ☀️"
            else -> "¡Buenas noches! 🌙"
        }
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "AR"))
        tvDate.text = sdf.format(Date()).replaceFirstChar { it.uppercase() }
    }

    private fun loadBloodPressureCard() {
        val avg = db.getAvgBloodPressureWeek()
        val weekRecords = db.getBloodPressureThisWeek()

        if (avg != null) {
            val sys = avg.first.toInt()
            val dia = avg.second.toInt()
            tvBpAvgSystolic.text = sys.toString()
            tvBpAvgDiastolic.text = dia.toString()
            tvBpReadingsCount.text = "${weekRecords.size} mediciones esta semana"

            val (status, color) = classifyBloodPressure(sys, dia)
            tvBpStatus.text = status
            tvBpStatus.setTextColor(getColor(color))
        } else {
            tvBpAvgSystolic.text = "--"
            tvBpAvgDiastolic.text = "--"
            tvBpStatus.text = "Sin datos esta semana"
            tvBpReadingsCount.text = "Empezá a registrar"
        }
    }

    private fun loadWeightCard() {
        val latest = db.getLatestWeight()
        val change = db.getWeightChangeLastMonth()

        if (latest != null) {
            tvWeightCurrent.text = String.format("%.1f kg", latest.weightKg)
            if (change != null) {
                val sign = if (change >= 0) "+" else ""
                tvWeightChange.text = "${sign}${String.format("%.1f", change)} kg este mes"
                tvWeightStatus.text = when {
                    change < -2 -> "Bajando bien 📉"
                    change > 2 -> "Subiendo 📈"
                    else -> "Estable ✓"
                }
            } else {
                tvWeightChange.text = "Primera medición"
                tvWeightStatus.text = "Seguí midiendo"
            }
        } else {
            tvWeightCurrent.text = "-- kg"
            tvWeightChange.text = "Sin datos"
            tvWeightStatus.text = "Empezá a registrar"
        }
    }

    private fun loadCaloriesCard() {
        val todayTotal = db.getTotalCaloriesToday()
        val weekAvg = db.getAvgCaloriesWeek()

        tvCaloriesToday.text = "$todayTotal kcal"
        tvCaloriesAvg.text = "Promedio semanal: ${weekAvg.toInt()} kcal"

        tvCaloriesStatus.text = when {
            todayTotal == 0 -> "Sin registros hoy"
            todayTotal < 1200 -> "Muy pocas calorías ⚠️"
            todayTotal in 1200..2500 -> "Dentro del rango ✓"
            else -> "Por encima del límite ⚠️"
        }
    }

    private fun classifyBloodPressure(sys: Int, dia: Int): Pair<String, Int> {
        return when {
            sys < 120 && dia < 80 -> Pair("Normal ✓", R.color.green_status)
            sys < 130 && dia < 80 -> Pair("Elevada ⚠️", R.color.yellow_status)
            sys < 140 || dia < 90 -> Pair("Alta Grado 1 ⚠️", R.color.orange_status)
            else -> Pair("Alta Grado 2 ⛔", R.color.red_status)
        }
    }
}
