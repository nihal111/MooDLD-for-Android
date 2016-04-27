package com.moodld.moodld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class Splash extends AppCompatActivity {

    public SharedPreferences prefs=null;
    private static final String TAG="Splash";
    public String login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        prefs = getSharedPreferences("login", MODE_PRIVATE);
        login = prefs.getString("login", null);
        Log.d(TAG, "Logged in through: " + login);

        Thread timerThread = new Thread() {
            public void run() {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (login != null) {
                        Intent intent = new Intent(Splash.this, LoginActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(Splash.this, LoginActivity.class);
                        startActivity(intent);
                    }
                }
            }
        };

        timerThread.start();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        finish();
    }
}