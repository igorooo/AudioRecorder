package com.example.swim_zad6_audio;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import java.io.File;

public class ListActivity extends AppCompatActivity {


    private ListView listView;
    private PositionListAdapter adapter;

    File dir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
    }
}
