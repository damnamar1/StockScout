package com.example.stockscout.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PickRequestDto(
    @SerializedName("itemCode") val itemCode: String,
    @SerializedName("newQuantity") val newQuantity: Int,
    @SerializedName("timestamp") val timestamp: Long
)
