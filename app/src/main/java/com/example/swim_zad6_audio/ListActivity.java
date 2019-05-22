package com.example.swim_zad6_audio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    private final String FOLDER_NAME = "AudioRecApp";

    Map<Position, CheckBox> map;

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

        delB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDeleteClick();
            }
        });

        mergeB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMergeCLick();
            }
        });


    }

    private void onDeleteClick(){

        int kk = 0;

        for(Map.Entry<Position,CheckBox> entry: map.entrySet()){
            if(entry.getValue().isChecked()){
                entry.getKey().getFile().delete();
                //kk++;
            }
        }

        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);


        //Toast.makeText(this, Integer.toString(kk), Toast.LENGTH_SHORT).show();

    }

    private void onMergeCLick(){

        ArrayList<Position> mergeArray = new ArrayList<>();

        int bytesLength = 0;

        for(Map.Entry<Position,CheckBox> entry: map.entrySet()){
            if(entry.getValue().isChecked()){
                mergeArray.add(entry.getKey());
                bytesLength += entry.getKey().getFile().length() - 36;
            }
        }

        bytesLength += 36;

        if(mergeArray.size() == 0){
            return;
        }


        File tempFile = new File(this.getFilesDir().toString() + File.separator + "temp-"+System.currentTimeMillis() + ".pcm");

        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(mergeArray.get(0).getFile()));

            byte[] bytes = new byte[bytesLength];

            buf.read(bytes, 0, (int)mergeArray.get(0).getFile().length());
            buf.close();

            for(int i = 1; i < mergeArray.size();i++){

                buf = new BufferedInputStream(new FileInputStream(mergeArray.get(i).getFile()));

                buf.read(bytes, 36, (int)mergeArray.get(i).getFile().length());
                buf.close();
            }



            FileOutputStream fos = new FileOutputStream(tempFile);

            fos.write(bytes);

            fos.close();





        } catch (Exception e) {
            e.printStackTrace();
        }




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


    private File getPublicStorageDir(){

        File pathFile = new File(Environment.getExternalStorageDirectory()+File.separator+FOLDER_NAME);

        Log.d("LOG ###", "MK DIR -----------");

        if (!pathFile.exists())
            Toast.makeText(this,
                    (pathFile.mkdirs() ? "Directory has been created" : "Directory not created"),
                    Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Directory exists", Toast.LENGTH_SHORT).show();


        /*

        pathFile.mkdir();
        if(!pathFile.exists()){
            pathFile.mkdir();
        } */

        return pathFile;
    }

    private File getFileNameInExternalStorage(File pathFile){


        String path = pathFile.getAbsolutePath() + "/" + "audio-" + System.currentTimeMillis() + ".wav";
        ((TextView)findViewById(R.id.debug)).setText(path);

        return new File(path);
    }


}
