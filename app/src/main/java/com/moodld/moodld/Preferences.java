package com.moodld.moodld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
    private ArrayAdapter<Course> listAdapter ;
    private TextView root_dir_value;
    ArrayList<Course> CourseList = new ArrayList<Course>();
    SharedPreferences  coursePrefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Typeface font = Typeface.createFromAsset( getAssets(), "fontawesome-webfont.ttf" );
        LayoutInflaterCompat.setFactory(getLayoutInflater(), new IconicsLayoutInflater(getDelegate()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        root_dir_value = (TextView) findViewById(R.id.root_dir_value);
        coursePrefs = getSharedPreferences("CourseList", MODE_PRIVATE);
        // Find the ListView resource.
        listView = (ListView) findViewById( R.id.listView );

        // When item is tapped, toggle checked properties of CheckBox and Planet.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick( AdapterView<?> parent, View item,
                                     int position, long id) {
                Course course = listAdapter.getItem( position );
                course.toggleChecked();
                CourseViewHolder viewHolder = (CourseViewHolder) item.getTag();
                viewHolder.getCheckBox().setChecked( course.isChecked() );
                Log.d(TAG, CourseList.toString());
            }
        });

        Button select_all = (Button) findViewById(R.id.select_all);
        Button deselect_all = (Button) findViewById(R.id.deselect_all);
        try {
            select_all.setTypeface(font);
            deselect_all.setTypeface(font);
        } catch (NullPointerException e){
            Log.e(TAG, e.getStackTrace().toString());
        }

        String json = coursePrefs.getString("CourseList", null);
        if (json != null) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Course>>(){}.getType();
            CourseList = (ArrayList<Course>) gson.fromJson(json, listType);
            Log.d(TAG, "Courses fetched from saved data: " + CourseList.toString());
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
            finish();
        }
        JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
        jsoupAsyncTask.execute(mainPageUrl, sessionCookie);

    }

    public void SelectAll(View view) {
        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
            Course course = listAdapter.getItem( i );
            course.setChecked(Boolean.TRUE);
        }
        listView.setAdapter(listAdapter);
    }

    public void DeselectAll(View view) {
        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
            Course course = listAdapter.getItem( i );
            course.setChecked(Boolean.FALSE);
        }
        listView.setAdapter(listAdapter);
    }

    public void Save(View view) {
        SharedPreferences.Editor prefsEditor = coursePrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(CourseList);
        prefsEditor.putString("CourseList", json);
        prefsEditor.commit();
        Log.d(TAG, json);
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
            } else {
                root_dir_value.setText("Select a root directory");
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
//                htmlDocument = Jsoup.parse("<div role=\"main\"><span id=\"maincontent\"></span><a href=\"#skipmycourses\" class=\"skip-block\">Skip my courses</a><div id=\"frontpage-course-list\"><h2>My courses</h2><div class=\"courses frontpage-course-list-enrolled\"><div class=\"coursebox clearfix odd first\" data-courseid=\"2939\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2939\">MA 214-2015-2-S1 Introduction to Numerical Analysis</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Introduction to Numerical Analysis</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11686&amp;course=1\">Tony J. Puthenpurakal  </a></li></ul></div></div><div class=\"coursebox clearfix even\" data-courseid=\"2877\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2877\">EP 230-2015-2 Elec Lab III</a></h3><div class=\"moreinfo\"></div><div class=\"enrolmenticons\"><img alt=\"Guest access\" class=\"smallicon\" title=\"Guest access\" src=\"http://moodle.iitb.ac.in/theme/image.php/clean/enrol_guest/1444075825/withoutpassword\" /></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Elec Lab III</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11803&amp;course=1\">Pradeep Sarin  </a></li></ul></div></div><div class=\"coursebox clearfix odd\" data-courseid=\"2876\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2876\">EP 228-2015-2 Quant Mech I</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Quant Mech I</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11801&amp;course=1\">Pichai Ramadevi  </a></li></ul></div></div><div class=\"coursebox clearfix even\" data-courseid=\"2778\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2778\">CS 213-2015-2 Minor Data Structures and Algorithms Minor</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Data Structures and Algorithms Minor</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=9351&amp;course=1\">Ankur Rawat   </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=4573&amp;course=1\">DEVDEEP UJJAL RAY  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=4570&amp;course=1\">FEGADE PRATIK PRAMOD  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=9353&amp;course=1\">Rameshwar Prasad Meghwal   </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=9319&amp;course=1\">Saurabh Jain   </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11395&amp;course=1\">Supratim Biswas  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=9323&amp;course=1\">NANDA KUMAR A</a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=13281&amp;course=1\">Sona Praneeth Akula</a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=13424&amp;course=1\">DEEPAK GARG</a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=13538&amp;course=1\">PRATEEKSHA KESHARI</a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=13558&amp;course=1\">SRISHTI THAKKAR</a></li></ul></div></div><div class=\"coursebox clearfix odd\" data-courseid=\"2534\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2534\">MA 214-2015-2 Introduction to Numerical Analysis</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Introduction to Numerical Analysis</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11686&amp;course=1\">Tony J. Puthenpurakal  </a></li></ul></div></div><div class=\"coursebox clearfix even\" data-courseid=\"2354\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2354\">EP 226-2015-2 Waves &amp; Oscillations &amp; Thermodynamics</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Waves & Oscillations & Thermodynamics</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11813&amp;course=1\">Tapanendu  Kundu  </a></li></ul></div></div><div class=\"coursebox clearfix odd\" data-courseid=\"2353\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2353\">EP 213-2015-2 Physics Laboratory I</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Physics Laboratory I</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11795&amp;course=1\">M. Senthil Kumar  </a></li></ul></div></div><div class=\"coursebox clearfix even\" data-courseid=\"2267\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2267\">EE 224-2015-2 Digital Systems</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Digital Systems</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=4048&amp;course=1\">GUNTAKA MADHU LEKHA  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11497&amp;course=1\">Madhav  Pandurang Desai  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=4042&amp;course=1\">NARISETTY CHAITANYA PRASAD  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=4036&amp;course=1\">NISHANT GURUNATH  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=3288&amp;course=1\">PATIL SUKANYA VIJAYSING  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=5300&amp;course=1\">Sahil Sharma  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=4034&amp;course=1\">SOUBHIK DEB  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=4009&amp;course=1\">Vikas  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=4010&amp;course=1\">VINAY VILAS BAGUL  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=4044&amp;course=1\">P ANUSRI</a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=12997&amp;course=1\">RAKASREE DASGUPTA</a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=13667&amp;course=1\">NEHA NEHA</a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=13121&amp;course=1\">SHANKAR PRASAD</a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=12992&amp;course=1\">ASWIN JITH S</a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=9440&amp;course=1\">suhani suhani</a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=12986&amp;course=1\">Kartikey Thakar</a></li></ul></div></div><div class=\"coursebox clearfix odd\" data-courseid=\"2264\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2264\">EE 210-2015-2 Signals &amp; System</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Signals & System</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11509&amp;course=1\">Preeti S. Rao  </a></li></ul></div></div><div class=\"coursebox clearfix even\" data-courseid=\"2217\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2217\">CS 213-2015-2 Data Structures and Algorithms</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Data Structures and Algorithms</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11395&amp;course=1\">Supratim Biswas  </a></li></ul></div></div><div class=\"coursebox clearfix odd\" data-courseid=\"2054\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2054\">AE 706-2015-2 Computational Fluid Dynamics</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Computational Fluid Dynamics</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=9133&amp;course=1\">G Vijay Saida Babu  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11161&amp;course=1\">Prabhu Ramachandran  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11169&amp;course=1\">Vasudeva Raghavendra Kowsik Bodi  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=12711&amp;course=1\">Anant Diwakar</a></li></ul></div></div><div class=\"coursebox clearfix even\" data-courseid=\"2037\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=2037\">AE 320-2015-2 Computational Fluid Dynamics</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Computational Fluid Dynamics</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11161&amp;course=1\">Prabhu Ramachandran  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11169&amp;course=1\">Vasudeva Raghavendra Kowsik Bodi  </a></li></ul></div></div><div class=\"coursebox clearfix odd\" data-courseid=\"1927\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=1927\">MA 207-2015-1-S1  Differential Equations II</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Differential Equations II</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11684&amp;course=1\">Swapneel A. Mahajan  </a></li></ul></div></div><div class=\"coursebox clearfix even\" data-courseid=\"1843\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=1843\">MA 205-2015-1-S1  Complex Analysis</a></h3><div class=\"moreinfo\"></div><div class=\"enrolmenticons\"><img alt=\"Guest access\" class=\"smallicon\" title=\"Guest access\" src=\"http://moodle.iitb.ac.in/theme/image.php/clean/enrol_guest/1444075825/withoutpassword\" /></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Complex Analysis</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11687&amp;course=1\">U. K. Anandavardhanan  </a></li></ul></div></div><div class=\"coursebox clearfix odd\" data-courseid=\"1837\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=1837\">HS 101-2015-1-S2  Economics</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Economics</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11564&amp;course=1\">Ananthakrishnan Ramanathan  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11583&amp;course=1\">Puja Padhi  </a></li></ul></div></div><div class=\"coursebox clearfix even\" data-courseid=\"1754\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=1754\">PH 542-2015-1 Non-linear Dynamics</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Non-linear Dynamics</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=13314&amp;course=1\">Raghunath Chelakkot</a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=12601&amp;course=1\">Amitabha Nandi</a></li></ul></div></div><div class=\"coursebox clearfix odd\" data-courseid=\"1632\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=1632\">MA 207-2015-1 Differential Equations II</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Differential Equations II</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11684&amp;course=1\">Swapneel A. Mahajan  </a></li></ul></div></div><div class=\"coursebox clearfix even\" data-courseid=\"1631\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=1631\">MA 205-2015-1 Complex Analysis</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Complex Analysis</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11687&amp;course=1\">U. K. Anandavardhanan  </a></li></ul></div></div><div class=\"coursebox clearfix odd\" data-courseid=\"1593\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=1593\">HS 101-2015-1 Economics</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Economics</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11564&amp;course=1\">Ananthakrishnan Ramanathan  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11569&amp;course=1\">Haripriya  S. Gundimeda  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11572&amp;course=1\">Krishnan Narayanan  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11583&amp;course=1\">Puja Padhi  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11597&amp;course=1\">Surajit Bhattacharyya  </a></li><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11599&amp;course=1\">Tara S. Shaw  </a></li></ul></div></div><div class=\"coursebox clearfix even last\" data-courseid=\"1524\" data-type=\"1\"><div class=\"info\"><h3 class=\"coursename\"><a class=\"\" href=\"http://moodle.iitb.ac.in/course/view.php?id=1524\">EP 223-2015-1 Electronics Lab II</a></h3><div class=\"moreinfo\"></div></div><div class=\"content\"><div class=\"summary\"><div class=\"no-overflow\">Electronics Lab II</div></div><ul class=\"teachers\"><li>Teacher: <a href=\"http://moodle.iitb.ac.in/user/view.php?id=11803&amp;course=1\">Pradeep Sarin  </a></li></ul></div></div><div class=\"paging paging-morelink\"><a href=\"http://moodle.iitb.ac.in/my/\">My courses</a></div></div></div><span class=\"skip-block-to\" id=\"skipmycourses\"></span><br /></div>                </section>");
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
                    course = new Course(link.text(),link.attr("abs:href"));
                    Boolean flag = false;
                    for (int i=0; i< CourseList.size(); i++) {
                        if (CourseList.get(i).getUrl().equals(course.getUrl())) {
                            flag = true;
                        }
                    }
                    if (flag==false) {
                        CourseList.add(course);
                    }
                }
            }
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
