package com.example.covid19_local_messenger;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface MaskApi {
    @Headers("Accept: application/json")
    @GET("/corona19-masks/v1/storesByGeo/json")

    //getStoresByGeo API호출 -> StoreSaleResult 객체 반환
    Call<StoreSaleResult> getStoresByGeo(@Query("lat") double lat, @Query("lng") double lng, @Query("m") int m); //위도, 경도, 반경
}
