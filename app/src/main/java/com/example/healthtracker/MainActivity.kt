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

    private lateinit var viewPagerHistorical: ViewPager2

    private lateinit var dotsContainerHistorical: LinearLayout

    private val cards = mutableListOf<DashboardCard>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)
        viewPager = findViewById(R.id.viewPagerCards)
        dotsContainer = findViewById(R.id.dotsContainer)
        tvGreeting = findViewById(R.id.tvGreeting)
        tvDate = findViewById(R.id.tvDate)
        viewPagerHistorical = findViewById(R.id.viewPagerHistorical)
        dotsContainerHistorical = findViewById(R.id.dotsContainerHistorical)

        setupNavigation()
        setGreeting()

        findViewById<TextView>(R.id.btnConfigMenu).setOnClickListener {
            NavigationBuilder.showConfigDialog(this) { rebuildUi() }
        }

        if (!ModulePrefs.isConfigured(this)) {
            NavigationBuilder.showConfigDialog(this) { rebuildUi() }
        }
    }

    override fun onResume() {
        super.onResume()
        setupNavigation()
        buildCards()
        setupCarousel()
        buildHistoricalCards()
        setupHistoricalCarousel()
    }

    private fun rebuildUi() {
        setupNavigation()
        buildCards()
        setupCarousel()
        buildHistoricalCards()
        setupHistoricalCarousel()
    }

    // ── Builds card data from DB ──────────────────────────────────────────────

    private fun buildCards() {
        cards.clear()

        // Card 1 — Blood Pressure
        if (ModulePrefs.isEnabled(this, Module.BP)) {
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
        }

        // Card 2 — Weight
        if (ModulePrefs.isEnabled(this, Module.WEIGHT)) {
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
        }

        // Card 3 — Calories
        if (ModulePrefs.isEnabled(this, Module.FOOD)) {
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

    private val historicalCards = mutableListOf<DashboardCard>()
    private fun buildHistoricalCards() {
        historicalCards.clear()

        // Card 1 — Blood Pressure histórico
        if (ModulePrefs.isEnabled(this, Module.BP)) {
            val bpAvg = db.getAvgBloodPressureAllTime()
            val bpCount = db.getBloodPressureCount()
            historicalCards.add(if (bpAvg != null) {
                val sys = bpAvg.first.toInt()
                val dia = bpAvg.second.toInt()
                val (status, _) = classifyBloodPressure(sys, dia)
                DashboardCard(
                    icon = "❤️",
                    title = "Presión Arterial — histórico",
                    main = "$sys/$dia",
                    mainSuffix = " mmHg",
                    sub = status,
                    detail = "$bpCount mediciones en total",
                    onClick = { startActivity(Intent(this, BloodPressureActivity::class.java)) }
                )
            } else {
                DashboardCard(
                    icon = "❤️",
                    title = "Presión Arterial",
                    main = "--/--",
                    mainSuffix = "",
                    sub = "Sin registros",
                    detail = "Tocá para registrar",
                    onClick = { startActivity(Intent(this, BloodPressureActivity::class.java)) }
                )
            })
        }

        // Card 2 — Weight histórico
        if (ModulePrefs.isEnabled(this, Module.WEIGHT)) {
            val stats = db.getWeightAllTimeStats()
            historicalCards.add(if (stats != null) {
                val status = when {
                    stats.count >= 10 -> "Suficientes datos ✓"
                    stats.count >= 3 -> "Más registros = mejor"
                    else -> "Pocos registros"
                }
                DashboardCard(
                    icon = "⚖️",
                    title = "Peso — histórico",
                    main = "${String.format("%.1f", stats.minKg)} — ${String.format("%.1f", stats.maxKg)}",
                    mainSuffix = " kg",
                    sub = "Promedio: ${String.format("%.1f", stats.avgKg)} kg",
                    detail = "${stats.count} registros en total",
                    onClick = { startActivity(Intent(this, WeightActivity::class.java)) }
                )
            } else {
                DashboardCard(
                    icon = "⚖️",
                    title = "Peso",
                    main = "--",
                    mainSuffix = " kg",
                    sub = "Sin registros",
                    detail = "Tocá para registrar",
                    onClick = { startActivity(Intent(this, WeightActivity::class.java)) }
                )
            })
        }

        // Card 3 — Calorías histórico
        if (ModulePrefs.isEnabled(this, Module.FOOD)) {
            val calAvg = db.getAvgCaloriesAllTime()
            val foodCount = db.getFoodCount()
            val calStatus = when {
                foodCount == 0 -> "Sin registros"
                calAvg < 1200 -> "Promedio bajo ⚠️"
                calAvg in 1200.0..2500.0 -> "Dentro del rango ✓"
                else -> "Por encima del límite ⚠️"
            }
            historicalCards.add(DashboardCard(
                icon = "🍽️",
                title = "Calorías — histórico",
                main = calAvg.toInt().toString(),
                mainSuffix = " kcal/día",
                sub = calStatus,
                detail = "$foodCount comidas registradas",
                onClick = { startActivity(Intent(this, FoodActivity::class.java)) }
            ))
        }
    }

    private fun setupHistoricalCarousel() {
        val adapter = DashboardCardAdapter(historicalCards)
        viewPagerHistorical.adapter = adapter

        viewPagerHistorical.offscreenPageLimit = 1
        val pageTransformer = ViewPager2.PageTransformer { page, position ->
            val absPos = Math.abs(position)
            page.scaleY = 1f - (absPos * 0.05f)
            page.alpha = 1f - (absPos * 0.3f)
        }
        viewPagerHistorical.setPageTransformer(pageTransformer)

        setupHistoricalDots(historicalCards.size)

        viewPagerHistorical.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateHistoricalDots(position)
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

    private fun setupHistoricalDots(count: Int) {
        dotsContainerHistorical.removeAllViews()
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
            dotsContainerHistorical.addView(dot)
        }
    }

    private fun updateHistoricalDots(selected: Int) {
        for (i in 0 until dotsContainerHistorical.childCount) {
            val dot = dotsContainerHistorical.getChildAt(i) as TextView
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

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        NavigationBuilder.setup(bottomNav, this, Module.HOME)
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
