package com.moodld.moodld;

/**
 * Created by Nihal on 05-07-2016.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

/**
 * Custom adapter for displaying an array of Planet objects.
 */
public class CourseArrayAdapter extends ArrayAdapter<Course> {

    private LayoutInflater inflater;

    public CourseArrayAdapter(Context context, List<Course> planetList) {
        super(context, R.layout.simplerow, R.id.rowTextView, planetList);
        // Cache the LayoutInflate to avoid asking for a new one each time.
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Planet to display
        Course course = (Course) this.getItem(position);

        // The child views in each row.
        CheckBox checkBox;
        TextView textView;

        // Create a new row view
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.simplerow, null);

            // Find the child views.
            textView = (TextView) convertView.findViewById(R.id.rowTextView);
            checkBox = (CheckBox) convertView.findViewById(R.id.CheckBox01);

            // Optimization: Tag the row with it's child views, so we don't have to
            // call findViewById() later when we reuse the row.
            convertView.setTag(new CourseViewHolder(textView, checkBox));

            // If CheckBox is toggled, update the planet it is tagged with.
            checkBox.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v;
                    Course course = (Course) cb.getTag();
                    course.setChecked(cb.isChecked());
                }
            });
        }
        // Reuse existing row view
        else {
            // Because we use a ViewHolder, we avoid having to call findViewById().
            CourseViewHolder viewHolder = (CourseViewHolder) convertView.getTag();
            checkBox = viewHolder.getCheckBox();
            textView = viewHolder.getTextView();
        }

        // Tag the CheckBox with the Planet it is displaying, so that we can
        // access the planet in onClick() when the CheckBox is toggled.
        checkBox.setTag(course);

        // Display planet data
        checkBox.setChecked(course.isChecked());
        textView.setText(course.getName());

        return convertView;
    }

}