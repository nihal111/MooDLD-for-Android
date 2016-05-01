package com.moodld.moodld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Button logout;
    private static final String TAG="MainActivity";
    private static final String mainPageUrl = "http://moodle.iitb.ac.in/";
    private ArrayList<String> downloadLinks = new ArrayList<String>();
    private ArrayList<String> courseNames = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        String sessionCookie = preferences.getString("MoodleSession", null);
        if(sessionCookie != null) {
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
                if(link.attr("abs:href").startsWith("http://moodle.iitb.ac.in/course")) {
                    downloadLinks.add(link.attr("abs:href"));
                    courseNames.add(link.text().substring(0,6));
                }
            }
            Log.d(TAG, downloadLinks.toString());
            Log.d(TAG, courseNames.toString());
        }
    }

}
