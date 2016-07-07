package com.moodld.moodld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DLD_files extends AppCompatActivity {

    private static final String TAG = "DLD_files";
    private final int REQUEST_DIRECTORY = 0;
    private static final String mainPageUrl = "http://moodle.iitb.ac.in/";
    private String sessionCookie;
    ArrayList<Course> CourseList = new ArrayList<Course>();
    SharedPreferences coursePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dld_files);

        SharedPreferences preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        sessionCookie = preferences.getString("MoodleSession", null);
        if (sessionCookie == null) {
            Log.d(TAG, "Session cookie not present");
            Intent intent = new Intent(DLD_files.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        coursePrefs = getSharedPreferences("CourseList", MODE_PRIVATE);
        String json = coursePrefs.getString("CourseList", null);
        if (json != null) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Course>>(){}.getType();
            CourseList = (ArrayList<Course>) gson.fromJson(json, listType);
            Log.d(TAG, "Courses fetched from saved data: " + CourseList.toString());
            downloadFromCourses();
        }
        else {
            Log.d(TAG, "No CourseList saved. Redirecting to Preferences.");
            Intent intent = new Intent(DLD_files.this, Preferences.class);
            startActivity(intent);
            finish();
        }
    }

    private void downloadFromCourses() {
        for (int i=0; i< CourseList.size(); i++) {
            Course course = CourseList.get(i);
            Log.d(TAG, course.getName() + ": " + course.getUrl());
            JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
            jsoupAsyncTask.execute(course.getUrl(), sessionCookie);
            Log.d(TAG, course.getName());
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
//                htmlDocument = Jsoup.parse("<div class=\"summary\"></div><ul class=\"section img-text\"><li class=\"activity assign modtype_assign \" id=\"module-21233\"><div><div class=\"mod-indent-outer\"><div class=\"mod-indent\"></div><div><div class=\"activityinstance\"><a class=\"\" onclick=\"\" href=\"http://moodle.iitb.ac.in/mod/assign/view.php?id=21233\"><img src=\"http://moodle.iitb.ac.in/theme/image.php/clean/assign/1444075825/icon\" class=\"iconlarge activityicon\" alt=\" \" role=\"presentation\" /><span class=\"instancename\">Assignment 6 Submission Link: Due April 4, 2355 hours.</span></a></div></div></div></div></li><li class=\"activity resource modtype_resource \" id=\"module-22063\"><div><div class=\"mod-indent-outer\"><div class=\"mod-indent\"></div><div><div class=\"activityinstance\"><a class=\"\" onclick=\"\" href=\"http://moodle.iitb.ac.in/mod/resource/view.php?id=22063\"><img src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/f/pdf-24\" class=\"iconlarge activityicon\" alt=\" \" role=\"presentation\" /><span class=\"instancename\">Assignment 4 Solutions<span class=\"accesshide \" > File</span></span></a></div></div></div></div></li></ul></div></li><li id=\"section-15\" class=\"section main clearfix\" role=\"region\" aria-label=\"11 April - 17 April\"><div class=\"left side\"><img width=\"1\" height=\"1\" class=\"spacer\" alt=\"\" title=\"\" src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/spacer\" /></div><div class=\"right side\"><img width=\"1\" height=\"1\" class=\"spacer\" alt=\"\" title=\"\" src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/spacer\" /></div><div class=\"content\"><h3 class=\"sectionname\">11 April - 17 April</h3><div class=\"summary\"></div><ul class=\"section img-text\"></ul></div></li><li id=\"section-16\" class=\"section main clearfix\" role=\"region\" aria-label=\"18 April - 24 April\"><div class=\"left side\"><img width=\"1\" height=\"1\" class=\"spacer\" alt=\"\" title=\"\" src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/spacer\" /></div><div class=\"right side\"><img width=\"1\" height=\"1\" class=\"spacer\" alt=\"\" title=\"\" src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/spacer\" /></div><div class=\"content\"><h3 class=\"sectionname\">18 April - 24 April</h3><div class=\"summary\"></div><ul class=\"section img-text\"><li class=\"activity resource modtype_resource \" id=\"module-22586\"><div><div class=\"mod-indent-outer\"><div class=\"mod-indent\"></div><div><div class=\"activityinstance\"><a class=\"\" onclick=\"\" href=\"http://moodle.iitb.ac.in/mod/resource/view.php?id=22586\"><img src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/f/pdf-24\" class=\"iconlarge activityicon\" alt=\" \" role=\"presentation\" /><span class=\"instancename\">Endsem solutions.<span class=\"accesshide \" > File</span></span></a></div></div></div></div></li><li class=\"activity assign modtype_assign \" id=\"module-22589\"><div><div class=\"mod-indent-outer\"><div class=\"mod-indent\"></div><div><div class=\"activityinstance\"><a class=\"\" onclick=\"\" href=\"http://moodle.iitb.ac.in/mod/assign/view.php?id=22589\"><img src=\"http://moodle.iitb.ac.in/theme/image.php/clean/assign/1444075825/icon\" class=\"iconlarge activityicon\" alt=\" \" role=\"presentation\" /><span class=\"instancename\">Endsem <span class=\"accesshide \" > Assignment</span></span></a></div></div></div></div></li></ul></div></li><li id=\"section-17\" class=\"section main clearfix\" role=\"region\" aria-label=\"25 April - 1 May\"><div class=\"left side\"><img width=\"1\" height=\"1\" class=\"spacer\" alt=\"\" title=\"\" src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/spacer\" /></div><div class=\"right side\"><img width=\"1\" height=\"1\" class=\"spacer\" alt=\"\" title=\"\" src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/spacer\" /></div><div class=\"content\"><h3 class=\"sectionname\">25 April - 1 May</h3><div class=\"summary\"></div><ul class=\"section img-text\"></ul></div></li><li id=\"section-18\" class=\"section main clearfix\" role=\"region\" aria-label=\"2 May - 8 May\"><div class=\"left side\"><img width=\"1\" height=\"1\" class=\"spacer\" alt=\"\" title=\"\" src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/spacer\" /></div><div class=\"right side\"><img width=\"1\" height=\"1\" class=\"spacer\" alt=\"\" title=\"\" src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/spacer\" /></div><div class=\"content\"><h3 class=\"sectionname\">2 May - 8 May</h3><div class=\"summary\"></div><ul class=\"section img-text\"><li class=\"activity resource modtype_resource \" id=\"module-22857\"><div><div class=\"mod-indent-outer\"><div class=\"mod-indent\"></div><div><div class=\"activityinstance\"><a class=\"\" onclick=\"\" href=\"http://moodle.iitb.ac.in/mod/resource/view.php?id=22857\"><img src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/f/pdf-24\" class=\"iconlarge activityicon\" alt=\" \" role=\"presentation\" /><span class=\"instancename\">Assignment 5 Solutions.<span class=\"accesshide \" > File</span></span></a></div></div></div></div></li><li class=\"activity resource modtype_resource \" id=\"module-22858\"><div><div class=\"mod-indent-outer\"><div class=\"mod-indent\"></div><div><div class=\"activityinstance\"><a class=\"\" onclick=\"\" href=\"http://moodle.iitb.ac.in/mod/resource/view.php?id=22858\"><img src=\"http://moodle.iitb.ac.in/theme/image.php/clean/core/1444075825/f/pdf-24\" class=\"iconlarge activityicon\" alt=\" \" role=\"presentation\" /><span class=\"instancename\">Assignment 6: Divider implementation.<span class=\"accesshide \" > File</span>");
                links = htmlDocument.select("a[href]");
//                Log.d(TAG, links.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Course course;
            for (Element link : links) {
                if (!link.attr("abs:href").startsWith(mainPageUrl + "logout.php") || !link.attr("abs:href").startsWith(mainPageUrl + "mod/forum") || !link.attr("abs:href").startsWith(mainPageUrl + "my") || !link.attr("abs:href").startsWith(mainPageUrl + "user") || !link.attr("abs:href").startsWith(mainPageUrl + "badges") || !link.attr("abs:href").startsWith(mainPageUrl + "my") || !link.attr("abs:href").startsWith(mainPageUrl + "user") || !link.attr("abs:href").startsWith(mainPageUrl + "calendar")|| !link.attr("abs:href").startsWith(mainPageUrl + "my") || !link.attr("abs:href").startsWith(mainPageUrl + "user") || !link.attr("abs:href").startsWith(mainPageUrl + "grade")|| !link.attr("abs:href").startsWith(mainPageUrl + "my") || !link.attr("abs:href").startsWith(mainPageUrl + "user") || !link.attr("abs:href").startsWith(mainPageUrl + "message")) {
                    Log.d(TAG, link.text() + ": " + link.attr("abs:href"));
                }
            }

        }
    }
}