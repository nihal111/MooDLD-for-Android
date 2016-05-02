package com.moodld.moodld;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    private Button logout;
    private static final String TAG="MainActivity";
    private String sessionCookie = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        
        logout = (Button) findViewById(R.id.logout);

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = getSharedPreferences("LoginDetails", MODE_PRIVATE).edit();
                editor.putString("username", null);
                editor.putString("password", null);
                editor.putString("MoodleSession", null);
                editor.commit();
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
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... params) {
            int count;
            try {
                URL url = new URL(params[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Cookie", "MoodleSession="+sessionCookie);
                urlConnection.connect();


                //set the path where we want to save the file
                File SDCardRoot = Environment.getExternalStorageDirectory();
                //create a new file, to save the downloaded file
                File file = new File(SDCardRoot,"downloaded_file.pdf");

                FileOutputStream fileOutput = new FileOutputStream(file);

                //Stream used for reading the data from the internet
                InputStream inputStream = urlConnection.getInputStream();

                //this is the total size of the file which we are downloading
                int totalSize = urlConnection.getContentLength();

                //create a buffer...
                byte[] buffer = new byte[1024];
                int bufferLength = 0;

                while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
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
}


