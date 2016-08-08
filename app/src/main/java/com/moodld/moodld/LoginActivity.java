package com.moodld.moodld;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
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
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        final SharedPreferences prefs = LoginActivity.this.getSharedPreferences("LoginDetails", MODE_PRIVATE);
        Button login = (Button) findViewById(R.id.login);
        final EditText ed1 = (EditText) findViewById(R.id.name);
        final EditText ed2 = (EditText) findViewById(R.id.pass);

        /*
        final String color = prefs.getString("color", null);
        final String contrast = prefs.getString("contrast", null);
        RelativeLayout li = (RelativeLayout) findViewById(R.id.LoginBackground);

        if (li != null) {
            li.setBackgroundColor(Color.parseColor(color));
        }

        if (login != null) {
            login.setBackgroundColor(Color.parseColor(contrast));
        }


        if (ed1 != null && ed2 != null && login != null) {
            ed1.getBackground().setColorFilter(Color.parseColor(contrast), PorterDuff.Mode.SRC_ATOP);

        ed2.getBackground().setColorFilter(Color.parseColor(contrast), PorterDuff.Mode.SRC_ATOP);
        }
        */

        if (login != null) {
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


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
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Login Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.moodld.moodld/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Login Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.moodld.moodld/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    private class LoginAsyncTask extends AsyncTask<String, Void, Void> {

        public final MediaType MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded");
        private String sessionCookie, username, password;

        @Override
        public void onPreExecute() {
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
                    Log.d(TAG, response.body().string());
                } else {
                    Log.d(TAG, String.valueOf(response.code()));
                    Log.d(TAG, response.toString());
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.wtf(TAG, "Login request failed. Internet problems (?)");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Please check your internet connection and try again", Toast.LENGTH_LONG).show();
                    }
                });
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
            else {
                dialog.dismiss();
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
        public Response intercept(Chain chain) throws IOException {
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
                    if (response.body().string().contains("Log in to the site")) {
                        Log.d(TAG, "Login details INCORRECT.");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Invalid Username or Password!", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Log.d(TAG, "Cannot access Moodle.");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Cannot access Moodle. Check if you are on insti network!", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
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

