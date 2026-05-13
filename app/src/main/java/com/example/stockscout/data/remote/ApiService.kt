package com.example.stockscout.data.remote

import com.example.stockscout.data.remote.dto.ItemDto
import com.example.stockscout.data.remote.dto.PickRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    /** Returns wrapped Response so we can inspect status before touching the body. */
    @GET("items")
    suspend fun getItems(): Response<List<ItemDto>>

    /**
     * Pick audit log. ResponseBody return type means Retrofit never tries to deserialize
     * mockapi.io's echo response into a typed class — historical source of the
     * "Parameter specified as non-null is null" crashes.
     */
    @POST("picks")
    suspend fun postPick(@Body request: PickRequestDto): Response<ResponseBody>

    /**
     * Full-object PUT. Using a typed DTO triggered Gson serialization edge cases on some
     * mockapi.io schemas; a raw Map serializes to a plain JSON object every time.
     * `@JvmSuppressWildcards` keeps Retrofit's Java-side reflection happy with Map<String, Any>.
     */
    @PUT("items/{id}")
    suspend fun updateItem(
        @Path("id") remoteId: String,
        @Body item: @JvmSuppressWildcards Map<String, Any>
    ): Response<ResponseBody>
}
