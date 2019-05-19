package com.example.swim_zad6_audio;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Queue;

public class ListActivity extends AppCompatActivity {

    private final String SHARED_PREFS = "SHAREDPREFS";
    private final String SP_LIST_OF_RECORDS = "RECORDS";

    ArrayList<Position> arrayList;


    private ListView listView;
    private TextView tv_Discription;
    private PositionListAdapter adapter;

    MediaPlayer mediaPlayer;

    File dir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        loadData();

        listView = (ListView) findViewById(R.id.listView);
        tv_Discription = (TextView) findViewById(R.id.tv_elementDicription);

        adapter = new PositionListAdapter(this, R.layout.adapter_view_layout, arrayList);
        listView.setAdapter(adapter);



        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Uri uri = Uri.fromFile(arrayList.get(position).getFile());

                mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);

                mediaPlayer.start();

                return false;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                tv_Discription.setText(arrayList.get(position).getDiscription());
            }
        });
    }


    public void loadData(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(SP_LIST_OF_RECORDS,"");

        Type type = new TypeToken<ArrayList<Position>>(){}.getType();

        arrayList = gson.fromJson(json, type);



        if(arrayList == null){
            arrayList = new ArrayList<Position>();
        }

        ArrayList<Position> indexes = new ArrayList<>();

        for(Position pos: arrayList){

            if( !pos.getFile().exists() ){
                indexes.add(pos);
            }
        }

        for(Position pos: indexes){
            arrayList.remove(pos);
        }
    }
}
