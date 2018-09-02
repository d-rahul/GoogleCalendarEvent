package com.hb.googlecalendareventssample;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

public class SplashActivity extends AppCompatActivity {

    private static final int DELAY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler() {
            @Override
            public void handleMessage(Message msg) {
                splashNavigation();
            }
        }.sendEmptyMessageDelayed(0, DELAY);
    }

    private void splashNavigation() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }
}
