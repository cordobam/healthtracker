package com.example.healthtracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsContainer: LinearLayout
    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView

    private val cards = mutableListOf<DashboardCard>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)
        viewPager = findViewById(R.id.viewPagerCards)
        dotsContainer = findViewById(R.id.dotsContainer)
        tvGreeting = findViewById(R.id.tvGreeting)
        tvDate = findViewById(R.id.tvDate)

        setupNavigation()
        setGreeting()
    }

    override fun onResume() {
        super.onResume()
        buildCards()
        setupCarousel()
    }

    // ── Builds card data from DB ──────────────────────────────────────────────

    private fun buildCards() {
        cards.clear()

        // Card 1 — Blood Pressure
        val bpAvg = db.getAvgBloodPressureWeek()
        val bpCount = db.getBloodPressureThisWeek().size
        cards.add(if (bpAvg != null) {
            val sys = bpAvg.first.toInt()
            val dia = bpAvg.second.toInt()
            val (status, _) = classifyBloodPressure(sys, dia)
            DashboardCard(
                icon = "❤️",
                title = "Presión Arterial — promedio semanal",
                main = "$sys/$dia",
                mainSuffix = " mmHg",
                sub = status,
                detail = "$bpCount mediciones esta semana",
                onClick = { startActivity(Intent(this, BloodPressureActivity::class.java)) }
            )
        } else {
            DashboardCard(
                icon = "❤️",
                title = "Presión Arterial",
                main = "--/--",
                mainSuffix = "",
                sub = "Sin datos esta semana",
                detail = "Tocá para registrar",
                onClick = { startActivity(Intent(this, BloodPressureActivity::class.java)) }
            )
        })

        // Card 2 — Weight
        val latest = db.getLatestWeight()
        val change = db.getWeightChangeLastMonth()
        cards.add(if (latest != null) {
            val changeText = change?.let {
                val sign = if (it >= 0) "+" else ""
                "${sign}${String.format("%.1f", it)} kg este mes"
            } ?: "Primera medición"
            val status = when {
                change == null -> "Seguí midiendo"
                change < -2 -> "Bajando 📉"
                change > 2 -> "Subiendo 📈"
                else -> "Estable ✓"
            }
            DashboardCard(
                icon = "⚖️",
                title = "Peso — último registro",
                main = String.format("%.1f", latest.weightKg),
                mainSuffix = " kg",
                sub = status,
                detail = changeText,
                onClick = { startActivity(Intent(this, WeightActivity::class.java)) }
            )
        } else {
            DashboardCard(
                icon = "⚖️",
                title = "Peso",
                main = "--",
                mainSuffix = " kg",
                sub = "Sin datos",
                detail = "Tocá para registrar",
                onClick = { startActivity(Intent(this, WeightActivity::class.java)) }
            )
        })

        // Card 3 — Calories
        val todayTotal = db.getTotalCaloriesToday()
        val weekAvg = db.getAvgCaloriesWeek()
        val calStatus = when {
            todayTotal == 0 -> "Sin registros hoy"
            todayTotal < 1200 -> "Muy pocas calorías ⚠️"
            todayTotal in 1200..2500 -> "Dentro del rango ✓"
            else -> "Por encima del límite ⚠️"
        }
        cards.add(DashboardCard(
            icon = "🍽️",
            title = "Calorías — hoy",
            main = todayTotal.toString(),
            mainSuffix = " kcal",
            sub = calStatus,
            detail = "Promedio semanal: ${weekAvg.toInt()} kcal",
            onClick = { startActivity(Intent(this, FoodActivity::class.java)) }
        ))
    }

    // ── Carousel setup ────────────────────────────────────────────────────────

    private fun setupCarousel() {
        val adapter = DashboardCardAdapter(cards)
        viewPager.adapter = adapter

        // Mostrar preview de la card siguiente
        viewPager.offscreenPageLimit = 1
        val pageTransformer = ViewPager2.PageTransformer { page, position ->
            val absPos = Math.abs(position)
            page.scaleY = 1f - (absPos * 0.05f)
            page.alpha = 1f - (absPos * 0.3f)
        }
        viewPager.setPageTransformer(pageTransformer)

        setupDots(cards.size)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
            }
        })
    }

    private fun setupDots(count: Int) {
        dotsContainer.removeAllViews()
        repeat(count) { i ->
            val dot = TextView(this).apply {
                text = if (i == 0) "●" else "○"
                textSize = 12f
                setTextColor(getColor(R.color.primary))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = 8
                layoutParams = params
            }
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots(selected: Int) {
        for (i in 0 until dotsContainer.childCount) {
            val dot = dotsContainer.getChildAt(i) as TextView
            dot.text = if (i == selected) "●" else "○"
            dot.alpha = if (i == selected) 1f else 0.4f
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private fun classifyBloodPressure(sys: Int, dia: Int): Pair<String, Int> {
        return when {
            sys < 120 && dia < 80 -> Pair("Normal ✓", R.color.green_status)
            sys < 130 && dia < 80 -> Pair("Elevada ⚠️", R.color.yellow_status)
            sys < 140 || dia < 90 -> Pair("Alta Grado 1 ⚠️", R.color.orange_status)
            else -> Pair("Alta Grado 2 ⛔", R.color.red_status)
        }
    }

    private fun navigateTo(destination: Class<*>) {
        if (this::class.java == destination) return
        startActivity(Intent(this, destination).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> true
                R.id.nav_bp      -> { navigateTo(BloodPressureActivity::class.java); false }
                R.id.nav_weight  -> { navigateTo(WeightActivity::class.java); false }
                R.id.nav_food    -> { navigateTo(FoodActivity::class.java); false }
                R.id.nav_habits  -> { navigateTo(HabitsActivity::class.java); false }
                else -> false
            }
        }
    }
}

// ── Data class ────────────────────────────────────────────────────────────────

data class DashboardCard(
    val icon: String,
    val title: String,
    val main: String,
    val mainSuffix: String,
    val sub: String,
    val detail: String,
    val onClick: () -> Unit
)

// ── Adapter ───────────────────────────────────────────────────────────────────

class DashboardCardAdapter(
    private val cards: List<DashboardCard>
) : RecyclerView.Adapter<DashboardCardAdapter.CardViewHolder>() {

    class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView
        val tvIcon: TextView = view.findViewById(R.id.tvCardIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvCardTitle)
        val tvMain: TextView = view.findViewById(R.id.tvCardMain)
        val tvSub: TextView = view.findViewById(R.id.tvCardSub)
        val tvDetail: TextView = view.findViewById(R.id.tvCardDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val c = cards[position]
        holder.tvIcon.text = c.icon
        holder.tvTitle.text = c.title
        holder.tvMain.text = "${c.main}${c.mainSuffix}"
        holder.tvSub.text = c.sub
        holder.tvDetail.text = c.detail
        holder.card.setOnClickListener { c.onClick() }
    }

    override fun getItemCount() = cards.size
}
