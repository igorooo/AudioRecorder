package com.example.swim_zad6_audio;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Equalizer {


    private final int SAMPLING_RATE_IN_HZ = 44100;
    private int PART_OF_SECOND = 4;  // 1 part is 0.25 (1/4) of second
    private int SILANCE_PERIOD_TO_RM = 8; // in PACKS
    private int TRESHOLD = 500;


    private int SAMPLES_IN_ONE_PACK = SAMPLING_RATE_IN_HZ/PART_OF_SECOND;

    FileOutputStream fos;

    ArrayList<Pack> arrayPack;
    Pack currentPack;
    ByteBuffer byteBuffer;
    short val;


    public Equalizer(FileOutputStream fos){
        this.fos = fos;
        this.arrayPack = new ArrayList<>();
        this.currentPack = new Pack(this.SAMPLES_IN_ONE_PACK);
    }

    public boolean put(byte[] b){

        byteBuffer = ByteBuffer.wrap(b);
        val = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getShort();

        boolean isPackFull = currentPack.put(b, val);

        if(isPackFull){


            arrayPack.add(currentPack);


            if( currentPack.getAverage() < this.TRESHOLD ){

                Log.d("EQUALIZER ###", "UNDER TRESHOLD");

                if(arrayPack.size() >= SILANCE_PERIOD_TO_RM){
                    arrayPack.clear();
                }
            }

            else{
                this.writeToFile();
            }

            //this.writeToFile();
           // Log.d("WIRTE TO FILE - ##", "written");
            currentPack = new Pack(this.SAMPLES_IN_ONE_PACK);

        }



        return true;
    }

    public boolean writeToFile(){
        for(Pack pack: arrayPack){
            pack.writeToFile(this.fos);
        }
        arrayPack.clear();
        return true;
    }







}
