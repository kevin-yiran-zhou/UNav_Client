package com.example.opencv;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;

import java.util.Locale;

public class StartActivity extends AppCompatActivity {
    Handler mHandler = new Handler();
    private TextToSpeech mTTS;
    private EditText server_id, port_id,localization_interval;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        RelativeLayout relativeLayout=findViewById(R.id.startactivity);
        AnimationDrawable animationDrawable= (AnimationDrawable) relativeLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2500);
        animationDrawable.setExitFadeDuration(2500);
        animationDrawable.start();
        Button start_button = (Button) findViewById(R.id.start_navigation);
        Button mode_toggle = findViewById(R.id.localization_mode);
        //////
        Button mode_toggle2 = findViewById(R.id.instruction_mode);
        Slider slidebar = findViewById(R.id.placeholder);
        //////

        String[] localization_modes = {"Using mode: localize upon screen touch", "Using mode: localize after interval", "Using mode: localize after previous result"};
        final int[] localization_mode = {0};
        mode_toggle.setText(localization_modes[localization_mode[0]]);
        //////
        String[] instruction_modes = {"Trip overviews all the time", "Trip overview at the start. segment-by-segment instructions for remainder", "Segment-by-segment instructions all the time"};
        final int[] instruction_mode = {0};
        mode_toggle2.setText(instruction_modes[instruction_mode[0]]);
        //////
        setTitle("UNav");

        View logo=findViewById(R.id.logo);

        Runnable runnable0=new Runnable() {
            @Override
            public void run() {
                logo.animate().rotationYBy(360f).setDuration(2000).withEndAction(this).setInterpolator(new LinearInterpolator()).start();
            }
        };

        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                logo.animate().rotationYBy(360f).alpha(1f).scaleXBy(-.5f).scaleYBy(-.5f).translationYBy(1200.5f).translationXBy(278.5f).withEndAction(runnable0).setDuration(750).setInterpolator(new LinearInterpolator()).start();
            }
        };
        logo.setOnClickListener((position) -> {
            logo.animate().rotationXBy(360f).alpha(.5f).scaleXBy(.5f).scaleYBy(.5f).translationYBy(-1200.5f).translationXBy(-278.5f).withEndAction(runnable).setDuration(1000).setInterpolator(new LinearInterpolator()).start();
            });

        mTTS=new TextToSpeech(this,new TextToSpeech.OnInitListener(){

            @Override
            public void onInit(int status) {
                if (status==TextToSpeech.SUCCESS){
                    int result=mTTS.setLanguage(Locale.US);
                    float pitch=(float) 1.0,speed=(float) 2.0;
                    mTTS.setPitch(pitch);
                    mTTS.setSpeechRate(speed);
                    String message="Please enter information and press connect button at bottom";
                    mTTS.speak(message,TextToSpeech.QUEUE_FLUSH,null);
                    if (result==TextToSpeech.LANG_MISSING_DATA || result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("TTS","Language not Supported");
                    }
                } else{
                    Log.e("TTS","Initialization failed");
                }
            }
        });

        mode_toggle.setOnClickListener(view -> {
            localization_mode[0] = (localization_mode[0] + 1) % 3;
            mode_toggle.setText(localization_modes[localization_mode[0]]);
        });

        //////
        mode_toggle2.setOnClickListener(view -> {
            instruction_mode[0] = (instruction_mode[0] + 1) % 3;
            mode_toggle2.setText(instruction_modes[instruction_mode[0]]);
        });
        //////

        start_button.setOnClickListener((position) -> {
            server_id = (EditText) findViewById(R.id.server_id) ;
            port_id = (EditText) findViewById(R.id.port_id) ;
            localization_interval = (EditText) findViewById(R.id.localization_interval) ;
//                mHandler.post(() -> Toast.makeText(getBaseContext(), selectedItem, Toast.LENGTH_SHORT).show());
            Intent switchActivityIntent = new Intent(this, PlaceActivity.class);
            switchActivityIntent.putExtra("server_id",server_id.getText().toString());
            switchActivityIntent.putExtra("port_id",port_id.getText().toString());
            switchActivityIntent.putExtra("localization_interval", localization_interval.getText().toString());
            switchActivityIntent.putExtra("localization_mode", mode_toggle.getText().toString());
            //////
            switchActivityIntent.putExtra("instruction_mode", mode_toggle2.getText().toString());
            Float frequency_value = slidebar.getValue();
            String frequency;
            if (frequency_value > 6.66){
                frequency = "Frequency: high";
            }
            else if (frequency_value > 3.33){
                frequency = "Frequency: normal";
            }
            else {
                frequency = "Frequency: low";
            }
            switchActivityIntent.putExtra("frequency_mode", frequency);
            //////
            startActivity(switchActivityIntent);
        });

    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (mTTS!=null){
            mTTS.stop();
            mTTS.shutdown();
        }
    }
}