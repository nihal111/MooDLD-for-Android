package com.moodld.moodld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Splash extends AppCompatActivity {

    private Handler handler = new Handler();
    private Boolean handlerneeded = false, started = false;
    private Long stime, timeout_time=5000L;
    private Runnable timeout = new Runnable(){
        public void run() {
            if(handlerneeded) {

                if(!started) {
                    stime = System.currentTimeMillis();
                    started=true;
                }
                Long now = System.currentTimeMillis();
                if(now > stime + timeout_time) {
                    handlerneeded=false;
                    started=false;
                    Intent intent = new Intent(Splash.this, LoginActivity.class);
                    startActivity(intent);
                }
                handler.post(this);
            }
        }
    };

    private static final String TAG="Splash";
    private final String loginPageUrl = "http://moodle.iitb.ac.in/login/index.php";
    private String sessionCookie = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final SharedPreferences prefs = Splash.this.getSharedPreferences("LoginDetails", MODE_PRIVATE);
        final String username = prefs.getString("username", null);
        final String password = prefs.getString("password", null);
        if (username != null && password != null) {
            LoginAsyncTask loginAsyncTask = new LoginAsyncTask();
            loginAsyncTask.execute(username, password);
            timeout_time = 10000L;
        }
        handlerneeded = true;
        handler.post(timeout);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        finish();
    }


    private class LoginAsyncTask extends AsyncTask<String, Void, Void> {

        public final MediaType MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded");
        private String sessionCookie, username, password;

        @Override
        protected Void doInBackground(String... params) {
            CookieJar cookieJar = new CookieJar() {
                private List<Cookie> cookies;
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cook) {
                    try {
                        //Give me a cookie
                        cookies = new ArrayList<Cookie>();
                        Cookie c = cook.get(1);
                        sessionCookie = c.value();
                        cookies.add(c);
                        Log.d(TAG, "Cookies = " + sessionCookie);
                    }
                    catch (NullPointerException npe) {
                        npe.printStackTrace();
                        //This will happen.
                    }
                    catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        //This will also happen.
                    }
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    if(cookies != null) {
                        return cookies;
                    }
                    return new ArrayList<Cookie>();
                }
            };
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.cookieJar(cookieJar);
            OkHttpClient client = builder.build();

            username = params[0];
            password = params[1];
            String postBody = "username=" + username + "&password=" + password;

            Request request = new Request.Builder()
                    .url(loginPageUrl)
                    .post(RequestBody.create(MEDIA_TYPE, postBody))
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    Log.d(TAG, String.valueOf(response.code()));
                }
                else {
                    Log.d(TAG, String.valueOf(response.code()));
                    Log.d(TAG, response.body().string());
                    Log.d(TAG, response.toString());
                }
            }
            catch (IOException ioe){
                ioe.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            SharedPreferences prefs = Splash.this.getSharedPreferences("LoginDetails", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("MoodleSession", sessionCookie);
            editor.apply();
            handlerneeded = false;
            Log.d(TAG, "Username = " + username);
            Log.d(TAG, "Password = " + password);
            Log.d(TAG, "Session Cookie = " + sessionCookie);
            Toast.makeText(Splash.this, "Login successful", Toast.LENGTH_SHORT).show();
        }
    }
}