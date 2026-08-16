package com.example.healthtracker

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationBuilder {

    fun setup(
        bottomNav: BottomNavigationView,
        activity: Activity,
        currentModule: Module
    ) {
        val selected = ModulePrefs.getSelected(activity)

        bottomNav.menu.clear()
        var order = 0
        addItem(bottomNav, Module.HOME, order++)

        val overflow = if (selected.size > Module.MAX_MODULES_NO_OVERFLOW) {
            selected.drop(Module.MAX_VISIBLE_WITH_OVERFLOW)
        } else {
            emptyList()
        }
        val visible = if (overflow.isEmpty()) selected else selected.take(Module.MAX_VISIBLE_WITH_OVERFLOW)

        visible.forEach { addItem(bottomNav, it, order++) }

        if (overflow.isNotEmpty()) {
            bottomNav.menu.add(0, Module.NAV_MORE, order++, Module.MORE_TITLE)
                .setIcon(android.R.drawable.ic_menu_more)
        }

        if (currentModule == Module.HOME || visible.contains(currentModule)) {
            bottomNav.selectedItemId = currentModule.itemId
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                Module.NAV_MORE -> {
                    showOverflowDialog(activity, overflow)
                    false
                }
                currentModule.itemId -> true
                else -> {
                    val module = Module.fromItemId(item.itemId)
                    if (module != null && module != currentModule) {
                        navigateTo(activity, module)
                    }
                    false
                }
            }
        }
    }

    private fun addItem(bottomNav: BottomNavigationView, module: Module, order: Int) {
        bottomNav.menu.add(0, module.itemId, order, module.title)
            .setIcon(module.iconRes)
    }

    private fun showOverflowDialog(
        activity: Activity,
        overflow: List<Module>
    ) {
        val options = overflow.map { "📦  ${it.title}" } + "⚙️  Configurar menú"
        AlertDialog.Builder(activity)
            .setTitle("Más módulos")
            .setItems(options.toTypedArray()) { _, which ->
                if (which == overflow.size) {
                    showConfigDialog(activity) {
                        goHome(activity)
                    }
                } else {
                    navigateTo(activity, overflow[which])
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    fun showConfigDialog(activity: Activity, onSaved: () -> Unit = {}) {
        val all = Module.entries.filter { it != Module.HOME }
        val current = ModulePrefs.getSelected(activity)
        val checked = BooleanArray(all.size) { current.contains(all[it]) }

        AlertDialog.Builder(activity)
            .setTitle("Elegí los módulos del menú")
            .setMultiChoiceItems(
                all.map { it.title }.toTypedArray(),
                checked
            ) { _, index, isChecked -> checked[index] = isChecked }
            .setPositiveButton("Guardar") { _, _ ->
                val selected = if (checked.any { it }) {
                    all.filterIndexed { index, _ -> checked[index] }
                } else {
                    listOf(Module.BP)
                }
                ModulePrefs.saveSelected(activity, selected)
                ModulePrefs.setConfigured(activity)
                onSaved()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun goHome(activity: Activity) {
        activity.startActivity(Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
    }

    private fun navigateTo(activity: Activity, module: Module) {
        activity.startActivity(Intent(activity, module.activity).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
    }
}