package com.moodld.moodld;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.google.android.gms.auth.firstparty.shared.FACLConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String mainPageUrl = "http://moodle.iitb.ac.in/";
    private final int WRITE_EXTERNAL_STORAGE = 1;
    private final int REQUEST_DIRECTORY = 0, NOTIFICATION_ID = 1;
    public AccountHeader headerResult;
    ProgressDialog dialog;
    ArrayList<Course> CourseList = new ArrayList<Course>();
    ArrayList<String> nfthreads = new ArrayList<String>();
    ArrayList<String> fileNames = new ArrayList<String>();
    SharedPreferences coursePrefs;
    int downloadsRemaining = 0;
    TextView log;
    NotificationManager notifManager;
    NotificationCompat.Builder notifBuilder;
    boolean nfDownloaded = false;
    private Toolbar toolbar;
    private Drawer result;
    private FrameLayout dld;
    private String sessionCookie = null, email = null, rootDir = null;
    private ArrayList<String> downloadLinks = new ArrayList<String>();
    private ArrayList<String> courseNames = new ArrayList<String>();
    private ArcProgress arcProgress;
    TextView filenametv, downloadtv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialog = new ProgressDialog(this);
        dialog.setTitle("Please wait");
        dialog.setMessage("Loading data...");
        dialog.setCancelable(false);
        dialog.show();

        log = (TextView) findViewById(R.id.log);

        SharedPreferences prefs = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        sessionCookie = prefs.getString("MoodleSession", null);
        email = prefs.getString("username", null);
        email = email + "@iitb.ac.in";

        /* Setting Color and Contrast Color */
        dld = (FrameLayout) findViewById(R.id.dld);
        String color = prefs.getString("color", null);
        String contrast = prefs.getString("contrast", null);
        dld.setBackgroundColor(Color.parseColor(color));

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(Color.parseColor(color));

        arcProgress = (ArcProgress) findViewById(R.id.arc_progress);
        if (arcProgress != null) {
            arcProgress.setVisibility(View.INVISIBLE);
            arcProgress.bringToFront();
        }
        arcProgress.setBackgroundColor(Color.parseColor(color));
        arcProgress.setTextColor(Color.parseColor(contrast));
        arcProgress.setUnfinishedStrokeColor(Color.parseColor(contrast));

        filenametv = (TextView) findViewById(R.id.filenametv);
        filenametv.setTextColor(Color.parseColor(contrast));
        downloadtv = (TextView) findViewById(R.id.downloadtv);
        downloadtv.setTextColor(Color.parseColor(contrast));

        SharedPreferences coursePrefs = getSharedPreferences("CourseList", MODE_PRIVATE);

        if (sessionCookie != null) {
            JsoupNameAsyncTask jsoupNameAsyncTask = new JsoupNameAsyncTask();
            jsoupNameAsyncTask.execute(mainPageUrl, sessionCookie);
        } else {
            Log.d(TAG, "Session cookie not present");
            logout();
            return;
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
            if (CourseList.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, Preferences.class);
                intent.putExtra("status", 1);
                startActivity(intent);
                finish();
            } else {
                log.append("Tap the above screen to start downloading.\n\n");
                scrollToBottom();
            }
        } else {
            Log.d(TAG, "No CourseList saved. Redirecting to Preferences.");
            Intent intent = new Intent(MainActivity.this, Preferences.class);
            intent.putExtra("status", 0);
            startActivity(intent);
            finish();
        }
        // Getting root directory from shared preferences
        rootDir = coursePrefs.getString("rootDir", null);
        if (rootDir == null) {
            rootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            File directory = new File(rootDir, "MooDLD/");
            if (!directory.exists()) {
                directory.mkdirs();
            }
        }
        rootDir = rootDir + "/MooDLD";

        /**
         * Initialising Notifications setup for progress update
         */
        notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifBuilder = new NotificationCompat.Builder(this);
        notifBuilder.setContentTitle("MooDLD Download")
                .setSmallIcon(R.drawable.logo)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo))
                .setOngoing(true);

        InitialiseDrawer();

        dld.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    AskWritePermission();
                } else {
                    downloadFromCourses();
                    dld.setEnabled(false);
                    log.setText("");
                    log.append("Fetching courses from preferences.\n\n");
                }
            }
        });

    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch (keycode) {
            case KeyEvent.KEYCODE_MENU:
                result.openDrawer();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (result.isDrawerOpen()) {
                    result.closeDrawer();
                } else {
                    finish();
                }
                return true;
        }

        return super.onKeyDown(keycode, e);
    }

    public void InitialiseDrawer() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_dashboard);
        //if you want to update the items at a later time it is recommended to keep it in a variable
        PrimaryDrawerItem preferences = new PrimaryDrawerItem().withName(R.string.nav_item_preference).withIdentifier(1).withSelectable(false).withIcon(FontAwesome.Icon.faw_wrench);
        PrimaryDrawerItem directory = new PrimaryDrawerItem().withName(R.string.nav_item_directory).withIdentifier(2).withSelectable(false).withIcon(FontAwesome.Icon.faw_download);
        PrimaryDrawerItem help = new PrimaryDrawerItem().withName(R.string.nav_item_help).withIdentifier(3).withSelectable(false).withIcon(FontAwesome.Icon.faw_question);
        PrimaryDrawerItem feedback = new PrimaryDrawerItem().withName(R.string.nav_item_feedback).withIdentifier(4).withSelectable(false).withIcon(FontAwesome.Icon.faw_comment);
        PrimaryDrawerItem about = new PrimaryDrawerItem().withName(R.string.nav_item_about).withIdentifier(5).withSelectable(false).withIcon(FontAwesome.Icon.faw_info);
        final PrimaryDrawerItem logout = new PrimaryDrawerItem().withName(R.string.nav_item_logout).withIdentifier(6).withSelectable(false).withIcon(FontAwesome.Icon.faw_sign_out);

//        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
//            @Override
//            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
//                // super.set(imageView, uri, placeholder);
//                Log.d(TAG, "Loading Profile picture via Picasso");
//                Picasso.with(Dashboard.this).load(uri).resize(100, 100).centerCrop().into(imageView);
//            }
//
//        });

        // Create the AccountHeader
        headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .withSelectionListEnabledForSingleProfile(false)
                .build();


        //create the drawer and remember the `Drawer` result object
        result = new DrawerBuilder()
                .withAccountHeader(headerResult)
                .withActivity(this)
                .withToolbar(toolbar)
                .addDrawerItems(
                        preferences, directory, new DividerDrawerItem(), help, feedback, about, new DividerDrawerItem(), logout
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null && drawerItem.getIdentifier() == 1) {
                            Intent intent = new Intent(MainActivity.this, Preferences.class);
                            startActivity(intent);
                        }
                        if (drawerItem != null && drawerItem.getIdentifier() == 2) {
                            try {
                                Log.d(TAG, "Opening " + rootDir);
                                Uri selectedUri = Uri.parse(rootDir);
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(selectedUri, "resource/folder");
                                startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(), "File Manager not found. Manually open " + rootDir, Toast.LENGTH_LONG).show();
                            }
                        }
                        if (drawerItem != null && drawerItem.getIdentifier() == 3) {
                            Intent intent = new Intent(MainActivity.this, Intro.class);
                            startActivity(intent);
                        }
                        if (drawerItem != null && drawerItem.getIdentifier() == 4) {
                            String url = "https://github.com/nihal111/MooDLD-for-Android/issues/new";
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);
                        }
                        if (drawerItem != null && drawerItem.getIdentifier() == 5) {
                            Intent intent = new Intent(MainActivity.this, AboutTeam.class);
                            startActivity(intent);
                        }
                        if (drawerItem != null && drawerItem.getIdentifier() == 6) {
                            logout();
                        }

                        return false;
                    }
                })
                .withOnDrawerListener(new Drawer.OnDrawerListener() {
                    @Override
                    public void onDrawerOpened(View drawerView) {

                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {

                    }

                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {

                    }
                })
                .withSelectedItem(-1)
                .build();

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

    public void AskWritePermission() {
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
                    downloadFromCourses();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Snackbar.make(this.getWindow().getDecorView().findViewById(android.R.id.content), "Allow storage access to proceed with downloading files.",
                            Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                        public void onClick(View view) {
                        }
                    })
                            .show();
                }

                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     *
     */
    private void downloadFromCourses() {
        downloadtv.setVisibility(View.INVISIBLE);
        arcProgress.setVisibility(View.VISIBLE);
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

    void scrollToBottom() {
        final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    /*
    * Downloads from main thread and folders inside
    * */
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

                if (link.attr("abs:href").startsWith(mainPageUrl + "mod/folder")) {
                    Log.d("Folder", link.toString());
                }
                if (link.attr("abs:href").startsWith(mainPageUrl + "mod/assign")) {
                    Log.d("Assignment", link.toString());
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

        @Override
        protected void onPreExecute() {
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
            nfDownloaded = true;
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
            /**
             * Removing duplicates from links obtained on news forum page.
             */
            boolean found = false;
            ArrayList<Element> linksToDownload = new ArrayList<>();
            for (Element link : links) {
                if (found) {
                    found = false;
                    continue;
                }
                if (link.attr("abs:href").startsWith(mainPageUrl + "pluginfile.php")) {
                    linksToDownload.add(link);
                    found = true;
                }
            }

            downloadsRemaining += linksToDownload.size();
            for (Element link : linksToDownload) {
                Log.d("Nf thread downloadable", link.attr("abs:href"));
                DownloadFileFromURL download = new DownloadFileFromURL();
                try {
                    download.execute(link.attr("abs:href"), courseName.substring(0, 6) + "/NewsForum/" +
                            java.net.URLDecoder.decode(link.attr("abs:href"), "UTF-8").substring(
                                    java.net.URLDecoder.decode(link.attr("abs:href"), "UTF-8").lastIndexOf("/") + 1));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
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
            arcProgress.setVisibility(View.VISIBLE);
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
                final String dir = params[1].substring(0, endIndex);
                final String filename = params[1].substring(endIndex + 1);
                Log.d(TAG, "Directory = " + dir);
                Log.d(TAG, "File name = " + filename);
                File directory = new File(rootDir, dir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                final File file = new File(directory, filename + ".pdf");
                Log.d(TAG, "Storage path = " + file.getPath());

                if (!file.exists()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            arcProgress.setBottomText(dir.substring(0,6));
                            filenametv.setText(filename);
                            log.append("Downloading " + filename + " to " + file.getPath() + "\n\n");
                            scrollToBottom();
                            try {
                                contentLength = Integer.parseInt(response.header("Content-Length"));
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
                            scrollToBottom();
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
            arcProgress.setVisibility(View.VISIBLE);
            arcProgress.setProgress(values[0]*100/contentLength);
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

            if (nfDownloaded && downloadsRemaining == 0) {
                Log.d(TAG, "All downloads complete.");
                dld.setEnabled(true);
                filenametv.setText("All downloads complete!");
                log.append("All downloads complete.\n");
                scrollToBottom();

                notifBuilder.setContentText("All downloads complete")
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                notifManager.notify(NOTIFICATION_ID, notifBuilder.build());
            }
            Log.d(TAG, "Downloads remaining = " + downloadsRemaining);
            arcProgress.setProgress(100);
//            arcProgress.setVisibility(View.INVISIBLE);
        }

    }

    private class JsoupNameAsyncTask extends AsyncTask<String, String, Void> {

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
            dialog.dismiss();
            for (Element link : links) {
                if (link.attr("abs:href").startsWith(mainPageUrl + "course")) {
                    downloadLinks.add(link.attr("abs:href"));
                    courseNames.add(link.text().substring(0, 6));
                } else if (link.attr("abs:href").startsWith(mainPageUrl + "user/profile.php") && !link.text().equals("My profile") && !link.text().equals("View profile")) {
                    String myname = link.text();
                    Log.d(TAG, myname);
                    final IProfile profile = new ProfileDrawerItem().withName(myname).withEmail(email).withIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.user, null));
                    headerResult.addProfiles(profile);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifManager!=null) {
            notifManager.cancelAll();
        }
    }

}


