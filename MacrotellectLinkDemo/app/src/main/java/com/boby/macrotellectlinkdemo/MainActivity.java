package com.boby.macrotellectlinkdemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.boby.bluetoothconnect.LinkManager;
import com.boby.bluetoothconnect.bean.BrainWave;
import com.boby.bluetoothconnect.bean.Gravity;
import com.boby.bluetoothconnect.classic.bean.BlueConnectDevice;
import com.boby.bluetoothconnect.classic.listener.EEGPowerDataListener;
import com.boby.bluetoothconnect.classic.listener.OnConnectListener;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int RC_GPS = 4483;
    private static final int RC_BT = 4484;
    private static final int PER_LOC = 4484;
    CheckBox checkbox1;
    CheckBox checkbox2;
    LinearLayout mLinearLayout;
    RadioGroup connectTypeGroup;
    Spinner spinner;
    Switch mSwitch;

    EditText ed_whiteList;
    RadioButton all, only3, only4;
    TextView tv_connectSize;
    public static final String TAG = MainActivity.class.getSimpleName();
    private OnConnectListener onConnectListener;
    private EEGPowerDataListener eegPowerDataListener;
    private LinkManager bluemanage;
    private TextToSpeech tts;
    private SeekBar seekBarAtt;
    private TextView seekBarAttValue;
    private SeekBar seekBarMed;
    private TextView seekBarMedValue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initChart();
        initBlueManager();
        initTTS();
        initSeekBar();
    }

    private void initSeekBar() {
        seekBarAtt = findViewById(R.id.seekBar_att);
        seekBarAttValue = findViewById(R.id.seekBar_att_value);
        seekBarMed = findViewById(R.id.seekBar_med);
        seekBarMedValue = findViewById(R.id.seekBar_med_value);

        seekBarAtt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarAttValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do something
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do something
            }
        });

        seekBarMed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarMedValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do something
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do something
            }
        });
    }

    private void initTTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
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
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_BT) {
            if (permissions.length > 0 &&
                    permissions[0].equals(Manifest.permission.BLUETOOTH_CONNECT) && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    permissions[1].equals(Manifest.permission.BLUETOOTH_SCAN) && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GPS) {
            initBlueManager();
        } else if (requestCode == RC_BT && resultCode == RESULT_OK) {
            startScan();
        }
    }


    /**
     * 初始化蓝牙管理，设置监听
     */
    public void initBlueManager() {


        //位置权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setTitle("权限")
                    .setMessage("请先授予位置权限")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PER_LOC);
                        }
                    })
                    .setCancelable(false)
                    .show();
            return;
        }

        //打开gps
        if (!isOPenGps(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("位置服务")
                    .setMessage("请先打开GPS位置服务")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, RC_GPS);
                        }
                    })
                    .setCancelable(false)
                    .show();
            return;
        }


        onConnectListener = new OnConnectListener() {
            @Override
            public void onConnectionLost(BlueConnectDevice blueConnectDevice) {
                final String mac = blueConnectDevice.getAddress();
                Log.e(TAG, "连接丢失 namne:" + blueConnectDevice.getName() + " mac: " + mac);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_connectSize.setText("" + bluemanage.getConnectSize());
                        BlueItemView viewWithTag = mLinearLayout.findViewWithTag(mac);
                        if (viewWithTag != null) {
                            viewWithTag.setMac("", "", "");
                            viewWithTag.setTag("temp");
                        }
                    }
                });
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

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_connectSize.setText("" + bluemanage.getConnectSize());
                        View viewWithTag = mLinearLayout.findViewWithTag("temp");
                        if (viewWithTag == null) {
                            viewWithTag = new BlueItemView(MainActivity.this);
                            mLinearLayout.addView(viewWithTag);
                        }
                        viewWithTag.setTag(mac);
                        BlueItemView mBlueItemView = (BlueItemView) viewWithTag;
                        mBlueItemView.setMac(mac, connectType, blueConnectDevice.getName());
                    }
                });

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
//                Log.e(mac, brainWave.toString() );
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (brainWave.att < seekBarAtt.getProgress()) {
                            tts.speak(String.valueOf(brainWave.att), TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                        if (brainWave.med < seekBarMed.getProgress()) {
                            tts.speak(String.valueOf(brainWave.med), TextToSpeech.QUEUE_FLUSH, null, null);
                        }

                        BlueItemView viewWithTag = mLinearLayout.findViewWithTag(mac);
                        if (viewWithTag != null) {
                            viewWithTag.addData(brainWave, checkbox1.isChecked(), checkbox2.isChecked());
                        }
                    }
                });

            }

            @Override
            public void onRawData(String mac, int raw) {

            }

            @Override
            public void onGravity(String mac, Gravity gravity) {
                BlueItemView viewWithTag = mLinearLayout.findViewWithTag(mac);
                if (viewWithTag != null) {
                    viewWithTag.setGravity(gravity);
                }
            }


            @Override
            public void onRR(String mac, ArrayList<Integer> rr, int oxygen) {

            }

        };

        bluemanage = LinkManager.init(this);
        bluemanage.setMultiEEGPowerDataListener(eegPowerDataListener);
        bluemanage.setOnConnectListener(onConnectListener);
        bluemanage.setDebug(true);

    }


    void initChart() {
        checkbox1 = findViewById(R.id.checkbox1);
        checkbox2 = findViewById(R.id.checkbox2);
        mLinearLayout = findViewById(R.id.mLinearLayout);
        connectTypeGroup = findViewById(R.id.connectTypeGroup);
        mSwitch = findViewById(R.id.mSwitch);

        ed_whiteList = findViewById(R.id.ed_whiteList);
        tv_connectSize = findViewById(R.id.tv_connectSize);
        spinner = findViewById(R.id.spinner);
        all = findViewById(R.id.all);
        only3 = findViewById(R.id.only3);
        only4 = findViewById(R.id.only4);
        spinner.setSelection(0);


        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    startScan();
                } else {
                    mLinearLayout.removeAllViews();
                    bluemanage.close();
                    tv_connectSize.setText("" + bluemanage.getConnectSize());

                    setCanTouch(spinner, true);
                    setCanTouch(ed_whiteList, true);
                    setCanTouch(all, true);
                    setCanTouch(only3, true);
                    setCanTouch(only4, true);
                }


            }
        });


        connectTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

            }
        });


    }

    void startScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // Request the BLUETOOTH_CONNECT and BLUETOOTH_SCAN permissions if they are not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, RC_BT);
            return;
        }
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter == null) {
            return;
        }
        if (!defaultAdapter.isEnabled()) {
            mSwitch.setChecked(false);
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // Check if the required permissions are granted

            startActivityForResult(intent, RC_BT);
            return;
        } else {
            mSwitch.setChecked(true);
            setCanTouch(spinner, false);
            setCanTouch(ed_whiteList, false);
            setCanTouch(all, false);
            setCanTouch(only3, false);
            setCanTouch(only4, false);
        }
        String selectedItem = (String) spinner.getSelectedItem();
        bluemanage.setMaxConnectSize(Integer.parseInt(selectedItem));
        bluemanage.setWhiteList(ed_whiteList.getText().toString());
        bluemanage.startScan();
    }


    public void setCanTouch(View view, boolean canTouch) {
//        view.setFocusable(canTouch);
//        view.setFocusableInTouchMode(canTouch);
        view.setClickable(canTouch);
        view.setEnabled(canTouch);
        view.setAlpha(canTouch ? 1f : 0.5f);
    }

    public final boolean isOPenGps(final Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluemanage != null) {
            bluemanage.onDestroy();
        }

    }

}
