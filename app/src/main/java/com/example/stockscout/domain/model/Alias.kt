package com.example.stockscout.domain.model

data class Alias(val type: AliasType, val value: String)

enum class AliasType { UPC_A, EAN_13, GS1, TEXT }
