package com.example.healthtracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "health_tracker.db"
        const val DATABASE_VERSION = 3

        // Blood Pressure Table
        const val TABLE_BLOOD_PRESSURE = "blood_pressure"
        const val BP_ID = "id"
        const val BP_SYSTOLIC = "systolic"
        const val BP_DIASTOLIC = "diastolic"
        const val BP_PULSE = "pulse"
        const val BP_DATE = "date"
        const val BP_TIME = "time"

        // Weight Table
        const val TABLE_WEIGHT = "weight"
        const val W_ID = "id"
        const val W_VALUE = "weight_kg"
        const val W_DATE = "date"

        // Food Table
        const val TABLE_FOOD = "food"
        const val F_ID = "id"
        const val F_NAME = "food_name"
        const val F_CALORIES = "calories"
        const val F_DATE = "date"
        const val F_MEAL_TYPE = "meal_type" // breakfast, lunch, dinner, snack

        //habits
        const val TABLE_HABITS = "habits"
        const val H_ID = "id"
        const val H_NAME = "name"
        const val H_ACTIVE = "is_active"

        const val TABLE_HABIT_LOGS = "habit_logs"
        const val HL_ID = "id"
        const val HL_HABIT_ID = "habit_id"
        const val HL_DATE = "date"
        const val HL_COMPLETED = "completed"

        // Workout Tables
        const val TABLE_WORKOUTS = "entrenamientos"
        const val WK_ID = "id"
        const val WK_NAME = "nombre"
        const val WK_LEVEL = "nivel"
        const val WK_DESC = "descripcion"
        const val WK_ORDER = "orden"

        const val TABLE_WORKOUT_EXERCISES = "entrenamiento_ejercicios"
        const val WE_ID = "id"
        const val WE_WORKOUT_ID = "entrenamiento_id"
        const val WE_NAME = "nombre"
        const val WE_SETS = "series"
        const val WE_REPS = "reps"
        const val WE_REST = "descanso"
        const val WE_ORDER = "orden"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_BLOOD_PRESSURE (
                $BP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $BP_SYSTOLIC INTEGER NOT NULL,
                $BP_DIASTOLIC INTEGER NOT NULL,
                $BP_PULSE INTEGER,
                $BP_DATE TEXT NOT NULL,
                $BP_TIME TEXT NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_WEIGHT (
                $W_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $W_VALUE REAL NOT NULL,
                $W_DATE TEXT NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_FOOD (
                $F_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $F_NAME TEXT NOT NULL,
                $F_CALORIES INTEGER NOT NULL,
                $F_DATE TEXT NOT NULL,
                $F_MEAL_TYPE TEXT NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_HABITS (
                $H_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $H_NAME TEXT NOT NULL,
                $H_ACTIVE INTEGER DEFAULT 1
            )
       """)

       db.execSQL("""
            CREATE TABLE $TABLE_HABIT_LOGS (
                $HL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $HL_HABIT_ID INTEGER NOT NULL,
                $HL_DATE TEXT NOT NULL,
                $HL_COMPLETED INTEGER DEFAULT 0,
                FOREIGN KEY ($HL_HABIT_ID) REFERENCES $TABLE_HABITS($H_ID) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_WORKOUTS (
                $WK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $WK_NAME TEXT NOT NULL,
                $WK_LEVEL TEXT NOT NULL,
                $WK_DESC TEXT,
                $WK_ORDER INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_WORKOUT_EXERCISES (
                $WE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $WE_WORKOUT_ID INTEGER NOT NULL,
                $WE_NAME TEXT NOT NULL,
                $WE_SETS INTEGER DEFAULT 1,
                $WE_REPS TEXT NOT NULL,
                $WE_REST INTEGER DEFAULT 60,
                $WE_ORDER INTEGER,
                FOREIGN KEY ($WE_WORKOUT_ID) REFERENCES $TABLE_WORKOUTS($WK_ID) ON DELETE CASCADE
            )
        """)

        seedWorkoutPlans(db)
    }

    private fun seedWorkoutPlans(db: SQLiteDatabase) {
        val plans = listOf(
            Triple("Inicio", "Principiante", "Entrenamiento de arranque para familiarizarse con el peso corporal."),
            Triple("Acondicionamiento", "Básico", "Sesiones que aumentan volumen y resistencia básica."),
            Triple("Fuerza", "Intermedio", "Trabajo de fuerza con variantes más exigentes."),
            Triple("Dominio", "Avanzado", "Plan avanzado con dominadas, fondos y pistols.")
        )

        val exercisesByPlan = listOf(
            listOf(
                arrayOf("Sentadilla", "3", "10", "60"),
                arrayOf("Flexión de rodillas", "3", "8", "60"),
                arrayOf("Plancha frontal", "3", "20s", "45"),
                arrayOf("Elevación de pantorrillas", "3", "15", "45")
            ),
            listOf(
                arrayOf("Sentadilla", "3", "15", "60"),
                arrayOf("Flexión estándar", "3", "10", "60"),
                arrayOf("Plancha frontal", "3", "30s", "45"),
                arrayOf("Remo invertido", "3", "8", "75")
            ),
            listOf(
                arrayOf("Sentadilla búlgara", "3", "12/pierna", "75"),
                arrayOf("Flexión diamante", "4", "10", "60"),
                arrayOf("Plancha lateral", "3", "25s/lado", "45"),
                arrayOf("Dominada australiana", "4", "8", "90")
            ),
            listOf(
                arrayOf("Sentadilla pistol asistida", "3", "8/pierna", "90"),
                arrayOf("Dominada", "4", "6", "90"),
                arrayOf("Fondos en paralelas", "4", "8", "75"),
                arrayOf("Plancha con elevación de pierna", "3", "30s", "45")
            )
        )

        plans.forEachIndexed { planIndex, plan ->
            val planValues = ContentValues().apply {
                put(WK_NAME, plan.first)
                put(WK_LEVEL, plan.second)
                put(WK_DESC, plan.third)
                put(WK_ORDER, planIndex + 1)
            }
            val planId = db.insert(TABLE_WORKOUTS, null, planValues)

            exercisesByPlan[planIndex].forEachIndexed { exIndex, ex ->
                val exValues = ContentValues().apply {
                    put(WE_WORKOUT_ID, planId)
                    put(WE_NAME, ex[0])
                    put(WE_SETS, ex[1].toInt())
                    put(WE_REPS, ex[2])
                    put(WE_REST, ex[3].toInt())
                    put(WE_ORDER, exIndex + 1)
                }
                db.insert(TABLE_WORKOUT_EXERCISES, null, exValues)
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BLOOD_PRESSURE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WEIGHT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOOD")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HABITS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HABIT_LOGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKOUTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKOUT_EXERCISES")
        onCreate(db)
    }

    // ─── Blood Pressure ───────────────────────────────────────────────────────

    fun insertBloodPressure(systolic: Int, diastolic: Int, pulse: Int?, date: String, time: String): Long {
        val values = ContentValues().apply {
            put(BP_SYSTOLIC, systolic)
            put(BP_DIASTOLIC, diastolic)
            pulse?.let { put(BP_PULSE, it) }
            put(BP_DATE, date)
            put(BP_TIME, time)
        }
        return writableDatabase.insert(TABLE_BLOOD_PRESSURE, null, values)
    }

    fun getBloodPressureThisWeek(): List<BloodPressureRecord> {
        val db = readableDatabase
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = sdf.format(cal.time)

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_BLOOD_PRESSURE WHERE $BP_DATE >= ? ORDER BY $BP_DATE DESC, $BP_TIME DESC",
            arrayOf(weekAgo)
        )
        val records = mutableListOf<BloodPressureRecord>()
        while (cursor.moveToNext()) {
            records.add(BloodPressureRecord(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(BP_ID)),
                systolic = cursor.getInt(cursor.getColumnIndexOrThrow(BP_SYSTOLIC)),
                diastolic = cursor.getInt(cursor.getColumnIndexOrThrow(BP_DIASTOLIC)),
                pulse = cursor.getInt(cursor.getColumnIndexOrThrow(BP_PULSE)),
                date = cursor.getString(cursor.getColumnIndexOrThrow(BP_DATE)),
                time = cursor.getString(cursor.getColumnIndexOrThrow(BP_TIME))
            ))
        }
        cursor.close()
        return records
    }

    fun getBloodPressureByDate(date: String): List<BloodPressureRecord> {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_BLOOD_PRESSURE WHERE $BP_DATE = ? ORDER BY $BP_TIME DESC",
            arrayOf(date)
        )
        val records = mutableListOf<BloodPressureRecord>()
        while (cursor.moveToNext()) {
            records.add(BloodPressureRecord(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(BP_ID)),
                systolic = cursor.getInt(cursor.getColumnIndexOrThrow(BP_SYSTOLIC)),
                diastolic = cursor.getInt(cursor.getColumnIndexOrThrow(BP_DIASTOLIC)),
                pulse = cursor.getInt(cursor.getColumnIndexOrThrow(BP_PULSE)),
                date = cursor.getString(cursor.getColumnIndexOrThrow(BP_DATE)),
                time = cursor.getString(cursor.getColumnIndexOrThrow(BP_TIME))
            ))
        }
        cursor.close()
        return records
    }

    fun getBloodPressureToday(): List<BloodPressureRecord> {
        val db = readableDatabase
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_BLOOD_PRESSURE WHERE $BP_DATE = ? ORDER BY $BP_TIME DESC",
            arrayOf(today)
        )
        val records = mutableListOf<BloodPressureRecord>()
        while (cursor.moveToNext()) {
            records.add(BloodPressureRecord(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(BP_ID)),
                systolic = cursor.getInt(cursor.getColumnIndexOrThrow(BP_SYSTOLIC)),
                diastolic = cursor.getInt(cursor.getColumnIndexOrThrow(BP_DIASTOLIC)),
                pulse = cursor.getInt(cursor.getColumnIndexOrThrow(BP_PULSE)),
                date = cursor.getString(cursor.getColumnIndexOrThrow(BP_DATE)),
                time = cursor.getString(cursor.getColumnIndexOrThrow(BP_TIME))
            ))
        }
        cursor.close()
        return records
    }

    fun getAvgBloodPressureWeek(): Pair<Double, Double>? {
        val db = readableDatabase
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = sdf.format(cal.time)
        val cursor = db.rawQuery(
            "SELECT AVG($BP_SYSTOLIC), AVG($BP_DIASTOLIC) FROM $TABLE_BLOOD_PRESSURE WHERE $BP_DATE >= ?",
            arrayOf(weekAgo)
        )
        return if (cursor.moveToFirst() && !cursor.isNull(0)) {
            val result = Pair(cursor.getDouble(0), cursor.getDouble(1))
            cursor.close()
            result
        } else {
            cursor.close()
            null
        }
    }

    fun deleteBloodPressure(id: Int) {
        writableDatabase.delete(TABLE_BLOOD_PRESSURE, "$BP_ID = ?", arrayOf(id.toString()))
    }

    // ─── Weight ───────────────────────────────────────────────────────────────

    fun insertWeight(weightKg: Double): Long {
        val db = writableDatabase
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val values = ContentValues().apply {
            put(W_VALUE, weightKg)
            put(W_DATE, today)
        }
        return db.insert(TABLE_WEIGHT, null, values)
    }

    fun getWeightLastMonth(): List<WeightRecord> {
        val db = readableDatabase
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val monthAgo = sdf.format(cal.time)
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_WEIGHT WHERE $W_DATE >= ? ORDER BY $W_DATE ASC",
            arrayOf(monthAgo)
        )
        val records = mutableListOf<WeightRecord>()
        while (cursor.moveToNext()) {
            records.add(WeightRecord(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(W_ID)),
                weightKg = cursor.getDouble(cursor.getColumnIndexOrThrow(W_VALUE)),
                date = cursor.getString(cursor.getColumnIndexOrThrow(W_DATE))
            ))
        }
        cursor.close()
        return records
    }

    fun getLatestWeight(): WeightRecord? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_WEIGHT ORDER BY $W_DATE DESC LIMIT 1", null
        )
        return if (cursor.moveToFirst()) {
            val r = WeightRecord(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(W_ID)),
                weightKg = cursor.getDouble(cursor.getColumnIndexOrThrow(W_VALUE)),
                date = cursor.getString(cursor.getColumnIndexOrThrow(W_DATE))
            )
            cursor.close()
            r
        } else {
            cursor.close()
            null
        }
    }

    fun getWeightChangeLastMonth(): Double? {
        val records = getWeightLastMonth()
        return if (records.size >= 2) records.last().weightKg - records.first().weightKg else null
    }

    fun deleteWeight(id: Int) {
        writableDatabase.delete(TABLE_WEIGHT, "$W_ID = ?", arrayOf(id.toString()))
    }

    // ─── Food ─────────────────────────────────────────────────────────────────

    fun insertFood(name: String, calories: Int, mealType: String, date: String): Long {
        val values = ContentValues().apply {
            put(F_NAME, name)
            put(F_CALORIES, calories)
            put(F_DATE, date)
            put(F_MEAL_TYPE, mealType)
        }
        return writableDatabase.insert(TABLE_FOOD, null, values)
    }

    fun getFoodByDate(date: String): List<FoodRecord> {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_FOOD WHERE $F_DATE = ? ORDER BY $F_ID ASC",
            arrayOf(date)
        )
        val records = mutableListOf<FoodRecord>()
        while (cursor.moveToNext()) {
            records.add(FoodRecord(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(F_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(F_NAME)),
                calories = cursor.getInt(cursor.getColumnIndexOrThrow(F_CALORIES)),
                date = cursor.getString(cursor.getColumnIndexOrThrow(F_DATE)),
                mealType = cursor.getString(cursor.getColumnIndexOrThrow(F_MEAL_TYPE))
            ))
        }
        cursor.close()
        return records
    }

    fun getTotalCaloriesByDate(date: String): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT SUM($F_CALORIES) FROM $TABLE_FOOD WHERE $F_DATE = ?",
            arrayOf(date)
        )
        val total = if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) else 0
        cursor.close()
        return total
    }

    fun getFoodToday(): List<FoodRecord> {
        val db = readableDatabase
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_FOOD WHERE $F_DATE = ? ORDER BY $F_ID ASC",
            arrayOf(today)
        )
        val records = mutableListOf<FoodRecord>()
        while (cursor.moveToNext()) {
            records.add(FoodRecord(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(F_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(F_NAME)),
                calories = cursor.getInt(cursor.getColumnIndexOrThrow(F_CALORIES)),
                date = cursor.getString(cursor.getColumnIndexOrThrow(F_DATE)),
                mealType = cursor.getString(cursor.getColumnIndexOrThrow(F_MEAL_TYPE))
            ))
        }
        cursor.close()
        return records
    }

    fun getTotalCaloriesToday(): Int {
        val db = readableDatabase
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cursor = db.rawQuery(
            "SELECT SUM($F_CALORIES) FROM $TABLE_FOOD WHERE $F_DATE = ?",
            arrayOf(today)
        )
        val total = if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) else 0
        cursor.close()
        return total
    }

    fun getAvgCaloriesWeek(): Double {
        val db = readableDatabase
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = sdf.format(cal.time)
        val cursor = db.rawQuery(
            "SELECT AVG(daily) FROM (SELECT SUM($F_CALORIES) as daily FROM $TABLE_FOOD WHERE $F_DATE >= ? GROUP BY $F_DATE)",
            arrayOf(weekAgo)
        )
        val avg = if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getDouble(0) else 0.0
        cursor.close()
        return avg
    }

    fun deleteFood(id: Int) {
        writableDatabase.delete(TABLE_FOOD, "$F_ID = ?", arrayOf(id.toString()))
    }

    // habits

    fun insertHabit(name: String): Long {
        val values = ContentValues().apply {
            put(H_NAME, name)
            put(H_ACTIVE, 1)
        }
        return writableDatabase.insert(TABLE_HABITS, null, values)
    }

    fun getActiveHabits(): List<HabitRecord> {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_HABITS WHERE $H_ACTIVE = 1 ORDER BY $H_ID ASC", null
        )
        val list = mutableListOf<HabitRecord>()
        while (cursor.moveToNext()) {
            list.add(HabitRecord(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(H_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(H_NAME))
            ))
        }
        cursor.close()
        return list
    }

    fun renameHabit(id: Int, newName: String) {
        val values = ContentValues().apply { put(H_NAME, newName) }
        writableDatabase.update(TABLE_HABITS, values, "$H_ID = ?", arrayOf(id.toString()))
    }

    fun deleteHabit(id: Int) {
        writableDatabase.delete(TABLE_HABIT_LOGS, "$HL_HABIT_ID = ?", arrayOf(id.toString()))
        writableDatabase.delete(TABLE_HABITS, "$H_ID = ?", arrayOf(id.toString()))
    }

    // ─── Habit Logs ───────────────────────────────────────────────────────────

    fun setHabitLog(habitId: Int, date: String, completed: Boolean) {
        val db = writableDatabase
        // Upsert: delete existing then insert
        db.delete(TABLE_HABIT_LOGS,
            "$HL_HABIT_ID = ? AND $HL_DATE = ?",
            arrayOf(habitId.toString(), date))
        val values = ContentValues().apply {
            put(HL_HABIT_ID, habitId)
            put(HL_DATE, date)
            put(HL_COMPLETED, if (completed) 1 else 0)
        }
        db.insert(TABLE_HABIT_LOGS, null, values)
    }

    fun getHabitLogsForDate(date: String): List<HabitLogRecord> {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_HABIT_LOGS WHERE $HL_DATE = ?", arrayOf(date)
        )
        val list = mutableListOf<HabitLogRecord>()
        while (cursor.moveToNext()) {
            list.add(HabitLogRecord(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(HL_ID)),
                habitId = cursor.getInt(cursor.getColumnIndexOrThrow(HL_HABIT_ID)),
                date = cursor.getString(cursor.getColumnIndexOrThrow(HL_DATE)),
                completed = cursor.getInt(cursor.getColumnIndexOrThrow(HL_COMPLETED)) == 1
            ))
        }
        cursor.close()
        return list
    }

    // Returns map of date -> Pair(completedCount, totalHabits) for a given month
    fun getMonthCompletionMap(year: Int, month: Int): Map<String, Pair<Int, Int>> {
        val prefix = "%04d-%02d".format(year, month)
        val totalHabits = getActiveHabits().size
        val cursor = readableDatabase.rawQuery(
            """SELECT $HL_DATE, SUM($HL_COMPLETED) as done
               FROM $TABLE_HABIT_LOGS
               WHERE $HL_DATE LIKE '$prefix%'
               GROUP BY $HL_DATE""", null
        )
        val map = mutableMapOf<String, Pair<Int, Int>>()
        while (cursor.moveToNext()) {
            val date = cursor.getString(0)
            val done = cursor.getInt(1)
            map[date] = Pair(done, totalHabits)
        }
        cursor.close()
        return map
    }

    // ─── Historical (all-time) ─────────────────────────────────────────────

    fun getAvgBloodPressureAllTime(): Pair<Double, Double>? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT AVG($BP_SYSTOLIC), AVG($BP_DIASTOLIC) FROM $TABLE_BLOOD_PRESSURE", null
        )
        return if (cursor.moveToFirst() && !cursor.isNull(0)) {
            val result = Pair(cursor.getDouble(0), cursor.getDouble(1))
            cursor.close()
            result
        } else {
            cursor.close()
            null
        }
    }

    fun getBloodPressureCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_BLOOD_PRESSURE", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun getWeightAllTimeStats(): WeightAllTimeStats? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT MIN($W_VALUE), MAX($W_VALUE), AVG($W_VALUE), COUNT(*) FROM $TABLE_WEIGHT", null
        )
        return if (cursor.moveToFirst() && !cursor.isNull(0)) {
            val stats = WeightAllTimeStats(
                minKg = cursor.getDouble(0),
                maxKg = cursor.getDouble(1),
                avgKg = cursor.getDouble(2),
                count = cursor.getInt(3)
            )
            cursor.close()
            stats
        } else {
            cursor.close()
            null
        }
    }

    fun getAvgCaloriesAllTime(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT AVG(daily) FROM (SELECT SUM($F_CALORIES) as daily FROM $TABLE_FOOD GROUP BY $F_DATE)", null
        )
        val avg = if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getDouble(0) else 0.0
        cursor.close()
        return avg
    }

    fun getFoodCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_FOOD", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    // ─── Habits Stats ──────────────────────────────────────────────────────────

    fun getHabitCompletionWeek(): Pair<Int, Int> {
        val db = readableDatabase
        val totalActiveHabits = getActiveHabits().size
        if (totalActiveHabits == 0) return Pair(0, 7)

        val threshold = totalActiveHabits / 2.0
        val cursor = db.rawQuery("""
            SELECT COUNT(*) FROM (
                SELECT $HL_DATE, SUM($HL_COMPLETED) as done
                FROM $TABLE_HABIT_LOGS
                WHERE $HL_DATE >= date('now', '-6 days') AND $HL_DATE <= date('now')
                GROUP BY $HL_DATE
                HAVING done >= $threshold
            )
        """, null)
        val diasOk = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return Pair(diasOk, 7)
    }

    fun getHabitCompletionMonth(): Pair<Int, Int> {
        val db = readableDatabase
        val totalActiveHabits = getActiveHabits().size
        if (totalActiveHabits == 0) return Pair(0, 0)

        val threshold = totalActiveHabits / 2.0
        val cal = Calendar.getInstance()
        val totalDias = cal.get(Calendar.DAY_OF_MONTH)

        val cursor = db.rawQuery("""
            SELECT COUNT(*) FROM (
                SELECT $HL_DATE, SUM($HL_COMPLETED) as done
                FROM $TABLE_HABIT_LOGS
                WHERE $HL_DATE >= date('now', 'start of month') AND $HL_DATE <= date('now')
                GROUP BY $HL_DATE
                HAVING done >= $threshold
            )
        """, null)
        val diasOk = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return Pair(diasOk, totalDias)
    }

    // ─── Workouts ─────────────────────────────────────────────────────────────

    fun getWorkoutPlans(): List<WorkoutPlan> {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_WORKOUTS ORDER BY $WK_ORDER ASC", null
        )
        val list = mutableListOf<WorkoutPlan>()
        while (cursor.moveToNext()) {
            list.add(WorkoutPlan(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(WK_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(WK_NAME)),
                level = cursor.getString(cursor.getColumnIndexOrThrow(WK_LEVEL)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(WK_DESC)),
                order = cursor.getInt(cursor.getColumnIndexOrThrow(WK_ORDER))
            ))
        }
        cursor.close()
        return list
    }

    fun getWorkoutExercises(planId: Int): List<WorkoutExercise> {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_WORKOUT_EXERCISES WHERE $WE_WORKOUT_ID = ? ORDER BY $WE_ORDER ASC",
            arrayOf(planId.toString())
        )
        val list = mutableListOf<WorkoutExercise>()
        while (cursor.moveToNext()) {
            list.add(WorkoutExercise(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(WE_ID)),
                workoutId = cursor.getInt(cursor.getColumnIndexOrThrow(WE_WORKOUT_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(WE_NAME)),
                sets = cursor.getInt(cursor.getColumnIndexOrThrow(WE_SETS)),
                reps = cursor.getString(cursor.getColumnIndexOrThrow(WE_REPS)),
                restSeconds = cursor.getInt(cursor.getColumnIndexOrThrow(WE_REST)),
                order = cursor.getInt(cursor.getColumnIndexOrThrow(WE_ORDER))
            ))
        }
        cursor.close()
        return list
    }
}

// ─── Data Classes ─────────────────────────────────────────────────────────────

data class BloodPressureRecord(
    val id: Int,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val date: String,
    val time: String
)

data class WeightRecord(
    val id: Int,
    val weightKg: Double,
    val date: String
)

data class FoodRecord(
    val id: Int,
    val name: String,
    val calories: Int,
    val date: String,
    val mealType: String
)

data class HabitRecord(
    val id: Int,
    val name: String
)

data class HabitLogRecord(
    val id: Int,
    val habitId: Int,
    val date: String,
    val completed: Boolean
)

data class WeightAllTimeStats(
    val minKg: Double,
    val maxKg: Double,
    val avgKg: Double,
    val count: Int
)

data class WorkoutPlan(
    val id: Int,
    val name: String,
    val level: String,
    val description: String,
    val order: Int
)

data class WorkoutExercise(
    val id: Int,
    val workoutId: Int,
    val name: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val order: Int
)