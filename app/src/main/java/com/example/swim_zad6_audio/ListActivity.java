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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class ListActivity extends AppCompatActivity {

    private final String SHARED_PREFS = "SHAREDPREFS";
    private final String SP_LIST_OF_RECORDS = "RECORDS";

    ArrayList<Position> arrayList;


    private ListView listView;
    private TextView tv_Discription;
    private PositionListAdapter adapter;

    Map<File, CheckBox> map;

    ImageButton delB, mergeB;

    MediaPlayer mediaPlayer;

    File dir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        map = new HashMap<>();

        loadData();

        listView = (ListView) findViewById(R.id.listView);
        tv_Discription = (TextView) findViewById(R.id.tv_elementDicription);
        delB = (ImageButton) findViewById(R.id.buttonDelete);
        mergeB = (ImageButton) findViewById(R.id.buttonMerge);

        adapter = new PositionListAdapter(this, R.layout.adapter_view_layout, arrayList, map);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);



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

        mergeB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDeleteClick();
            }
        });


    }

    private void onDeleteClick(){

        int kk = 0;

        for(Map.Entry<File,CheckBox> entry: map.entrySet()){
            if(entry.getValue().isChecked()){
                kk++;
            }
        }

        Toast.makeText(this, Integer.toString(kk), Toast.LENGTH_SHORT).show();

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
