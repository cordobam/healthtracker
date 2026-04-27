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
        const val DATABASE_VERSION = 2

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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BLOOD_PRESSURE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WEIGHT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOOD")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HABITS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HABIT_LOGS")
        onCreate(db)
    }

    // ─── Blood Pressure ───────────────────────────────────────────────────────

    fun insertBloodPressure(systolic: Int, diastolic: Int, pulse: Int?): Long {
        val db = writableDatabase
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val tdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()
        val values = ContentValues().apply {
            put(BP_SYSTOLIC, systolic)
            put(BP_DIASTOLIC, diastolic)
            pulse?.let { put(BP_PULSE, it) }
            put(BP_DATE, sdf.format(now))
            put(BP_TIME, tdf.format(now))
        }
        return db.insert(TABLE_BLOOD_PRESSURE, null, values)
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

    fun insertFood(name: String, calories: Int, mealType: String): Long {
        val db = writableDatabase
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val values = ContentValues().apply {
            put(F_NAME, name)
            put(F_CALORIES, calories)
            put(F_DATE, today)
            put(F_MEAL_TYPE, mealType)
        }
        return db.insert(TABLE_FOOD, null, values)
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
