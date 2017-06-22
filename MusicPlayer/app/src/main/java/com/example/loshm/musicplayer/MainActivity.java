package com.example.loshm.musicplayer;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener{

    MediaPlayer mp;
    ArrayList<File> mySongs;
    Uri u;
    SeekBar sb;
    Button play, forward, next, rewind, previous;
    TextView songName;
    Thread updateSeeker;
    Sensor sensor;
    SensorManager SM;
    ListView playlist;
    EditText search;
    MenuItem shuffle, shaker;
    LinearLayout first, third;


    boolean isShuffle, isShaker, right, left, notFirst;
    int position;
    String[] items;
    List<Integer> history;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        play = (Button)findViewById(R.id.play);
        forward = (Button)findViewById(R.id.forward);
        next = (Button)findViewById(R.id.next);
        rewind = (Button)findViewById(R.id.rewind);
        previous = (Button)findViewById(R.id.previous);

        playlist = (ListView)findViewById(R.id.playlist);
        search = (EditText) findViewById(R.id.search);
        songName = (TextView)findViewById(R.id.songName);
        shuffle = (MenuItem)findViewById(R.id.shuffle);
        shaker = (MenuItem)findViewById(R.id.shaker);
        first = (LinearLayout)findViewById(R.id.first);
        third = (LinearLayout)findViewById(R.id.third);

        history = new ArrayList<>(10);//used for

        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                search(v);
                return false;
            }
        });

        play.setOnClickListener(this);
        forward.setOnClickListener(this);
        next.setOnClickListener(this);
        rewind.setOnClickListener(this);
        previous.setOnClickListener(this);

        SM = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SM.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        sb = (SeekBar)findViewById(R.id.seekBar);
        seeker();

        if(mp!=null){
            mp.stop();
            mp.release();
        }

        mySongs = findSongs(Environment.getExternalStorageDirectory());
        items = new String[mySongs.size()];

        for (int i=0; i<mySongs.size(); i++){
            items[i] = mySongs.get(i).getName().replace(".mp3", "");
        }

        ArrayAdapter<String> adp = new ArrayAdapter<String>(getApplicationContext(), R.layout.song_layout, R.id.textView, items);
        playlist.setAdapter(adp);
        playlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int pos, long id) {
                play.setText("||");
                songName.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 2.4f);
                first.setLayoutParams(param);
                LinearLayout.LayoutParams param1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 2.0f);
                third.setLayoutParams(param1);
                if(notFirst){
                    play(pos, true);
                }else{
                    Uri u = Uri.parse(mySongs.get(pos).toString());
                    mp = MediaPlayer.create(getApplicationContext(), u);
                    mp.start();
                    sb.setMax(mp.getDuration());
                    updateSeeker.start();
                    songName.setText(mySongs.get(pos).toString().substring((mySongs.get(pos).toString().lastIndexOf('/')+1),
                            (mySongs.get(pos).toString().lastIndexOf('.'))).replace('_', ' '));
                    history.add(pos);
                }
                notFirst = true;
                sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if((mp.getDuration() - progress)<200){
                            onClick(next);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mp.seekTo(seekBar.getProgress());
                    }
                });
            }
        });
    }

    public void play(int a, boolean b){
        mp.reset();
        position = a;
        u = Uri.parse(mySongs.get(position).toString());
        mp = MediaPlayer.create(getApplicationContext(), u);
        mp.start();
        sb.setMax(mp.getDuration());
        songName.setText(mySongs.get(position).toString().substring((mySongs.get(position).toString().lastIndexOf('/')+1),
                (mySongs.get(position).toString().lastIndexOf('.'))).replace('_', ' '));
            if(b){
                history.add(position);
            }
    }

    public void search(View view){
        int number=0;
        final ArrayList<File> result = findSongs(Environment.getExternalStorageDirectory());
        for (int i=0; i<result.size(); i++){
            if(result.get(i).getName().toString().toLowerCase().contains(search.getText().toString().toLowerCase())) {
                number++;
            }
        }
        String[] list = new String[number];
        int a = 0;
        for(int i=0; i<result.size(); i++){
            if(result.get(i).getName().toString().toLowerCase().contains(search.getText().toString().toLowerCase())) {
                list[a] = result.get(i).getName().toString().replace(".mp3", "");
                a++;
            }
        }
        ArrayAdapter<String> adp = new ArrayAdapter<String>(getApplicationContext(), R.layout.song_layout, R.id.textView, list);
        playlist.setAdapter(adp);
    }

    @Override
    public void onClick(View v) {
        right = false;
        left = false;
        seeker();
        updateSeeker.start();
        int id = v.getId();
        switch (id){
            case R.id.play:
                if(!notFirst){
                    break;
                }
                if(mp.isPlaying()){
                    play.setText(">");
                    mp.pause();
                }else{
                    play.setText("||");
                    mp.start();
                }
                break;
            case R.id.forward:
                if(!notFirst){
                    break;
                }
                mp.seekTo(mp.getCurrentPosition()+5000);
                break;
            case R.id.rewind:
                if(!notFirst){
                    break;
                }
                mp.seekTo(mp.getCurrentPosition()-5000);
                break;
            case R.id.next:
                if(!notFirst){
                    break;
                }
                if(isShuffle){
                    Random random = new Random();
                    play(random.nextInt(mySongs.size()), true);
                }else{
                    play((position+1)%mySongs.size(), true);
                }
                break;
            case R.id.previous:
                if (history.size() <= 1){
                    break;
                }
                history.remove(history.size()-1);
                play(history.get(history.size()-1), false);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.shuffle){
            if(item.getTitle().equals("Shuffle Off")){
                item.setTitle("Shuffle On");
                item.setIcon(R.drawable.shuffle_on);
                isShuffle = true;
            }else{
                item.setTitle("Shuffle Off");
                item.setIcon(R.drawable.shuffle_off);
                isShuffle = false;
            }
        }
        if(item.getItemId() == R.id.shaker){
            if(item.getTitle().equals("Shaker Off")){
                item.setTitle("Shaker On");
                item.setIcon(R.drawable.shaker_on);
                isShaker = true;
            }else{
                item.setTitle("Shaker Off");
                item.setIcon(R.drawable.shaker_off);
                isShaker = false;
            }
        }
        return true;
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if(!isShaker){
            return;
        }
        if(event.values[0]<-6) {
            if(left){
                onClick(next);
                left = right = false;
            }else{
                right = true;
                pause();
            }
            //songName.setText("Right:" + right + " Left:" + left);
        }
        if(event.values[0]>6) {
            if(right){
                onClick(previous);
                left = right = false;
            }else{
                left = true;
                pause();
            }
            //songName.setText("Right:" + right + " Left:" + left);
        }
        //songName.setText("Right:" + right + " Left:" + left);
    }

    public void pause(){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                left = right = false;
            }
        }, 1000);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public ArrayList<File> findSongs(File root){
        ArrayList<File> al = new ArrayList<File>();
        File[] files = root.listFiles();
        for (File singleFile : files){
            if (singleFile.isDirectory() && !singleFile.isHidden()){
                al.addAll(findSongs(singleFile));
            }else{
                if(singleFile.getName().endsWith(".mp3")){
                    al.add(singleFile);
                }
            }
        }
        return al;
    }

    public void seeker(){
        updateSeeker = new Thread(){
            public void run(){
                int totalDuration = mp.getDuration();
                int currentPosition = 0;
                sb.setMax(totalDuration);
                while (currentPosition < totalDuration){
                    try{
                        sleep(10);
                        currentPosition = mp.getCurrentPosition();
                        sb.setProgress(currentPosition);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };
    }

}

//fadein and fadeout, layouts
