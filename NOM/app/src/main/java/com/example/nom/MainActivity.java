package com.example.nom;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    byte[] byte_array;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final int REQUEST_WRITE_STORAGE_REQUEST_CODE = 1;
    Bitmap bitmap_result;
    byte[] result;
    // run flag
    int run;
    // UI
    ProgressBar running;
    TextView run_text;
    ImageView result_image;

    // user chose the picture from the photo lib
    // get the image tensor here
    ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult o) {
                            int result = o.getResultCode();
                            Intent data = o.getData();

                            if (result == RESULT_OK) {
                                // get the user selected image data
                                assert data != null;
                                Uri imageUri = data.getData();

                                try {
                                    // decode the image
                                    assert imageUri != null;
                                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                    byte_array = stream.toByteArray();
                                    run();
                                } catch (FileNotFoundException e) { // if no image is found
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
        run = 0;
        Button cam = findViewById(R.id.cam);
        Button lib = findViewById(R.id.lib);
        Button run_b = findViewById(R.id.run);
        running = findViewById(R.id.run_bar);
        run_text = findViewById(R.id.run_text);
        result_image = findViewById(R.id.result_image);

        memory();
        // in case the program delete while running
//        if (run == 0) {
//            deleteAppDirectory("output");
//        }

        // ask for file access permission
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_REQUEST_CODE);
        }

        // to be implemented
        // camera feature
        cam.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
            }
        });

        // select photo in library by user
        lib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                result_image.setVisibility(View.GONE);
                run = 1;
                folder_creation();
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                activityResultLauncher.launch(intent);
            }
        });

        // run bulk images
        run_b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                result_image.setVisibility(View.GONE);
                run = 1;
                folder_creation();
                run_bulk();
            }
        });
    }

    // result of the file access permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // The permission request was granted
                Toast.makeText(MainActivity.this, "Access Granted", Toast.LENGTH_LONG).show();
            } else {
                // The permission request was denied
                // Handle the case where permission is denied
                Toast.makeText(MainActivity.this, "Required Permission to Save Image", Toast.LENGTH_LONG).show();
            }
        }
    }

    // save the image into the photo library
    // if want to directly make the picture show in the application, change here (future plan)
    void saveImage() {
        // show the result image in App
        result_image.setImageBitmap(bitmap_result);
        result_image.setVisibility(View.VISIBLE);
        File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = new File(picturesDirectory, "result_import" + ".png");
        try (OutputStream out = new FileOutputStream(imageFile)) {
            bitmap_result.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
        }
        catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error saving image", Toast.LENGTH_SHORT).show();
            return;
        }

        // notify to the photo library about the new image
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imageFile.getAbsolutePath());
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        MainActivity.this.sendBroadcast(mediaScanIntent);
    }

    // memory check (show the RAM usage)
    void memory() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        double availableMegs = (double) mi.availMem / 0x100000L;

        double percentAvail = 100 - (mi.availMem / (double) mi.totalMem * 100.0);

        Log.d("available memory", String.format ("%.0f", availableMegs));
        Log.d("percentage used memory", String.format ("%.0f", percentAvail));
    }

    // run bulk images function, same as run but can run more images
    void run_bulk() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                running.setVisibility(View.VISIBLE);
                run_text.setVisibility(View.VISIBLE);
            }
        });
        // handle the async problem
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // Run Python script using Chaquopy
                if (!Python.isStarted()) {
                    Log.d("debug", "run3");
                    Python.start(new AndroidPlatform(MainActivity.this));
                }
                Log.d("debug", "run4");
                Python py = Python.getInstance();
                Log.d("debug", "run5");
                PyObject pyobj = py.getModule("detect_UNetFR");
                Log.d("debug", "run6");
                PyObject bool = pyobj.callAttr("main", "False");

                // Run on the main thread after script is executed
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // saveImage();
                        // Update UI
                        running.setVisibility(View.GONE);
                        run_text.setVisibility(View.GONE);
                        // Continue with the result
                        deleteAppDirectory("tensor");
                        // run flag initialized
                        run = 0;
                    }
                });
            }
        });
    }

    // run only one image (will be optimized in the future plan (combine run_bulk and run)
    private void run() {
        // Display a Toast message from the background thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                running.setVisibility(View.VISIBLE);
                run_text.setVisibility(View.VISIBLE);
            }
        });
        // handle the async problem
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // Run Python script using Chaquopy
                if (!Python.isStarted()) {
                    Log.d("debug", "run3");
                    Python.start(new AndroidPlatform(MainActivity.this));
                }
                Log.d("debug", "run4");
                Python py = Python.getInstance();
                Log.d("debug", "run5");
                PyObject pyobj = py.getModule("lib_UNetFR");
                Log.d("debug", "run6");
                result = pyobj.callAttr("main", byte_array).toJava(byte[].class);
                Log.d("debug", "run7");
                bitmap_result = BitmapFactory.decodeByteArray(result, 0, result.length);

                // Run on the main thread after script is executed
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        saveImage();
                        // Update UI
                        running.setVisibility(View.GONE);
                        run_text.setVisibility(View.GONE);
                        // Continue with the result
                        deleteAppDirectory("tensor");
                        // run flag initialized
                        run = 0;
                    }
                });
            }
        });
    }

    // this function helps to create the folder that needs to store the output and tensors (optimized version)
    private void folder_creation() {
        PackageManager m = getPackageManager();
        String s = getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;

            // Path for the new directory
            String tensor_dir = s + File.separator + "tensor";
            String output_dir = s + File.separator + "output";
            String log_dir = s + File.separator + "log";

            // Create a File object for the new directory
            File t_dir = new File(tensor_dir);
            File o_dir = new File(output_dir);
            File l_dir = new File(log_dir);

            // Check if the directory exists. If not, create it.
            if (!t_dir.exists()) {
                if (t_dir.mkdir()) {
                    Log.d("DIR", "Directory created: " + t_dir);
                }
                else {
                    Log.d("DIR", "Failed to create directory: " + t_dir);
                }
            }
            else {
                Log.d("DIR", "Directory already exists: " + t_dir);
            }
            // output dir
            if (!o_dir.exists()) {
                if (o_dir.mkdir()) {
                    Log.d("DIR", "Directory created: " + o_dir);
                }
                else {
                    Log.d("DIR", "Failed to create directory: " + o_dir);
                }
            }
            else {
                Log.d("DIR", "Directory already exists: " + o_dir);
            }
            // log dir
            if (!l_dir.exists()) {
                if (l_dir.mkdir()) {
                    Log.d("DIR", "Directory created: " + l_dir);
                }
                else {
                    Log.d("DIR", "Failed to create directory: " + l_dir);
                }
            }
            else {
                Log.d("DIR", "Directory already exists: " + l_dir);
            }
        }
        catch (PackageManager.NameNotFoundException e) {
            Log.d("DIR", "Error: Package Not Found ", e);
        }
    }

    // delete the stored unused tensors to release the storage
    // will only run after the running process stops
    // parameter: directory name
    private void deleteAppDirectory(String dirName) {
        PackageManager m = getPackageManager();
        String packageName = getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(packageName, 0);
            String dataDir = p.applicationInfo.dataDir;

            // Path for the directory you want to delete
            String dirPath = dataDir + File.separator + dirName;
            File dirToDelete = new File(dirPath);

            if (deleteDirectory(dirToDelete)) {
                Log.d("DIR", "Directory deleted successfully: " + dirPath);
            } else {
                Log.d("DIR", "Failed to delete directory: " + dirPath);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("DIR", "Error: Package Not Found ", e);
        }
    }

    // helper function to the delete tensor or output or both directory
    // parameter: directory name
    private boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            assert children != null;
            for (String child : children) {
                boolean success = deleteDirectory(new File(dir, child));
                if (!success) {
                    return false;  // If failed to delete, return false
                }
            }
        }
        // The directory is now empty or it is a file, so delete it
        return dir.delete();
    }
}