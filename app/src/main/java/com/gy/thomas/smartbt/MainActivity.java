package com.gy.thomas.smartbt;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyachi.stepview.bean.StepBean;
import com.gy.thomas.smartbt.ble.Code;
import com.gy.thomas.smartbt.ble.originV2.BluetoothLeInitialization;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mehdi.sakout.fancybuttons.FancyButton;

import static com.inuker.bluetooth.library.Code.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_DEVICE_DISCONNECTED;


public class MainActivity extends AppCompatActivity {

    private static final UUID UUID_SERVICE_CHANNEL
            = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_CHANNEL_NOTIFY
            = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_CHANNEL_WRITE
            = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");
    //读取指令
    private static final byte[] READ = {0x05, (byte) 0xA9, 0x00, 0x00, 0x00, 0x0d};
    //去皮指令
    private static final byte[] RESET = {0x05, (byte) 0xA6, 0x00, 0x00, 0x00, 0x0d};
    //按钮文本
    private static final String BTKEY = "btkey";
    //提示文本
    private static final String TIPKEY = "tipkey";
    //指令
    private static final String COMKEY = "comkey";
    //回调
    private static final String CALLKEY = "callkey";

    private com.inuker.bluetooth.library.BluetoothClient mBluetoothClient;
    private String mac;
    private TextView tvWeight;
    private TextView tvTip;
    private TextView tvM1;
    private TextView tvM2;
    private TextView tvDensity;
    private TextView tvMaterial;
    private HorizontalStepView hsview;
    private FancyButton btNext;
    private FancyButton btReset;
    private List<Map<String, Object>> steps = new ArrayList<>();
    private List<StepBean> stepsBeanList = new ArrayList<>();
    private int curIndex;
    private float m1;
    private float m2;

    private ProgressDialog mProgressDialog;
    private ScheduledThreadPoolExecutor mExecutorService = new ScheduledThreadPoolExecutor(1);
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sp=getSharedPreferences("sp", MODE_PRIVATE);

        long time = sp.getLong("time", 0);
        if(time==0){
            sp.edit().putLong("time",System.currentTimeMillis()).apply();
        }else {
            //30天
            if(30L*24*60*60*1000<System.currentTimeMillis()-time){
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("版本已过有效期,请购买正式版")
                        .setMessage("联系邮箱:1071931588@qq.com")
                        .setCancelable(false)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                              finish();
                            }
                        })
                        .show();
            }
        }
        mac = sp.getString("mac", "");
        if (mac.length() == 0) {
            startActivity(new Intent(this, ScanActivity.class));
            finish();
        }
        setContentView(R.layout.activity_main);
        initView();
        initSteps();
        refreshView();
        mBluetoothClient = BluetoothLeInitialization.getInstance(this);
        mBluetoothClient.openBluetooth();
        connect();
        mExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Log.e("getConnectStatus", mBluetoothClient.getConnectStatus(mac) + "");
                if (mBluetoothClient.getConnectStatus(mac) == STATUS_DEVICE_DISCONNECTED)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "设备已断开连接，请重连", Toast.LENGTH_SHORT).show();
                        }
                    });

            }
        }, 5000, 5000, TimeUnit.MILLISECONDS);
    }

    private void initSteps() {
        Map<String, Object> step0 = new HashMap<>();
        step0.put(BTKEY, "开始");
        step0.put(TIPKEY, "1.请取走所有电子秤上的物品，然后<开始>");
        step0.put(COMKEY, RESET);

        Map<String, Object> step1 = new HashMap<>();
        step1.put(BTKEY, "确定");
        step1.put(TIPKEY, "2.请放上饰物，然后<确定>");
        step1.put(COMKEY, READ);
        step1.put(CALLKEY, new Stepable() {
            @Override
            public void callback(Object o) {
                m1 = (float) o;
                tvM1.setText(Float.toString(m1));
            }
        });
        Map<String, Object> step2 = new HashMap<>();
        step2.put(BTKEY, "确定");
        step2.put(TIPKEY, "3.请放上装有标准高度水的容器，然后<确定>");
        step2.put(COMKEY, RESET);

        Map<String, Object> step3 = new HashMap<>();
        step3.put(BTKEY, "确定");
        step3.put(TIPKEY, "4.勿取走容器，请将饰物放入容器中，静止后<确定>");
        step3.put(COMKEY, READ);
        step3.put(CALLKEY, new Stepable() {
            @Override
            public void callback(Object o) {
                m2 = (float) o;
                tvM2.setText(Float.toString(m2));
            }
        });
        Map<String, Object> step4 = new HashMap<>();
        step4.put(BTKEY, "密度计算");
        step4.put(TIPKEY, "5.按<密度计算>计算饰物密度");

        steps.add(step0);
        steps.add(step1);
        steps.add(step2);
        steps.add(step3);
        steps.add(step4);

        StepBean stepBean0 = new StepBean("开始", -1);
        StepBean stepBean1 = new StepBean("M1", -1);
        StepBean stepBean2 = new StepBean("去皮", -1);
        StepBean stepBean3 = new StepBean("M2", -1);
        StepBean stepBean4 = new StepBean("计算", -1);

        stepsBeanList.add(stepBean0);
        stepsBeanList.add(stepBean1);
        stepsBeanList.add(stepBean2);
        stepsBeanList.add(stepBean3);
        stepsBeanList.add(stepBean4);
        hsview
                .setStepViewTexts(stepsBeanList)//总步骤
                .setTextSize(12)//set textSize
                .setStepsViewIndicatorCompletedLineColor(ContextCompat.getColor(this, R.color.colorPrimary))//设置StepsViewIndicator完成线的颜色
                .setStepsViewIndicatorUnCompletedLineColor(ContextCompat.getColor(this, R.color.colorPrimary))//设置StepsViewIndicator未完成线的颜色
                .setStepViewComplectedTextColor(ContextCompat.getColor(this, R.color.colorPrimary))//设置StepsView text完成线的颜色
                .setStepViewUnComplectedTextColor(ContextCompat.getColor(this, R.color.gray))//设置StepsView text未完成线的颜色
                .setStepsViewIndicatorCompleteIcon(ContextCompat.getDrawable(this, R.drawable.complete))//设置StepsViewIndicator CompleteIcon
                .setStepsViewIndicatorDefaultIcon(ContextCompat.getDrawable(this, R.drawable.defualt))//设置StepsViewIndicator DefaultIcon
                .setStepsViewIndicatorAttentionIcon(ContextCompat.getDrawable(this, com.baoyachi.stepview.R.drawable.attention));//设置StepsViewIndicator AttentionIcon
    }

    private void initView() {
        final String name = getSharedPreferences("sp", MODE_PRIVATE).getString("name", "");
        btNext = findViewById(R.id.bt_next);
        btReset = findViewById(R.id.bt_reset);
        tvTip = findViewById(R.id.tv_tip);
        tvWeight = findViewById(R.id.tv_weight);
        tvM1 = findViewById(R.id.tv_m1);
        tvM2 = findViewById(R.id.tv_m2);
        tvDensity = findViewById(R.id.tv_density);
        tvMaterial = findViewById(R.id.tv_material);
        hsview = findViewById(R.id.step_view);


        btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (curIndex == steps.size() - 1) {
                    curIndex = 0;
                    calculate();
                    refreshView();
                } else {

                    write((byte[]) steps.get(curIndex).get(COMKEY));
                }
            }
        });
        btReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("提示")
                        .setMessage("是否要重置步骤？")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                btNext.setEnabled(true);
                                curIndex = 0;
                                refreshView();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();

            }
        });
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(name);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mBluetoothClient = BluetoothLeInitialization.getInstance(this);
        mBluetoothClient.openBluetooth();
    }

    private void calculate() {
        if (m1 > m2 && m2 > 0) {
            DecimalFormat formatter = new DecimalFormat("0.000");

            String density = formatter.format(m1 / (m1 - m2));
            tvDensity.setText(density);
        } else {
            Toast.makeText(MainActivity.this, "称重数据存在错误，请重新测量", Toast.LENGTH_LONG).show();
        }

    }

    private void connect() {
        curIndex = 0;
        refreshView();
        mProgressDialog.setMessage("正在连接蓝牙设备...");
        mProgressDialog.show();
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();

        mBluetoothClient.connect(mac, options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile data) {
                if (code == REQUEST_SUCCESS) {
                    mProgressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    registerNotify();
                } else {
                    Toast.makeText(MainActivity.this, "蓝牙连接失败，请重试:" + Code.toString(code), Toast.LENGTH_SHORT).show();
                    mProgressDialog.dismiss();
                }
            }
        });

    }

    private void registerNotify() {
        mBluetoothClient.notify(mac, UUID_SERVICE_CHANNEL, UUID_CHARACTERISTIC_CHANNEL_NOTIFY, new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {

                Log.e("onNotify", Arrays.toString(value));
                //防止第一次开机后清零数据异常
                if (Arrays.equals((byte[]) steps.get(curIndex).get(COMKEY), RESET)
                        && (value[3] != 0 || value[4] != 0 || value[5] != 0)) {
                    write(RESET);
                    return;
                }
                mHandler.removeCallbacksAndMessages(null);
                btNext.setEnabled(true);
                btNext.setText((String) steps.get(curIndex).get(BTKEY));

                float weight = parseData(value);
                tvWeight.setText("" + weight);
                mProgressDialog.dismiss();
                Stepable stepable = (Stepable) steps.get(curIndex).get(CALLKEY);
                if (stepable != null)
                    stepable.callback(weight);
                curIndex++;
                refreshView();
            }

            @Override
            public void onResponse(int code) {
                if (code != REQUEST_SUCCESS) {
                    btNext.setEnabled(true);
                    btNext.setText((String) steps.get(curIndex).get(BTKEY));
                    Toast.makeText(MainActivity.this, "蓝牙监听失败，请重连蓝牙:" + Code.toString(code), Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private float parseData(byte[] data) {
        float res = byteArrayToInt(Arrays.copyOfRange(data, 3, data.length - 1)) / 10;
        if (data[1] == 0x10)
            res = 0 - res;
        return res;
    }

    //byte 数组与 int 的相互转换
    public static float byteArrayToInt(byte[] b) {
        return b[2] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[0] & 0xFF) << 16;
    }


    private void write(byte[] value) {

        btNext.setEnabled(false);
        btNext.setText("通讯中");
        mBluetoothClient.write(mac, UUID_SERVICE_CHANNEL, UUID_CHARACTERISTIC_CHANNEL_WRITE, value, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                if (code != REQUEST_SUCCESS) {
                    Toast.makeText(MainActivity.this, "蓝牙连接失败，请重新连接:" + Code.toString(code), Toast.LENGTH_LONG).show();
                    btNext.setEnabled(true);
                    btNext.setText((String) steps.get(curIndex).get(BTKEY));
                } else {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            curIndex = 0;
                            btNext.setEnabled(true);
                            btNext.setText((String) steps.get(curIndex).get(BTKEY));
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("提示")
                                    .setMessage("通讯异常，请稍后再试")
                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .show();

                        }
                    }, 10000);
                }

            }
        });
    }

    private void refreshView() {
        if (curIndex == 1) {
            tvM1.setText("待测量");
            tvM2.setText("待测量");
            tvDensity.setText("待计算");
            tvMaterial.setText("待计算");
        }
        for (int i = 0; i < steps.size(); i++) {
            if (i == curIndex)
                stepsBeanList.get(i).setState(0);
            else if (i < curIndex)
                stepsBeanList.get(i).setState(1);
            else
                stepsBeanList.get(i).setState(-1);
        }
        hsview.setStepViewTexts(stepsBeanList);
        hsview.refresh();
        this.tvTip.setText((String) this.steps.get(curIndex).get(TIPKEY));
        this.btNext.setText((String) this.steps.get(curIndex).get(BTKEY));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.retry:
                mBluetoothClient.disconnect(mac);
                connect();
                break;
            case R.id.search:
                startActivity(new Intent(MainActivity.this, ScanActivity.class));
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("是否退出应用？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
//
//    @Override
//    protected void attachBaseContext(Context newBase) {
//        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        mExecutorService.shutdownNow();
        mBluetoothClient.disconnect(mac);
    }

    interface Stepable {
        void callback(Object o);
    }
}
