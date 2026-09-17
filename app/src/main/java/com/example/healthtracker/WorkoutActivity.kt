package com.example.healthtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class WorkoutActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        db = DatabaseHelper(this)
        recyclerView = findViewById(R.id.recyclerViewWorkout)

        recyclerView.layoutManager = LinearLayoutManager(this)

        setupNavigation()
        loadPlans()
    }

    override fun onResume() {
        super.onResume()
        setupNavigation()
        loadPlans()
    }

    private fun loadPlans() {
        val plans = db.getWorkoutPlans()
        recyclerView.adapter = WorkoutPlansAdapter(plans) { plan ->
            showExercisesDialog(plan)
        }
    }

    private fun showExercisesDialog(plan: WorkoutPlan) {
        val exercises = db.getWorkoutExercises(plan.id)
        val lines = exercises.map { ex ->
            "${ex.name} — ${ex.sets}x${ex.reps} · ${ex.restSeconds}s descanso"
        }

        AlertDialog.Builder(this)
            .setTitle("🏋️ ${plan.name} (${plan.level})")
            .setItems(lines.toTypedArray(), null)
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        NavigationBuilder.setup(bottomNav, this, Module.WORKOUT)
    }
}

class WorkoutPlansAdapter(
    private val plans: List<WorkoutPlan>,
    private val onClick: (WorkoutPlan) -> Unit
) : RecyclerView.Adapter<WorkoutPlansAdapter.PlanViewHolder>() {

    class PlanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPlanName)
        val tvLevel: TextView = view.findViewById(R.id.tvPlanLevel)
        val tvDescription: TextView = view.findViewById(R.id.tvPlanDescription)
        val card: MaterialCardView = view as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_plan, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = plans[position]
        holder.tvName.text = plan.name
        holder.tvLevel.text = plan.level
        holder.tvDescription.text = plan.description
        holder.card.setOnClickListener { onClick(plan) }
    }

    override fun getItemCount() = plans.size
}