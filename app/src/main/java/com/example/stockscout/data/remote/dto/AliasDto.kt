package com.example.stockscout.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AliasDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("value") val value: String? = null
)
