package com.ancata.prima_focus.utils

/**
 * "💼 Trabajo - Entrega • 2026-09-25": the category line shown under a task title in the app
 * screens and in both widgets.
 */
fun formatCategoryLine(
    emoji: String,
    category: String,
    subcategory: String?,
    date: String?,
    separator: String = " - ",
    fallbackCategory: String = "Sin categoría"
): String {
    val categoryCap = category.replaceFirstChar { it.uppercase() }.ifBlank { fallbackCategory }
    val subcategoryCap = subcategory?.replaceFirstChar { it.uppercase() }
    val categoryPart = if (!subcategoryCap.isNullOrBlank()) "$categoryCap$separator$subcategoryCap" else categoryCap
    val datePart = date?.let { " • $it" } ?: ""
    return "$emoji $categoryPart$datePart"
}
