package ru.homebuhg.core.data.database

data class MonthlyTotal(val month: String, val type: String, val total: Long)
data class CategoryTotal(val categoryId: String, val total: Long)
