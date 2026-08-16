package com.example.healthtracker

import android.app.Activity

enum class Module(
    val itemId: Int,
    val title: String,
    val iconRes: Int,
    val activity: Class<out Activity>
) {
    HOME(1, "Inicio", android.R.drawable.ic_menu_today, MainActivity::class.java),
    BP(2, "Presión", android.R.drawable.ic_menu_compass, BloodPressureActivity::class.java),
    WEIGHT(3, "Peso", android.R.drawable.ic_menu_sort_by_size, WeightActivity::class.java),
    FOOD(4, "Comidas", android.R.drawable.ic_menu_agenda, FoodActivity::class.java),
    HABITS(5, "Rutina", android.R.drawable.ic_menu_my_calendar, HabitsActivity::class.java);

    companion object {
        const val NAV_MORE = 999
        const val MAX_MODULES_NO_OVERFLOW = 4
        const val MAX_VISIBLE_WITH_OVERFLOW = 3
        const val MORE_TITLE = "Más"

        fun fromItemId(id: Int): Module? = entries.firstOrNull { it.itemId == id }
    }
}