package com.example.swim_zad6_audio;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Array;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    /**
     * SHARED PREFERENCES CONSANTS
     */

    private final String SHARED_PREFS = "SHAREDPREFS";
    private final String SP_LIST_OF_RECORDS = "RECORDS";

    ArrayList<Position> arrayList;


    private final int  INACTIVE_STATE = 1, RECORDING_STATE = 2, PAUSED_STATE = 3, RECORDED_STATE = 4;
    private int INTERFACE_STATE;

    private final int SAMPLING_RATE_IN_HZ = 44100;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int BITS_PER_SAMPLE = 16;
    private final int bufferSize = 2;
    private final int bytesPerSecond = SAMPLING_RATE_IN_HZ * 2; // (2 bytes per sample)
    private int totalDataLen = 36;

    private final String FOLDER_NAME = "AudioRecApp";

    ImageButton record_Button, start_stop_Button, save_Button, delete_Button, list_Button;
    EditText et_name, et_surname, et_title, et_discription;

    AudioRecord audioRecord;

    LinkedBlockingQueue<byte[]> audioBufferQueue;

    private AtomicBoolean recordingInProgress;

    File fileDir; // PATH to directory
    File currentFile; // wav file
    File tempFile; // temp pcm file


    Equalizer equalizer;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);



        record_Button = (ImageButton) findViewById(R.id.rec_button);
        start_stop_Button = (ImageButton) findViewById(R.id.stop_start_button);
        save_Button = (ImageButton) findViewById(R.id.save_button);
        delete_Button = (ImageButton) findViewById(R.id.delete_button);
        list_Button= (ImageButton) findViewById(R.id.list_button);

        et_name = (EditText) findViewById(R.id.et_name);
        et_surname = (EditText) findViewById(R.id.et_surname);
        et_title = (EditText) findViewById(R.id.et_title);
        et_discription = (EditText) findViewById(R.id.et_discription);

        list_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListActivity();
            }
        });

        record_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnClickRecordButton();
            }
        });

        start_stop_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnClickStartPauseButton();
            }
        });

        save_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnClickSaveButton();
            }
        });

        delete_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnClickDeleteButton();
            }
        });

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                8192
        );

        changeInterfaceState(INACTIVE_STATE);
        audioBufferQueue = new LinkedBlockingQueue<byte[]>();
        recordingInProgress = new AtomicBoolean(false);
        fileDir = getPublicStorageDir();

    }

    @Override
    protected void onResume() {
        super.onResume();

        audioRecord.stop();
        recordingInProgress.set(false);
        setINACTIVE_STATE();
    }

    private void recording(){


        audioRecord.startRecording();
        changeInterfaceState(RECORDING_STATE);

        currentFile = getFileNameInExternalStorage(fileDir);
        tempFile = new File(this.getFilesDir().toString() + File.separator + "temp-"+System.currentTimeMillis() + ".pcm");

        recordingInProgress.set(true);


        BufferRecording provider = new BufferRecording();
        AudioProcessor processor = new AudioProcessor(tempFile, false);

        Thread provThread = new Thread(provider);
        Thread procThread = new Thread(processor);

        provThread.start();
        procThread.start();

    }

    private void pausing(){

        changeInterfaceState(PAUSED_STATE);

        audioRecord.stop();
        recordingInProgress.set(false);
    }

    private void unpause(){
        changeInterfaceState(RECORDING_STATE);

        audioRecord.startRecording();
        recordingInProgress.set(true);

        BufferRecording provider = new BufferRecording();
        AudioProcessor processor = new AudioProcessor(tempFile, true);

        Thread provThread = new Thread(provider);
        Thread procThread = new Thread(processor);

        provThread.start();
        procThread.start();
    }

    private void stop(){
        changeInterfaceState(RECORDED_STATE);

        audioRecord.stop();
        recordingInProgress.set(false);

    }

    public void save(){
        changeInterfaceState(INACTIVE_STATE);

        try{
            rawToWave(tempFile, currentFile);
            addPosition(currentFile);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void delete(){
        changeInterfaceState(INACTIVE_STATE);

        currentFile.delete();
        tempFile.delete();
    }


    /***
     * ITERAFACE METHODS HERE
     *
     *
     */

    private void OnClickRecordButton(){

        if(INTERFACE_STATE == INACTIVE_STATE){
            Toast.makeText(this, "RECORDING ...", Toast.LENGTH_SHORT).show();
            recording();
        }

        else if(INTERFACE_STATE == RECORDING_STATE || INTERFACE_STATE == PAUSED_STATE){
            Toast.makeText(this, "RECORDED", Toast.LENGTH_SHORT).show();
            stop();
        }

    }

    public void OnClickStartPauseButton(){

        if(INTERFACE_STATE == RECORDING_STATE){
            pausing();
        }

        else if(INTERFACE_STATE == PAUSED_STATE){
            unpause();
        }
    }

    public void OnClickSaveButton(){
        if(INTERFACE_STATE == RECORDED_STATE){
            save();
        }
    }

    public void OnClickDeleteButton(){
        if(INTERFACE_STATE == RECORDED_STATE){
            delete();
        }
    }




    private void changeInterfaceState(int STATE){

        switch (STATE){
            case INACTIVE_STATE:
                setINACTIVE_STATE();
                Log.d("INTERFACE STATE: ", "INACTIVE");
                return;
            case RECORDING_STATE:
                setRECORDING_STATE();
                Log.d("INTERFACE STATE: ", "RECORDING");
                return;
            case PAUSED_STATE:
                setPAUSED_STATE();
                Log.d("INTERFACE STATE: ", "PAUSED");
                return;
            case RECORDED_STATE:
                setRECORDED_STATE();
                Log.d("INTERFACE STATE: ", "FINISHED RECORDING");
                return;
        }
    }

    private void setActiveButtons(boolean rec_B, boolean ststop_B, boolean save_B, boolean delete_B){
        record_Button.setEnabled(rec_B);
        start_stop_Button.setEnabled(ststop_B);
        save_Button.setEnabled(save_B);
        delete_Button.setEnabled(delete_B);
    }

    private void setINACTIVE_STATE(){

        setActiveButtons(true, false, false, false);
        INTERFACE_STATE = INACTIVE_STATE;

        record_Button.setBackgroundResource(R.drawable.ic_record);
        start_stop_Button.setBackgroundResource(R.drawable.ic_pause);
        save_Button.setBackgroundResource(R.drawable.ic_save_inactive);
        delete_Button.setBackgroundResource(R.drawable.ic_delete_inactive);
    }

    private void setRECORDING_STATE(){

        setActiveButtons(true, true, false, false);
        INTERFACE_STATE = RECORDING_STATE;

        record_Button.setBackgroundResource(R.drawable.ic_stop);
        start_stop_Button.setBackgroundResource(R.drawable.ic_pause);
        save_Button.setBackgroundResource(R.drawable.ic_save_inactive);
        delete_Button.setBackgroundResource(R.drawable.ic_delete_inactive);
    }

    private void setPAUSED_STATE(){

        setActiveButtons(true, true, false, false);
        INTERFACE_STATE = PAUSED_STATE;

        record_Button.setBackgroundResource(R.drawable.ic_stop);
        start_stop_Button.setBackgroundResource(R.drawable.ic_start);
        save_Button.setBackgroundResource(R.drawable.ic_save_inactive);
        delete_Button.setBackgroundResource(R.drawable.ic_delete_inactive);
    }

    private void setRECORDED_STATE(){

        setActiveButtons(false, false, true, true);
        INTERFACE_STATE = RECORDED_STATE;

        record_Button.setBackgroundResource(R.drawable.ic_stop);
        start_stop_Button.setBackgroundResource(R.drawable.ic_pause);
        save_Button.setBackgroundResource(R.drawable.ic_save_active);
        delete_Button.setBackgroundResource(R.drawable.ic_delete_active);

    }

    public void startListActivity(){
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }


    /***
     * RECORDING AND WRITING THREADS HERE
     *
     *
     */





    private class AudioProcessor implements Runnable{

        File tempFile;
        Boolean append;

        public AudioProcessor( File tempFile, boolean append){

            this.tempFile = tempFile;
            this.append = append;
        }


        @Override
        public void run() {

            byte[] b = new byte[bufferSize];



            try{
                FileOutputStream fos = new FileOutputStream(tempFile, append);
               // fos.write(prepareWavFileHeader(BITS_PER_SAMPLE, 0, totalDataLen, SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, bytesPerSecond));
                //fos.write(prepareWavHeader(totalDataLen));

                equalizer = new Equalizer(fos);

                while(recordingInProgress.get()){
                    b = audioBufferQueue.take();

                    equalizer.put(b);

                    //fos.write(b);

                }
                equalizer.writeToFile();
                fos.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }





    private class BufferRecording implements Runnable{

        byte buffer[];
        int returnCode;

        private byte[] readBuffer(){

            buffer = new byte[bufferSize];

            returnCode = audioRecord.read(buffer, 0, bufferSize);

            if(returnCode < 0){
                throw new RuntimeException("Rading audio failed, reason: "+getBufferReadFailureReason(returnCode));
            }

            else{
                return buffer;
            }

        }

        @Override
        public void run() {

            while (recordingInProgress.get()){

                try{
                    audioBufferQueue.put(readBuffer());
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
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



    // to WAV from SOF

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


    /***
     * SHARED PREFS METHODS
     */

    public void loadData(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(SP_LIST_OF_RECORDS,"");

        Type type = new TypeToken<ArrayList<Position>>(){}.getType();

        arrayList = gson.fromJson(json, type);

        if(arrayList == null){
            arrayList = new ArrayList<Position>();
        }
    }

    public void addPosition(File file){

        boolean nameF = (et_name.getText().toString()).equals(getString(R.string.get_name));
        boolean surnameF = (et_surname.getText().toString()).equals(getString(R.string.get_surname));
        boolean titleF = (et_title.getText().toString()).equals(getString(R.string.get_title));
        boolean discrF = (et_discription.getText().toString()).equals(getString(R.string.get_discription));




        String name = ( nameF ? "-" : et_name.getText().toString());
        String surname = ( surnameF ? "-" : et_surname.getText().toString());
        String title = ( titleF ? "-" : et_title.getText().toString());
        String discription = ( discrF ? "-" : et_discription.getText().toString());

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

        Position position = new Position(name,surname,title,discription,TIME, file);

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


    public void EditTextClick(View v){
        ((EditText)v).getText().clear();
    }




}
