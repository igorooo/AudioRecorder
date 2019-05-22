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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class ListActivity extends AppCompatActivity {

    private final String SHARED_PREFS = "SHAREDPREFS";
    private final String SP_LIST_OF_RECORDS = "RECORDS";

    ArrayList<Position> arrayList;

    public final int SAMPLING_RATE_IN_HZ = 44100;


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

            byte[] bytes = new byte[bytesLength*2];
            byte[] trash = new byte[40];

            buf.read(bytes, 0, (int)mergeArray.get(0).getFile().length());
            buf.close();

            int offset = (int)mergeArray.get(0).getFile().length();

            for(int i = 1; i < mergeArray.size();i++){

                buf = new BufferedInputStream(new FileInputStream(mergeArray.get(i).getFile()));

                buf.read(trash, 0, 36);

                buf.read(bytes, offset, (int)mergeArray.get(i).getFile().length());
                buf.close();
                offset += (int)mergeArray.get(i).getFile().length()-36;
            }



            FileOutputStream fos = new FileOutputStream(tempFile);

            fos.write(bytes);

            fos.close();

            File fileDir = getPublicStorageDir();
            File currentFile = getFileNameInExternalStorage(fileDir);

            rawToWave(tempFile, currentFile);

            addPosition(currentFile, mergeArray);

            Intent intent = new Intent(this, ListActivity.class);
            startActivity(intent);


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


        return new File(path);
    }


    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, 44100); // sample rate
            writeInt(output, SAMPLING_RATE_IN_HZ * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }
    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    public void addPosition(File file, ArrayList<Position> mergeArray){

        String Title = "";
        String Name = "";
        String Surname = "";

        for(Position pos: mergeArray){
            Title += pos.getTitle() + " ";
            Name += pos.getName() + " ";
            Surname += pos.getSurname() + ":";
        }

        String Discription = "File made with "+ Integer.toString(mergeArray.size()) + " files";
        Date time;
        String TIME = "";


        try{
            String fileName = file.getAbsolutePath();
            String[] tokens = fileName.split("-");
            tokens = tokens[1].split("\\.");
            Log.d("TIME LOG", tokens[0]);
            time = new Date(Long.parseLong(tokens[0]));
            TIME = time.toString();
            Log.d("TIME LOG", TIME);


        }catch (Exception e){
            e.printStackTrace();
        }

        Position position = new Position(Name,Surname,Title,Discription,TIME, file);

        loadData();

        arrayList.add(position);

        ArrayList<Position> indexes = new ArrayList<>();

        for(Position pos: arrayList){

            if( !pos.getFile().exists() ){
                indexes.add(pos);
            }
        }

        for(Position pos: indexes){
            arrayList.remove(pos);
        }

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString(SP_LIST_OF_RECORDS,json);
        editor.apply();


    }


}
