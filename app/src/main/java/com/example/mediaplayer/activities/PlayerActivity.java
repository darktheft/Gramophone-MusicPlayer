package com.example.mediaplayer.activities;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mediaplayer.adapters.SongAdapter;
import com.example.mediaplayer.adapters.SongAlbumAdapter;
import com.example.mediaplayer.models.Song;
import com.example.mediaplayer.notification.NofiticationCenter;
import com.example.mediaplayer.R;

import java.util.ArrayList;
import java.util.Collections;


public class PlayerActivity extends AppCompatActivity {
    public static boolean playin;
    private ArrayList<Song>Asongs;
    private SeekBar mSeekBar;
    private static PlayerActivity instance;
    int position;
    private TextView curTime,totTime,songTitle,artistname;
    private ImageView pause,prev,next;
    private ImageView imageView;
    protected int val;

     protected NofiticationCenter nofiticationCenter;
     protected LinearLayout linearLayout;
    private static MediaPlayer mMediaPlayer;



    public int getPosition() {
        return position;
    }





    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        nofiticationCenter=new NofiticationCenter();
        instance=this;
        mSeekBar = findViewById(R.id.seek);
        songTitle = findViewById(R.id.song_name);
        artistname=findViewById(R.id.artist_name);
        totTime = findViewById(R.id.total_time);
        pause = findViewById(R.id.pause);
        linearLayout=findViewById(R.id.linear_layout);
        prev = findViewById(R.id.previous);
        next = findViewById(R.id.next);
        curTime=findViewById(R.id.current_time);
        imageView=findViewById(R.id.imageplayer);

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        position = bundle.getInt("index");
        val=bundle.getInt("val");
        if(val==1){
            Asongs= SongAlbumAdapter.albumSong;
        }else if(val==2){
            Asongs= SongAdapter.songs;
            Collections.shuffle(Asongs);
        }
        else{
            Asongs= SongAdapter.songs;
        }
        if(Asongs.get(position).getName()=="shufflee")position=(position+1)% Asongs.size();
        initPlayer(position);


        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (position == 0) {
                    position = Asongs.size() - 1;
                } else {
                    position--;
                }
                initPlayer(position);

            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                position=(position+1)% Asongs.size();
                initPlayer(position);
            }
        });

    }

    public static PlayerActivity getInstance() {
        return instance;
    }

     public void initPlayer(final int position) {

        playin=true;
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.reset();
        }

        String name= Asongs.get(position).getName();
        String artist= Asongs.get(position).getArtist();
        songTitle.setText(name);
        artistname.setText(artist);

        try {

            Glide
                    .with(getApplicationContext())
                    .load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),Asongs.get(position).getAlbumID()).toString())
                    .thumbnail(0.2f)
                    .centerCrop()
                    .placeholder(R.drawable.track)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)

                    .into(imageView);
        }
        catch (Exception e){

            Glide.with(this).load(R.drawable.track).into(imageView);
        }
        MainActivity.getInstance().sendOnChannel(name,artist,position);



        mMediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(Asongs.get(position).getPath())); // create and load mediaplayer with song resources
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                String totalTime = createTimeLabel(mMediaPlayer.getDuration());
                totTime.setText(totalTime);
                mSeekBar.setMax(mMediaPlayer.getDuration());
                mMediaPlayer.start();
                pause.setBackgroundResource(R.drawable.pause_24dp);

            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                int curSongPoition = position;
                curSongPoition = (curSongPoition + 1) % (Asongs.size());
                initPlayer(curSongPoition);

            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (fromUser) {
                    mMediaPlayer.seekTo(progress);
                    mSeekBar.setProgress(progress);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mMediaPlayer != null) {
                    try {
                        if (mMediaPlayer.isPlaying()) {
                            Message msg = new Message();
                            msg.what = mMediaPlayer.getCurrentPosition();
                            handler.sendMessage(msg);
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int current_position = msg.what;
            mSeekBar.setProgress(current_position);
            String cTime = createTimeLabel(current_position);
            curTime.setText(cTime);
        }
    };
     public void play() {

        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            playin=true;
            mMediaPlayer.start();
            pause.setBackgroundResource(R.drawable.pause_24dp);
            MainActivity.getInstance().sendOnChannel( Asongs.get(position).getName(), Asongs.get(position).getArtist(),position);
        } else {
            pause();
        }

    }

     public void pause() {
        if (mMediaPlayer.isPlaying()) {
            playin=false;
            mMediaPlayer.pause();
            pause.setBackgroundResource(R.drawable.play_arrow_24dp);
            MainActivity.getInstance().sendOnChannel( Asongs.get(position).getName(), Asongs.get(position).getArtist(),position);

        }

    }


    public String createTimeLabel(int duration) {
        String timeLabel = "";
        int min = duration / 1000 / 60;
        int sec = duration / 1000 % 60;

        timeLabel += min + ":";
        if (sec < 10) timeLabel += "0";
        timeLabel += sec;

        return timeLabel;


    }
}