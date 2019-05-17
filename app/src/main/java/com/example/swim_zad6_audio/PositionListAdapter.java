package com.example.swim_zad6_audio;

import android.content.Context;
import android.media.Image;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class PositionListAdapter extends ArrayAdapter<Position> {

    private static final String TAG = "PositionListAdapter";


    private Context mContext;
    private int mResource;

    public PositionListAdapter(Context context, int resource,List<Position> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position,View convertView, ViewGroup parent) {

        String Name = getItem(position).getName();
        String Title = getItem(position).getTitle();
        String Surname = getItem(position).getSurname();
        String Discription= getItem(position).getDiscription();
        String Date = getItem(position).getDate();


        //Position pos = new Position(Title,Author,Date,Rating,isFilm);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        TextView tv_title = (TextView) convertView.findViewById(R.id.ad_title);
        TextView tv_discription = (TextView) convertView.findViewById(R.id.ad_discription);
        TextView tv_name= (TextView) convertView.findViewById(R.id.ad_name);
        TextView tv_surname= (TextView) convertView.findViewById(R.id.ad_surname);
        TextView tv_date= (TextView) convertView.findViewById(R.id.ad_date);

        tv_title.setText(Title);
        tv_name.setText(Name);
        tv_discription.setText(Discription);
        tv_surname.setText(Surname);

        return convertView;
    }
}
