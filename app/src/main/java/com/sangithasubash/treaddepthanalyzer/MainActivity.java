package com.sangithasubash.treaddepthanalyzer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "TreadDepthAnalyzer";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

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

        // Check if the app has required permissions
        checkPermissions();

        // Open image selector
        selectImageButton.setOnClickListener(v -> openImageSelector());

        // Cancel image selection
        cancelButton.setOnClickListener(v -> {
            imageView.setImageDrawable(null);
            imageUri = null;
            cancelButton.setVisibility(View.GONE);
            selectImageButton.setVisibility(View.VISIBLE);
            resultTextView.setText("");
            classificationResultTextView.setText("");
        });

        // Upload image for tread depth
        uploadButton.setOnClickListener(v -> uploadImageForTreadDepth());

        // Classify tire image
        classifyButton.setOnClickListener(v -> classifyTireImage());
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission denied, cannot access storage", Toast.LENGTH_SHORT).show();
            }
        }
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

            // Hide previous results
            resultTextView.setText("");
            classificationResultTextView.setText("");

            // Show the cancel button and hide the select image button
            cancelButton.setVisibility(View.VISIBLE);
            selectImageButton.setVisibility(View.GONE);

            // Make result views visible
            resultTextView.setVisibility(View.VISIBLE);
            classificationResultTextView.setVisibility(View.VISIBLE);

            Log.d(TAG, "Image selected: " + imageUri);
        }
    }

    private void uploadImageForTreadDepth() {
        if (imageUri != null) {
            classificationResultTextView.setText("");

            File file = new File(FileUtils.getPath(this, imageUri));
            Log.d(TAG, "Uploading file: " + file.getAbsolutePath());

            if (!file.exists()) {
                Toast.makeText(this, "File does not exist: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                return;
            }

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
                            Log.d(TAG, "Upload Response: " + result);

                            JSONObject jsonObject = new JSONObject(result);
                            String treadDepthMM = jsonObject.getString("tread_depth_mm");
                            String treadDepthInch = jsonObject.getString("tread_depth_inch");

                            String formattedResult = "Tread Depth: " + treadDepthMM + " (" + treadDepthInch + ")";
                            resultTextView.setText(formattedResult);
                        } catch (IOException | JSONException e) {
                            Log.e(TAG, "Error parsing response", e);
                            Toast.makeText(MainActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Upload failed: " + response.code());
                        Toast.makeText(MainActivity.this, "Failed to get response", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "Upload API call failed", t);
                    Toast.makeText(MainActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
        }
    }

    private void classifyTireImage() {
        if (imageUri != null) {
            classificationResultTextView.setText("");

            File file = new File(FileUtils.getPath(this, imageUri));
            Log.d(TAG, "Classifying file: " + file.getAbsolutePath());

            if (!file.exists()) {
                Toast.makeText(this, "File does not exist: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                return;
            }

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
                            Log.d(TAG, "Classification Response: " + result);

                            JSONObject jsonObject = new JSONObject(result);
                            String classification = jsonObject.getString("classification");

                            String formattedResult = "Tire Condition: " + classification;
                            classificationResultTextView.setText(formattedResult);
                        } catch (IOException | JSONException e) {
                            Log.e(TAG, "Error parsing response", e);
                            Toast.makeText(MainActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Classification failed: " + response.code());
                        Toast.makeText(MainActivity.this, "Failed to get response", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "Classification API call failed", t);
                    Toast.makeText(MainActivity.this, "Classification failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
        }
    }
}
