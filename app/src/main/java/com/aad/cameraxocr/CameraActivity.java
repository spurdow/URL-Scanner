package com.aad.cameraxocr;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.aad.cameraxocr.urlfinder.UrlFinder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_PERMISSION_CODE = 1000;
    private static final String TAG = CameraActivity.class.getSimpleName();

    private ExecutorService mImageExecutorService = Executors.newSingleThreadExecutor(r -> {
        // Setting thread as daemon so we don't need to explicitly shutdown the executor
        Log.d(TAG , "Thread called");
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Camera mCamera;
    private FirebaseVisionTextRecognizer detector;
    private ProcessCameraProvider mCameraProvider;

    View mCameraRequiredLayout;
    View mCameraParentLayout;
    PreviewView mCameraPreviewView;
    Button mEnableCamera;
    TextView mTxtLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraRequiredLayout = findViewById(R.id.cameraRequiredLayout);
        mCameraParentLayout = findViewById(R.id.cameraPreviewParentLayout);
        mCameraPreviewView = findViewById(R.id.cameraPreview);
        mEnableCamera = findViewById(R.id.btnOK);
        mTxtLink = findViewById(R.id.txtLink);
        mTxtLink.setMovementMethod(LinkMovementMethod.getInstance());
        mEnableCamera.setOnClickListener(v -> showPermissionDialog());

        if (isCameraPermissionGranted()){
            setUpCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this , Manifest.permission.CAMERA ) == PackageManager.PERMISSION_DENIED){
            showPermissionDialog();
        }

        if (mCamera == null && mCameraProvider != null && isCameraPermissionGranted()){
            showCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_PERMISSION_CODE){
            if (isCameraPermissionGranted()){
                showCamera();
            }else{
                showError();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showError(){
        mCameraRequiredLayout.setVisibility(View.VISIBLE);
        mCameraParentLayout.setVisibility(View.GONE);
    }

    private void showCamera(){
        mCameraRequiredLayout.setVisibility(View.GONE);
        mCameraParentLayout.setVisibility(View.VISIBLE);
        setUpCamera();

    }

    private void showPermissionDialog(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.enable_camera_permission_title_3)
                .setMessage(R.string.enable_camera_permission_content_4)
                .setPositiveButton(R.string.enable_camera_permission_button_2, (dialog, which) -> {
                    if (isCameraPermissionGranted()){
                        showCamera();
                    }else {
                        ActivityCompat.requestPermissions(CameraActivity.this , new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_PERMISSION_CODE);
                    }

                })
                .setNegativeButton(R.string.enable_camera_permission_button_5, (dialog, which) -> showError())
                .create().show();
    }

    protected boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this , Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }


    // Camera Setup
    private void setUpCamera(){
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {

                mCameraProvider = cameraProviderFuture.get();

                startProcessing();

            } catch (ExecutionException | InterruptedException e) {
                // TODO: Show error message
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopProcessing(){
        if (mCameraProvider != null){
            mCameraProvider.unbindAll();
            mCamera = null;
        }

        Log.i(TAG , "OCR Processing stopped!!");
    }

    private void startProcessing(){
        Preview preview = new Preview.Builder()
                .build();

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        imageAnalysis.setAnalyzer(mImageExecutorService , new CameraImageAnalyser());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(mCameraPreviewView.createSurfaceProvider());
        mCamera = mCameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis,  preview);
    }

    private void processTextRecognitionAndFindHyperLinks(FirebaseVisionText text){
        Log.i(TAG , "Processing firebaseVisionText...");

        final String allText = text.getText();

        if (UrlFinder.doesURLExists(allText)){
            mTxtLink.post(() -> {
                mTxtLink.setVisibility(View.VISIBLE);
                mTxtLink.setText(UrlFinder.getUrl(allText));
                stopProcessing();
            });
        }else {
            mTxtLink.post(() -> mTxtLink.setVisibility(View.GONE));
            stopProcessing();
            startProcessing();
        }
    }

    private void processFailure(Exception e){
        Log.e(TAG , "Processing failed...");
        stopProcessing();
        startProcessing();
    }

    private class CameraImageAnalyser implements ImageAnalysis.Analyzer {

        private int degreesToFirebaseRotation(int degrees) {
            switch (degrees) {
                case 0:
                    return FirebaseVisionImageMetadata.ROTATION_0;
                case 90:
                    return FirebaseVisionImageMetadata.ROTATION_90;
                case 180:
                    return FirebaseVisionImageMetadata.ROTATION_180;
                case 270:
                    return FirebaseVisionImageMetadata.ROTATION_270;
                default:
                    throw new IllegalArgumentException(
                            "Rotation must be 0, 90, 180, or 270.");
            }
        }
        @Override
        public void analyze(@NonNull ImageProxy image) {
            if (image == null || image.getImage() == null){
                Log.d(TAG , "ImageProxy not found...");
                return;
            }
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            int fbRotation = degreesToFirebaseRotation(rotationDegrees);
            FirebaseVisionImage firebaseVisionImage  =
                    FirebaseVisionImage.fromMediaImage(image.getImage(), fbRotation);

            if (detector == null) {
                detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
            }
            detector.processImage(firebaseVisionImage)
                    .addOnSuccessListener(CameraActivity.this::processTextRecognitionAndFindHyperLinks)
                    .addOnFailureListener(CameraActivity.this::processFailure);

            Log.d(TAG , "ImageProxy found...");
        }
    }

}
