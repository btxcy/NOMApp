package com.example.nom;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.List;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button start = (Button) findViewById(R.id.start);
        TextView status_txt = (TextView) findViewById(R.id.status);

        memory();
        run();

        start.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                // status_txt.setVisibility(View.GONE);
                Log.d("debug", "run0");
                run();
                Log.d("debug", "run1");
                // status.setText(result);
                // status_txt.setVisibility(View.VISIBLE);
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
        PyObject pyobj = py.getModule("detect_UNetFR");
        Log.d("debug", "run6");
        PyObject obj = pyobj.callAttr("main");
        Log.d("debug", "run7");
        // return pyobj.toString();
    }
}