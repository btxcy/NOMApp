package com.example.nom;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity {

    TextView status = (TextView) findViewById(R.id.status);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button start = (Button) findViewById(R.id.start);

        start.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                status.setVisibility(View.GONE);
                Log.d("debug", "run0");
                run();
                Log.d("debug", "run1");
                // status.setText(result);

            }
        });

//        if (should_start) {
//
//            status.setText("Working...");
//            if (!Python.isStarted()) {
//                Python.start(new AndroidPlatform(MainActivity.this));
//            }
//            Python py = Python.getInstance();
//            PyObject pyobj = py.getModule("detect_UNetFR");
//
//            PyObject obj = pyobj.callAttr("main");
//
//            should_start = false;
//
//            status.setText(obj.toString());
//        }
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
        Log.d("debug", obj.toString());
        status.setVisibility(View.VISIBLE);
    }
}