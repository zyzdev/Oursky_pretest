package com.example.oursky_pretest;

import static java.lang.Thread.sleep;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(() -> {
            try {
                int initialCapacity = 4;    //map 容量
                MyMap <String, Integer> mapForQ2 = new MyMap<>(initialCapacity);
                mapForQ2.put("key1", 1, 1);
                sleep(100);
                mapForQ2.put("key2", 2, 1);
                sleep(100);
                mapForQ2.put("key3", 3, 1);
                sleep(100);
                mapForQ2.put("key4", 4, 1);
                sleep(100);
                mapForQ2.put("key5", 5, 1);

                for(int i = 1; i < 6; i++) {
                    String key = "key" + i;
                    Log.d("MainActivity", "key:" + key + ", value:" + mapForQ2.get(key));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}