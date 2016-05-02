package com.moodld.moodld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.context.IconicsLayoutInflater;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class Preferences extends AppCompatActivity {

    private static final String TAG = "Preferences";
    private final int PICKFILE_REQUEST_CODE = 0;
    private static final String mainPageUrl = "http://moodle.iitb.ac.in/";
    private ArrayList<String> downloadLinks = new ArrayList<String>();
    private ArrayList<String> courseNames = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory(getLayoutInflater(), new IconicsLayoutInflater(getDelegate()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        ImageButton select_all = (ImageButton) findViewById(R.id.select_all);
        ImageButton deselect_all = (ImageButton) findViewById(R.id.deselect_all);
        try {
            select_all.setBackground(new IconicsDrawable(this)
                    .icon(FontAwesome.Icon.faw_check_square_o)
                    .color(Color.BLACK)
                    .sizeDp(24));
            deselect_all.setBackground(new IconicsDrawable(this)
                    .icon(FontAwesome.Icon.faw_times)
                    .color(Color.BLACK)
                    .sizeDp(24));
        } catch (NullPointerException npe) {
            Log.d(TAG, "Icons not found");
        }

        SharedPreferences preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        String sessionCookie = preferences.getString("MoodleSession", null);
        if (sessionCookie != null) {
            JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
            jsoupAsyncTask.execute(mainPageUrl, sessionCookie);
        } else {
            Log.d(TAG, "Session cookie not present");
            Intent intent = new Intent(Preferences.this, LoginActivity.class);
            startActivity(intent);
        }

    }

    public void RootDirectorySelect(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICKFILE_REQUEST_CODE:
                String Fpath = data.getDataString();
                Log.d(TAG, Fpath);
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
                    courseNames.add(link.text());
                }
            }
            Log.d(TAG, downloadLinks.toString());
            Log.d(TAG, courseNames.toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PrefListAdapter adapter = new PrefListAdapter(courseNames, Preferences.this);
                    ListView listView = (ListView) findViewById(R.id.listView);
                    listView.setAdapter(adapter);
                }
            });
        }
    }
}
