package com.example.healthtracker

import android.content.Context

object ModulePrefs {

    private const val PREFS = "menu_config"
    private const val KEY_SELECTED = "selected_modules"
    private const val KEY_CONFIGURED = "configured"

    fun isConfigured(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CONFIGURED, false)

    fun setConfigured(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CONFIGURED, true).apply()
    }

    fun getSelected(context: Context): List<Module> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED, null)
            ?: return Module.entries.filter { it != Module.HOME }
        return raw.split(",")
            .mapNotNull { id -> Module.entries.firstOrNull { it.itemId == id.toIntOrNull() } }
            .filter { it != Module.HOME }
    }

    fun saveSelected(context: Context, modules: List<Module>) {
        val ids = modules.filter { it != Module.HOME }.joinToString(",") { it.itemId.toString() }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SELECTED, ids).apply()
    }

    fun isEnabled(context: Context, module: Module): Boolean =
        module == Module.HOME || getSelected(context).contains(module)
}