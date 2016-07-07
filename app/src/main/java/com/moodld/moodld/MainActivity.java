package com.moodld.moodld;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Manifest;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private Button logout, dld, preferences;
    private static final String TAG = "MainActivity";
    private String sessionCookie = null;
    private static final String mainPageUrl = "http://moodle.iitb.ac.in/";
    private ArrayList<String> downloadLinks = new ArrayList<String>();
    private ArrayList<String> courseNames = new ArrayList<String>();
    private final int WRITE_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        sessionCookie = prefs.getString("MoodleSession", null);
        if (sessionCookie != null) {
            JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
            jsoupAsyncTask.execute(mainPageUrl, sessionCookie);
        } else {
            Log.d(TAG, "Session cookie not present");
            logout();
        }

        logout = (Button) findViewById(R.id.logout);
        dld = (Button) findViewById(R.id.dld);
        preferences = (Button) findViewById(R.id.preferences);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);


        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        dld.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    AskWritePermission();
                } else {
                    dldfiles();
                }
            }
        });

        preferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "Logged out.");
                Intent intent = new Intent(MainActivity.this, Preferences.class);
                startActivity(intent);
                finish();

            }
        });


    }

    //onCLick of "Logout" button
    private void logout() {
        SharedPreferences.Editor editor = getSharedPreferences("LoginDetails", MODE_PRIVATE).edit();
        editor.putString("username", null);
        editor.putString("password", null);
        editor.putString("MoodleSession", null);
        editor.apply();
        Log.d(TAG, "Logged out.");
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    //onClick of "DLD Files" button
    private void dldfiles() {
        Log.d(TAG, "DLD Files");
        Intent intent = new Intent(MainActivity.this, DLD_files.class);
        startActivity(intent);
//        String file_url = "http://moodle.iitb.ac.in/pluginfile.php/53165/mod_resource/content/0/PH_108_Kumar_ppt.pdf";
//        String address = "downloaded_file.pdf";
//        //Download call
//        new DownloadFileFromURL().execute(file_url, address);

    }

    public void AskWritePermission(){
        // Requesting Storage Permission
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Snackbar.make(this.getWindow().getDecorView().findViewById(android.R.id.content), "Storage access is required to download files to your device.",
                        Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Request the permission
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                WRITE_EXTERNAL_STORAGE);
                    }
                }).show();

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // task you need to do.
                    dldfiles();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Snackbar.make(this.getWindow().getDecorView().findViewById(android.R.id.content), "Allow storage access to proceed with downloading files.",
                            Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {public void onClick(View view) {}})
                            .show();
                }

                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private class JsoupAsyncTask extends AsyncTask<String, String, Void> {

        Elements links;
        Document htmlDocument;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                htmlDocument = Jsoup.connect(params[0]).cookie("MoodleSession", params[1]).get();
                links = htmlDocument.select("a[href]");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            for (Element link : links) {
                if (link.attr("abs:href").startsWith(mainPageUrl + "course")) {
                    downloadLinks.add(link.attr("abs:href"));
                    courseNames.add(link.text().substring(0, 6));
                } else if (link.attr("abs:href").startsWith(mainPageUrl + "user/profile.php")) {
                    final String myname = link.text();
                    Log.d(TAG, myname);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView nameTV = (TextView) findViewById(R.id.nameTextView);
                            nameTV.setText("Welcome, " + myname);
                        }
                    });
                }
            }
            Log.d(TAG, downloadLinks.toString());
            Log.d(TAG, courseNames.toString());
        }
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


            return response;
        }
    }

}


