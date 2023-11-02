package com.example.opencv;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.json.JSONException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class PlaceActivity extends AppCompatActivity {
    public static Socket s;
//    public static DatagramSocket s2;
    Handler mHandler = new Handler();
    private TextToSpeech mTTS;
    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    public String server_id, port_id, localization_interval, localization_mode, instruction_mode, frequency_mode;
    private Typeface mTypeface;
    ImageButton microphone_button;

    public static synchronized Socket getSocket() throws SocketException {
        return PlaceActivity.s;
    }
//    public static synchronized DatagramSocket getSocket2() throws SocketException {
//        return PlaceActivity.s2;
//    }
    public static synchronized void setSocket(Socket socket){
        PlaceActivity.s = socket;
    }
//    public static synchronized void setSocket2(DatagramSocket socket2){ PlaceActivity.s2 = socket2;}
//    final ActionBar actionBar = getActionBar();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        server_id = extras.getString("server_id");
        port_id = extras.getString("port_id");
        localization_interval = extras.getString("localization_interval");
        localization_mode = extras.getString("localization_mode");
        //////
        instruction_mode = extras.getString("instruction_mode");
        frequency_mode = extras.getString("frequency_mode");
        //////
        mTypeface = ResourcesCompat.getFont(this, R.font.lato_medium);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO},
                        1);
            }
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, Long.valueOf(2000));

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
            }
            @Override
            public void onBeginningOfSpeech() {
            }
            @Override
            public void onRmsChanged(float v) {
            }
            @Override
            public void onBufferReceived(byte[] bytes) {
            }
            @Override
            public void onEndOfSpeech() {
            }
            @Override
            public void onError(int i) {
            }
            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                System.out.println(data.get(0));
            }
            @Override
            public void onPartialResults(Bundle bundle) {
            }
            @Override
            public void onEvent(int i, Bundle bundle) {
            }
        });
//        speechRecognizer.startListening(speechRecognizerIntent);

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.US);
                    float pitch = (float) 1.0, speed = (float) 2.0;
                    mTTS.setPitch(pitch);
                    mTTS.setSpeechRate(speed);
                    mTTS.speak("Choose the Place where you are located now", TextToSpeech.QUEUE_FLUSH, null);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not Supported");
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });

        // microphone_button=findViewById(R.id.microphone);
        setContentView(R.layout.activity_place);
        RelativeLayout relativeLayout=findViewById(R.id.placeactivity);
        AnimationDrawable animationDrawable= (AnimationDrawable) relativeLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2500);
        animationDrawable.setExitFadeDuration(2500);
        animationDrawable.start();
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

        ListView place_list = (ListView) findViewById(R.id.list_place);

        StartSocket foo = new StartSocket();
//        StartSocket2 foo0 = new StartSocket2();
        Thread get_building = new Thread(foo);
        get_building.start();
        try {
            get_building.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        JSONObject PlaceObject = foo.getValue();
        if (PlaceObject == null) {
            mHandler.post(() -> Toast.makeText(getBaseContext(), "The Server is under maintenance, please try connect later", Toast.LENGTH_SHORT).show());
        } else {
            String[] placestring = new String[]{};
            List<String> Place_list = new ArrayList<>(Arrays.asList(placestring));

            Iterator<String> listKEY = PlaceObject.keys();
            do {
                String newkey = listKEY.next();
                Place_list.add(newkey);
            } while (listKEY.hasNext());
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Place_list){
                @Override
                public View getView(int position, View convertView, ViewGroup parent){
                    TextView item = (TextView) super.getView(position,convertView,parent);
                    item.setTypeface(mTypeface);
                    item.setTextColor(Color.parseColor("#ffffff"));
                    item.setTypeface(item.getTypeface(), Typeface.BOLD);
                    item.setTextSize(TypedValue.COMPLEX_UNIT_DIP,18);
                    item.setOutlineAmbientShadowColor(Color.parseColor("#ff0000"));
                    item.setShadowLayer(10,0,0,Color.parseColor("#FFFF00"));
                    return item;
                }
            };
            place_list.setAdapter(arrayAdapter);
            place_list.setOnItemClickListener((parent, view, position, id) -> {
                String selectedItem = (String) parent.getItemAtPosition(position);
                mHandler.post(() -> Toast.makeText(getBaseContext(), selectedItem, Toast.LENGTH_SHORT).show());
                try {
                    Intent switchActivityIntent = new Intent(this, BuildingActivity.class);
                    switchActivityIntent.putExtra("server_id", server_id);
                    switchActivityIntent.putExtra("port_id", port_id);
                    switchActivityIntent.putExtra("localization_interval", localization_interval);
                    switchActivityIntent.putExtra("localization_mode", localization_mode);
                    switchActivityIntent.putExtra("Place", selectedItem);
                    JSONObject Building_list = PlaceObject.getJSONObject(selectedItem);
                    switchActivityIntent.putExtra("Building_list", Building_list.toString());
                    startActivity(switchActivityIntent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    public class StartSocket implements Runnable {
        private JSONObject Places;
        @Override
        public void run() {
            try {
                s = new Socket(server_id, Integer.parseInt(port_id));
                setSocket(s);
                InputStreamReader isr = new InputStreamReader(s.getInputStream());
                DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                BufferedReader br = new BufferedReader(isr);
                // dout.writeInt(0);
                //////
                String[] instruction_modes = {"Trip overviews all the time", "Trip overview at the start. segment-by-segment instructions for remainder", "Segment-by-segment instructions all the time"};
                Integer ind1 = Arrays.asList(instruction_modes).indexOf(instruction_mode);
                String[] frequency_modes = {"Frequency: high", "Frequency: normal", "Frequency: low"};
                Integer ind2 = Arrays.asList(frequency_modes).indexOf(frequency_mode);
                dout.writeInt(10*(ind1+1)+(ind2+1));
                //////
                String dict = "";
                dict = br.readLine();
                Places = new JSONObject(dict);
            } catch (IOException | JSONException e) {
                Places = null;
                e.printStackTrace();
            }
        }

        public JSONObject getValue() {
            return Places;
        }
    }

//    public class StartSocket2 implements Runnable {
//        private JSONObject Places;
//        @Override
//        public void run() {
//            try {
//                DatagramSocket s2 = new DatagramSocket();
//                setSocket2(s2);
//                byte[] buffer = new byte[2048];
//                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                s2.receive(packet);
//                // Reading data from DatagramPacket
//                ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength());
//                DataInputStream dataInputStream = new DataInputStream(byteStream);
//                // Writing data to DatagramPacket
//                DatagramPacket packet2 = new DatagramPacket(buffer, buffer.length); // You might need to initialize this with appropriate data
//                ByteArrayOutputStream byteStream2 = new ByteArrayOutputStream();
//                DataOutputStream dout = new DataOutputStream(byteStream2);
//                dout.writeUTF("Your data here"); // Write your data using appropriate methods
//                // Send the packet
//                s2.send(packet2);
//                // Reading from DataInputStream
//                String data = dataInputStream.readUTF(); // Assuming you are reading a string, use appropriate methods as per your data type
//                // Using BufferedReader with InputStreamReader
//                BufferedReader br = new BufferedReader(new InputStreamReader(byteStream));
//
//
//                // dout.writeInt(0);
//                //////
//                String[] instruction_modes = {"Trip overviews all the time", "Trip overview at the start. segment-by-segment instructions for remainder", "Segment-by-segment instructions all the time"};
//                Integer ind1 = Arrays.asList(instruction_modes).indexOf(instruction_mode);
//                String[] frequency_modes = {"Frequency: high", "Frequency: normal", "Frequency: low"};
//                Integer ind2 = Arrays.asList(frequency_modes).indexOf(frequency_mode);
//                dout.writeInt(10*(ind1+1)+(ind2+1));
//                //////
//                String dict = "";
//                dict = br.readLine();
//                Places = new JSONObject(dict);
//            } catch (IOException | JSONException e) {
//                Places = null;
//                e.printStackTrace();
//            }
//        }
//
//        public JSONObject getValue() {
//            return Places;
//        }
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
        }
    }
}