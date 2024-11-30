package com.sangithasubash.treaddepthanalyzer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView imageView;
    private ImageView selectImageButton;
    private ImageView cancelButton;
    private TextView resultTextView;
    private TextView classificationResultTextView;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        selectImageButton = findViewById(R.id.selectImageButton);
        cancelButton = findViewById(R.id.cancelButton);
        resultTextView = findViewById(R.id.resultTextView);
        classificationResultTextView = findViewById(R.id.classificationResultTextView);
        Button uploadButton = findViewById(R.id.uploadButton);
        Button classifyButton = findViewById(R.id.classifyButton);

        // Open image selector
        selectImageButton.setOnClickListener(v -> openImageSelector());

        // Cancel image selection
        cancelButton.setOnClickListener(v -> {
            imageView.setImageDrawable(null);
            imageUri = null;
            cancelButton.setVisibility(View.GONE);
            selectImageButton.setVisibility(View.VISIBLE);
        });

        // Upload image for tread depth
        uploadButton.setOnClickListener(v -> uploadImageForTreadDepth());

        // Classify tire image
        classifyButton.setOnClickListener(v -> classifyTireImage());
    }

    private void openImageSelector() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
            cancelButton.setVisibility(View.VISIBLE);
            selectImageButton.setVisibility(View.GONE);
            resultTextView.setVisibility(View.GONE);
            classificationResultTextView.setVisibility(View.GONE);
            resultTextView.setVisibility(View.VISIBLE);
            classificationResultTextView.setVisibility(View.VISIBLE);
        }
    }

    private void uploadImageForTreadDepth() {
        if (imageUri != null) {
            File file = new File(FileUtils.getPath(this, imageUri));
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

            ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
            Call<ResponseBody> call = apiService.uploadImage(body);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String result = response.body().string();
                            JSONObject jsonObject = new JSONObject(result);
                            String treadDepthMM = jsonObject.getString("tread_depth_mm");
                            String treadDepthInch = jsonObject.getString("tread_depth_inch");

                            String formattedResult = "Tread Depth: " + treadDepthMM + "(" + treadDepthInch + ")";
                            resultTextView.setText(formattedResult);
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to get response", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
        }
    }

    private void classifyTireImage() {
        if (imageUri != null) {
            File file = new File(FileUtils.getPath(this, imageUri));
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

            ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
            Call<ResponseBody> call = apiService.classifyImage(body);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String result = response.body().string();
                            JSONObject jsonObject = new JSONObject(result);
                            String classification = jsonObject.getString("classification");

                            String formattedResult = "Tire Condition: " + classification;
                            classificationResultTextView.setText(formattedResult);
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to get response", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Classification failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
        }
    }
}
