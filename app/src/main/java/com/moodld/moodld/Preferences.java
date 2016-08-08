package com.moodld.moodld;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mikepenz.iconics.context.IconicsLayoutInflater;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Preferences extends AppCompatActivity {

    private static final String TAG = "Preferences";
    private static final String mainPageUrl = "http://moodle.iitb.ac.in/";
    private final int REQUEST_DIRECTORY = 0, MAX_RETRIES = 5;
    ArrayList<Course> CourseList = new ArrayList<Course>();
    SharedPreferences coursePrefs;
    String rootDir, sessionCookie;
    Integer pending = 0;
    ProgressDialog dialog;
    private ListView listView;
    private ArrayAdapter<Course> listAdapter;
    private TextView root_dir_value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Typeface font = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
        LayoutInflaterCompat.setFactory(getLayoutInflater(), new IconicsLayoutInflater(getDelegate()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        final SharedPreferences prefs = Preferences.this.getSharedPreferences("LoginDetails", MODE_PRIVATE);

        Button select_all = (Button) findViewById(R.id.select_all);
        Button deselect_all = (Button) findViewById(R.id.deselect_all);
        Button root_dir_button = (Button) findViewById(R.id.root_dir_button);
        Button save = (Button) findViewById(R.id.save_button);
        RelativeLayout PreferencesBackground = (RelativeLayout) findViewById(R.id.PreferencesBackground);

        /* Color changes disabled
        final String color = prefs.getString("color", null);
        final String contrast = prefs.getString("contrast", null);
        PreferencesBackground.setBackgroundColor(Color.parseColor(color));
        root_dir_button.setBackgroundColor(Color.parseColor(contrast));
        select_all.setBackgroundColor(Color.parseColor(contrast));
        deselect_all.setBackgroundColor(Color.parseColor(contrast));
        save.setBackgroundColor(Color.parseColor(contrast));
        */

        root_dir_value = (TextView) findViewById(R.id.root_dir_value);
        coursePrefs = getSharedPreferences("CourseList", MODE_PRIVATE);
        // Find the ListView resource.
        listView = (ListView) findViewById(R.id.listView);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getInt("status") == 0) {
                Toast.makeText(getApplicationContext(), "Please set your Preferences first!", Toast.LENGTH_LONG).show();
            }
            if (extras.getInt("status") == 1) {
                Toast.makeText(getApplicationContext(), "No courses are selected. Please select courses and save!", Toast.LENGTH_LONG).show();
            }
        }

        dialog = new ProgressDialog(this);
        dialog.setTitle("Please wait");
        dialog.setMessage("Loading courses...");
        dialog.setCancelable(false);
        dialog.show();

        rootDir = coursePrefs.getString("rootDir", null);
        if (rootDir == null) {
            rootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            root_dir_value.setText(rootDir + "/MooDLD");
            Log.d(TAG, "No saved rootDir. rootDir is now" + rootDir);
        } else {
            root_dir_value.setText(rootDir + "/MooDLD");
        }
        // When item is tapped, toggle checked properties of CheckBox and Planet.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View item,
                                    int position, long id) {
                Course course = listAdapter.getItem(position);
                course.toggleChecked();
                CourseViewHolder viewHolder = (CourseViewHolder) item.getTag();
                viewHolder.getCheckBox().setChecked(course.isChecked());
                Log.d(TAG, CourseList.toString());
            }
        });

        try {
            select_all.setTypeface(font);
            deselect_all.setTypeface(font);
        } catch (NullPointerException e) {
            Log.e(TAG, e.getStackTrace().toString());
        }

        String json = coursePrefs.getString("CourseList", null);
        if (json != null) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Course>>() {
            }.getType();
            CourseList = (ArrayList<Course>) gson.fromJson(json, listType);
            Log.d(TAG, "Courses fetched from saved data: " + CourseList.toString());
        }

        SharedPreferences preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        sessionCookie = preferences.getString("MoodleSession", null);
        if (sessionCookie != null) {
            JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
            jsoupAsyncTask.execute(mainPageUrl, sessionCookie);
        } else {
            Log.d(TAG, "Session cookie not present");
            Intent intent = new Intent(Preferences.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
//        JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
//        jsoupAsyncTask.execute(mainPageUrl, sessionCookie);

    }

    public void SelectAll(View view) {
        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
            Course course = listAdapter.getItem(i);
            course.setChecked(Boolean.TRUE);
        }
        listView.setAdapter(listAdapter);
    }

    public void DeselectAll(View view) {
        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
            Course course = listAdapter.getItem(i);
            course.setChecked(Boolean.FALSE);
        }
        listView.setAdapter(listAdapter);
    }

    public void Save(View view) {
        SharedPreferences.Editor prefsEditor = coursePrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(CourseList);
        prefsEditor.putString("CourseList", json);
        prefsEditor.putString("rootDir", rootDir);
        prefsEditor.commit();
        Log.d(TAG, json);
        Intent intent = new Intent(Preferences.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void RootDirectorySelect(View view) {
        final Intent chooserIntent = new Intent(Preferences.this, DirectoryChooserActivity.class);

        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("DirChooserSample")
                .allowReadOnlyDirectory(true)
                .allowNewDirectoryNameModification(true)
                .build();

        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);

// REQUEST_DIRECTORY is a constant integer to identify the request, e.g. 0
        startActivityForResult(chooserIntent, REQUEST_DIRECTORY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_DIRECTORY) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                rootDir = data
                        .getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                root_dir_value.setText(rootDir + "/MooDLD");
            } else {
                root_dir_value.setText(rootDir + "/MooDLD");
            }
        }
    }

    private void onLoadComplete() {
        Log.d(TAG, "CourseList after merging fetched and saved data: " + CourseList.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
                listAdapter = new CourseArrayAdapter(Preferences.this, CourseList);
                listView.setAdapter(listAdapter);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onDestroy();
    }

    private class JsoupAsyncTask extends AsyncTask<String, String, Void> {
        String mainPageUrl, sessionCookie;
        Elements links = new Elements();
        Document htmlDocument = new Document("filler data");

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                int retries = 0;
                mainPageUrl = params[0];
                sessionCookie = params[1];
                while (links.size() == 0 && retries < MAX_RETRIES) {
                    htmlDocument = Jsoup.connect(mainPageUrl).cookie("MoodleSession", sessionCookie).get();
                    links = htmlDocument.select("a[href]");
                    Log.d(TAG, String.valueOf(links.size()));
                    retries += 1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (links != null) {
                Course course;
                for (Element link : links) {
                    if (link.attr("abs:href").startsWith(mainPageUrl + "course")) {
                        course = new Course(link.text(), link.attr("abs:href"), rootDir + "/MooDLD/" + link.text().substring(0, 6));
                        Boolean flag = false;
                        for (int i = 0; i < CourseList.size(); i++) {
                            if (CourseList.get(i).getUrl().equals(course.getUrl())) {
                                flag = true;
                            }
                        }
                        if (flag == false) {
                            JsoupAsyncTaskFetchNewsForumUrl nf = new JsoupAsyncTaskFetchNewsForumUrl();
                            nf.execute(course, sessionCookie);
                            pending++;
                            Log.d(TAG, "Find nf url for " + pending + " " + course.getName());
                        }
                    }
                }

                if (pending == 0) {
                    onLoadComplete();
                }

            } else {
                JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
                jsoupAsyncTask.execute(mainPageUrl, sessionCookie);
            }
        }
    }

    private class JsoupAsyncTaskFetchNewsForumUrl extends AsyncTask<Object, String, Void> {

        Course course;
        String sessionCookie;
        Elements links = new Elements();
        Document htmlDocument = new Document("filler data");

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Object... params) {
            try {
                course = (Course) params[0];
                sessionCookie = (String) params[1];
                int retries = 0;
                while (links.size() == 0 && retries < MAX_RETRIES) {
                    htmlDocument = Jsoup.connect(course.getUrl()).cookie("MoodleSession", sessionCookie).get();
                    links = htmlDocument.select("a[href]");
                    Log.d(TAG, String.valueOf(links.size()));
                    retries += 1;
                }
                if (links.size() == 0) {
                    throw new IOException();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Intent noInternetIntent = new Intent(Preferences.this, MainActivity.class);
                noInternetIntent.putExtra("noInternet", true);
                startActivity(noInternetIntent);
                finish();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (links != null) {
                for (Element link : links) {
                    if (link.attr("abs:href").startsWith(mainPageUrl + "mod/forum/view.php?id=")) {
                        Log.d("Inside course link: ", link.toString());
                        course.setNewsForumUrl(link.attr("abs:href"));
                        CourseList.add(course);
                        break;
                    }
                }
                pending--;
                if (pending == 0) {
                    onLoadComplete();
                }
            } else {
                JsoupAsyncTaskFetchNewsForumUrl nf = new JsoupAsyncTaskFetchNewsForumUrl();
                nf.execute(course, sessionCookie);
            }
        }
    }
}
