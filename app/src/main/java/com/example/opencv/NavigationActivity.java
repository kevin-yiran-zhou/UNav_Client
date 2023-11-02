package com.example.opencv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import android.speech.tts.UtteranceProgressListener;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NavigationActivity extends Activity implements CvCameraViewListener2, OnTouchListener, SensorEventListener {

    private static String Tag="MainActivity",Tag1="IMU",Tag2 = "Camera";
    private SensorManager sensorManager;
    private Sensor linear_accelerator,gravity,gyroscope_vector;
    private TextToSpeech mTTS;
    private static int Time_interval;
    private int current_time;
    private boolean isGravitySensorPresent;
    private int past_time=(int) System.currentTimeMillis()/1000;
    public String server_id, port_id, localization_interval, localization_mode, instruction_mode, frequency_mode;
    private Vibrator v;
    private float[] acceleration= new float[3],gyroscope= new float[3];
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float[] deltaRotationVector = new float[4];
    private float thetax,thetay,thetaz;
    private float rotationx,deltarot,deltadis;
    private float[] deltaRotationMatrix = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    private float[] norm_matrix = {0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f};
    private float timestamp;
    private ToneGenerator toneGen_rotation,toneGen_distance;
    private String finalMessage;
    private boolean waitfeedback,ishorizontal,isbeep;
    private Thread beep_rot,gravity_vib;

    Socket s;
    String Place,Building,Floor,Destination;

    JavaCameraView javaCameraView;
    Mat mRgba;
    boolean touched = false;
    Handler mHandler = new Handler();

    BaseLoaderCallback mLoaderCallBack=new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
//        server_id = extras.getString("server_id");
//        port_id = extras.getString("port_id");
        localization_interval = extras.getString("localization_interval");
        localization_mode = extras.getString("localization_mode");
        Time_interval=Integer.parseInt(localization_interval);
        //////
        instruction_mode = extras.getString("instruction_mode");
        frequency_mode = extras.getString("frequency_mode_");
        //////
        Place = extras.getString("Place");
        Building = extras.getString("Building");
        Floor = extras.getString("Floor");
        Destination = extras.getString("Destination");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        javaCameraView=(JavaCameraView)findViewById(R.id.java_camera_view);
        javaCameraView.enableView();
        javaCameraView.setOnTouchListener(NavigationActivity.this);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        v=(Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        toneGen_rotation=new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGen_distance=new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

        gravity_vib=new Thread(starthorizontalvibrate);

        sensorManager=(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)!=null)
        {
            sensorManager.registerListener(this,gravity,SensorManager.SENSOR_DELAY_NORMAL);
            isGravitySensorPresent=true;
        }else {
            isGravitySensorPresent=false;
        }

        linear_accelerator=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gravity=sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        gyroscope_vector=sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorManager.registerListener(NavigationActivity.this,linear_accelerator,SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(NavigationActivity.this,gravity,SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(NavigationActivity.this,gyroscope_vector,SensorManager.SENSOR_DELAY_NORMAL);

        waitfeedback=false;
        ishorizontal=false;
        isbeep=false;
        rotationx=0;
        beep_rot = new Thread(startSoundRunnable);

        mTTS=new TextToSpeech(this, status -> {
            if (status==TextToSpeech.SUCCESS){
                int result=mTTS.setLanguage(Locale.US);
                float pitch=(float) 1.1,speed=(float) 1.2;
                mTTS.setPitch(pitch);
                mTTS.setSpeechRate(speed);
                mTTS.speak("Please place the phone horizontally",TextToSpeech.QUEUE_FLUSH,null);
                if (result==TextToSpeech.LANG_MISSING_DATA || result==TextToSpeech.LANG_NOT_SUPPORTED){
                    Log.e("TTS","Language not Supported");
                }
            } else{
                Log.e("TTS","Initialization failed");
            }
        });
        mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            public void onStart(String utteranceId) {
                // Speech started
            }
            public void onDone(String utteranceId) {
                // Speech completed
                current_time=(int) System.currentTimeMillis()/1000;
                past_time = current_time;
                waitfeedback = false;
            }
            public void onError(String utteranceId) {
                // Error occurred
            }
        });
    }
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        Thread send_stop = new Thread((Runnable) () -> {
            try {
                new DataOutputStream(s.getOutputStream()).writeInt(2);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        send_stop.start();
        try {
            send_stop.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (javaCameraView!=null) {
            javaCameraView.disableView();
        }
        if (mTTS!=null){
            mTTS.stop();
            mTTS.shutdown();
        }
        if (!s.isClosed()){
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)!=null)
            sensorManager.registerListener(this,gravity,SensorManager.SENSOR_DELAY_NORMAL);
        if (OpenCVLoader.initDebug()){
            Log.i(Tag2,"Opencv loaded successfully");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            Log.i(Tag2,"Opencv not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9,this,mLoaderCallBack);
        }
    }
    @Override
    public void onCameraViewStarted(int width, int height) {

        mRgba=new Mat(height,width, CvType.CV_8UC4);
    }
    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba=inputFrame.rgba();
        current_time=(int) System.currentTimeMillis()/1000;
        // Timing for sending images would be refined in the future
        boolean send_image = false;
        String[] localization_modes = {"Using mode: localize upon screen touch", "Using mode: localize after interval", "Using mode: localize after previous result"};
        if (localization_modes[0].equals(localization_mode)) {
            send_image = touched;
        } else if (localization_modes[1].equals(localization_mode)) {
            send_image = current_time - past_time > Time_interval;
        } else if (localization_modes[2].equals(localization_mode)) {
            send_image = !waitfeedback;
        }
        if(send_image) {
            waitfeedback = true;
            past_time = current_time;
            rotationx=0;
            StartSocket(mRgba);
            touched = false;
        }
        return mRgba;
    }

    private void StartSocket(final Mat img) {
        Thread send = new Thread(() -> {
            try {
                int new_height = (int) (img.rows() * (640.0f / img.cols()));
                Mat fit = new Mat(new_height,640,CvType.CV_8UC4);
                Size newSize = new Size(640, new_height);
                Imgproc.resize(img,fit,newSize);
                MatOfByte bytemat = new MatOfByte();
                Imgcodecs.imencode(".jpg", fit, bytemat);
                byte[] bytes = bytemat.toArray();

                //--- SEND IMAGE TO SERVER ---//
                //                    s = new Socket (server_id, Integer.parseInt(port_id));
                s=PlaceActivity.getSocket();
                InputStreamReader isr=new InputStreamReader(s.getInputStream());
                BufferedReader br=new BufferedReader(isr);
                String message="";
                DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                Integer bytes_len=(Integer) bytes.length;

                dout.writeInt(1);
                dout.writeInt(bytes_len);
                Log.d(Tag, String.valueOf(bytes.length));

                dout.write(bytes);
                dout.writeUTF(Place+','+Building+','+Floor+','+Destination);

                mHandler.post(() -> Toast.makeText(getBaseContext(), "Sent an image to server", Toast.LENGTH_SHORT).show());
                // mTTS.speak("Start localization",TextToSpeech.QUEUE_FLUSH,null);
                message=br.readLine();
                finalMessage = message;

                mHandler.post(() -> Toast.makeText(getBaseContext(), finalMessage, Toast.LENGTH_SHORT).show());
                String utteranceId = "unique_id"; // Assign a unique id for the speech
                mTTS.speak(finalMessage, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
//                mTTS.speak(finalMessage,TextToSpeech.QUEUE_FLUSH,null);
//                current_time=(int) System.currentTimeMillis()/1000;
//                past_time = current_time;
//                waitfeedback=false;
                //////
//                isr.close();
//                br.close();
//
//                dout.flush();
//                dout.close();
//                s.close();
                //////
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        send.start();
    }

//    private void StartSocket2(final Mat img) {
//        Thread send = new Thread(() -> {
//            try {
//                int new_height = (int) (img.rows() * (640.0f / img.cols()));
//                Mat fit = new Mat(new_height,640,CvType.CV_8UC4);
//                Size newSize = new Size(640, new_height);
//                Imgproc.resize(img,fit,newSize);
//                MatOfByte bytemat = new MatOfByte();
//                Imgcodecs.imencode(".jpg", fit, bytemat);
//                byte[] bytes = bytemat.toArray();
//
//                //--- SEND IMAGE TO SERVER ---//
//                Socket s = new Socket (server_id, Integer.parseInt(port_id));
//                InputStreamReader isr = new InputStreamReader(s.getInputStream());
//                BufferedReader br = new BufferedReader(isr);
//                String message = "";
//                DataOutputStream dout = new DataOutputStream(s.getOutputStream());
//                Integer bytes_len = bytes.length;
//
//                dout.writeInt(1);
//                dout.writeInt(bytes_len);
//                Log.d(Tag, String.valueOf(bytes.length));
//
//                dout.write(bytes);
//                dout.writeUTF(Place+','+Building+','+Floor+','+Destination);
//
//                mHandler.post(() -> Toast.makeText(getBaseContext(), "Sent an image to server", Toast.LENGTH_SHORT).show());
//                // mTTS.speak("Start localization",TextToSpeech.QUEUE_FLUSH,null);
//                message = br.readLine();
//                finalMessage = message;
//
//                mHandler.post(() -> Toast.makeText(getBaseContext(), finalMessage, Toast.LENGTH_SHORT).show());
//                mTTS.speak(finalMessage,TextToSpeech.QUEUE_FLUSH,null);
//                waitfeedback = false;
//
//                isr.close();
//                br.close();
//                dout.flush();
//                dout.close();
//                s.close();
//            } catch (UnknownHostException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//        send.start();
//    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(Tag,"onTouch event");
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touched = true;
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                break;
        }
        return true;
    }

    private final Runnable startSoundRunnable = new Runnable() {
        @Override
        public void run() {
            // Issue: STREAM_MUSIC's volume controls both tone and text to voice
            int beep_param = Math.abs((int) rotationx);
            Log.d(Tag1, "New duration: " + beep_param);
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int max_volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            beep_param = Math.min(beep_param, max_volume);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, beep_param, 0);
            Log.d(Tag1, "Output duration: " + beep_param);

            toneGen_rotation.startTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, beep_param);

            try {
                // System.out.println("8888888888888");
                Thread.sleep(1000 - beep_param);
                toneGen_rotation.stopTone();
                // System.out.println("777777777777");
            } catch (InterruptedException e) {
                System.out.println("999999999999999");
                e.printStackTrace();
            }

        }
    };

    private final Runnable starthorizontalvibrate = new Runnable() {
        @Override
        public void run() {
            v.vibrate(VibrationEffect.createOneShot(100,10));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        float x=event.values[0],y=event.values[1],z=event.values[2];
        if (sensor.getType() == Sensor.TYPE_GRAVITY) {
//            Log.d(Tag2,"Gravity Changed: X: "+x+" Y: "+y+" Z: "+z);
            if (event.values[0]<=9.6){
                ishorizontal=false;
//                if (!gravity_vib.isAlive()) {
//                    gravity_vib.start();
////            beep_rot.
//                }
            } else {
                ishorizontal=true;
            }
        }
        else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Log.d(Tag2,"Acceleration Changed: X: "+x+" Y: "+y+" Z: "+z);
            acceleration=event.values;
            float axisX = acceleration[0];
            float axisY = acceleration[1];
            float axisZ = acceleration[2];
            Log.d(Tag1, "Acceleration x: " + axisX + " y: " + axisY + " z: " + axisZ);
            // Acceleration visualizer
            /*
            float[] accels = {acceleration[0], acceleration[1], acceleration[2]};
            CharSequence[][] accel_visuals = new CharSequence[accels.length][10];
            for (CharSequence[] sequence : accel_visuals) {
                Arrays.fill(sequence, " ");
            }
            for (int accel = 0; accel < accels.length; accel++) {
                int index = accel_visuals[accel].length / 2;
                while (-1 < index && index < accel_visuals[accel].length && Math.abs(index - accel_visuals[accel].length / 2) < Math.abs(accels[accel])) {
                    accel_visuals[accel][index] = "=";
                    if (accels[accel] < 0) {
                        index --;
                    } else {
                        index ++;
                    }
                }
            }
            Log.d(Tag1, "Acceleration x: " + String.join("", accel_visuals[0])
                    + "\tAcceleration y: " + String.join("", accel_visuals[1])
                    + "\tAcceleration z: " + String.join("", accel_visuals[2]));
            */
            /*
            float[] rot_matrix = deltaRotationMatrix;
            float det = 0;

            Calculate the determinant using laplace expansion using the following indexing pattern (assuming matrix is row-first)
            0 * ( 4 8 - 5 7 )
            1 * ( 5 6 - 3 8 )
            2 * ( 3 7 - 4 6 )

            for (int col = 0; col < 3; col ++) {
                det += rot_matrix[col] * (rot_matrix[3 + (col + 1) % 3] * rot_matrix[6 + (col + 2) % 3]
                        - rot_matrix[3 + (col + 2) % 3] * rot_matrix[6 + (col + 1) % 3]);
            }
            Log.d(Tag1, "Determinant: " + det);
            // Calculate the inverse
            float[] inv_matrix = new float[9];
            Arrays.fill(inv_matrix, 0.0f);
            for (int col = 0; col < 3; col ++) {
                for (int row = 0; row < 3; row ++) {
                    inv_matrix[row * 3 + col] = (rot_matrix[((row + 1) % 3) * 3 + (col + 1) % 3] * rot_matrix[((row + 2) % 3) * 3 + (col + 2) % 3] -
                            rot_matrix[((row + 2) % 3) * 3 + (col + 1) % 3] * rot_matrix[((row + 1) % 3) * 3 + (col + 2) % 3]) / det;
                }
            }
            Log.d(Tag1, "Inverse rotation" + Arrays.toString(inv_matrix));
            */
            // Left-multiply rotation matrix to an aggregate product of net rotation
            Log.d(Tag1, "Normalized rotation: " + Arrays.toString(norm_matrix));
            Log.d(Tag1, "Rotation matrix" + Arrays.toString(deltaRotationMatrix));
            float[] new_norm = new float[9];
            Arrays.fill(new_norm, 0.0f);
            for (int col = 0; col < 3; col ++) {
                for (int comp = 0; comp < 3; comp ++) {
                    for (int row = 0; row < 3; row ++) {
                        new_norm[row * 3 + col] += deltaRotationMatrix[row * 3 + comp] * norm_matrix[comp * 3 + col];
                    }
                }
            }
            Log.d(Tag1, "Normalized rotation: " + Arrays.toString(new_norm));
            float normX = 0;
            float normY = 0;
            float normZ = 0;
            for (int comp = 0; comp < acceleration.length; comp ++) {
                normX += acceleration[comp] * new_norm[comp];
                normY += acceleration[comp] * new_norm[3 + comp];
                normZ += acceleration[comp] * new_norm[6 + comp];
            }
            norm_matrix = new_norm;
            Log.d(Tag1, "Norm x: " + normX + " y: " + normY + " z: " + normZ);
            // Cancelling out gravity: multiply new_norm to [0, 0, 9.81] and subtract this from the 3 normalized components above.
            // Normalized acceleration visualizer
            float[] norm_accels = {normX, normY, normZ};
            CharSequence[][] norm_visuals = new CharSequence[norm_accels.length][10];
            for (CharSequence[] sequence : norm_visuals) {
                Arrays.fill(sequence, " ");
            }
            for (int accel = 0; accel < norm_accels.length; accel++) {
                int index = norm_visuals[accel].length / 2;
                while (-1 < index && index < norm_visuals[accel].length && Math.abs(index - norm_visuals[accel].length / 2) < Math.abs(norm_accels[accel])) {
                    norm_visuals[accel][index] = "=";
                    if (norm_accels[accel] < 0) {
                        index --;
                    } else {
                        index ++;
                    }
                }
            }
            Log.d(Tag1, "Norm x: " + String.join("", norm_visuals[0])
                    + " Norm y: " + String.join("", norm_visuals[1])
                    + " Norm z: " + String.join("", norm_visuals[2]));

            /*
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                float placeholder_velocity = 0.0f;
                float displacement = placeholder_velocity * dT + normZ * dT * dT / 2;
                placeholder_velocity += axisZ * dT;
            }
            */
        }else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//            Log.d(Tag2,"Rotation Changed: X: "+x+" Y: "+y+" Z: "+z);
            gyroscope=event.values;
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = gyroscope[0];
                float axisY = gyroscope[1];
                float axisZ = gyroscope[2];
                // Calculate the angular speed of the sample
                float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                // (that is, EPSILON should represent your maximum allowable margin of error)
                if (omegaMagnitude > 0) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Log.d(Tag1, "axisX: " + axisX + " axisY: " + axisY + " axisZ: " + axisZ);
                /*
                CharSequence[][] axes_visuals = new CharSequence[3][10];
                float[] axes = {Math.abs(axisX), Math.abs(axisY), Math.abs(axisZ)};
                for (int axis = 0; axis < axes.length; axis ++) {
                    for (int i = 0; i < axes_visuals[axis].length; i++) {
                        if (i < (int) (10 * axes[axis])) {
                            axes_visuals[axis][i] = "=";
                        } else {
                            axes_visuals[axis][i] = " ";
                        }
                    }
                }

                Log.d(Tag1, "axisX: " + String.join("", axes_visuals[0])
                        + " axisY: " + String.join("", axes_visuals[1])
                        + " axisZ: " + String.join("", axes_visuals[2]));
                */
                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
            }
            timestamp = event.timestamp;
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            thetax=(float) Math.toDegrees(Math.atan2(deltaRotationMatrix[7],deltaRotationMatrix[8]));
            thetay=(float) Math.toDegrees(Math.atan2(-deltaRotationMatrix[6],Math.sqrt(deltaRotationMatrix[7]*deltaRotationMatrix[7]+deltaRotationMatrix[8]*deltaRotationMatrix[8])));
            thetaz=(float) Math.toDegrees(Math.atan2(deltaRotationMatrix[3],deltaRotationMatrix[0]));
//            Log.d(Tag2,"Acceleration Changed: X: "+acceleration[0]+" Y: "+acceleration[1]+" Z: "+acceleration[2]);
            // Log.d(Tag1, "thetaX: " + thetax + " thetaY: " + thetay + " thetaZ: " + thetaz);
            /*
            CharSequence[][] angle_visuals = new CharSequence[3][10];
            float[] angles = {Math.abs(thetax), Math.abs(thetay), Math.abs(thetaz)};
            for (int angle = 0; angle < angles.length; angle++) {
                for (int i = 0; i < angle_visuals[angle].length; i++) {
                    if (i < ((int) (angles[angle] / 90)) * 10) {
                        angle_visuals[angle][i] = "=";
                    } else {
                        angle_visuals[angle][i] = " ";
                    }
                }
            }
            Log.d(Tag1, "thetax: " + String.join("", angle_visuals[0])
                    + " thetay: " + String.join("", angle_visuals[1])
                    + " thetaz: " + String.join("", angle_visuals[2]));
            */
            rotationx+=thetax;
            /*
            CharSequence[] rotation_visual = new CharSequence[50];
            Arrays.fill(rotation_visual, " ");
            int index = rotation_visual.length / 2;
            while (-1 < index && index < rotation_visual.length && Math.abs(index - rotation_visual.length / 2) < Math.abs((int) rotationx)) {
                rotation_visual[index] = "=";
                if (rotationx < 0) {
                    index --;
                } else {
                    index ++;
                }
            }
            Log.d(Tag1, "rotationx: " + rotationx);
            Log.d(Tag1, String.join("", rotation_visual));
            */
            // Log.d(Tag,"11111111111111111111");
            /*
            if (!beep_rot.isAlive())
            {
                beep_rot = new Thread(startSoundRunnable);
                // Log.d(Tag,"2222222222222222222");
                beep_rot.start();

                try {
                    beep_rot.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            */
            // toneGen_rotation.startTone(ToneGenerator.TONE_PROP_BEEP,(int) Math.abs(rotationx));
            // toneGen_rotation.stopTone();
            // toneGen_rotation.release();

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}


