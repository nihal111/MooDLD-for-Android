package com.moodld.moodld;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity {

    private Button logout;
    private static final String TAG = "MainActivity";
    private String sessionCookie = null;
    private static final String mainPageUrl = "http://moodle.iitb.ac.in/";
    private ArrayList<String> downloadLinks = new ArrayList<String>();
    private ArrayList<String> courseNames = new ArrayList<String>();
    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWritePermission();

        SharedPreferences preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        String sessionCookie = preferences.getString("MoodleSession", null);
        if (sessionCookie != null) {
            JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
            jsoupAsyncTask.execute(mainPageUrl, sessionCookie);
        } else {
            Log.d(TAG, "Session cookie not present");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }

        logout = (Button) findViewById(R.id.logout);

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        });

        String file_url = "http://moodle.iitb.ac.in/pluginfile.php/53165/mod_resource/content/0/PH_108_Kumar_ppt.pdf";
        String address = Environment.getExternalStorageDirectory().toString() + "/nihal";

        final SharedPreferences prefs = MainActivity.this.getSharedPreferences("LoginDetails", MODE_PRIVATE);
        sessionCookie = prefs.getString("MoodleSession", null);
        Log.d(TAG, "MoodleSession=" + sessionCookie);

        //Download call
        new DownloadFileFromURL().execute(file_url, address);
    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected String doInBackground(String... params) {
            int count;
            try {
                URL url = new URL(params[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Cookie", "MoodleSession=" + sessionCookie);
                urlConnection.connect();


                //set the path where we want to save the file
                File SDCardRoot = Environment.getExternalStorageDirectory();
                //create a new file, to save the downloaded file
                File file = new File(SDCardRoot, "downloaded_file.pdf");
                Log.d(TAG, "Downloading " + url + " to " + file.getAbsolutePath());

                FileOutputStream fileOutput = new FileOutputStream(file);

                //Stream used for reading the data from the internet
                InputStream inputStream = urlConnection.getInputStream();

                //this is the total size of the file which we are downloading
                int totalSize = urlConnection.getContentLength();

                //create a buffer...
                byte[] buffer = new byte[1024];
                int bufferLength = 0;

                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);

                }
                //close the output stream when complete //
                fileOutput.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }


        @Override
        protected void onPostExecute(String file_url) {
            Log.d(TAG, "Download completed");
        }

    }

    public void getWritePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Write Storage permission not given.");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }
        else {
            Log.d(TAG, "Write storage permission given.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Write Storage Permission Granted.");
                }
                else {
                    Log.d(TAG, "Write Storage Permission denied.");
                    Toast.makeText(MainActivity.this, "You need to allow this permission to download files!", Toast.LENGTH_SHORT).show();
                    getWritePermission();
                }
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

}


