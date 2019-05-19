package com.example.swim_zad6_audio;

import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Pack {


    ArrayList<byte[]> array;
    private int AM_OF_SAMPLES;
    long VALUE;
    long COUNTER;


    public Pack(int AM_OF_SAMPLES){
        this.AM_OF_SAMPLES = AM_OF_SAMPLES;
        this.VALUE = 0;
        this.COUNTER = 1;
        this.array = new ArrayList<>();
    }

    public boolean put(byte[] B, long val){

        this.VALUE += Math.abs(val);
        this.COUNTER ++;
        array.add(B);

        if(this.COUNTER >= AM_OF_SAMPLES){
            return true;
        }

        return false;
    }

    public long getAverage(){
        return this.VALUE/this.COUNTER;
    }



    public void writeToFile(FileOutputStream fos){
        for(byte[] B: array){
            try{
                fos.write(B);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


}
