package com.suntiago.lame4androiddemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();
    private EncoderC mEncoderC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEncoderC = new EncoderC();
        encode();
    }

    private void encode() {
        File file = new File("file:///android_asset/" + "ccc.wav");
        mEncoderC.encodeAndSend(file, new EncoderC.Trans() {
            @Override
            public void trans(String path) {
                Log.d(TAG, path);
            }
        });
    }
}
