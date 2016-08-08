package com.moodld.moodld;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import de.hdodenhof.circleimageview.CircleImageView;

public class AboutTeam extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_team);

        CircleImageView nihal = (CircleImageView)findViewById(R.id.nihal111img);
        if (nihal != null) {
            nihal.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setData(Uri.parse("https://github.com/nihal111"));
                    startActivity(intent);
                }
            });
        }

        ImageView arpan = (ImageView)findViewById(R.id.arpan98img);
        if (arpan != null) {
            arpan.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setData(Uri.parse("https://github.com/arpan98"));
                    startActivity(intent);
                }
            });
        }

        ImageView trehan = (ImageView)findViewById(R.id.codemaxximg);
        if (trehan != null) {
            trehan.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setData(Uri.parse("https://github.com/codemaxx"));
                    startActivity(intent);
                }
            });
        }
    }
}
