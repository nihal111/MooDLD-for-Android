package com.moodld.moodld;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DLD_files extends AppCompatActivity {

    private static final String TAG = "DLD_files";
    private final int REQUEST_DIRECTORY = 0, NOTIFICATION_ID = 1;
    private static final String mainPageUrl = "http://moodle.iitb.ac.in/";
    String rootDir;
    private String sessionCookie;
    ArrayList<Course> CourseList = new ArrayList<Course>();
    ArrayList<String> nfthreads = new ArrayList<String>();
    ArrayList<String> fileNames = new ArrayList<String>();
    SharedPreferences coursePrefs;
    private ProgressBar progressBar;
    int downloadsRemaining = 0;
    TextView log;
    NotificationManager notifManager;
    NotificationCompat.Builder notifBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dld_files);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        log = (TextView) findViewById(R.id.log);

        /**
         * Getting MoodleSession cookie from SharedPreferences
         */
        SharedPreferences preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        sessionCookie = preferences.getString("MoodleSession", null);
        if (sessionCookie == null) {
            Log.d(TAG, "Session cookie not present");
            Intent intent = new Intent(DLD_files.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        /**
         * Getting CourseList from SharedPreferences
         */
        coursePrefs = getSharedPreferences("CourseList", MODE_PRIVATE);
        String json = coursePrefs.getString("CourseList", null);
        if (json != null) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Course>>() {
            }.getType();
            CourseList = (ArrayList<Course>) gson.fromJson(json, listType);
            Log.d(TAG, "Courses fetched from saved data: " + CourseList.toString());
            log.append("Fetching courses from preferences.\n\n");
            scrollToBottom();
            downloadFromCourses();
        } else {
            Log.d(TAG, "No CourseList saved. Redirecting to Preferences.");
            Intent intent = new Intent(DLD_files.this, Preferences.class);
            startActivity(intent);
            finish();
        }
        // Getting root directory from shared preferences
        rootDir = coursePrefs.getString("rootDir", Environment.getExternalStorageDirectory().getPath());

        /**
         * Initialising Notifications setup for progress update
         */
        notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifBuilder = new NotificationCompat.Builder(this);
        notifBuilder.setContentTitle("MooDLD Download")
                .setSmallIcon(R.drawable.logo)
                .setOngoing(true);
    }

    /**
     *
     */
    private void downloadFromCourses() {
        for (int i = 0; i < CourseList.size(); i++) {
            Course course = CourseList.get(i);
            if (course.isChecked()) {
                Log.d(TAG, course.getName() + ": " + course.getUrl());
                JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
                jsoupAsyncTask.execute(course, sessionCookie);
                JsoupAsyncTaskFetchNf jsoupAsyncTaskFetchNf = new JsoupAsyncTaskFetchNf();
                jsoupAsyncTaskFetchNf.execute(course, sessionCookie);
                Log.d(TAG, course.getName());
            }
        }
    }


    private class JsoupAsyncTask extends AsyncTask<Object, String, Void> {

        Course course;
        Elements links;
        Document htmlDocument;
        ArrayList<Element> linksToDownload;

        @Override
        protected void onPreExecute() {
            linksToDownload = new ArrayList<>();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Object... params) {
            try {
                course = (Course) params[0];
                htmlDocument = Jsoup.connect(course.getUrl()).cookie("MoodleSession", (String) params[1]).get();
                links = htmlDocument.select("a[href]");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            log.append("Downloading " + course.getName() + " files.\n\n");
            scrollToBottom();
            //Iterate over links and call DownloadFileFromUrl
            for (Element link : links) {
//                if (!link.attr("abs:href").startsWith(mainPageUrl + "logout.php") && !link.attr("abs:href").startsWith(mainPageUrl + "mod/forum") && !link.attr("abs:href").startsWith(mainPageUrl + "my") && !link.attr("abs:href").startsWith(mainPageUrl + "user") && !link.attr("abs:href").startsWith(mainPageUrl + "badges") && !link.attr("abs:href").startsWith(mainPageUrl + "my") && !link.attr("abs:href").startsWith(mainPageUrl + "user") && !link.attr("abs:href").startsWith(mainPageUrl + "calendar")&& !link.attr("abs:href").startsWith(mainPageUrl + "my") && !link.attr("abs:href").startsWith(mainPageUrl + "user") && !link.attr("abs:href").startsWith(mainPageUrl + "grade")&& !link.attr("abs:href").startsWith(mainPageUrl + "my") && !link.attr("abs:href").startsWith(mainPageUrl + "user") && !link.attr("abs:href").startsWith(mainPageUrl + "message")) {
                if (link.attr("abs:href").startsWith(mainPageUrl + "mod/resource")) {
                    linksToDownload.add(link);
                }
            }
            downloadsRemaining += linksToDownload.size();
            for (Element link : linksToDownload) {
                DownloadFileFromURL download = new DownloadFileFromURL();
                download.execute(link.attr("abs:href"), course.getName().substring(0, 6) + "/" + link.text());
//                    downloadLinks.add(course.getUrl());
//                    fileNames.add(course.getPath() + link.text());
                Log.d(TAG, link.text() + ": " + link.attr("abs:href"));
            }
        }
    }


    private class JsoupAsyncTaskFetchNf extends AsyncTask<Object, String, Void> {

        Course course;
        Elements links;
        Document htmlDocument;
        ArrayList<Element> linksToDownload;

        @Override
        protected void onPreExecute() {
            linksToDownload = new ArrayList<>();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Object... params) {
            try {
                course = (Course) params[0];
                htmlDocument = Jsoup.connect(course.getNewsforumurl()).cookie("MoodleSession", (String) params[1]).get();
                links = htmlDocument.select("a[href]");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            log.append("Downloading " + course.getName() + " News Forum files.\n\n");
            scrollToBottom();

            for (Element link : links) {

                String url = link.attr("abs:href");

                if (link.attr("abs:href").indexOf("&parent=") > -1) {
                    url = link.attr("abs:href").substring(0, link.attr("abs:href").indexOf("&parent="));
                }
                if (link.attr("abs:href").startsWith(mainPageUrl + "mod/forum/discuss.php") && !nfthreads.contains(url)) {
                    Log.d("Nf Links: ", url);
                    nfthreads.add(url);
                    JsoupAsyncTaskFetchNfThread jsoupAsyncTaskFetchNfThread = new JsoupAsyncTaskFetchNfThread();
                    jsoupAsyncTaskFetchNfThread.execute(link.attr("abs:href"), course.getName(), sessionCookie);
                }
            }
        }
    }


    private class JsoupAsyncTaskFetchNfThread extends AsyncTask<String, String, Void> {

        Elements links;
        Document htmlDocument;
        String courseName;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                htmlDocument = Jsoup.connect(params[0]).cookie("MoodleSession", params[2]).get();
                links = htmlDocument.select("a[href]");
                courseName = params[1];
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            for (int i=0 ; i<links.size() ; i+=2) {
                if (links.get(i).attr("abs:href").startsWith(mainPageUrl + "pluginfile.php")) {
                    Log.d("Nf thread downloadable", links.get(i).attr("abs:href"));
                    DownloadFileFromURL download = new DownloadFileFromURL();
                    download.execute(links.get(i).attr("abs:href"), courseName.substring(0, 6) + "/NewsForum/" + links.get(i).text());
                }
            }
        }
    }

    class DownloadFileFromURL extends AsyncTask<String, Integer, String> {

        int contentLength;
        CountDownTimer cdt;

        /**
         * Before starting background thread Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);

            /**
             * Initialising custom countdown timer
             * Ticks once every 1000ms
             */
            cdt = new CountDownTimer(100 * 60 * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    notifManager.notify(NOTIFICATION_ID, notifBuilder.build());
                }

                @Override
                public void onFinish() {
                    notifManager.notify(NOTIFICATION_ID, notifBuilder.build());
                }
            };
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected String doInBackground(final String... params) {
            int count;
            try {
                final int endIndex = params[1].lastIndexOf("/");

                final OkHttpClient.Builder builder = new OkHttpClient.Builder();
//                builder.addNetworkInterceptor(new LoggingInterceptor());
                OkHttpClient client = builder.build();
                Request request = new Request.Builder().url(params[0])
                        .addHeader("Cookie", "MoodleSession=" + sessionCookie)
                        .build();
                final Response response = client.newCall(request).execute();
                InputStream is = response.body().byteStream();

                BufferedInputStream input = new BufferedInputStream(is);
//                File ExternalStorageRoot = Environment.getExternalStorageDirectory();
//                params[1] = params[1].replace(" ","");
                String dir = params[1].substring(0, endIndex);
                final String filename = params[1].substring(endIndex + 1);
                Log.d(TAG, "Directory = " + dir);
                Log.d(TAG, "File name = " + filename);
                File directory = new File(rootDir, "MooDLD/" + dir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                final File file = new File(directory, filename + ".pdf");
                Log.d(TAG, "Storage path = " + file.getPath());

                if (!file.exists()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView filenametv = (TextView) findViewById(R.id.textView);
                            filenametv.setText(filename);
                            log.append("Downloading " + filename + " to " + file.getPath() + "\n\n");
                            scrollToBottom();
                            try {
                                contentLength = Integer.parseInt(response.header("Content-Length"));
                                progressBar.setMax(contentLength);
                                Log.d(TAG, "Content length = " + response.header("Content-Length"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            notifBuilder.setContentText(filename);
                            notifManager.notify(NOTIFICATION_ID, notifBuilder.build());
                            cdt.start();
                        }
                    });

                    OutputStream output = new FileOutputStream(file);

                    byte[] data = new byte[1024];

                    long total = 0;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        publishProgress((int) total);
                        output.write(data, 0, count);
                    }

                    output.flush();
                    output.close();
                    input.close();
                    response.body().close();

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            log.append(filename + " is already downloaded. Skipping download.\n\n");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(values[0]);
            notifBuilder.setProgress(contentLength, values[0], false);
        }

        @Override
        protected void onPostExecute(String file_url) {
            Log.d(TAG, "Download completed");
            downloadsRemaining -= 1;

            notifBuilder.setContentText("Download complete")
                    .setProgress(0, 0, false);
            cdt.onFinish();
            cdt.cancel();

            if (downloadsRemaining == 0) {
                Log.d(TAG, "All downloads complete.");
                TextView filenametv = (TextView) findViewById(R.id.textView);
                filenametv.setText("All downloads complete!");
                log.append("All downloads complete.\n");
                scrollToBottom();

                notifBuilder.setContentText("All downloads complete")
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                notifManager.notify(NOTIFICATION_ID, notifBuilder.build());
            }
            Log.d(TAG, "Downloads remaining = " + downloadsRemaining);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.INVISIBLE);
        }

    }

    void scrollToBottom() {
        final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notifManager.cancelAll();
    }
}