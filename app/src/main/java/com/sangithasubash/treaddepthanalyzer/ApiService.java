package com.sangithasubash.treaddepthanalyzer;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("/upload")
    Call<ResponseBody> uploadImage(@Part MultipartBody.Part image);

    @Multipart
    @POST("/classify")  // Add this endpoint for classification
    Call<ResponseBody> classifyImage(@Part MultipartBody.Part image);
}


