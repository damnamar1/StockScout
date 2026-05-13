package com.example.stockscout.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * All fields nullable with defaults. mockapi.io is permissive — fields may be missing,
 * null, or include extras like an auto-generated "id". Gson silently ignores unknown
 * fields; nullable types let us survive missing required ones without crashing.
 */
data class ItemDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("itemCode") val itemCode: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("unitOfMeasure") val unitOfMeasure: String? = null,
    @SerializedName("onHandQuantity") val onHandQuantity: Int? = null,
    @SerializedName("aliases") val aliases: List<AliasDto>? = null
)
