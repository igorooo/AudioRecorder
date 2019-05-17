package com.example.swim_zad6_audio;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

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
    Thread readThread, provider, processor;

    LinkedBlockingQueue<byte[]> audioBufferQueue;

    private AtomicBoolean recordingInProgress;

    File fileDir; // PATH to directory
    File currentFile;

    File tempFile;
    String tempFilepath;



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

            try{
                FileOutputStream fos = new FileOutputStream(tempFile, append);
               // fos.write(prepareWavFileHeader(BITS_PER_SAMPLE, 0, totalDataLen, SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, bytesPerSecond));
                //fos.write(prepareWavHeader(totalDataLen));

                while(recordingInProgress.get()){
                    fos.write(audioBufferQueue.take());
                }
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



    /***
     * WAV METHODS HERE
     *
     *
     */





    private byte[] prepareWavHeader(int pcmDataLengthInBytes) {
        int totalDataLen = pcmDataLengthInBytes + 36;
        byte[] wavHeader = prepareWavFileHeader(BITS_PER_SAMPLE, pcmDataLengthInBytes,totalDataLen,
                SAMPLING_RATE_IN_HZ,CHANNEL_CONFIG,bytesPerSecond);
        return wavHeader;
    }

    private byte[] prepareWavFileHeader(int bitsPerSample, long totalAudioLen,
                                        long totalDataLen, long longSampleRate,
                                        int channels, long byteRate) {
        byte[] header = new byte[44];
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = (byte) bitsPerSample;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        return header;
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



    // NO MINE

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


}
