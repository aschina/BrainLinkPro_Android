package com.boby.macrotellectlinkdemo.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;

import com.boby.bluetoothconnect.LinkManager;
import com.boby.bluetoothconnect.bean.BrainWave;
import com.boby.bluetoothconnect.bean.Gravity;
import com.boby.bluetoothconnect.classic.bean.BlueConnectDevice;
import com.boby.bluetoothconnect.classic.listener.EEGPowerDataListener;
import com.boby.bluetoothconnect.classic.listener.OnConnectListener;
import com.boby.macrotellectlinkdemo.BlueItemView;
import com.boby.macrotellectlinkdemo.MainActivity;

import java.util.ArrayList;
import java.util.Locale;

public class BluetoothService extends Service {
    private static final String TAG = BluetoothService.class.getSimpleName();
    private LinkManager bluemanage;
    private TextToSpeech tts;
    private OnConnectListener onConnectListener;
    private EEGPowerDataListener eegPowerDataListener;
    private MainActivity mainActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        mainActivity = MainActivity.instance;
        initBlueManager();
        initTTS();
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.CHINESE);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language is not supported");
                } else {
                    // TTS is initialized successfully
                    Log.d(TAG, "TTS is ready");
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }

    private void initBlueManager() {
        onConnectListener = new OnConnectListener() {
            @Override
            public void onConnectionLost(BlueConnectDevice blueConnectDevice) {
                final String mac = blueConnectDevice.getAddress();
                Log.e(TAG, "连接丢失 namne:" + blueConnectDevice.getName() + " mac: " + mac);
                mainActivity.handleConnectionLost(mac, bluemanage.getConnectSize());
            }

            @Override
            public void onConnectStart(BlueConnectDevice blueConnectDevice) {
                Log.e(TAG, "开始连接 name:" + blueConnectDevice.getName() + " mac: " + blueConnectDevice.getAddress());
            }

            @Override
            public void onConnectting(BlueConnectDevice blueConnectDevice) {
                Log.e(TAG, "连接中 name:" + blueConnectDevice.getName() + " mac: " + blueConnectDevice.getAddress());
            }

            @Override
            public void onConnectFailed(BlueConnectDevice blueConnectDevice) {
                Log.e(TAG, "连接失败 name:" + blueConnectDevice.getName() + " mac: " + blueConnectDevice.getAddress());

            }

            @Override
            public void onConnectSuccess(final BlueConnectDevice blueConnectDevice) {
                final String mac = blueConnectDevice.getAddress();
                final String connectType = blueConnectDevice.isBleConnect ? " 4.0 " : " 3.0 ";
                Log.e(TAG, "连接成功 name:" + blueConnectDevice.getName() + " mac: " + mac);

                mainActivity.updateUIOnConnectSuccess(mac, connectType, blueConnectDevice.getName(), bluemanage.getConnectSize());

            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "连接错误");
                e.printStackTrace();

            }
        };


        eegPowerDataListener = new EEGPowerDataListener() {
            @Override
            public void onBrainWavedata(final String mac, final BrainWave brainWave) {
                mainActivity.handleBrainWaveData(mac, brainWave, tts);
            }

            @Override
            public void onRawData(String mac, int raw) {

            }

            @Override
            public void onGravity(String mac, Gravity gravity) {
                mainActivity.updateGravity(mac, gravity);
            }


            @Override
            public void onRR(String mac, ArrayList<Integer> rr, int oxygen) {

            }

        };

        bluemanage = LinkManager.init(mainActivity);
        bluemanage.setMultiEEGPowerDataListener(eegPowerDataListener);
        bluemanage.setOnConnectListener(onConnectListener);
        bluemanage.setDebug(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        bluemanage.setMaxConnectSize(intent.getIntExtra("maxConnectSize", 0));
        String whiteList = intent.getStringExtra("whiteList");
        bluemanage.setWhiteList(whiteList);
        bluemanage.startScan();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluemanage != null) {
            bluemanage.close();
            bluemanage.onDestroy();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}