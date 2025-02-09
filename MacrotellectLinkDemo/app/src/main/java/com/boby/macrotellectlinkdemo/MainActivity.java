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

import com.boby.bluetoothconnect.bean.BrainWave;
import com.boby.bluetoothconnect.bean.Gravity;
import com.boby.macrotellectlinkdemo.service.BluetoothService;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int RC_GPS = 4483;
    private static final int RC_BT = 4484;
    private static final int PER_LOC = 4484;
    public static MainActivity instance;
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
    private SeekBar seekBarAtt;
    private TextView seekBarAttValue;
    private SeekBar seekBarMed;
    private TextView seekBarMedValue;
    private Intent bluetoothServiceIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);
        initChart();
        checkAndRequestPermissions();
        initSeekBar();
    }


    private void initSeekBar() {
        seekBarAtt = findViewById(R.id.seekBar_att);
        seekBarAttValue = findViewById(R.id.seekBar_att_value);
        seekBarMed = findViewById(R.id.seekBar_med);
        seekBarMedValue = findViewById(R.id.seekBar_med_value);

        seekBarAtt.setOnSeekBarChangeListener(createSeekBarChangeListener(seekBarAttValue));
        seekBarMed.setOnSeekBarChangeListener(createSeekBarChangeListener(seekBarMedValue));
    }

    private SeekBar.OnSeekBarChangeListener createSeekBarChangeListener(final TextView valueTextView) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueTextView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do something
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do something
            }
        };
    }

    public void handleBrainWaveData(String mac, BrainWave brainWave, TextToSpeech tts) {
        runOnUiThread(() -> {
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
        });


    }

    public void handleConnectionLost(String mac, int connectSize) {
        runOnUiThread(() -> {
            tv_connectSize.setText("" + connectSize);
            BlueItemView viewWithTag = mLinearLayout.findViewWithTag(mac);
            if (viewWithTag != null) {
                viewWithTag.setMac("", "", "");
                viewWithTag.setTag("temp");
            }
        });

    }

    public void updateUIOnConnectSuccess(String mac, String connectType, String deviceName, int connectSize) {
        runOnUiThread(() -> {
            tv_connectSize.setText("" + connectSize);
            View viewWithTag = mLinearLayout.findViewWithTag("temp");
            if (viewWithTag == null) {
                viewWithTag = new BlueItemView(MainActivity.this);
                mLinearLayout.addView(viewWithTag);
            }
            viewWithTag.setTag(mac);
            BlueItemView mBlueItemView = (BlueItemView) viewWithTag;
            mBlueItemView.setMac(mac, connectType, deviceName);
        });
    }

    public void updateGravity(String mac, Gravity gravity) {
        runOnUiThread(() -> {
            BlueItemView viewWithTag = mLinearLayout.findViewWithTag(mac);
            if (viewWithTag != null) {
                viewWithTag.setGravity(gravity);
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
            checkAndRequestPermissions();
        } else if (requestCode == RC_BT && resultCode == RESULT_OK) {
            startScan();
        }
    }


    public void checkAndRequestPermissions() {
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

        }


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
                    stopService(bluetoothServiceIntent);
                    //tv_connectSize.setText("" + bluemanage.getConnectSize());

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
        bluetoothServiceIntent = new Intent(this, BluetoothService.class);
        bluetoothServiceIntent.putExtra("maxConnectSize", Integer.parseInt(selectedItem));
        bluetoothServiceIntent.putExtra("whiteList", ed_whiteList.getText().toString());
        startService(bluetoothServiceIntent);
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

    }

}
