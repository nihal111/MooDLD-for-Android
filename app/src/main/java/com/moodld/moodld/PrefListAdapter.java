package com.moodld.moodld;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by arpan on 2/5/16.
 */
public class PrefListAdapter extends BaseAdapter {

    private ArrayList<String> course_names;
    private Context context;

    PrefListAdapter(ArrayList<String> course_names, Context context) {
        this.course_names = course_names;
        this.context = context;
    }

    @Override
    public int getCount() {
        return course_names.size();
    }

    @Override
    public Object getItem(int position) {
        return course_names.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;

        if(row==null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row=inflater.inflate(R.layout.pref_list_row, parent, false);
        }

        TextView course_name_tv = (TextView) row.findViewById(R.id.course_name);
        course_name_tv.setText(course_names.get(position));

        return row;
    }
}
