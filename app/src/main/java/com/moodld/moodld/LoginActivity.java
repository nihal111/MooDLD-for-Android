package com.moodld.moodld;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class LoginActivity extends AppCompatActivity {

    private final String loginPageUrl = "http://moodle.iitb.ac.in/login/index.php";
    private final String mainPageUrl = "http://moodle.iitb.ac.in/";
    private final String TAG = "LoginActivity";
    private final String PREFS_NAME = "LoginDetails";
    private boolean details_correct = false;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);



        Button login = (Button) findViewById(R.id.login);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText ed1 = (EditText) findViewById(R.id.name);
                EditText ed2 = (EditText) findViewById(R.id.pass);
                try {
                    String username = URLEncoder.encode(ed1.getText().toString(), "UTF-8");
                    String password = URLEncoder.encode(ed2.getText().toString(), "UTF-8");
                    Log.d("Login", "Username: " + username + " Password: " + password);

                    String[] login_details = {username, password};
                    LoginAsyncTask loginAsyncTask = new LoginAsyncTask();
                    loginAsyncTask.execute(login_details);

                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "UnsupportedEncodingException");
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                }
            }
        });
    }

    private class LoginAsyncTask extends AsyncTask<String, Void, Void> {

        public final MediaType MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded");
        private String sessionCookie, username, password;

        @Override
        public void onPreExecute () {
            dialog = new ProgressDialog(LoginActivity.this);
            dialog.setTitle("Please wait");
            dialog.setMessage("Logging you in...");
            dialog.setCancelable(false);
            dialog.show();
        }

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
                    } catch (NullPointerException npe) {
                        npe.printStackTrace();
                        //This will happen.
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        //This will also happen.
                    }
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    if (cookies != null) {
                        return cookies;
                    }
                    return new ArrayList<Cookie>();
                }
            };
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.cookieJar(cookieJar);
            builder.addNetworkInterceptor(new LoggingInterceptor());
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
                } else {
                    Log.d(TAG, String.valueOf(response.code()));
                    Log.d(TAG, response.body().string());
                    Log.d(TAG, response.toString());
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (details_correct) {
                SaveLoginDetails(username, password, sessionCookie);
                dialog.dismiss();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    private void SaveLoginDetails(String username, String password, String MoodleSession) {
        SharedPreferences preferences = LoginActivity.this.getSharedPreferences("LoginDetails", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        editor.putString("MoodleSession", MoodleSession);
        editor.apply();
        Log.d(TAG, "Saved Login Details.");
    }

    //For debugging purposes
    class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request request = chain.request();

            long t1 = System.nanoTime();
            Log.d(TAG, String.format("Sending request %s on %s\n%s\n%s\n%s",
                    request.url(), chain.connection(), request.method(), request.body(), request.headers()));

            Response response = chain.proceed(request);

            long t2 = System.nanoTime();
            Log.d(TAG, String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers()));

            if (request.url().toString().equals(loginPageUrl)) {
                if (response.header("Location", null) == null) {
                    Log.d(TAG, "Login details INCORRECT.");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Invalid Username or Password!", Toast.LENGTH_LONG).show();
                        }
                    });
                    details_correct = false;
                } else if (response.header("Location", null).startsWith(loginPageUrl + "?testsession")) {
                    Log.d(TAG, "Login details CORRECT");
                    details_correct = true;
                }
            }

            return response;
        }
    }

}

