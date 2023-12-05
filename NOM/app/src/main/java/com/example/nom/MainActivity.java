package com.example.nom;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {

    byte[] byte_array;

    ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult o) {
                            int result = o.getResultCode();
                            Intent data = o.getData();

                            if (result == RESULT_OK) {
                                // Toast.makeText(MainActivity.this, "Passed", Toast.LENGTH_LONG).show();
                                Uri imageUri = data.getData();

                                try {
                                    // decode the image
                                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                    byte_array = stream.toByteArray();


                                    // System.out.println(byte_array);
                                    Toast.makeText(MainActivity.this, "Passed Tensor", Toast.LENGTH_LONG).show();

                                    run();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                            else {
                                Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button cam = (Button) findViewById(R.id.cam);
        Button lib = (Button) findViewById(R.id.lib);
        Button run_b = (Button) findViewById(R.id.run);

        memory();

        cam.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
            }
        });

        lib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                activityResultLauncher.launch(intent);
            }
        });
    }

    void memory() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        double availableMegs = mi.availMem / 0x100000L;

        double percentAvail = 100 - (mi.availMem / (double) mi.totalMem * 100.0);

        Log.d("available memory", String.format ("%.0f", availableMegs));
        Log.d("percentage used memory", String.format ("%.0f", percentAvail));
    }

    void run() {

        if (!Python.isStarted()) {
            Log.d("debug", "run3");
            Python.start(new AndroidPlatform(this));
        }
        Log.d("debug", "run4");
        Python py = Python.getInstance();
        Log.d("debug", "run5");
        PyObject pyobj = py.getModule("lib_UNetFR");
        Log.d("debug", "run6");
        PyObject obj = pyobj.callAttr("main", byte_array);
        Log.d("debug", "run7");
        // return pyobj.toString();
    }
}