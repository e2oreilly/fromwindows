package com.example.carriboulou;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    static final int REQUEST_VIDEO_CAPTURE = 1;
    static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 2;
    VideoView videoView;
    ImageView iv;
    int counter = 0;
    int increment = 1000;

    int cycleRate = 1500;

    //FIREBASE DECLARATIONS
    FirebaseVisionBarcodeDetector detector;
    String rawValue = null;
    TextView text;

    String[] firebaseOutput = new String[6];
    Bitmap[] videoOutput = new Bitmap[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Firebase Crap Pasted Here
        FirebaseVisionBarcodeDetectorOptions options =
                new FirebaseVisionBarcodeDetectorOptions.Builder()
                        .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_PDF417, FirebaseVisionBarcode.FORMAT_QR_CODE)
                        .build();

        detector = FirebaseVision.getInstance()
                .getVisionBarcodeDetector(options);
        text = (TextView) findViewById(R.id.resulttext);


        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);

        dispatchTakeVideoIntent();
        videoView = (VideoView) findViewById(R.id.vv);
        iv = (ImageView) findViewById(R.id.iv);
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            final Uri videoUri = intent.getData();

            videoView.setVideoURI(videoUri);
            videoView.start();
            Log.e("ERIC", videoUri.getPath());

            //iv.setImageAlpha(0); // 0 = transparent
            //iv.setVisibility(View.INVISIBLE);

            videoView.setVisibility(View.INVISIBLE);

            for (int i=0; i<videoOutput.length; i++ )
            {
                videoOutput[i] = getFrame(videoUri, counter+(increment*i));
            }
            for (int i=0; i<firebaseOutput.length; i++ ) {
                detectorResultFromBmp(detector, videoOutput[i]);
            }

            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            setViews(iv, videoOutput[1], text, firebaseOutput[1]);
                        }
                    },
                    cycleRate*1);
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            setViews(iv, videoOutput[2], text, firebaseOutput[2]);
                        }
                    },
                    cycleRate*2);
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            setViews(iv, videoOutput[3], text, firebaseOutput[3]);
                        }
                    },
                    cycleRate*3);
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            setViews(iv, videoOutput[4], text, firebaseOutput[4]);
                        }
                    },
                    cycleRate*4);
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            setViews(iv, videoOutput[5], text, firebaseOutput[5]);
                        }
                    },
                    cycleRate*5);
        }
    }

    Bitmap getFrame(Uri uri, int seconds)
    {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(this, uri);

            Bitmap test = retriever.getFrameAtTime(seconds*1000000,MediaMetadataRetriever.OPTION_CLOSEST);
            Bitmap converted = test.copy(Bitmap.Config.ARGB_8888, false);
            //iv.setImageBitmap(test);
            return converted;

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
            }
        }
        return null;
    }

    public void detectorResultFromBmp(FirebaseVisionBarcodeDetector detector, Bitmap bmp)
    {
        FirebaseVisionImage firebaseImage = FirebaseVisionImage.fromBitmap(bmp);
        Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(firebaseImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionBarcode> barcodes) {
                        Log.e("ERIC", "Firebase Listener Returned");

                        int barcodeCount = 0;
                        for (FirebaseVisionBarcode barcode : barcodes) {
                            //Rect bounds = barcode.getBoundingBox();
                            //Point[] corners = barcode.getCornerPoints();
                            barcodeCount++;
                            rawValue = barcode.getRawValue();
                            Log.e("BARCODE #" + barcodeCount + " VALUE", rawValue);
                        }

                        firebaseOutput[counter] = rawValue;
                        counter++;
                        /*
                        if (barcodeCount == 0)
                        {
                            text.setTextColor(Color.parseColor("#FF0000"));
                            text.setText("FIREBASE  FAILED");
                        }
                        else
                        {
                            text.setTextColor(Color.parseColor("#00FF00"));
                            text.setText("BARCODE VALUE :  "+rawValue);
                        }
                        */
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ERIC", "LISTENER FAIL");
                        Log.e("ERIC", e.toString());// Task failed with an exception
                    }
                });

    }

    void setViews(ImageView iv, Bitmap bmp, TextView text, String output)
    {
        iv.setImageBitmap(bmp);
        if (output != null)
        {
            text.setTextColor(Color.parseColor("#00FF00"));
            text.setText("BARCODE VALUE :  "+output);
        }
        else
            {
                text.setTextColor(Color.parseColor("#FF0000"));
                text.setText("FIREBASE  FAILED");
        }
    }
}