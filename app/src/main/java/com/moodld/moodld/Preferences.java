package com.moodld.moodld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;
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
import java.util.Arrays;
import java.util.List;

public class Preferences extends AppCompatActivity {

    private static final String TAG = "Preferences";
    private final int REQUEST_DIRECTORY = 0;
    private static final String mainPageUrl = "http://moodle.iitb.ac.in/";
    private ListView listView;
    private ArrayAdapter<Course> listAdapter;
    private TextView root_dir_value;
    ArrayList<Course> CourseList = new ArrayList<Course>();
    SharedPreferences coursePrefs;
    String rootDir, sessionCookie;
    Integer pending = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Typeface font = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
        LayoutInflaterCompat.setFactory(getLayoutInflater(), new IconicsLayoutInflater(getDelegate()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        root_dir_value = (TextView) findViewById(R.id.root_dir_value);
        coursePrefs = getSharedPreferences("CourseList", MODE_PRIVATE);
        // Find the ListView resource.
        listView = (ListView) findViewById(R.id.listView);

        rootDir = coursePrefs.getString("rootDir", null);
        if (rootDir == null) {
            rootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            root_dir_value.setText(rootDir);
            Log.d(TAG, "No saved rootDir. rootDir is now" + rootDir);
        } else {
            root_dir_value.setText(rootDir);
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

        Button select_all = (Button) findViewById(R.id.select_all);
        Button deselect_all = (Button) findViewById(R.id.deselect_all);
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
                root_dir_value.setText(data
                        .getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR));
                rootDir = data
                        .getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
            } else {
                root_dir_value.setText(rootDir);
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
                    Log.d(TAG, links.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Course course;
                for (Element link : links) {
                    if (link.attr("abs:href").startsWith(mainPageUrl + "course")) {
                        course = new Course(link.text(), link.attr("abs:href"), rootDir + "/" + link.text().substring(0, 6));
                        Boolean flag = false;
                        for (int i = 0; i < CourseList.size(); i++) {
                            if (CourseList.get(i).getUrl().equals(course.getUrl())) {
                                flag = true;
                            }
                        }
                        if (flag == false) {
                            JsoupAsyncTaskFetchNewsForumUrl nf = new JsoupAsyncTaskFetchNewsForumUrl();
                            nf.execute(course, sessionCookie);
                            pending ++;
                            Log.d(TAG, "Find nf url for " + pending + " " + course.getName());
                        }
                    }
                }

            }
    }

    private class JsoupAsyncTaskFetchNewsForumUrl extends AsyncTask<Object, String, Void> {

        Course course;
        Elements links;
        Document htmlDocument;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Object... params) {
            try {
                course = (Course)params[0];
                htmlDocument = Jsoup.connect(course.getUrl()).cookie("MoodleSession", (String) params[1]).get();
                links = htmlDocument.select("a[href]");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            for (Element link : links) {
                    if (link.attr("abs:href").startsWith(mainPageUrl + "mod/forum/view.php?id=")) {
                        Log.d("Inside course link: ", link.toString());
                        course.setNewsForumUrl(link.attr("abs:href"));
                        CourseList.add(course);
                        break;
                    }
                }
            pending --;
            if (pending == 0) {
                Log.d(TAG, "CourseList after merging fetched and saved data: " + CourseList.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listAdapter = new CourseArrayAdapter(Preferences.this, CourseList);
                        listView.setAdapter(listAdapter);
                    }
                });
            }
            }
        }
}
