package com.example.stockscout.domain.model

data class Item(
    val itemCode: String,
    val name: String,
    val unitOfMeasure: String,
    val onHandQuantity: Int,
    val aliases: List<Alias>
)
