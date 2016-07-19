package com.moodld.moodld;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    public AccountHeader headerResult;
    private ProgressBar progressBar;
    private Button logout, dld, preferences;
    private static final String TAG = "MainActivity";
    private String sessionCookie = null, email = null;
    private static final String mainPageUrl = "http://moodle.iitb.ac.in/";
    private ArrayList<String> downloadLinks = new ArrayList<String>();
    private ArrayList<String> courseNames = new ArrayList<String>();
    private final int WRITE_EXTERNAL_STORAGE = 1;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialog = new ProgressDialog(this);
        dialog.setTitle("Please wait");
        dialog.setMessage("Loading data...");
        dialog.setCancelable(false);
        dialog.show();

        SharedPreferences prefs = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        sessionCookie = prefs.getString("MoodleSession", null);
        email = prefs.getString("username", null);
        email = email + "@iitb.ac.in";
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
            }
        });


        InitialiseDrawer();

    }


    public void InitialiseDrawer() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_dashboard);
        //if you want to update the items at a later time it is recommended to keep it in a variable
        PrimaryDrawerItem preferences = new PrimaryDrawerItem().withName(R.string.nav_item_preference).withIdentifier(1).withSelectable(false).withIcon(FontAwesome.Icon.faw_wrench);
        PrimaryDrawerItem directory = new PrimaryDrawerItem().withName(R.string.nav_item_directory).withIdentifier(2).withSelectable(false).withIcon(FontAwesome.Icon.faw_download);
        PrimaryDrawerItem help = new PrimaryDrawerItem().withName(R.string.nav_item_help).withIdentifier(3).withSelectable(false).withIcon(FontAwesome.Icon.faw_question);
        PrimaryDrawerItem feedback = new PrimaryDrawerItem().withName(R.string.nav_item_feedback).withIdentifier(4).withSelectable(false).withIcon(FontAwesome.Icon.faw_comment);
        PrimaryDrawerItem about = new PrimaryDrawerItem().withName(R.string.nav_item_about).withIdentifier(5).withSelectable(false).withIcon(FontAwesome.Icon.faw_info);
        PrimaryDrawerItem logout = new PrimaryDrawerItem().withName(R.string.nav_item_logout).withIdentifier(6).withSelectable(false).withIcon(FontAwesome.Icon.faw_sign_out);

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
        Drawer result = new DrawerBuilder()
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
                        }
                        if (drawerItem != null && drawerItem.getIdentifier() == 2) {
                        }
                        if (drawerItem != null && drawerItem.getIdentifier() == 3) {
                        }
                        if (drawerItem != null && drawerItem.getIdentifier() == 4) {
                        }
                        if (drawerItem != null && drawerItem.getIdentifier() == 5) {
                        }
                        if (drawerItem != null && drawerItem.getIdentifier() == 6) {
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
                    dldfiles();

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

}


