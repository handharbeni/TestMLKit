package com.example.testmlkit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.otaliastudios.cameraview.Audio;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;

public class MainActivity extends AppCompatActivity {
    private String TAG = MainActivity.class.getSimpleName();

    CameraView cameraView;
    ImageView imageView;
    RelativeLayout relativeLayoutPanelOverlayResult;
    RelativeLayout relativeLayoutPanelOverlayCamera;
    TextView textViewResult;
    Button buttonTakePicture;

    String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, 100);
        } else {
            ActivityCompat.requestPermissions(this, permissions, 100);
        }

        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera_view);
        imageView = findViewById(R.id.image_view);
        relativeLayoutPanelOverlayResult = findViewById(R.id.relative_layout_panel_overlay_result);
        relativeLayoutPanelOverlayCamera = findViewById(R.id.relative_layout_panel_overlay_camera);
        textViewResult = findViewById(R.id.text_view_result);
        buttonTakePicture = findViewById(R.id.button_take_picture);

        initCameraView();
        initListener();

    }

    void initCameraView() {
        cameraView.setAudio(Audio.OFF);
        cameraView.setPlaySounds(false);
        cameraView.setCropOutput(true);
    }

    void initListener() {
        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] jpeg) {
                cameraView.stop();
                CameraUtils.decodeBitmap(jpeg, new CameraUtils.BitmapCallback() {
                    @Override
                    public void onBitmapReady(Bitmap bitmap) {
                        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                        imageView.setImageBitmap(bitmap);
                        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
                        FirebaseVisionTextRecognizer textRecognizer =
                                FirebaseVision.getInstance().getOnDeviceTextRecognizer();
                        textRecognizer.processImage(firebaseVisionImage)
                                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                    @Override
                                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                        processTextRecognitionResult(firebaseVisionText);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "Failed to recognition", Toast.LENGTH_SHORT).show();
                                    }
                                });

                    }
                });
                super.onPictureTaken(jpeg);
            }
        });

        buttonTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.captureSnapshot();
            }
        });
    }

    void showCameraView() {
        if (cameraView.isStarted()){
            cameraView.stop();
        }
        cameraView.start();
        cameraView.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);
        relativeLayoutPanelOverlayCamera.setVisibility(View.VISIBLE);
        relativeLayoutPanelOverlayResult.setVisibility(View.GONE);
        buttonTakePicture.setVisibility(View.VISIBLE);
        textViewResult.setVisibility(View.GONE);
    }

    void showGalleryView() {
        if (cameraView.isStarted()) {
            cameraView.stop();
        }
        cameraView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        relativeLayoutPanelOverlayCamera.setVisibility(View.GONE);
        relativeLayoutPanelOverlayResult.setVisibility(View.GONE);

        Intent intentGallery = new Intent();
        intentGallery.setType("image/*");
        intentGallery.setAction(Intent.ACTION_GET_CONTENT);
        Intent intentChooser = Intent.createChooser(intentGallery, "Pilih Gambar");
        startActivityForResult(intentChooser, 100);
    }

    private void processTextRecognitionResult(FirebaseVisionText firebaseVisionText) {
        relativeLayoutPanelOverlayResult.setVisibility(View.VISIBLE);
        buttonTakePicture.setVisibility(View.GONE);
        textViewResult.setVisibility(View.VISIBLE);
        textViewResult.setText(firebaseVisionText.getText());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_camera:
                showCameraView();
                return true;
            case R.id.menu_item_upload_photo:
                showGalleryView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        if (cameraView.isStarted()) {
            cameraView.stop();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case 100:
                    Uri uriSelectedImage = data.getData();
                    String[] filePathColumn = new String[]{MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(
                            uriSelectedImage,
                            filePathColumn,
                            null,
                            null,
                            null
                    );

                    if (cursor == null || cursor.getCount() < 1){
                        return;
                    }

                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    if (columnIndex < 0){
                        Toast.makeText(MainActivity.this, "Invalid Image", Toast.LENGTH_SHORT).show();
                    }
                    String picturePath = cursor.getString(columnIndex);
                    if (picturePath == null){
                        Toast.makeText(MainActivity.this, "Picture Path not found",
                                Toast.LENGTH_SHORT).show();
                    }

                    cursor.close();

                    Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
                    imageView.setImageBitmap(bitmap);

                    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                    FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
                    textRecognizer.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                    processTextRecognitionResult(firebaseVisionText);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "Failed to recognition", Toast.LENGTH_SHORT).show();
                                }
                            });
                    break;
                default:
                    break;
            }
        }
    }
}